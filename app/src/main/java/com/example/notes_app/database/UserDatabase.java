package com.example.notes_app.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.notes_app.dao.UserDao;
import com.example.notes_app.Models.users;

@Database(entities = {users.class}, version = 1, exportSchema = false)
public abstract class UserDatabase extends RoomDatabase {

    private static UserDatabase instance;

    public abstract UserDao userDao();

    public static synchronized UserDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            UserDatabase.class,
                            "user_database"
                    )
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }

    public static void ensureDefaultAdmin(Context context) {
        java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
            try {
                UserDao dao = getInstance(context).userDao();
                users admin = dao.getUserByUsername("admin");
                if (admin == null) {
                    users defaultAdmin = new users(
                            "admin_system_uid",
                            "admin",
                            "admin@cloudify.local",
                            true,
                            System.currentTimeMillis(),
                            null
                    );
                    defaultAdmin.setPassword("admin123");
                    dao.insertOrUpdate(defaultAdmin);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
