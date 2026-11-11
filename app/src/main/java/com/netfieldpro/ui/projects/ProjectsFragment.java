package com.netfieldpro.ui.projects;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.netfieldpro.NetFieldApp;
import com.netfieldpro.R;
import com.netfieldpro.core.entitlement.QuotaDecision;
import com.netfieldpro.core.entitlement.Resource;
import com.netfieldpro.core.model.entity.Floor;
import com.netfieldpro.core.model.entity.Project;
import com.netfieldpro.core.project.NameRules;
import com.netfieldpro.data.db.dao.NetFieldDao;
import com.netfieldpro.data.db.entity.FloorRow;
import com.netfieldpro.data.repository.ProjectRepository;
import com.netfieldpro.databinding.FragmentProjectsBinding;
import com.netfieldpro.databinding.ItemProjectCardBinding;
import com.netfieldpro.ui.NavArgs;

public class ProjectsFragment extends Fragment {

    private FragmentProjectsBinding binding;
    private ProjectsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProjectsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sweepOrphans();

        binding.recyclerProjects.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ProjectsAdapter();
        binding.recyclerProjects.setAdapter(adapter);

        binding.btnNewProject.setOnClickListener(v -> onNewProjectClicked());

        loadProjects();
    }

    @Override
    public void onStart() {
        super.onStart();
        com.netfieldpro.session.SessionRecovery.run(requireContext(), rebootEnded -> {
            if (rebootEnded && isAdded() && binding != null) {
                com.google.android.material.snackbar.Snackbar.make(
                        binding.getRoot(),
                        R.string.session_ended_reboot,
                        com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                ).show();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadProjects();
    }

    private void sweepOrphans() {
        new Thread(() -> {
            try {
                List<Project> projects = NetFieldApp.projectRepository().getProjects();
                Set<String> floorIds = new HashSet<>();
                for (Project p : projects) {
                    List<Floor> floors = NetFieldApp.floorRepository().getFloorsForProject(p.id());
                    if (floors != null) {
                        for (Floor f : floors) {
                            floorIds.add(f.id());
                        }
                    }
                }
                NetFieldApp.planFileStore().sweepOrphans(floorIds);
            } catch (Exception ignored) {
            }
        }, "nf-orphan-sweeper").start();
    }

    public void loadProjects() {
        if (binding == null) return;

        List<Project> projects = NetFieldApp.projectRepository().getProjects();
        if (projects.isEmpty()) {
            binding.layoutEmpty.setVisibility(View.VISIBLE);
            binding.recyclerProjects.setVisibility(View.GONE);
        } else {
            binding.layoutEmpty.setVisibility(View.GONE);
            binding.recyclerProjects.setVisibility(View.VISIBLE);
            adapter.submitList(projects);
        }
    }

    private void onNewProjectClicked() {
        QuotaDecision decision = NetFieldApp.quotaGuard().check(Resource.PROJECT);
        if (!decision.allowed()) {
            Bundle args = new Bundle();
            args.putString(NavArgs.QUOTA, "PROJECT");
            NavHostFragment.findNavController(this).navigate(R.id.action_s02_to_s18, args);
            return;
        }

        showNewProjectDialog();
    }

    public void showNewProjectDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_new_project, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        TextInputLayout layoutName = dialogView.findViewById(R.id.layout_project_name);
        TextInputEditText editName = dialogView.findViewById(R.id.edit_project_name);
        TextInputEditText editSite = dialogView.findViewById(R.id.edit_project_site);
        TextView textError = dialogView.findViewById(R.id.text_name_error);
        View btnCancel = dialogView.findViewById(R.id.btn_cancel);
        View btnSave = dialogView.findViewById(R.id.btn_save);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
            String name = editName.getText() != null ? editName.getText().toString() : "";
            String site = editSite.getText() != null ? editSite.getText().toString() : null;

            NameRules.Status status = NameRules.validate(name);
            if (status != NameRules.Status.OK) {
                int errRes = switch (status) {
                    case EMPTY -> R.string.err_name_empty;
                    case TOO_LONG -> R.string.err_name_long;
                    case INVALID_CHARS -> R.string.err_name_chars;
                    default -> R.string.err_name_empty;
                };
                layoutName.setError(getString(errRes));
                textError.setText(errRes);
                textError.setVisibility(View.VISIBLE);
                return;
            }

            dialog.dismiss();
            try {
                Project p = NetFieldApp.projectRepository().createProject(name, null, site);
                Bundle args = new Bundle();
                args.putString(NavArgs.PROJECT_ID, p.id());
                NavHostFragment.findNavController(ProjectsFragment.this).navigate(R.id.action_s02_to_s03, args);
            } catch (Exception e) {
                loadProjects();
            }
        });

        dialog.show();
    }

    public void showRenameDialog(Project project) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_rename_project, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        TextInputLayout layoutName = dialogView.findViewById(R.id.layout_project_name);
        TextInputEditText editName = dialogView.findViewById(R.id.edit_project_name);
        TextView textError = dialogView.findViewById(R.id.text_name_error);
        View btnCancel = dialogView.findViewById(R.id.btn_cancel);
        View btnSave = dialogView.findViewById(R.id.btn_save);

        editName.setText(project.name());

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
            String newName = editName.getText() != null ? editName.getText().toString() : "";
            NameRules.Status status = NameRules.validate(newName);
            if (status != NameRules.Status.OK) {
                int errRes = switch (status) {
                    case EMPTY -> R.string.err_name_empty;
                    case TOO_LONG -> R.string.err_name_long;
                    case INVALID_CHARS -> R.string.err_name_chars;
                    default -> R.string.err_name_empty;
                };
                layoutName.setError(getString(errRes));
                textError.setText(errRes);
                textError.setVisibility(View.VISIBLE);
                return;
            }

            dialog.dismiss();
            NetFieldApp.projectRepository().renameProject(project.id(), newName);
            loadProjects();
        });

        dialog.show();
    }

    public void showDeleteConfirmation(Project project) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.project_delete_title)
                .setMessage(R.string.project_delete_body)
                .setPositiveButton(R.string.dialog_delete, (d, which) -> {
                    NetFieldApp.projectRepository().deleteProject(project.id());
                    loadProjects();
                })
                .setNegativeButton(R.string.dialog_cancel, (d, which) -> d.dismiss())
                .show();
    }

    private class ProjectsAdapter extends RecyclerView.Adapter<ProjectViewHolder> {

        private final List<Project> items = new ArrayList<>();

        public void submitList(List<Project> projects) {
            items.clear();
            if (projects != null) {
                items.addAll(projects);
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ProjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemProjectCardBinding itemBinding = ItemProjectCardBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false
            );
            return new ProjectViewHolder(itemBinding);
        }

        @Override
        public void onBindViewHolder(@NonNull ProjectViewHolder holder, int position) {
            holder.bind(items.get(position));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }

    private class ProjectViewHolder extends RecyclerView.ViewHolder {

        private final ItemProjectCardBinding itemBinding;

        public ProjectViewHolder(ItemProjectCardBinding itemBinding) {
            super(itemBinding.getRoot());
            this.itemBinding = itemBinding;
        }

        public void bind(Project project) {
            itemBinding.textProjectName.setText(project.name());

            List<Floor> floors = NetFieldApp.floorRepository().getFloorsForProject(project.id());
            int floorCount = (floors != null) ? floors.size() : 0;

            String floorsCountStr = (floorCount == 1)
                    ? getString(R.string.project_floors_count_one)
                    : getString(R.string.project_floors_count, floorCount);

            String subtitle;
            if (project.site() != null && !project.site().isEmpty()) {
                subtitle = project.site() + " · " + floorsCountStr;
            } else {
                subtitle = floorsCountStr;
            }
            itemBinding.textProjectSubtitle.setText(subtitle);

            itemBinding.cardProject.setOnClickListener(v -> {
                Bundle args = new Bundle();
                args.putString(NavArgs.PROJECT_ID, project.id());
                NavHostFragment.findNavController(ProjectsFragment.this).navigate(R.id.action_s02_to_s03, args);
            });

            itemBinding.btnProjectMenu.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(v.getContext(), v);
                popup.getMenu().add(0, 1, 0, R.string.project_menu_rename);
                popup.getMenu().add(0, 2, 1, R.string.project_menu_delete);
                popup.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == 1) {
                        showRenameDialog(project);
                        return true;
                    } else if (item.getItemId() == 2) {
                        showDeleteConfirmation(project);
                        return true;
                    }
                    return false;
                });
                popup.show();
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
