package com.example.notes_app.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.notes_app.Models.users;

import java.util.List;

@Dao
public interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(users user);

    @Update
    void update(users user);

    @Delete
    void delete(users user);

    @Query("SELECT * FROM users WHERE LOWER(username) = LOWER(:username) LIMIT 1")
    users getUserByUsername(String username);

    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    users getUserByEmail(String email);

    @Query("SELECT * FROM users WHERE uid = :uid LIMIT 1")
    users getUserById(String uid);

    @Query("SELECT * FROM users")
    List<users> getAllUsers();

    @Query("SELECT * FROM users")
    LiveData<List<users>> getAllUsersLive();

    @Query("SELECT * FROM users ORDER BY lastLogin DESC LIMIT :limit")
    List<users> getMostActiveUsers(int limit);

    @Query("DELETE FROM users WHERE uid = :uid")
    void deleteUserById(String uid);

    @Query("SELECT COUNT(*) FROM users")
    int getUserCount();
}
