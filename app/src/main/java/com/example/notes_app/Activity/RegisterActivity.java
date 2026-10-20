package com.example.notes_app.Activity;

import static android.content.ContentValues.TAG;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.notes_app.Models.users;
import com.example.notes_app.R;
import com.example.notes_app.SessionManager;
import com.example.notes_app.database.UserDatabase;
import com.example.notes_app.utility;

import java.util.UUID;
import java.util.concurrent.Executors;

public class RegisterActivity extends AppCompatActivity {

    private EditText inputEmail;
    private EditText inputUsername;
    private EditText inputPassword;
    private EditText inputConfirmPassword;
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

        setContentView(R.layout.activity_register);

        UserDatabase.ensureDefaultAdmin(this);

        initializeUI();
        setupListeners();
    }

    private final BroadcastReceiver themeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.example.notes_app.THEME_CHANGED".equals(intent.getAction())) {
                applyTheme();
                Log.d("RegisterActivity", "Broadcast sent for theme change..");
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

    private void initializeUI() {
        inputEmail = findViewById(R.id.Re_mail);
        inputUsername = findViewById(R.id.re_username);
        inputPassword = findViewById(R.id.re_pass1);
        inputConfirmPassword = findViewById(R.id.re_passconf);

        utility.setupPasswordVisibilityToggle(inputPassword, false);
        utility.setupPasswordVisibilityToggle(inputConfirmPassword, false);
    }

    private void setupListeners() {
        Button confirmButton = findViewById(R.id.re_submit);
        TextView loginButton = findViewById(R.id.login);

        loginButton.setOnClickListener(v -> navigateToLogin());

        confirmButton.setOnClickListener(v -> {
            if (validateInput()) {
                registerUser();
            }
        });
    }

    private void navigateToLogin() {
        startActivity(new Intent(RegisterActivity.this, LoginPage.class));
        finish();
    }

    private boolean validateInput() {
        return validateEmail() && validateUsername() && validatePassword();
    }

    private boolean validateEmail() {
        String emailInput = inputEmail.getText().toString().trim();

        if (emailInput.isEmpty()) {
            inputEmail.setError("Email field can't be empty");
            return false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
            inputEmail.setError("Invalid email format");
            return false;
        } else {
            inputEmail.setError(null);
            return true;
        }
    }

    private boolean validateUsername() {
        String usernameInput = inputUsername.getText().toString().trim();

        if (usernameInput.isEmpty()) {
            inputUsername.setError("Username field can't be empty");
            return false;
        } else if (usernameInput.length() > 15) {
            inputUsername.setError("Username is too long");
            return false;
        } else {
            inputUsername.setError(null);
            return true;
        }
    }

    private boolean validatePassword() {
        String passwordInput = inputPassword.getText().toString().trim();
        String confirmPasswordInput = inputConfirmPassword.getText().toString().trim();

        if (passwordInput.isEmpty()) {
            inputPassword.setError("Password field can't be empty");
            return false;
        } else if (passwordInput.length() < 6) {
            inputPassword.setError("Password must be at least 6 characters");
            return false;
        } else if (!passwordInput.equals(confirmPasswordInput)) {
            inputConfirmPassword.setError("Passwords do not match");
            return false;
        } else {
            inputPassword.setError(null);
            inputConfirmPassword.setError(null);
            return true;
        }
    }

    private void registerUser() {
        String email = inputEmail.getText().toString().trim();
        String password = inputPassword.getText().toString().trim();
        String username = inputUsername.getText().toString().trim();

        Executors.newSingleThreadExecutor().execute(() -> {
            UserDatabase db = UserDatabase.getInstance(RegisterActivity.this);

            // Ensure default admin exists
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

            if (db.userDao().getUserByUsername(username) != null) {
                runOnUiThread(() -> Toast.makeText(RegisterActivity.this, "Username already exists", Toast.LENGTH_SHORT).show());
                return;
            }

            if (db.userDao().getUserByEmail(email) != null) {
                runOnUiThread(() -> Toast.makeText(RegisterActivity.this, "Email already registered", Toast.LENGTH_SHORT).show());
                return;
            }

            String userId = UUID.randomUUID().toString();
            boolean isAdmin = "admin".equalsIgnoreCase(username);

            users newUser = new users(userId, username, email, isAdmin, System.currentTimeMillis(), null);
            newUser.setPassword(password);

            db.userDao().insertOrUpdate(newUser);

            runOnUiThread(() -> {
                // Auto-login the new user for instant access
                SessionManager.setLogin(RegisterActivity.this, userId, username, email, isAdmin);
                Toast.makeText(RegisterActivity.this, "Account created! Welcome, " + username + "!", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(RegisterActivity.this, home_page.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish();
            });
        });
    }
}
