package com.example.notes_app.Activity;

import static android.content.ContentValues.TAG;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.notes_app.Models.users;
import com.example.notes_app.R;
import com.example.notes_app.database.UserDatabase;

import java.util.concurrent.Executors;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText editTextEmail;
    private SharedPref sharedPreferences;
    private boolean isReceiverRegistered = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPreferences = new SharedPref(this);

        // Apply the saved theme
        if (sharedPreferences.loadNightModeState()) {
            setTheme(R.style.Dark);
        } else {
            setTheme(R.style.Light);
        }
        setContentView(R.layout.activity_forgot_password);

        editTextEmail = findViewById(R.id.email_forget);
        Button buttonResetPassword = findViewById(R.id.reset_button);

        findViewById(R.id.back_to_login).setOnClickListener(v -> {
            Intent intent = new Intent(ForgotPasswordActivity.this, LoginPage.class);
            startActivity(intent);
            finish();
        });

        buttonResetPassword.setOnClickListener(v -> {
            String email = editTextEmail.getText().toString().trim();
            if (email.isEmpty()) {
                Toast.makeText(ForgotPasswordActivity.this, "Please enter your email", Toast.LENGTH_SHORT).show();
                return;
            }

            Executors.newSingleThreadExecutor().execute(() -> {
                UserDatabase db = UserDatabase.getInstance(ForgotPasswordActivity.this);
                users user = db.userDao().getUserByEmail(email);

                if (user != null) {
                    runOnUiThread(() -> promptForNewPassword(user));
                } else {
                    runOnUiThread(() -> Toast.makeText(ForgotPasswordActivity.this, "No user found with this email", Toast.LENGTH_SHORT).show());
                }
            });
        });
    }

    private void promptForNewPassword(users user) {
        final EditText input = new EditText(this);
        input.setHint("Enter new password");
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);

        new AlertDialog.Builder(this)
                .setTitle("Reset Password for " + user.getUsername())
                .setMessage("Enter your new password (min 6 characters):")
                .setView(input)
                .setPositiveButton("Reset", (dialog, which) -> {
                    String newPass = input.getText().toString().trim();
                    if (newPass.length() < 6) {
                        Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Executors.newSingleThreadExecutor().execute(() -> {
                        user.setPassword(newPass);
                        UserDatabase.getInstance(this).userDao().update(user);
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Password updated successfully!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(this, LoginPage.class));
                            finish();
                        });
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private final BroadcastReceiver themeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.example.notes_app.THEME_CHANGED".equals(intent.getAction())) {
                applyTheme();
                recreate();
            }
        }
    };

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent("com.example.notes_app.THEME_CHANGED");
        sendBroadcast(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isReceiverRegistered) {
            registerReceiver(themeReceiver, new IntentFilter("com.example.notes_app.THEME_CHANGED"));
            isReceiverRegistered = true;
        }
        applyTheme();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isReceiverRegistered) {
            unregisterReceiver(themeReceiver);
            isReceiverRegistered = false;
        }
    }

    private void applyTheme() {
        if (sharedPreferences.loadNightModeState()) {
            setTheme(R.style.Dark);
        } else {
            setTheme(R.style.Light);
        }
    }
}
