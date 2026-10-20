package com.example.notes_app.Activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.notes_app.Models.users;
import com.example.notes_app.R;
import com.example.notes_app.SessionManager;
import com.example.notes_app.database.UserDatabase;
import com.google.android.material.button.MaterialButton;

import java.util.concurrent.Executors;

public class Setting extends AppCompatActivity {
    private SharedPref sharedPreferences;
    private TextView usernameText, logout, edit_profile, forget_password;
    private ImageView profile, back;
    private Context context;
    private RelativeLayout heading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sharedPreferences = new SharedPref(this);

        if (sharedPreferences.loadNightModeState()) {
            setTheme(R.style.Dark);
        } else {
            setTheme(R.style.Light);
        }
        setContentView(R.layout.activity_setting);
        super.onCreate(savedInstanceState);

        context = this;
        back = findViewById(R.id.back);
        profile = findViewById(R.id.profilePicture);
        usernameText = findViewById(R.id.usernameText);
        forget_password = findViewById(R.id.passwordchange);
        logout = findViewById(R.id.logout);
        edit_profile = findViewById(R.id.edit_profile);
        heading = findViewById(R.id.profile_heading);
        Switch toggleTheme = findViewById(R.id.darkModeSwitch);

        boolean isNightMode = sharedPreferences.loadNightModeState();
        toggleTheme.setChecked(isNightMode);

        toggleTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.setNightModeState(isChecked);
            Intent intent = new Intent("com.example.notes_app.THEME_CHANGED");
            sendBroadcast(intent);
            recreate();
        });

        forget_password.setOnClickListener(view -> showPasswordChangeDialog());

        logout.setOnClickListener(view -> logout());

        edit_profile.setOnClickListener(view -> {
            Intent intent = new Intent(Setting.this, profile.class);
            startActivity(intent);
        });

        heading.setOnClickListener(view -> {
            Intent intent = new Intent(Setting.this, profile.class);
            startActivity(intent);
        });

        back.setOnClickListener(view -> {
            Intent intent = new Intent(Setting.this, home_page.class);
            startActivity(intent);
            finish();
        });

        loadUserProfile();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserProfile();
    }

    private void loadUserProfile() {
        String userId = SessionManager.getUserId(this);
        Executors.newSingleThreadExecutor().execute(() -> {
            users user = UserDatabase.getInstance(this).userDao().getUserById(userId);
            runOnUiThread(() -> {
                if (user != null) {
                    usernameText.setText(user.getUsername());
                    if (user.getProfile_icon() != null && !user.getProfile_icon().isEmpty()) {
                        Glide.with(this)
                                .load(user.getProfile_icon())
                                .apply(RequestOptions.circleCropTransform())
                                .placeholder(R.drawable.ic_profile_placeholder)
                                .error(R.drawable.accicon)
                                .into(profile);
                    }
                } else {
                    usernameText.setText(SessionManager.getUsername(this));
                }
            });
        });
    }

    private void showPasswordChangeDialog() {
        View customView = LayoutInflater.from(this).inflate(R.layout.edit_profile_dialog, null);
        TextView dialogTitle = customView.findViewById(R.id.dialog_title);
        TextView dialogMessage = customView.findViewById(R.id.dialog_message);
        EditText newPasswordInput = customView.findViewById(R.id.username_cha);

        if (dialogTitle != null) dialogTitle.setText(R.string.reset);
        if (dialogMessage != null) dialogMessage.setText("New Password");
        if (newPasswordInput != null) {
            newPasswordInput.setHint("Enter new password");
            newPasswordInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }

        AlertDialog dialog = new AlertDialog.Builder(Setting.this, R.style.AppTheme_Dialog)
                .setView(customView)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        MaterialButton positiveButton = customView.findViewById(R.id.positive_button);
        MaterialButton negativeButton = customView.findViewById(R.id.negative_button);

        positiveButton.setOnClickListener(v -> {
            String newPass = newPasswordInput.getText().toString().trim();
            if (newPass.length() < 6) {
                Toast.makeText(context, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            Executors.newSingleThreadExecutor().execute(() -> {
                UserDatabase db = UserDatabase.getInstance(Setting.this);
                users user = db.userDao().getUserById(SessionManager.getUserId(Setting.this));
                if (user != null) {
                    user.setPassword(newPass);
                    db.userDao().update(user);
                }
                runOnUiThread(() -> {
                    Toast.makeText(context, "Password updated successfully!", Toast.LENGTH_SHORT).show();
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

        Intent loginIntent = new Intent(Setting.this, LoginPage.class);
        loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(loginIntent);
        finish();
    }
}