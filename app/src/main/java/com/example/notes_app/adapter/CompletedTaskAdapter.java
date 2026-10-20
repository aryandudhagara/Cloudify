package com.example.notes_app.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.notes_app.Models.Task;
import com.example.notes_app.R;
import com.example.notes_app.databinding.ItemTaskBinding;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.List;
import java.util.Locale;

public class CompletedTaskAdapter extends RecyclerView.Adapter<CompletedTaskAdapter.TaskViewHolder> {

    private final Context context;
    private final LayoutInflater inflater;
    private final List<Task> taskList;
    private final DateTimeFormatter outputDateFormatter = DateTimeFormat.forPattern("EEE dd MMM yyyy").withLocale(Locale.US);
    private final DateTimeFormatter inputDateFormatter = DateTimeFormat.forPattern("dd-MM-yyyy").withLocale(Locale.US);

    public CompletedTaskAdapter(Context context, List<Task> taskList) {
        this.context = context;
        this.taskList = taskList;
        this.inflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View view = inflater.inflate(R.layout.item_task, viewGroup, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);
        bindTaskDataToViews(holder, task);
    }

    private void bindTaskDataToViews(TaskViewHolder holder, Task task) {
        holder.binding.title.setText(task.getTaskTitle());
        holder.binding.description.setText(task.getTaskDescription());
        holder.binding.time.setText(task.getLastAlarm());
        holder.binding.Status.setText("COMPLETED");

        try {
            if (task.getDate() != null && !task.getDate().isEmpty()) {
                LocalDate date = LocalDate.parse(task.getDate(), inputDateFormatter);
                String outputDateString = date.toString(outputDateFormatter);
                String[] items = outputDateString.split(" ");
                if (items.length >= 3) {
                    holder.binding.day.setText(items[0]);
                    holder.binding.date.setText(items[1]);
                    holder.binding.month.setText(items[2]);
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

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        private final ItemTaskBinding binding;

        TaskViewHolder(@NonNull View view) {
            super(view);
            binding = ItemTaskBinding.bind(view);
        }
    }

    public void addTask(Task task) {
        taskList.add(task);
        notifyItemInserted(taskList.size() - 1);
    }

    public void updateTaskList(List<Task> newTaskList) {
        this.taskList.clear();
        this.taskList.addAll(newTaskList);
        notifyDataSetChanged();
    }

    public void removeTask(Task task) {
        int position = taskList.indexOf(task);
        if (position != -1) {
            taskList.remove(position);
            notifyItemRemoved(position);
        }
    }

    public void updateTask(Task task) {
        int position = taskList.indexOf(task);
        if (position != -1) {
            taskList.set(position, task);
            notifyItemChanged(position);
        }
    }
}
