package com.netfieldpro.ui.projects;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.netfieldpro.NetFieldApp;
import com.netfieldpro.R;
import com.netfieldpro.core.entitlement.QuotaDecision;
import com.netfieldpro.core.entitlement.Resource;
import com.netfieldpro.core.model.entity.Floor;
import com.netfieldpro.core.model.entity.Project;
import com.netfieldpro.core.model.entity.SurveySession;
import com.netfieldpro.databinding.FragmentProjectDetailBinding;
import com.netfieldpro.databinding.ItemFloorCardBinding;

public class ProjectDetailFragment extends Fragment {

    private FragmentProjectDetailBinding binding;
    private FloorAdapter adapter;
    private String projectId;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentProjectDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            projectId = getArguments().getString("projectId");
        }

        binding.recyclerFloors.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new FloorAdapter();
        binding.recyclerFloors.setAdapter(adapter);

        binding.btnAddFloor.setOnClickListener(v -> onAddFloorClicked());

        loadProjectAndFloors();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadProjectAndFloors();
    }

    public void loadProjectAndFloors() {
        if (binding == null || projectId == null) {
            return;
        }

        Project project = NetFieldApp.projectRepository().getProject(projectId);
        if (project != null) {
            if (project.site() != null && !project.site().isEmpty()) {
                binding.textProjectHeader.setText(project.name() + " · " + project.site());
            } else {
                binding.textProjectHeader.setText(project.name());
            }
        }

        List<Floor> floors = NetFieldApp.floorRepository().getFloorsForProject(projectId);
        if (floors.isEmpty()) {
            binding.layoutEmpty.setVisibility(View.VISIBLE);
            binding.recyclerFloors.setVisibility(View.GONE);
        } else {
            binding.layoutEmpty.setVisibility(View.GONE);
            binding.recyclerFloors.setVisibility(View.VISIBLE);
            adapter.setFloors(floors);
        }
    }

    private void onAddFloorClicked() {
        QuotaDecision decision = NetFieldApp.quotaGuard().check(Resource.FLOOR);
        if (!decision.allowed()) {
            Bundle args = new Bundle();
            args.putString("quota", Resource.FLOOR.name());
            NavController navController = Navigation.findNavController(requireView());
            try {
                navController.navigate(R.id.action_global_paywall, args);
            } catch (Exception e) {
                navController.navigate(R.id.nav_s18, args);
            }
        } else {
            Bundle args = new Bundle();
            args.putString("projectId", projectId);
            Navigation.findNavController(requireView()).navigate(R.id.action_s03_to_s04, args);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public class FloorAdapter extends RecyclerView.Adapter<FloorAdapter.FloorViewHolder> {

        private final List<Floor> floors = new ArrayList<>();

        public void setFloors(List<Floor> items) {
            floors.clear();
            if (items != null) {
                floors.addAll(items);
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public FloorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemFloorCardBinding itemBinding = ItemFloorCardBinding.inflate(
                    LayoutInflater.from(parent.getContext()),
                    parent,
                    false
            );
            return new FloorViewHolder(itemBinding);
        }

        @Override
        public void onBindViewHolder(@NonNull FloorViewHolder holder, int position) {
            holder.bind(floors.get(position));
        }

        @Override
        public int getItemCount() {
            return floors.size();
        }

        class FloorViewHolder extends RecyclerView.ViewHolder {

            private final ItemFloorCardBinding itemBinding;

            FloorViewHolder(ItemFloorCardBinding itemBinding) {
                super(itemBinding.getRoot());
                this.itemBinding = itemBinding;
            }

            void bind(Floor floor) {
                itemBinding.textFloorLevel.setText(
                        getString(R.string.floor_level_label, floor.levelIndex())
                );

                boolean hasScale = floor.scaleUncertaintyPct() > 0.0;
                if (hasScale) {
                    String template = getString(R.string.floor_scale_set);
                    String formattedUncertainty = String.format(Locale.US, "%.1f", floor.scaleUncertaintyPct());
                    itemBinding.chipScale.setText(template.replace("%1$s", formattedUncertainty));
                    itemBinding.btnStartSurvey.setVisibility(View.VISIBLE);
                    itemBinding.btnStartSurvey.setOnClickListener(v -> {
                        Bundle args = new Bundle();
                        args.putString("floorId", floor.id());
                        Navigation.findNavController(v).navigate(R.id.action_s03_to_s06, args);
                    });
                } else {
                    itemBinding.chipScale.setText(R.string.floor_scale_missing);
                    itemBinding.btnStartSurvey.setVisibility(View.GONE);
                }

                itemBinding.btnSetScale.setOnClickListener(v -> {
                    Bundle args = new Bundle();
                    args.putString("floorId", floor.id());
                    Navigation.findNavController(v).navigate(R.id.nav_s05, args);
                });

                itemBinding.btnFloorSurveys.setOnClickListener(v -> showSurveysDialog(floor));
            }

            private void showSurveysDialog(Floor floor) {
                List<SurveySession> sessions = NetFieldApp.surveyReadRepository().getSessionsForFloor(floor.id());
                if (sessions.isEmpty()) {
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle(R.string.surveys_title)
                            .setMessage(R.string.surveys_empty)
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                    return;
                }

                CharSequence[] items = new CharSequence[sessions.size()];
                for (int i = 0; i < sessions.size(); i++) {
                    SurveySession s = sessions.get(i);
                    int nPins = NetFieldApp.surveyReadRepository().countPointsForSession(s.id());
                    String dateStr = Instant.ofEpochMilli(s.createdAtMs())
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                            .toString();
                    items[i] = s.name() + " · " + dateStr + " · " + nPins + " pins";
                }

                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.surveys_title)
                        .setItems(items, (dialog, which) -> {
                            SurveySession chosen = sessions.get(which);
                            Bundle args = new Bundle();
                            args.putString("sessionId", chosen.id());
                            Navigation.findNavController(requireView()).navigate(R.id.action_s03_to_s09, args);
                        })
                        .setNegativeButton(R.string.dialog_cancel, null)
                        .show();
            }
        }
    }
}
