package com.example.notes_app.Activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.example.notes_app.Models.Task;
import com.example.notes_app.R;
import com.example.notes_app.SessionManager;
import com.example.notes_app.adapter.CompletedTaskAdapter;
import com.example.notes_app.adapter.TaskAdapter;
import com.example.notes_app.bottomSheetFragment.CreateTaskBottomSheetFragment;
import com.example.notes_app.bottomSheetFragment.ShowCalendarViewBottomSheet;
import com.example.notes_app.dao.TaskDao;
import com.example.notes_app.database.TaskDatabase;
import com.example.notes_app.databinding.ActivityTasksFragmentBinding;
import com.example.notes_app.entities.TaskEntity;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class TasksFragment extends Fragment implements CreateTaskBottomSheetFragment.SetRefreshListener {

    private ActivityTasksFragmentBinding binding;
    private TaskAdapter pendingTaskAdapter;
    private ImageView noDataImage;
    private CompletedTaskAdapter completedTaskAdapter;
    private List<Task> pendingTasks = new ArrayList<>();
    private List<Task> completedTasks = new ArrayList<>();
    private boolean isCompletedTasksVisible = false;
    private TaskDatabase taskDatabase;
    private TaskDao taskDao;
    private static final int RC_NOTIFICATION = 123;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = ActivityTasksFragmentBinding.inflate(inflater, container, false);

        taskDatabase = TaskDatabase.getInstance(getContext());
        taskDao = taskDatabase.taskDao();

        View view = binding.getRoot();
        noDataImage = view.findViewById(R.id.noDataImage);
        ImageView menuIcon = view.findViewById(R.id.menuIcon);

        setUpViews();
        setUpAdapters();

        binding.calendar.setOnClickListener(view1 -> {
            ShowCalendarViewBottomSheet showCalendarViewBottomSheet = new ShowCalendarViewBottomSheet();
            showCalendarViewBottomSheet.show(getParentFragmentManager(), showCalendarViewBottomSheet.getTag());
        });

        binding.buttonToggleCompleted.setOnClickListener(v -> toggleCompletedTasks());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.POST_NOTIFICATIONS}, RC_NOTIFICATION);
            }
        }

        FloatingActionButton optionOne = binding.optionOne;
        FloatingActionButton optionTwo = binding.optionTwo;
        FloatingActionsMenu fabMenu = binding.layoutquickactiontodo;

        optionOne.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), CreatingNoteActivity.class);
            startActivity(intent);
            fabMenu.collapse();
        });

        optionTwo.setOnClickListener(v -> {
            CreateTaskBottomSheetFragment fragment = new CreateTaskBottomSheetFragment();
            fragment.setRefreshListener(this);
            fragment.show(getParentFragmentManager(), "CreateTaskBottomSheet");
            fabMenu.collapse();
        });

        menuIcon.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(getContext(), v);
            MenuInflater inflater1 = requireActivity().getMenuInflater();
            inflater1.inflate(R.menu.task_menu, popupMenu.getMenu());

            popupMenu.show();
            popupMenu.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.refres) {
                    refresh();
                    return true;
                } else if (item.getItemId() == R.id.Delete) {
                    clearCompletedTasks();
                    return true;
                } else {
                    return false;
                }
            });
        });

        fetchAndSyncTasks();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        fetchAndSyncTasks();
    }

    private void setUpViews() {
        Glide.with(this).load(R.drawable.addyourfirsttask).into(binding.noDataImage);
    }

    private void setUpAdapters() {
        pendingTaskAdapter = new TaskAdapter(getContext(), pendingTasks, this, this::onTaskLongClick, this);
        binding.taskRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.taskRecycler.setAdapter(pendingTaskAdapter);

        completedTaskAdapter = new CompletedTaskAdapter(getContext(), completedTasks);
        binding.recyclerViewCompleted.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewCompleted.setAdapter(completedTaskAdapter);
    }

    private void fetchAndSyncTasks() {
        if (getContext() == null || taskDao == null) return;
        String userId = getCurrentUserId();

        Executors.newSingleThreadExecutor().execute(() -> {
            List<TaskEntity> pendingEntities = taskDao.getPendingTasksForUser(userId);
            List<TaskEntity> completedEntities = taskDao.getCompletedTasksForUser(userId);

            List<Task> pending = new ArrayList<>();
            for (TaskEntity e : pendingEntities) pending.add(e.toTask());

            List<Task> completed = new ArrayList<>();
            for (TaskEntity e : completedEntities) completed.add(e.toTask());

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    pendingTasks.clear();
                    pendingTasks.addAll(pending);
                    completedTasks.clear();
                    completedTasks.addAll(completed);

                    pendingTaskAdapter.notifyDataSetChanged();
                    completedTaskAdapter.notifyDataSetChanged();
                    updateNoDataImageVisibility();

                    if (completedTasks.isEmpty()) {
                        binding.cardViewCompleted.setVisibility(View.GONE);
                        binding.buttonToggleCompleted.setVisibility(View.GONE);
                    } else {
                        binding.cardViewCompleted.setVisibility(View.VISIBLE);
                        binding.buttonToggleCompleted.setVisibility(View.VISIBLE);
                    }
                });
            }
        });
    }

    private String getCurrentUserId() {
        if (getContext() == null) return "offline_user";
        return SessionManager.getUserId(requireContext());
    }

    private void onTaskLongClick(Task task) {
        CreateTaskBottomSheetFragment bottomSheet = new CreateTaskBottomSheetFragment();
        bottomSheet.setTaskId(task.getTaskId(), true, this);
        bottomSheet.setRefreshListener(this);
        bottomSheet.show(getParentFragmentManager(), bottomSheet.getTag());
    }

    @Override
    public void refresh() {
        fetchAndSyncTasks();
    }

    private void toggleCompletedTasks() {
        boolean hasCompletedTasks = !completedTasks.isEmpty();
        if (hasCompletedTasks) {
            binding.recyclerViewCompleted.setVisibility(isCompletedTasksVisible ? View.GONE : View.VISIBLE);
            binding.buttonToggleCompleted.setText(isCompletedTasksVisible ? "Show Completed Tasks" : "Hide Completed Tasks");
            isCompletedTasksVisible = !isCompletedTasksVisible;
        }
    }

    private void clearCompletedTasks() {
        if (getContext() == null || taskDao == null) return;
        String userId = getCurrentUserId();

        Executors.newSingleThreadExecutor().execute(() -> {
            taskDao.deleteCompletedTasksForUser(userId);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    completedTasks.clear();
                    completedTaskAdapter.notifyDataSetChanged();
                    binding.cardViewCompleted.setVisibility(View.GONE);
                    binding.buttonToggleCompleted.setVisibility(View.GONE);
                    updateNoDataImageVisibility();
                    Toast.makeText(getContext(), "Completed tasks cleared", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    public void onTaskCompleted(Task task) {
        pendingTasks.remove(task);
        completedTasks.add(task);

        Executors.newSingleThreadExecutor().execute(() -> {
            taskDao.updateTaskCompletion(task.getTaskId(), true);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    pendingTaskAdapter.notifyDataSetChanged();
                    completedTaskAdapter.notifyDataSetChanged();
                    updateNoDataImageVisibility();
                    updateCompletedTaskVisibility();
                });
            }
        });
    }

    private void updateCompletedTaskVisibility() {
        if (binding == null) return;
        if (completedTasks.isEmpty()) {
            binding.cardViewCompleted.setVisibility(View.GONE);
            binding.buttonToggleCompleted.setVisibility(View.GONE);
        } else {
            binding.cardViewCompleted.setVisibility(View.VISIBLE);
            binding.buttonToggleCompleted.setVisibility(View.VISIBLE);
        }
    }

    public void updateNoDataImageVisibility() {
        if (noDataImage == null) return;
        if (pendingTasks.isEmpty()) {
            noDataImage.setVisibility(View.VISIBLE);
        } else {
            noDataImage.setVisibility(View.GONE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RC_NOTIFICATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getContext(), "Notification permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Notification permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
