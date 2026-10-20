package com.example.notes_app.bottomSheetFragment;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.notes_app.Models.Task;
import com.example.notes_app.R;
import com.example.notes_app.SessionManager;
import com.example.notes_app.database.TaskDatabase;
import com.example.notes_app.databinding.FragmentCreateTaskBinding;
import com.example.notes_app.entities.TaskEntity;
import com.example.notes_app.notification.NotificationWorker;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormat;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class CreateTaskBottomSheetFragment extends BottomSheetDialogFragment {

    private FragmentCreateTaskBinding binding;
    private String taskId;
    private boolean isEdit;
    private SetRefreshListener refreshListener;

    @NonNull
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        binding = FragmentCreateTaskBinding.inflate(getLayoutInflater());
        dialog.setContentView(binding.getRoot());

        setupViews();
        if (isEdit) showTaskFromId();

        return dialog;
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupViews() {
        binding.addTask.setOnClickListener(view -> {
            if (validateFields()) {
                if (isEdit) updateTask();
                else createTask();
            }
        });

        binding.taskDate.setOnTouchListener((view, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                showDatePickerDialog();
            }
            return true;
        });

        binding.taskTime.setOnTouchListener((view, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                showTimePickerDialog();
            }
            return true;
        });
    }

    private void showDatePickerDialog() {
        LocalDate currentDate = LocalDate.now();

        DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(), R.style.CustomDatePickerDialog,
                (view, year, monthOfYear, dayOfMonth) -> {
                    LocalDate date = new LocalDate(year, monthOfYear + 1, dayOfMonth);
                    binding.taskDate.setText(date.toString("dd-MM-yyyy"));
                },
                currentDate.getYear(), currentDate.getMonthOfYear() - 1, currentDate.getDayOfMonth());

        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    private void showTimePickerDialog() {
        LocalTime currentTime = LocalTime.now();

        TimePickerDialog timePickerDialog = new TimePickerDialog(requireContext(), R.style.CustomDatePickerDialog,
                (view, hourOfDay, minute) -> {
                    LocalTime time = new LocalTime(hourOfDay, minute);
                    binding.taskTime.setText(time.toString("hh:mm a"));
                },
                currentTime.getHourOfDay(), currentTime.getMinuteOfHour(), false);

        timePickerDialog.show();
    }

    private boolean validateFields() {
        if (binding.addTaskTitle.getText().toString().trim().isEmpty()) {
            showToast("Please enter a title");
            return false;
        } else if (binding.taskDate.getText().toString().trim().isEmpty()) {
            showToast("Please enter a date");
            return false;
        } else if (binding.taskTime.getText().toString().trim().isEmpty()) {
            showToast("Please enter time");
            return false;
        } else {
            return true;
        }
    }

    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void createTask() {
        String taskTitle = binding.addTaskTitle.getText().toString().trim();
        LocalDate date;
        LocalTime time;

        try {
            date = LocalDate.parse(binding.taskDate.getText().toString(), DateTimeFormat.forPattern("dd-MM-yyyy"));
            time = LocalTime.parse(binding.taskTime.getText().toString(), DateTimeFormat.forPattern("hh:mm a"));
        } catch (IllegalArgumentException e) {
            showToast("Invalid date or time format.");
            return;
        }

        long deadlineInMillis = date.toDateTime(time).getMillis();
        long notificationTimeInMillis = deadlineInMillis;
        long currentTimeMillis = System.currentTimeMillis();

        if (deadlineInMillis <= currentTimeMillis) {
            showToast("The deadline must be in the future.");
            return;
        }

        String newTaskId = UUID.randomUUID().toString();
        String userId = SessionManager.getUserId(requireContext());

        TaskEntity newTask = new TaskEntity(
                newTaskId,
                taskTitle,
                binding.addTaskDescription.getText().toString().trim(),
                date.toString("dd-MM-yyyy"),
                time.toString("HH:mm"),
                "UPCOMING",
                binding.taskEvent.getText().toString().trim(),
                binding.taskEvent.getText().toString().trim(),
                false,
                false,
                userId,
                deadlineInMillis
        );

        Executors.newSingleThreadExecutor().execute(() -> {
            TaskDatabase.getInstance(requireContext()).taskDao().insertOrUpdate(newTask);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (refreshListener != null) {
                        refreshListener.refresh();
                    }
                    scheduleNotification(taskTitle, notificationTimeInMillis);
                    dismiss();
                });
            }
        });
    }

    private void updateTask() {
        String taskTitle = binding.addTaskTitle.getText().toString().trim();
        LocalDate date;
        LocalTime time;

        try {
            date = LocalDate.parse(binding.taskDate.getText().toString(), DateTimeFormat.forPattern("dd-MM-yyyy"));
            time = LocalTime.parse(binding.taskTime.getText().toString(), DateTimeFormat.forPattern("hh:mm a"));
        } catch (IllegalArgumentException e) {
            showToast("Invalid date or time format.");
            return;
        }

        long deadlineInMillis = date.toDateTime(time).getMillis();
        long notificationTimeInMillis = deadlineInMillis;
        long currentTimeMillis = System.currentTimeMillis();

        if (deadlineInMillis <= currentTimeMillis) {
            showToast("The deadline must be in the future.");
            return;
        }

        String userId = SessionManager.getUserId(requireContext());

        TaskEntity updatedTask = new TaskEntity(
                taskId,
                taskTitle,
                binding.addTaskDescription.getText().toString().trim(),
                date.toString("dd-MM-yyyy"),
                time.toString("HH:mm"),
                "UPCOMING",
                binding.taskEvent.getText().toString().trim(),
                binding.taskEvent.getText().toString().trim(),
                false,
                false,
                userId,
                deadlineInMillis
        );

        Executors.newSingleThreadExecutor().execute(() -> {
            TaskDatabase.getInstance(requireContext()).taskDao().insertOrUpdate(updatedTask);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (refreshListener != null) {
                        refreshListener.refresh();
                    }
                    scheduleNotification(taskTitle, notificationTimeInMillis);
                    dismiss();
                });
            }
        });
    }

    private void showTaskFromId() {
        Executors.newSingleThreadExecutor().execute(() -> {
            TaskEntity task = TaskDatabase.getInstance(requireContext()).taskDao().getTaskById(taskId);
            if (task != null && getActivity() != null) {
                getActivity().runOnUiThread(() -> setDataInUI(task.toTask()));
            }
        });
    }

    public void setTaskId(String taskId, boolean isEdit, SetRefreshListener setRefreshListener) {
        this.taskId = taskId;
        this.isEdit = isEdit;
        this.refreshListener = setRefreshListener;
    }

    private void setDataInUI(Task task) {
        if (binding == null) return;
        binding.addTaskTitle.setText(task.getTaskTitle());
        binding.addTaskDescription.setText(task.getTaskDescription());
        binding.taskDate.setText(task.getDate());
        binding.taskTime.setText(task.getLastAlarm());
        binding.taskEvent.setText(task.getEvent());
    }

    private void scheduleNotification(String taskTitle, long deadlineInMillis) {
        long delay = deadlineInMillis - System.currentTimeMillis();

        if (delay <= 0) {
            return;
        }

        Data data = new Data.Builder()
                .putString("taskTitle", taskTitle)
                .putLong("deadline", deadlineInMillis)
                .build();

        OneTimeWorkRequest notificationRequest = new OneTimeWorkRequest.Builder(NotificationWorker.class)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .build();

        WorkManager.getInstance(requireContext()).enqueueUniqueWork(
                "TaskDeadlineNotification_" + taskTitle,
                ExistingWorkPolicy.REPLACE,
                notificationRequest
        );
    }

    public interface SetRefreshListener {
        void refresh();
    }

    public void setRefreshListener(SetRefreshListener listener) {
        this.refreshListener = listener;
    }
}
