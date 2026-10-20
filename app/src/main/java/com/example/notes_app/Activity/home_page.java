package com.example.notes_app.Activity;

import static android.content.ContentValues.TAG;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.notes_app.Models.users;
import com.example.notes_app.R;
import com.example.notes_app.SessionManager;
import com.example.notes_app.bottomSheetFragment.CreateTaskBottomSheetFragment;
import com.example.notes_app.database.UserDatabase;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;

import java.util.concurrent.Executors;

import nl.joery.animatedbottombar.AnimatedBottomBar;

public class home_page extends AppCompatActivity {
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private AnimatedBottomBar bottomBar;
    private TextView helloTextView;

    private Fragment notesFragment;
    private Fragment tasksFragment;
    private Fragment binFragment;

    private View userImageView;
    private ImageView profile_picture;

    private Fragment currentFragment;
    private int currentTabIndex = 0;

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

        setContentView(R.layout.activity_home_page);

        // Initialize views
        bottomBar = findViewById(R.id.animatedBottomBar);
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        helloTextView = findViewById(R.id.hellotexth);
        userImageView = findViewById(R.id.userImage);
        FloatingActionButton optionOne = findViewById(R.id.option_one);
        FloatingActionButton optionTwo = findViewById(R.id.option_two);
        FloatingActionButton optionThree = findViewById(R.id.option_three);

        // Initialize fragments
        notesFragment = new NotesFragment();
        tasksFragment = new TasksFragment();
        binFragment = new Bin_fragment();

        // Set default fragment
        if (savedInstanceState == null) {
            setInitialFragment();
        }

        // Setup Navigation Drawer
        setupNavigationDrawer();

        // Set up bottom bar tab selection listener
        bottomBar.setOnTabSelectListener(new AnimatedBottomBar.OnTabSelectListener() {
            @Override
            public void onTabSelected(int lastIndex, AnimatedBottomBar.Tab lastTab, int newIndex, AnimatedBottomBar.Tab newTab) {
                Fragment selectedFragment = null;

                if (newTab.getId() == R.id.tab_notes) {
                    selectedFragment = notesFragment;
                } else if (newTab.getId() == R.id.tab_tasks) {
                    selectedFragment = tasksFragment;
                } else if (newTab.getId() == R.id.tab_bin) {
                    selectedFragment = binFragment;
                }

                if (selectedFragment != null) {
                    replaceFragment(selectedFragment, newIndex > currentTabIndex);
                    currentTabIndex = newIndex;
                }
            }

            @Override
            public void onTabReselected(int index, AnimatedBottomBar.Tab tab) {
                Log.d("AnimatedBottomBar", "Reselected tab: " + tab.getTitle());
            }
        });

        if (getIntent().getBooleanExtra("open_tasks_fragment", false)) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainerh, new TasksFragment())
                    .commit();
        }

        optionOne.setOnClickListener(v -> {
            Intent intent = new Intent(this, CreatingNoteActivity.class);
            startActivity(intent);
        });

        optionTwo.setOnClickListener(v -> {
            CreateTaskBottomSheetFragment fragment = new CreateTaskBottomSheetFragment();
            fragment.show(getSupportFragmentManager(), "CreateTaskBottomSheet");
        });

        userImageView.setOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        setupUserProfile();
        checkIfUserIsAdmin();
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
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
            Intent intent = new Intent("com.example.notes_app.THEME_CHANGED");
            sendBroadcast(intent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isReceiverRegistered) {
            registerReceiver(themeReceiver, new IntentFilter("com.example.notes_app.THEME_CHANGED"));
            isReceiverRegistered = true;
        }
        applyTheme();
        setupUserProfile();
        checkIfUserIsAdmin();
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

    private void setupNavigationDrawer() {
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, R.string.open_drawer, R.string.close_drawer);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        View headerView = navigationView.getHeaderView(0);
        TextView usernameTextView = headerView.findViewById(R.id.username);
        TextView emailTextView = headerView.findViewById(R.id.email);
        profile_picture = headerView.findViewById(R.id.profile_picture);

        String userId = SessionManager.getUserId(this);
        fetchUserProfile(userId,
                username -> usernameTextView.setText(username),
                imageUrl -> {
                    if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                        Glide.with(this)
                                .load(imageUrl)
                                .apply(RequestOptions.circleCropTransform())
                                .placeholder(R.drawable.accicon)
                                .error(R.drawable.accicon)
                                .into(profile_picture);
                    }
                },
                email -> emailTextView.setText(email)
        );

        headerView.setOnClickListener(v -> {
            startActivity(new Intent(home_page.this, profile.class));
            drawerLayout.closeDrawer(GravityCompat.START);
        });

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.home) {
                replaceFragment(notesFragment, false);
            } else if (id == R.id.setting) {
                startActivity(new Intent(home_page.this, Setting.class));
            } else if (id == R.id.about_us) {
                startActivity(new Intent(home_page.this, About_us_page.class));
            } else if (id == R.id.logout) {
                showLogoutConfirmationDialog();
            } else if (id == R.id.admin_panel) {
                startActivity(new Intent(home_page.this, admin_page.class));
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    private void showLogoutConfirmationDialog() {
        View customView = LayoutInflater.from(this).inflate(R.layout.custom_dialog, null);
        TextView dialogTitle = customView.findViewById(R.id.dialog_message);
        dialogTitle.setText(R.string.are_you_sure_you_want_to_Logout);

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.AppTheme_Dialog)
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
    }

    private void logout() {
        SessionManager.logout(this);
        Toast.makeText(this, "Logged out successfully!", Toast.LENGTH_SHORT).show();

        Intent loginIntent = new Intent(this, LoginPage.class);
        loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(loginIntent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }

    private void checkIfUserIsAdmin() {
        boolean isAdmin = SessionManager.isAdmin(this);
        updateAdminMenuVisibility(isAdmin);
    }

    private void updateAdminMenuVisibility(boolean isAdmin) {
        Menu menu = navigationView.getMenu();
        MenuItem adminPanelItem = menu.findItem(R.id.admin_panel);
        if (adminPanelItem != null) {
            adminPanelItem.setVisible(isAdmin);
        }
    }

    private void setInitialFragment() {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.add(R.id.fragmentContainerh, notesFragment).commit();
        currentFragment = notesFragment;
    }

    private void setupUserProfile() {
        String userId = SessionManager.getUserId(this);
        fetchUserProfile(userId,
                username -> {
                    if (helloTextView != null) {
                        helloTextView.setText("Hi, " + username);
                    }
                },
                imageUrl -> {
                    if (imageUrl != null && !imageUrl.trim().isEmpty() && userImageView instanceof ImageView) {
                        Glide.with(this)
                                .load(imageUrl)
                                .apply(RequestOptions.circleCropTransform())
                                .placeholder(R.drawable.ic_profile_placeholder)
                                .into((ImageView) userImageView);
                    }
                },
                email -> {}
        );
    }

    public interface ImageCallback {
        void onImageFetched(String imageUrl);
    }

    public interface EmailCallback {
        void onEmailFetched(String email);
    }

    public interface UsernameCallback {
        void onUsernameFetched(String username);
    }

    private void fetchUserProfile(String userId, UsernameCallback usernameCallback, ImageCallback imageCallback, EmailCallback emailCallback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            users user = UserDatabase.getInstance(this).userDao().getUserById(userId);
            String username = user != null ? user.getUsername() : SessionManager.getUsername(this);
            String email = user != null ? user.getEmail() : SessionManager.getEmail(this);
            String imageUrl = user != null ? user.getProfile_icon() : null;

            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                usernameCallback.onUsernameFetched(username != null ? username : "User");
                imageCallback.onImageFetched(imageUrl);
                emailCallback.onEmailFetched(email != null ? email : "No Email");
            });
        });
    }

    private void replaceFragment(Fragment fragment, boolean forward) {
        if (currentFragment == fragment) return;

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        if (forward) {
            fragmentTransaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left);
        } else {
            fragmentTransaction.setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right);
        }

        fragmentTransaction.replace(R.id.fragmentContainerh, fragment);
        fragmentTransaction.commit();
        currentFragment = fragment;
    }
}
