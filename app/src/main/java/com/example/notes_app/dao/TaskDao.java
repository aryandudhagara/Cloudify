package com.example.notes_app.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Transaction;

import com.example.notes_app.entities.TaskEntity;

import java.util.List;

@Dao
public interface TaskDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(TaskEntity task);

    @Query("SELECT * FROM tasks WHERE isDeleted = 0")
    LiveData<List<TaskEntity>> getAllTasks();

    @Query("SELECT * FROM tasks WHERE (title LIKE '%' || :searchQuery || '%' OR description LIKE '%' || :searchQuery || '%') AND isDeleted = 0")
    LiveData<List<TaskEntity>> searchTasks(String searchQuery);

    @Update
    void update(TaskEntity task);

    @Query("UPDATE tasks SET isDeleted = 1 WHERE id = :taskId")
    void markAsDeleted(String taskId);

    @Query("DELETE FROM tasks WHERE id = :taskId")
    void deleteTaskById(String taskId);

    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    TaskEntity getTaskById(String taskId);

    @Query("DELETE FROM tasks WHERE userId = :userId")
    void deleteAllTasksForUser(String userId);

    @Query("DELETE FROM tasks WHERE isCompleted = 1")
    void deleteCompletedTasks();

    @Query("DELETE FROM tasks WHERE userId = :userId AND isCompleted = 1")
    void deleteCompletedTasksForUser(String userId);

    @Query("DELETE FROM tasks WHERE id IN (:taskIds)")
    void deleteCompletedTasksByIds(List<String> taskIds);

    @Query("SELECT * FROM tasks WHERE isCompleted = 1 AND isDeleted = 0")
    List<TaskEntity> getCompletedTasks();

    @Query("UPDATE tasks SET isCompleted = :isCompleted WHERE id = :taskId")
    void updateTaskCompletion(String taskId, boolean isCompleted);

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND isDeleted = 0")
    LiveData<List<TaskEntity>> getPendingTasks();

    @Query("SELECT * FROM tasks WHERE userId = :userId AND isCompleted = 0 AND isDeleted = 0")
    List<TaskEntity> getPendingTasksForUser(String userId);

    @Query("SELECT * FROM tasks WHERE userId = :userId AND isCompleted = 1 AND isDeleted = 0")
    List<TaskEntity> getCompletedTasksForUser(String userId);

    @Query("SELECT * FROM tasks WHERE userId = :userId AND isDeleted = 0")
    List<TaskEntity> getAllTasksForUser(String userId);

    @Query("SELECT * FROM tasks WHERE userId = :userId AND isCompleted = 0 AND isDeleted = 0")
    LiveData<List<TaskEntity>> getPendingTasksForUserLive(String userId);

    @Query("SELECT * FROM tasks WHERE userId = :userId AND isCompleted = 1 AND isDeleted = 0")
    LiveData<List<TaskEntity>> getCompletedTasksForUserLive(String userId);

    @Transaction
    default void deleteTasksAndInsertNew(List<TaskEntity> tasksToDelete, TaskEntity taskToInsert) {
        for (TaskEntity task : tasksToDelete) {
            markAsDeleted(task.getId());
        }
        insertOrUpdate(taskToInsert);
    }
}
