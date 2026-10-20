package com.example.notes_app.bottomSheetFragment;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import com.applandeo.materialcalendarview.CalendarView;
import com.applandeo.materialcalendarview.EventDay;
import com.example.notes_app.Models.Task;
import com.example.notes_app.R;
import com.example.notes_app.SessionManager;
import com.example.notes_app.database.TaskDatabase;
import com.example.notes_app.databinding.FragmentCalendarViewBinding;
import com.example.notes_app.entities.TaskEntity;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Executors;

public class ShowCalendarViewBottomSheet extends BottomSheetDialogFragment {

    private FragmentCalendarViewBinding binding;
    private List<Task> tasks = new ArrayList<>();

    private BottomSheetBehavior.BottomSheetCallback mBottomSheetBehaviorCallback = new BottomSheetBehavior.BottomSheetCallback() {

        @Override
        public void onStateChanged(@NonNull View bottomSheet, int newState) {
            if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                dismiss();
            }
        }

        @Override
        public void onSlide(@NonNull View bottomSheet, float slideOffset) {
        }
    };

    @SuppressLint("ResourceType")
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        binding = FragmentCalendarViewBinding.inflate(getLayoutInflater());
        dialog.setContentView(binding.getRoot());

        binding.calendarView.setHeaderColor(ContextCompat.getColor(requireContext(), R.color.colorAccent));
        binding.calendarView.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white));

        getSavedTasks();

        binding.back.setOnClickListener(view -> dialog.dismiss());
        return dialog;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void getSavedTasks() {
        if (getContext() == null) return;
        String currentUserId = SessionManager.getUserId(requireContext());

        Executors.newSingleThreadExecutor().execute(() -> {
            List<TaskEntity> taskEntities = TaskDatabase.getInstance(requireContext())
                    .taskDao().getPendingTasksForUser(currentUserId);
            tasks.clear();
            for (TaskEntity entity : taskEntities) {
                tasks.add(entity.toTask());
            }
            if (getActivity() != null && binding != null) {
                getActivity().runOnUiThread(() -> binding.calendarView.setEvents(getHighlightedDays()));
            }
        });
    }

    public List<EventDay> getHighlightedDays() {
        List<EventDay> events = new ArrayList<>();

        for (Task task : tasks) {
            Calendar calendar = Calendar.getInstance();
            String[] dateParts = task.getDate().split("-");
            if (dateParts.length == 3) {
                int day = Integer.parseInt(dateParts[0]);
                int month = Integer.parseInt(dateParts[1]) - 1;
                int year = Integer.parseInt(dateParts[2]);

                calendar.set(year, month, day);
                events.add(new EventDay(calendar, R.drawable.dot));
            }
        }
        return events;
    }
}
