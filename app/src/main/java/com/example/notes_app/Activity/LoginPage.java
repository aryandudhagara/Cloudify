package com.example.notes_app.Activity;

import static android.content.ContentValues.TAG;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.notes_app.Models.users;
import com.example.notes_app.R;
import com.example.notes_app.SessionManager;
import com.example.notes_app.database.UserDatabase;
import com.example.notes_app.utility;

import java.util.UUID;
import java.util.concurrent.Executors;

import de.timonknispel.ktloadingbutton.KTLoadingButton;

public class LoginPage extends AppCompatActivity {

    private EditText usernameEditText, passwordEditText;
    private KTLoadingButton loginButton;
    private SharedPref sharedPref;
    private boolean isReceiverRegistered = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPref = new SharedPref(this);

        // Apply the saved theme
        if (sharedPref.loadNightModeState()) {
            setTheme(R.style.Dark);
        } else {
            setTheme(R.style.Light);
        }

        setContentView(R.layout.activity_login_page);

        // Ensure default admin user is initialized
        UserDatabase.ensureDefaultAdmin(this);

        // Check if user is already logged in
        checkAutoLogin();

        // Initialize UI elements
        usernameEditText = findViewById(R.id.username);
        passwordEditText = findViewById(R.id.editTextTextPassword);
        loginButton = findViewById(R.id.loginbutton);
        TextView registerTextView = findViewById(R.id.register);

        // Password toggle utility
        utility.setupPasswordVisibilityToggle(passwordEditText, false);

        // Register new user
        registerTextView.setOnClickListener(view -> startActivity(new Intent(LoginPage.this, RegisterActivity.class)));

        // Login button click
        loginButton.setOnClickListener(view -> handleLogin());

        // Forgot password
        findViewById(R.id.forget).setOnClickListener(view -> startActivity(new Intent(LoginPage.this, ForgotPasswordActivity.class)));
    }

    private final BroadcastReceiver themeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.example.notes_app.THEME_CHANGED".equals(intent.getAction())) {
                applyTheme();
                Log.d("LoginPage", "Broadcast sent for theme change..");
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
        if (sharedPref.loadNightModeState()) {
            setTheme(R.style.Dark);
        } else {
            setTheme(R.style.Light);
        }
    }

    private void checkAutoLogin() {
        if (SessionManager.isLoggedIn(this)) {
            navigateToMainActivity();
            finish();
        }
    }

    private void handleLogin() {
        String username = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Username or Password is empty", Toast.LENGTH_SHORT).show();
            try {
                loginButton.reset();
            } catch (Exception ignored) {}
            return;
        }

        try {
            loginButton.startLoading();
        } catch (Exception ignored) {}

        Executors.newSingleThreadExecutor().execute(() -> {
            UserDatabase db = UserDatabase.getInstance(LoginPage.this);

            // Ensure admin account exists
            users admin = db.userDao().getUserByUsername("admin");
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
                db.userDao().insertOrUpdate(defaultAdmin);
            }

            users user = db.userDao().getUserByUsername(username);
            if (user == null) {
                user = db.userDao().getUserByEmail(username);
            }

            if (user != null) {
                if (user.getPassword() == null || user.getPassword().equals(password)) {
                    user.setLastLogin(System.currentTimeMillis());
                    db.userDao().update(user);

                    users finalUser = user;
                    runOnUiThread(() -> {
                        SessionManager.setLogin(
                                LoginPage.this,
                                finalUser.getUid(),
                                finalUser.getUsername(),
                                finalUser.getEmail(),
                                finalUser.isAdmin()
                        );

                        Toast.makeText(LoginPage.this, "Welcome, " + finalUser.getUsername() + "!", Toast.LENGTH_SHORT).show();

                        try {
                            loginButton.doResult(true, null);
                        } catch (Exception ignored) {}

                        new Handler(Looper.getMainLooper()).postDelayed(this::startLoginTransition, 250);
                    });
                } else {
                    runOnUiThread(() -> showError("Incorrect password"));
                }
            } else {
                runOnUiThread(() -> showError("User not found. Please register."));
            }
        });
    }

    private void startLoginTransition() {
        navigateToMainActivity();
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(LoginPage.this, home_page.class);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }

    private void showError(String message) {
        try {
            loginButton.doResult(false, null);
        } catch (Exception ignored) {}
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            try {
                loginButton.reset();
            } catch (Exception ignored) {}
        }, 400);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
