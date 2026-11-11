package com.netfieldpro.ui.geometry;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.netfieldpro.NetFieldApp;
import com.netfieldpro.R;
import com.netfieldpro.core.geom.PolygonValidator;
import com.netfieldpro.core.geom.Pt;
import com.netfieldpro.core.model.entity.Floor;
import com.netfieldpro.core.model.entity.FloorBoundary;
import com.netfieldpro.core.model.entity.Wall;
import com.netfieldpro.core.model.enums.MaterialId;
import com.netfieldpro.core.model.enums.SessionState;
import com.netfieldpro.core.util.IdGenerator;
import com.netfieldpro.data.repository.SurveyReadRepository;
import com.netfieldpro.databinding.FragmentBoundaryWallsBinding;

public class BoundaryWallsFragment extends Fragment {

    private static final int MODE_BOUNDARY = 0;
    private static final int MODE_WALLS = 1;

    private FragmentBoundaryWallsBinding binding;
    private String floorId;
    private String sessionId;
    private Floor floor;
    private boolean isFinalized = false;
    private double mpp = 0.025;

    private int currentMode = MODE_BOUNDARY;
    private MaterialId currentMaterial = MaterialId.DRYWALL_LIGHT;

    private final List<Pt> boundaryVertices = new ArrayList<>();
    private final List<Wall> savedWalls = new ArrayList<>();
    private final List<Pt> currentWallVertices = new ArrayList<>();
    private final List<Pt> sessionPoints = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentBoundaryWallsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            floorId = getArguments().getString("floorId");
            sessionId = getArguments().getString("sessionId");
        }

        loadData();
        setupModeToggle();
        setupBoundaryControls();
        setupWallControls();
        setupPlanViewTouch();
        setupDoneButton();

        updateOverlay();
        validateBoundaryLive();
    }

    private void loadData() {
        if (floorId == null && sessionId != null) {
            try {
                SurveyReadRepository.SessionGraph graph = NetFieldApp.surveyReadRepository().loadSessionGraph(sessionId);
                if (graph != null && graph.floor() != null) {
                    floorId = graph.floor().id();
                }
            } catch (Exception ignored) {
            }
        }

        if (sessionId != null) {
            try {
                SurveyReadRepository.SessionGraph graph = NetFieldApp.surveyReadRepository().loadSessionGraph(sessionId);
                if (graph != null) {
                    if (graph.session().state() == SessionState.FINALIZED) {
                        isFinalized = true;
                    }
                    if (graph.points() != null) {
                        for (SurveyReadRepository.PointRecord pr : graph.points()) {
                            sessionPoints.add(new Pt(pr.point().x(), pr.point().y()));
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        if (floorId != null) {
            try {
                floor = NetFieldApp.floorRepository().getFloor(floorId);
                if (floor != null) {
                    mpp = floor.metersPerPixel() > 0 ? floor.metersPerPixel() : 0.025;
                    File planFile = NetFieldApp.planFileStore().findPlanFile(floor.id());
                    if (planFile != null && planFile.exists()) {
                        binding.planView.loadPlanFile(planFile);
                    } else if (floor.planWpx() > 0 && floor.planHpx() > 0) {
                        binding.planView.setImageDimensions(floor.planWpx(), floor.planHpx());
                    }
                }

                FloorBoundary fb = NetFieldApp.floorRepository().getBoundary(floorId);
                if (fb != null && fb.vertices() != null && !fb.vertices().isEmpty()) {
                    for (FloorBoundary.Vertex v : fb.vertices()) {
                        boundaryVertices.add(new Pt(v.x(), v.y()));
                    }
                }

                List<Wall> walls = NetFieldApp.floorRepository().getWalls(floorId);
                if (walls != null) {
                    savedWalls.addAll(walls);
                }
            } catch (Exception ignored) {
            }
        }

        if (isFinalized) {
            binding.bannerReadOnly.setVisibility(View.VISIBLE);
            binding.btnDone.setEnabled(false);
            binding.btnFitRectangle.setEnabled(false);
            binding.btnDrawBoundary.setEnabled(false);
            binding.btnClearBoundary.setEnabled(false);
            binding.btnFinishWall.setEnabled(false);
            binding.btnUndoWall.setEnabled(false);
            binding.chipGroupMaterials.setEnabled(false);
        }
    }

    private void setupModeToggle() {
        binding.toggleMode.check(R.id.btn_mode_boundary);
        binding.toggleMode.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.btn_mode_boundary) {
                currentMode = MODE_BOUNDARY;
                binding.layoutBoundaryControls.setVisibility(View.VISIBLE);
                binding.layoutWallsControls.setVisibility(View.GONE);
            } else if (checkedId == R.id.btn_mode_walls) {
                currentMode = MODE_WALLS;
                binding.layoutBoundaryControls.setVisibility(View.GONE);
                binding.layoutWallsControls.setVisibility(View.VISIBLE);
            }
            updateOverlay();
        });
    }

    private void setupBoundaryControls() {
        binding.btnFitRectangle.setOnClickListener(v -> fitRectangle());

        binding.btnDrawBoundary.setOnClickListener(v -> {
            // Draw mode is the default interactive tap mode for Boundary
        });

        binding.btnClearBoundary.setOnClickListener(v -> {
            if (isFinalized) return;
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.dialog_clear_boundary_title)
                    .setMessage(R.string.dialog_clear_boundary_message)
                    .setPositiveButton(R.string.btn_clear_boundary, (dialog, which) -> {
                        boundaryVertices.clear();
                        validateBoundaryLive();
                        updateOverlay();
                    })
                    .setNegativeButton(R.string.btn_cancel, null)
                    .show();
        });
    }

    private void fitRectangle() {
        if (isFinalized) return;

        double extentW = 40.0;
        double extentH = 25.0;
        if (floor != null && floor.planWpx() > 0 && floor.planHpx() > 0 && mpp > 0) {
            extentW = floor.planWpx() * mpp;
            extentH = floor.planHpx() * mpp;
        }

        double minX;
        double minY;
        double maxX;
        double maxY;

        if (sessionPoints.size() < 3) {
            minX = 0.0;
            minY = 0.0;
            maxX = extentW;
            maxY = extentH;
        } else {
            double pMinX = Double.POSITIVE_INFINITY;
            double pMinY = Double.POSITIVE_INFINITY;
            double pMaxX = Double.NEGATIVE_INFINITY;
            double pMaxY = Double.NEGATIVE_INFINITY;
            for (Pt p : sessionPoints) {
                if (p.x() < pMinX) pMinX = p.x();
                if (p.y() < pMinY) pMinY = p.y();
                if (p.x() > pMaxX) pMaxX = p.x();
                if (p.y() > pMaxY) pMaxY = p.y();
            }
            minX = Math.max(0.0, pMinX - 2.0);
            minY = Math.max(0.0, pMinY - 2.0);
            maxX = Math.min(extentW, pMaxX + 2.0);
            maxY = Math.min(extentH, pMaxY + 2.0);
        }

        boundaryVertices.clear();
        boundaryVertices.add(new Pt(minX, minY));
        boundaryVertices.add(new Pt(maxX, minY));
        boundaryVertices.add(new Pt(maxX, maxY));
        boundaryVertices.add(new Pt(minX, maxY));

        validateBoundaryLive();
        updateOverlay();
    }

    private void setupWallControls() {
        binding.chipDrywall.setOnClickListener(v -> currentMaterial = MaterialId.DRYWALL_LIGHT);
        binding.chipGlass.setOnClickListener(v -> currentMaterial = MaterialId.GLASS_PARTITION);
        binding.chipBrick.setOnClickListener(v -> currentMaterial = MaterialId.BRICK_MEDIUM);
        binding.chipConcrete.setOnClickListener(v -> currentMaterial = MaterialId.CONCRETE_HEAVY);
        binding.chipMetal.setOnClickListener(v -> currentMaterial = MaterialId.METAL_ELEVATOR);

        binding.btnFinishWall.setOnClickListener(v -> finishWallChain());
        binding.btnUndoWall.setOnClickListener(v -> undoLast());
    }

    private void finishWallChain() {
        if (isFinalized) return;
        if (savedWalls.size() >= 200) {
            Snackbar.make(binding.getRoot(), R.string.wall_limit_reached, Snackbar.LENGTH_SHORT).show();
            return;
        }

        if (currentWallVertices.size() >= 2) {
            List<Wall.Vertex> wallVerts = new ArrayList<>(currentWallVertices.size());
            for (Pt pt : currentWallVertices) {
                wallVerts.add(new Wall.Vertex(pt.x(), pt.y()));
            }
            String wallId = IdGenerator.generate();
            String fId = floorId != null ? floorId : "default_floor";
            Wall wall = new Wall(wallId, fId, currentMaterial, wallVerts);
            savedWalls.add(wall);
            currentWallVertices.clear();
            binding.btnFinishWall.setEnabled(false);
            updateOverlay();
        }
    }

    private void undoLast() {
        if (isFinalized) return;
        if (!currentWallVertices.isEmpty()) {
            currentWallVertices.remove(currentWallVertices.size() - 1);
            binding.btnFinishWall.setEnabled(currentWallVertices.size() >= 2);
            updateOverlay();
        } else if (!savedWalls.isEmpty()) {
            savedWalls.remove(savedWalls.size() - 1);
            updateOverlay();
        }
    }

    private void setupPlanViewTouch() {
        binding.planView.setPlanTapListener((imgX, imgY) -> {
            if (isFinalized) return;
            double meterX = imgX * mpp;
            double meterY = imgY * mpp;

            if (currentMode == MODE_BOUNDARY) {
                boundaryVertices.add(new Pt(meterX, meterY));
                validateBoundaryLive();
                updateOverlay();
            } else if (currentMode == MODE_WALLS) {
                if (currentWallVertices.isEmpty()) {
                    Wall hitWall = findWallNear(meterX, meterY, 1.0);
                    if (hitWall != null) {
                        confirmDeleteWall(hitWall);
                        return;
                    }
                }

                if (savedWalls.size() >= 200) {
                    Snackbar.make(binding.getRoot(), R.string.wall_limit_reached, Snackbar.LENGTH_SHORT).show();
                    return;
                }

                currentWallVertices.add(new Pt(meterX, meterY));
                binding.btnFinishWall.setEnabled(currentWallVertices.size() >= 2);
                updateOverlay();
            }
        });
    }

    private Wall findWallNear(double px, double py, double maxDistM) {
        for (Wall w : savedWalls) {
            List<Wall.Vertex> verts = w.vertices();
            if (verts == null || verts.size() < 2) continue;
            for (int i = 0; i < verts.size() - 1; i++) {
                Wall.Vertex v1 = verts.get(i);
                Wall.Vertex v2 = verts.get(i + 1);
                double d = distanceToSegment(px, py, v1.x(), v1.y(), v2.x(), v2.y());
                if (d <= maxDistM) {
                    return w;
                }
            }
        }
        return null;
    }

    private static double distanceToSegment(double px, double py, double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double l2 = dx * dx + dy * dy;
        if (l2 == 0) return Math.hypot(px - x1, py - y1);
        double t = Math.max(0, Math.min(1, ((px - x1) * dx + (py - y1) * dy) / l2));
        double projX = x1 + t * dx;
        double projY = y1 + t * dy;
        return Math.hypot(px - projX, py - projY);
    }

    private void confirmDeleteWall(Wall wall) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_delete_wall_title)
                .setMessage(R.string.dialog_delete_wall_message)
                .setPositiveButton(R.string.btn_delete, (dialog, which) -> {
                    savedWalls.remove(wall);
                    updateOverlay();
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void validateBoundaryLive() {
        if (boundaryVertices.isEmpty()) {
            binding.textStatus.setText(R.string.status_no_boundary);
            binding.btnDone.setEnabled(false);
            return;
        }

        PolygonValidator.Result res = PolygonValidator.validate(boundaryVertices);
        switch (res) {
            case OK -> {
                long area = Math.round(PolygonValidator.shoelaceArea(boundaryVertices));
                binding.textStatus.setText(getString(R.string.status_boundary_ok, area));
                binding.btnDone.setEnabled(!isFinalized);
            }
            case TOO_FEW -> {
                binding.textStatus.setText(R.string.status_too_few);
                binding.btnDone.setEnabled(false);
            }
            case TOO_SMALL -> {
                binding.textStatus.setText(R.string.status_too_small);
                binding.btnDone.setEnabled(false);
            }
            case SELF_INTERSECT -> {
                binding.textStatus.setText(R.string.status_self_intersect);
                binding.btnDone.setEnabled(false);
            }
        }
    }

    private void updateOverlay() {
        float density = getResources().getDisplayMetrics().density;
        GeometryOverlay overlay = new GeometryOverlay(
                boundaryVertices,
                savedWalls,
                currentWallVertices,
                currentMaterial,
                mpp,
                density
        );
        binding.planView.setOverlayDrawer(overlay);
        binding.planView.invalidate();
    }

    private void setupDoneButton() {
        binding.btnDone.setOnClickListener(v -> {
            if (isFinalized) {
                Navigation.findNavController(v).popBackStack();
                return;
            }

            PolygonValidator.Result res = PolygonValidator.validate(boundaryVertices);
            if (res == PolygonValidator.Result.OK && floorId != null) {
                List<FloorBoundary.Vertex> bVerts = new ArrayList<>(boundaryVertices.size());
                for (Pt pt : boundaryVertices) {
                    bVerts.add(new FloorBoundary.Vertex(pt.x(), pt.y()));
                }
                FloorBoundary fb = new FloorBoundary(floorId, bVerts);
                NetFieldApp.floorRepository().saveBoundary(fb);
                NetFieldApp.floorRepository().saveWalls(floorId, savedWalls);
            }
            Navigation.findNavController(v).popBackStack();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

