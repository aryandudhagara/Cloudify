package com.example.notes_app.Models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity(tableName = "users")
public class users implements Serializable {
    @PrimaryKey
    @NonNull
    private String uid;
    private String username;
    private String email;
    private String password;
    private boolean admin;
    private long joinedDate;
    private long lastLogin;
    private String profile_icon;

    // Constructor that initializes all fields
    @Ignore
    public users(@NonNull String uid, String username, String email, boolean admin, long joinedDate, String profile_icon) {
        this.uid = uid;
        this.username = username;
        this.email = email;
        this.admin = admin;
        this.joinedDate = joinedDate;
        this.profile_icon = profile_icon;
        this.lastLogin = System.currentTimeMillis();
    }

    // Default constructor
    public users() {
        this.uid = "";
    }

    // Getter for UID
    @NonNull
    public String getUid() {
        return uid;
    }

    // Setter for UID
    public void setUid(@NonNull String uid) {
        this.uid = uid;
    }

    // Getter for username
    public String getUsername() {
        return username;
    }

    // Setter for username
    public void setUsername(String username) {
        this.username = username;
    }

    // Getter for email
    public String getEmail() {
        return email;
    }

    // Setter for email
    public void setEmail(String email) {
        this.email = email;
    }

    // Getter for password
    public String getPassword() {
        return password;
    }

    // Setter for password
    public void setPassword(String password) {
        this.password = password;
    }

    // Getter for admin status
    public boolean isAdmin() {
        return admin;
    }

    // Setter for admin status
    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    // Getter for joined date
    public long getJoinedDate() {
        return joinedDate;
    }

    // Setter for joined date
    public void setJoinedDate(long joinedDate) {
        this.joinedDate = joinedDate;
    }

    // Getter for last login
    public long getLastLogin() {
        return lastLogin;
    }

    // Setter for last login
    public void setLastLogin(long lastLogin) {
        this.lastLogin = lastLogin;
    }

    // Getter for profile image URL / local path
    public String getProfile_icon() {
        return profile_icon;
    }

    public void setProfile_icon(String profile_icon) {
        this.profile_icon = profile_icon;
    }
}
