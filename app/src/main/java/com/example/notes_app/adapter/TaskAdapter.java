package com.example.notes_app.adapter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.notes_app.Activity.TasksFragment;
import com.example.notes_app.Models.Task;
import com.example.notes_app.R;
import com.example.notes_app.bottomSheetFragment.CreateTaskBottomSheetFragment;
import com.example.notes_app.database.TaskDatabase;
import com.example.notes_app.databinding.ItemTaskBinding;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private final Context context;
    private final List<Task> taskList;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("EE dd MMM yyyy", Locale.US);
    private final SimpleDateFormat inputDateFormat = new SimpleDateFormat("dd-M-yyyy", Locale.US);
    private final CreateTaskBottomSheetFragment.SetRefreshListener setRefreshListener;
    private final OnTaskLongClickListener onTaskLongClickListener;
    private final TasksFragment tasksFragment;

    public interface OnTaskLongClickListener {
        void onTaskLongClick(Task task);
    }

    public TaskAdapter(Context context, List<Task> taskList, CreateTaskBottomSheetFragment.SetRefreshListener setRefreshListener,
                       OnTaskLongClickListener onTaskLongClickListener, TasksFragment tasksFragment) {
        this.context = context;
        this.taskList = taskList;
        this.setRefreshListener = setRefreshListener;
        this.onTaskLongClickListener = onTaskLongClickListener;
        this.tasksFragment = tasksFragment;
    }

    public void updateTaskList(List<Task> newTaskList) {
        this.taskList.clear();
        this.taskList.addAll(newTaskList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        ItemTaskBinding binding = ItemTaskBinding.inflate(inflater, viewGroup, false);
        return new TaskViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);
        bindTaskDataToViews(holder, task);

        holder.itemView.setOnLongClickListener(v -> {
            onTaskLongClickListener.onTaskLongClick(task);
            return true;
        });

        holder.binding.options.setOnClickListener(view -> showPopUpMenu(view, position));
    }

    private void bindTaskDataToViews(TaskViewHolder holder, Task task) {
        holder.binding.title.setText(task.getTaskTitle());
        holder.binding.description.setText(task.getTaskDescription());
        holder.binding.time.setText(task.getLastAlarm());
        holder.binding.Status.setText(task.isComplete() ? "COMPLETED" : "UPCOMING");

        try {
            if (task.getDate() != null && !task.getDate().isEmpty()) {
                Date date = inputDateFormat.parse(task.getDate());
                if (date != null) {
                    String outputDateString = dateFormat.format(date);
                    String[] items = outputDateString.split(" ");
                    if (items.length >= 3) {
                        holder.binding.day.setText(items[0]);
                        holder.binding.date.setText(items[1]);
                        holder.binding.month.setText(items[2]);
                    }
                }
            } else {
                clearDateFields(holder);
            }
        } catch (Exception e) {
            clearDateFields(holder);
        }
    }

    private void clearDateFields(TaskViewHolder holder) {
        holder.binding.day.setText("");
        holder.binding.date.setText("");
        holder.binding.month.setText("");
    }

    private void showPopUpMenu(View view, int position) {
        PopupMenu popupMenu = new PopupMenu(context, view);
        popupMenu.getMenuInflater().inflate(R.menu.menu, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.menuDelete) {
                showCustomDialog(taskList.get(position).getTaskId(), position);
                return true;
            } else if (itemId == R.id.menuUpdate) {
                showUpdateTaskBottomSheet(taskList.get(position).getTaskId());
                return true;
            } else if (itemId == R.id.menuComplete) {
                markTaskAsComplete(taskList.get(position));
                return true;
            }
            return false;
        });
        popupMenu.show();
    }

    private void showCustomDialog(String taskId, int position) {
        View customView = LayoutInflater.from(context).inflate(R.layout.custom_dialog, null);
        TextView dialogTitleTask = customView.findViewById(R.id.dialog_message);
        dialogTitleTask.setText(R.string.are_you_sure_you_want_to_delete_this_task);

        AlertDialog dialog = new AlertDialog.Builder(context, R.style.AppTheme_Dialog)
                .setView(customView)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        MaterialButton positiveButton = customView.findViewById(R.id.positive_button);
        MaterialButton negativeButton = customView.findViewById(R.id.negative_button);
        positiveButton.setOnClickListener(v -> {
            deleteTask(taskId, position);
            dialog.dismiss();
        });
        negativeButton.setOnClickListener(v -> dialog.cancel());

        dialog.show();
    }

    private void showCompleteDialog() {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_completed_theme, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AppTheme_Dialog);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        Button completeButton = dialogView.findViewById(R.id.closeButton);
        if (completeButton != null) {
            completeButton.setOnClickListener(v -> {
                tasksFragment.updateNoDataImageVisibility();
                dialog.dismiss();
            });
        }
        dialog.show();
    }

    private void deleteTask(String taskId, int position) {
        Executors.newSingleThreadExecutor().execute(() -> {
            TaskDatabase.getInstance(context).taskDao().deleteTaskById(taskId);
            if (context instanceof Activity) {
                ((Activity) context).runOnUiThread(() -> {
                    if (position >= 0 && position < taskList.size()) {
                        taskList.remove(position);
                        notifyItemRemoved(position);
                        notifyItemRangeChanged(position, taskList.size());
                    }
                    tasksFragment.updateNoDataImageVisibility();
                    Toast.makeText(context, R.string.task_deleted, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void showUpdateTaskBottomSheet(String taskId) {
        CreateTaskBottomSheetFragment bottomSheetFragment = new CreateTaskBottomSheetFragment();
        bottomSheetFragment.setTaskId(taskId, true, setRefreshListener);
        bottomSheetFragment.show(tasksFragment.getParentFragmentManager(), bottomSheetFragment.getTag());
    }

    private void markTaskAsComplete(Task task) {
        Executors.newSingleThreadExecutor().execute(() -> {
            TaskDatabase.getInstance(context).taskDao().updateTaskCompletion(task.getTaskId(), true);
            if (context instanceof Activity) {
                ((Activity) context).runOnUiThread(() -> {
                    task.setComplete(true);
                    showCompleteDialog();
                    tasksFragment.onTaskCompleted(task);
                });
            }
        });
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        private final ItemTaskBinding binding;

        TaskViewHolder(@NonNull ItemTaskBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    public void addTask(Task task) {
        this.taskList.add(task);
        notifyItemInserted(taskList.size() - 1);
    }
}