package com.example.notes_app.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.example.notes_app.Models.Task;

@Entity(tableName = "tasks")
public class TaskEntity {

    @PrimaryKey
    @NonNull
    private String id;
    private String title;
    private String description;
    private String date;
    private String lastAlarm;
    private String status;
    private String taskEvent;
    private String event;
    private boolean isCompleted;
    private boolean isDeleted;
    private String userId;
    private long dueDate;

    public TaskEntity() {
        this.id = "";
    }

    @Ignore
    public TaskEntity(@NonNull String id, String title, String description, String date,
                      String lastAlarm, String status, String taskEvent, String event,
                      boolean isCompleted, boolean isDeleted, String userId, long dueDate) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.date = date;
        this.lastAlarm = lastAlarm;
        this.status = status;
        this.taskEvent = taskEvent;
        this.event = event;
        this.isCompleted = isCompleted;
        this.isDeleted = isDeleted;
        this.userId = userId;
        this.dueDate = dueDate;
    }

    public Task toTask() {
        return new Task(id, title, date, description, isCompleted, lastAlarm, status, taskEvent, event, userId, dueDate);
    }

    public static TaskEntity fromTask(Task task) {
        if (task == null) return null;
        return new TaskEntity(
                task.getTaskId() != null ? task.getTaskId() : "",
                task.getTaskTitle(),
                task.getTaskDescription(),
                task.getDate(),
                task.getLastAlarm(),
                task.getStatus(),
                task.getTaskEvent(),
                task.getEvent(),
                task.isComplete(),
                false,
                task.getUserId(),
                task.getDeadline() != null ? task.getDeadline() : 0L
        );
    }

    // Getters and Setters

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getLastAlarm() {
        return lastAlarm;
    }

    public void setLastAlarm(String lastAlarm) {
        this.lastAlarm = lastAlarm;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTaskEvent() {
        return taskEvent;
    }

    public void setTaskEvent(String taskEvent) {
        this.taskEvent = taskEvent;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public long getDueDate() {
        return dueDate;
    }

    public void setDueDate(long dueDate) {
        this.dueDate = dueDate;
    }
}
