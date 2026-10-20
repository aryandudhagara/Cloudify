package com.example.notes_app.Activity;

import static android.content.ContentValues.TAG;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.notes_app.Models.users;
import com.example.notes_app.R;
import com.example.notes_app.SessionManager;
import com.example.notes_app.database.NoteDatabase;
import com.example.notes_app.database.UserDatabase;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;

public class profile extends AppCompatActivity {

    private Context context;
    private TextView usernameTextView, emailTextView, joinedDateTextView, notesCountTextView;
    private ImageView profilePictureImageView;
    private Button logoutButton, editProfileButton, adminPanelButton;
    private static final int PICK_IMAGE_REQUEST = 1;
    private SharedPref sharedPreferences;
    private Button toggleTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sharedPreferences = new SharedPref(this);

        if (sharedPreferences.loadNightModeState()) {
            setTheme(R.style.Dark);
        } else {
            setTheme(R.style.Light);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        context = this;
        toggleTheme = findViewById(R.id.theme1);
        adminPanelButton = findViewById(R.id.admin_panel);

        if (sharedPreferences.loadNightModeState()) {
            toggleTheme.setText("Switch to Light Mode");
        } else {
            toggleTheme.setText("Switch to Dark Mode");
        }

        toggleTheme.setOnClickListener(v -> {
            boolean isNightMode = sharedPreferences.loadNightModeState();
            if (isNightMode) {
                sharedPreferences.setNightModeState(false);
                toggleTheme.setText("Switch to Dark Mode");
            } else {
                sharedPreferences.setNightModeState(true);
                toggleTheme.setText("Switch to Light Mode");
            }
            Intent intent = new Intent("com.example.notes_app.THEME_CHANGED");
            sendBroadcast(intent);
            recreate();
        });

        usernameTextView = findViewById(R.id.usernameText);
        emailTextView = findViewById(R.id.emailText);
        joinedDateTextView = findViewById(R.id.joinedDateText);
        notesCountTextView = findViewById(R.id.notesCountText);
        profilePictureImageView = findViewById(R.id.profilePicture);
        logoutButton = findViewById(R.id.logoutButton);
        editProfileButton = findViewById(R.id.editProfileButton);

        loadUserProfile();

        logoutButton.setOnClickListener(view -> {
            View customView = LayoutInflater.from(context).inflate(R.layout.custom_dialog, null);
            TextView dialogTitleTask = customView.findViewById(R.id.dialog_message);
            dialogTitleTask.setText(R.string.are_you_sure_you_want_to_Logout);

            AlertDialog dialog = new AlertDialog.Builder(context, R.style.AppTheme_Dialog)
                    .setView(customView)
                    .create();
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }

            MaterialButton positiveButton = customView.findViewById(R.id.positive_button);
            MaterialButton negativeButton = customView.findViewById(R.id.negative_button);
            positiveButton.setOnClickListener(v -> {
                logout();
                dialog.dismiss();
            });
            negativeButton.setOnClickListener(v -> dialog.cancel());

            dialog.show();
        });

        editProfileButton.setOnClickListener(v -> showEditUsernameDialog());
        profilePictureImageView.setOnClickListener(v -> openImagePicker());

        Button resetPasswordButton = findViewById(R.id.pass_res);
        if (resetPasswordButton != null) {
            resetPasswordButton.setOnClickListener(v -> showPasswordResetDialog());
        }

        adminPanelButton.setOnClickListener(v -> openAdminPanel());
    }

    private void loadUserProfile() {
        String userId = SessionManager.getUserId(this);

        Executors.newSingleThreadExecutor().execute(() -> {
            users user = UserDatabase.getInstance(this).userDao().getUserById(userId);
            int notesCount = NoteDatabase.getInstance(this).noteDao().countNotesForUserSync(userId);

            runOnUiThread(() -> {
                if (user != null) {
                    displayUserProfile(user, notesCount);
                } else {
                    usernameTextView.setText(SessionManager.getUsername(this));
                    emailTextView.setText(SessionManager.getEmail(this));
                    notesCountTextView.setText("Total Notes: " + notesCount);
                }
            });
        });
    }

    private void displayUserProfile(users user, int notesCount) {
        usernameTextView.setText(user.getUsername() != null ? user.getUsername() : "User");
        emailTextView.setText(user.getEmail() != null ? user.getEmail() : "No email");

        if (user.getJoinedDate() > 0) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
            joinedDateTextView.setText("Joined: " + dateFormat.format(new Date(user.getJoinedDate())));
        } else {
            joinedDateTextView.setText("Joined date unknown");
        }

        notesCountTextView.setText("Total Notes: " + notesCount);

        if (user.isAdmin()) {
            adminPanelButton.setVisibility(View.VISIBLE);
        } else {
            adminPanelButton.setVisibility(View.GONE);
        }

        if (user.getProfile_icon() != null && !user.getProfile_icon().isEmpty()) {
            Glide.with(this)
                    .load(user.getProfile_icon())
                    .apply(RequestOptions.circleCropTransform())
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .error(R.drawable.accicon)
                    .into(profilePictureImageView);
        }
    }

    private void showEditUsernameDialog() {
        View customView = LayoutInflater.from(profile.this).inflate(R.layout.edit_profile_dialog, null);
        EditText usernameChange = customView.findViewById(R.id.username_cha);
        TextView dialogTitleTask = customView.findViewById(R.id.dialog_message);
        dialogTitleTask.setText(R.string.username);

        AlertDialog dialog = new AlertDialog.Builder(context, R.style.AppTheme_Dialog)
                .setView(customView)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        MaterialButton positiveButton = customView.findViewById(R.id.positive_button);
        MaterialButton negativeButton = customView.findViewById(R.id.negative_button);

        positiveButton.setOnClickListener(v -> {
            String newUsername = usernameChange.getText().toString().trim();
            if (newUsername.isEmpty()) {
                Toast.makeText(profile.this, "Username cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            Executors.newSingleThreadExecutor().execute(() -> {
                UserDatabase db = UserDatabase.getInstance(profile.this);
                users existing = db.userDao().getUserByUsername(newUsername);
                if (existing != null && !existing.getUid().equals(SessionManager.getUserId(profile.this))) {
                    runOnUiThread(() -> Toast.makeText(profile.this, "Username already taken", Toast.LENGTH_SHORT).show());
                    return;
                }

                users currentUser = db.userDao().getUserById(SessionManager.getUserId(profile.this));
                if (currentUser != null) {
                    currentUser.setUsername(newUsername);
                    db.userDao().update(currentUser);
                }

                SessionManager.setUsername(profile.this, newUsername);

                runOnUiThread(() -> {
                    usernameTextView.setText(newUsername);
                    Toast.makeText(profile.this, "Username updated successfully", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                });
            });
        });

        negativeButton.setOnClickListener(v -> dialog.cancel());
        dialog.show();
    }

    private void logout() {
        SessionManager.logout(this);
        Toast.makeText(this, "Logged out successfully!", Toast.LENGTH_SHORT).show();

        Intent loginIntent = new Intent(profile.this, LoginPage.class);
        loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(loginIntent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            saveProfileImageLocally(imageUri);
        }
    }

    private void saveProfileImageLocally(Uri imageUri) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                File file = new File(getFilesDir(), "profile_picture_" + System.currentTimeMillis() + ".jpg");
                FileOutputStream outputStream = new FileOutputStream(file);

                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }

                outputStream.close();
                if (inputStream != null) inputStream.close();

                String localPath = file.getAbsolutePath();
                UserDatabase db = UserDatabase.getInstance(profile.this);
                users user = db.userDao().getUserById(SessionManager.getUserId(profile.this));
                if (user != null) {
                    user.setProfile_icon(localPath);
                    db.userDao().update(user);
                }

                runOnUiThread(() -> {
                    Glide.with(profile.this)
                            .load(localPath)
                            .apply(RequestOptions.circleCropTransform())
                            .placeholder(R.drawable.ic_profile_placeholder)
                            .into(profilePictureImageView);
                    Toast.makeText(profile.this, "Profile picture updated", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(profile.this, "Error saving picture", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void showPasswordResetDialog() {
        View customView = LayoutInflater.from(context).inflate(R.layout.edit_profile_dialog, null);
        TextView dialogTitle = customView.findViewById(R.id.dialog_title);
        TextView dialogMessage = customView.findViewById(R.id.dialog_message);
        EditText newPasswordInput = customView.findViewById(R.id.username_cha);

        if (dialogTitle != null) dialogTitle.setText(R.string.reset);
        if (dialogMessage != null) dialogMessage.setText("New Password");
        if (newPasswordInput != null) {
            newPasswordInput.setHint("Enter new password");
            newPasswordInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }

        AlertDialog dialog = new AlertDialog.Builder(context, R.style.AppTheme_Dialog)
                .setView(customView)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        MaterialButton positiveButton = customView.findViewById(R.id.positive_button);
        MaterialButton negativeButton = customView.findViewById(R.id.negative_button);

        positiveButton.setOnClickListener(v -> {
            String newPassword = newPasswordInput.getText().toString().trim();
            if (newPassword.length() < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            Executors.newSingleThreadExecutor().execute(() -> {
                UserDatabase db = UserDatabase.getInstance(profile.this);
                users user = db.userDao().getUserById(SessionManager.getUserId(profile.this));
                if (user != null) {
                    user.setPassword(newPassword);
                    db.userDao().update(user);
                }
                runOnUiThread(() -> {
                    Toast.makeText(profile.this, "Password updated successfully!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                });
            });
        });

        negativeButton.setOnClickListener(v -> dialog.cancel());
        dialog.show();
    }

    private void openAdminPanel() {
        Intent adminIntent = new Intent(this, admin_page.class);
        startActivity(adminIntent);
    }
}
