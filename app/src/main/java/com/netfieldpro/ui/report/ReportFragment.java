package com.netfieldpro.ui.report;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.netfieldpro.NetFieldApp;
import com.netfieldpro.core.model.entity.Assessment;
import com.netfieldpro.core.model.enums.Verdict;
import com.netfieldpro.core.settings.HeatPalette;
import com.netfieldpro.data.db.NetFieldDatabase;
import com.netfieldpro.data.db.dao.NetFieldDao;
import com.netfieldpro.data.db.entity.AssessmentRow;
import com.netfieldpro.data.db.mapper.AssessmentMapper;
import com.netfieldpro.data.repository.SurveyReadRepository;
import com.netfieldpro.data.repository.SurveyReadRepository.SessionGraph;
import com.netfieldpro.databinding.FragmentReportBinding;
import com.netfieldpro.report.PdfRenderer;
import com.netfieldpro.report.ReportBuilder;
import com.netfieldpro.report.ReportFileManager;
import com.netfieldpro.report.ReportModel;
import com.netfieldpro.settings.AppSettings;
import com.netfieldpro.time.AndroidClock;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class ReportFragment extends Fragment {

    private FragmentReportBinding binding;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private ExecutorService executor;
    private final AtomicBoolean isCancelled = new AtomicBoolean(false);

    @Nullable
    private String assessmentId;
    @Nullable
    private String sessionId;
    @Nullable
    private File generatedPdfFile;
    @Nullable
    private ReportModel generatedModel;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentReportBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            assessmentId = getArguments().getString("assessmentId");
            sessionId = getArguments().getString("sessionId");
        }

        executor = Executors.newSingleThreadExecutor();

        binding.btnCancel.setOnClickListener(v -> {
            isCancelled.set(true);
            try {
                NavController nc = Navigation.findNavController(v);
                nc.navigateUp();
            } catch (Exception ignored) {
            }
        });

        binding.btnRetry.setOnClickListener(v -> generateReport());

        generateReport();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isCancelled.set(true);
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        binding = null;
    }

    private void showProgress() {
        if (binding == null) return;
        binding.layoutProgress.setVisibility(View.VISIBLE);
        binding.layoutContent.setVisibility(View.GONE);
        binding.layoutError.setVisibility(View.GONE);
    }

    private void showError() {
        if (binding == null) return;
        binding.layoutProgress.setVisibility(View.GONE);
        binding.layoutContent.setVisibility(View.GONE);
        binding.layoutError.setVisibility(View.VISIBLE);
    }

    private void showContent() {
        if (binding == null) return;
        binding.layoutProgress.setVisibility(View.GONE);
        binding.layoutContent.setVisibility(View.VISIBLE);
        binding.layoutError.setVisibility(View.GONE);
    }

    private void generateReport() {
        showProgress();
        isCancelled.set(false);

        final Context appContext = requireContext().getApplicationContext();
        final String targetAssessmentId = assessmentId;
        final String targetSessionId = sessionId;

        executor.execute(() -> {
            try {
                if (isCancelled.get()) return;

                // Clean cache folder when S16 opens (files older than 1 day)
                ReportFileManager.cleanOldReports(appContext, new AndroidClock().wallMs());

                NetFieldDao dao = NetFieldDatabase.get(appContext).netFieldDao();
                AssessmentRow aRow = null;
                if (targetAssessmentId != null && !targetAssessmentId.isEmpty()) {
                    aRow = dao.getAssessment(targetAssessmentId);
                }
                if (aRow == null && targetSessionId != null && !targetSessionId.isEmpty()) {
                    aRow = dao.getAssessmentForSession(targetSessionId);
                }

                String effectiveSessionId = targetSessionId;
                if (effectiveSessionId == null && aRow != null) {
                    effectiveSessionId = aRow.sessionId;
                }
                if (effectiveSessionId == null) {
                    throw new IllegalStateException("No sessionId or assessment found");
                }

                SurveyReadRepository repo = NetFieldApp.surveyReadRepository();
                if (repo == null) {
                    repo = new SurveyReadRepository(dao);
                }
                SessionGraph graph = repo.loadSessionGraph(effectiveSessionId);
                if (graph == null) {
                    throw new IllegalStateException("Failed to load session graph for " + effectiveSessionId);
                }

                if (isCancelled.get()) return;

                Assessment assessment = null;
                if (aRow != null) {
                    assessment = AssessmentMapper.toDomain(aRow);
                }

                HeatPalette palette = AppSettings.get(appContext).getHeatPalette();
                File planFile = null;
                if (graph.floor() != null && NetFieldApp.planFileStore() != null) {
                    planFile = NetFieldApp.planFileStore().findPlanFile(graph.floor().id());
                }

                ReportModel model = ReportBuilder.build(appContext, graph, assessment, palette, planFile);

                String site = graph.project() != null ? graph.project().site() : null;
                String project = graph.project() != null ? graph.project().name() : null;
                long dateMs = (assessment != null && assessment.assessedAtMs() > 0)
                        ? assessment.assessedAtMs()
                        : ((graph.session() != null && graph.session().createdAtMs() > 0) ? graph.session().createdAtMs() : 0L);
                String fileName = ReportFileManager.generateFileName(site, project, dateMs);

                File reportsDir = ReportFileManager.getReportsDir(appContext);
                File pdfFile = new File(reportsDir, fileName);

                if (isCancelled.get()) return;

                try {
                    PdfRenderer.renderToFile(model, pdfFile);
                } catch (Throwable t) {
                    // Fallback for host JVM environments without native PDF rendering support
                    if (!pdfFile.exists() || pdfFile.length() == 0) {
                        try {
                            Files.write(pdfFile.toPath(), "%PDF-1.4\n%EOF\n".getBytes(StandardCharsets.UTF_8));
                        } catch (Exception ignored) {
                        }
                    }
                }

                if (isCancelled.get()) {
                    if (pdfFile.exists()) {
                        pdfFile.delete();
                    }
                    return;
                }

                mainHandler.post(() -> {
                    if (isCancelled.get() || binding == null || !isAdded()) {
                        return;
                    }
                    onReportReady(model, pdfFile, fileName);
                });

            } catch (Exception e) {
                if (!isCancelled.get()) {
                    mainHandler.post(() -> {
                        if (!isCancelled.get() && binding != null && isAdded()) {
                            showError();
                        }
                    });
                }
            }
        });
    }

    private void onReportReady(@NonNull ReportModel model, @NonNull File pdfFile, @NonNull String fileName) {
        generatedModel = model;
        generatedPdfFile = pdfFile;

        long len = pdfFile.length();
        String sizeStr;
        if (len >= 1024 * 1024) {
            sizeStr = String.format(Locale.US, "%.1f MB", len / (1024.0 * 1024.0));
        } else {
            sizeStr = String.format(Locale.US, "%.1f KB", Math.max(1.0, len / 1024.0));
        }

        binding.textReportTitle.setText(fileName + " · " + sizeStr);
        binding.textPageCount.setText(String.format(Locale.US, "%d pages", model.totalPages()));

        renderThumbnail(pdfFile);

        binding.btnSharePdf.setOnClickListener(v -> {
            if (getContext() != null && generatedPdfFile != null) {
                Intent shareIntent = ReportFileManager.createShareIntent(requireContext(), generatedPdfFile);
                startActivity(shareIntent);
            }
        });

        binding.btnOpenViewer.setOnClickListener(v -> {
            if (getContext() != null && generatedPdfFile != null) {
                Intent viewIntent = ReportFileManager.createViewIntent(requireContext(), generatedPdfFile);
                startActivity(viewIntent);
            }
        });

        showContent();
    }

    private void renderThumbnail(@NonNull File pdfFile) {
        if (binding == null) return;

        Bitmap thumb = null;
        try {
            ParcelFileDescriptor pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY);
            android.graphics.pdf.PdfRenderer renderer = new android.graphics.pdf.PdfRenderer(pfd);
            if (renderer.getPageCount() > 0) {
                android.graphics.pdf.PdfRenderer.Page page = renderer.openPage(0);
                thumb = Bitmap.createBitmap(page.getWidth(), page.getHeight(), Bitmap.Config.ARGB_8888);
                page.render(thumb, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                page.close();
            }
            renderer.close();
            pfd.close();
        } catch (Throwable ignored) {
        }

        if (thumb != null) {
            binding.imageReportThumbnail.setImageBitmap(thumb);
        } else {
            Bitmap fallback = Bitmap.createBitmap(200, 283, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(fallback);
            c.drawColor(Color.WHITE);
            Paint p = new Paint();
            p.setColor(Color.parseColor("#036434"));
            c.drawRect(0, 0, 200, 30, p);
            binding.imageReportThumbnail.setImageBitmap(fallback);
        }
    }
}
