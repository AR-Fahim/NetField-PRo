package com.netfieldpro.data.db;

import java.io.Closeable;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class BatchedWriter implements Closeable {

    private final NetFieldDatabase db;
    private final ExecutorService executor;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public BatchedWriter(NetFieldDatabase db) {
        this.db = Objects.requireNonNull(db, "db cannot be null");
        this.executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "nf-db-writer"));
    }

    public void submit(WriteBatch b, WriteCallback cb) {
        Objects.requireNonNull(b, "WriteBatch cannot be null");
        if (closed.get()) {
            throw new IllegalStateException("BatchedWriter is closed");
        }

        executor.submit(() -> {
            try {
                db.runInTransaction(() -> {
                    try {
                        b.run(db);
                    } catch (RuntimeException re) {
                        throw re;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
                if (cb != null) {
                    cb.onSuccess();
                }
            } catch (Throwable t) {
                Throwable cause = (t instanceof RuntimeException && t.getCause() != null) ? t.getCause() : t;
                if (cb != null) {
                    cb.onError(cause);
                }
            }
        });
    }

    public void flush() {
        if (executor.isShutdown()) {
            return;
        }
        Future<?> future = executor.submit(() -> {});
        try {
            future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("flush interrupted", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("flush failed", e);
        }
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            flush();
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                executor.shutdownNow();
            }
        }
    }
}

