package com.netfieldpro.latency;

import android.net.Network;
import android.system.Os;
import android.system.OsConstants;
import android.system.StructPollfd;

import com.netfieldpro.core.latency.IcmpPacket;
import com.netfieldpro.core.latency.IcmpProbe;
import com.netfieldpro.core.latency.ProbeResult;
import com.netfieldpro.core.latency.ProbeSequence;
import com.netfieldpro.core.time.Clock;

import java.io.FileDescriptor;
import java.net.InetAddress;
import java.util.function.BooleanSupplier;

public class DgramIcmpProbe implements IcmpProbe {

    private final Network network;
    private final String target;
    private final Clock clock;
    private final BooleanSupplier isAborted;

    public DgramIcmpProbe(Network network, String target, Clock clock, BooleanSupplier isAborted) {
        this.network = network;
        this.target = target;
        this.clock = clock;
        this.isAborted = isAborted;
    }

    @Override
    public ProbeResult probe(int seq, long timeoutNs) {
        if (isAborted != null && isAborted.getAsBoolean()) {
            return ProbeResult.aborted();
        }

        FileDescriptor fd = null;
        try {
            InetAddress targetAddr = InetAddress.getByName(target);
            fd = Os.socket(OsConstants.AF_INET, OsConstants.SOCK_DGRAM, OsConstants.IPPROTO_ICMP);
            if (network != null) {
                network.bindSocket(fd);
            }

            int id = (int) (Thread.currentThread().getId() & 0xFFFF);
            byte[] payload = ProbeSequence.createDefaultPayload();
            byte[] packet = IcmpPacket.echoRequest(id, seq, payload);

            long startMonoNs = clock.monoNs();
            Os.sendto(fd, packet, 0, packet.length, 0, targetAddr, 0);

            long deadlineNs = startMonoNs + timeoutNs;
            byte[] recvBuf = new byte[128];
            StructPollfd[] pfd = new StructPollfd[]{new StructPollfd()};
            pfd[0].fd = fd;
            pfd[0].events = (short) OsConstants.POLLIN;

            while (clock.monoNs() < deadlineNs) {
                if (isAborted != null && isAborted.getAsBoolean()) {
                    return ProbeResult.aborted();
                }
                long remainingNs = deadlineNs - clock.monoNs();
                int sliceMs = (int) Math.min(100L, remainingNs / 1_000_000L);
                if (sliceMs <= 0) {
                    break;
                }
                int pollRes = Os.poll(pfd, sliceMs);
                if (pollRes > 0 && (pfd[0].revents & OsConstants.POLLIN) != 0) {
                    int bytesRead = Os.recvfrom(fd, recvBuf, 0, recvBuf.length, 0, null);
                    if (bytesRead >= 8) {
                        int replySeq = IcmpPacket.parseEchoReply(recvBuf, bytesRead);
                        if (replySeq == seq) {
                            long rttNs = clock.monoNs() - startMonoNs;
                            return ProbeResult.ok(rttNs / 1000L);
                        }
                    }
                }
            }
            return ProbeResult.lost();
        } catch (Throwable t) {
            if (isAborted != null && isAborted.getAsBoolean()) {
                return ProbeResult.aborted();
            }
            return ProbeResult.lost();
        } finally {
            if (fd != null && fd.valid()) {
                try {
                    Os.close(fd);
                } catch (Exception ignored) {
                }
            }
        }
    }
}
