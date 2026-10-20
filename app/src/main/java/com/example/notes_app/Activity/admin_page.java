package com.example.notes_app.Activity;

import static android.content.ContentValues.TAG;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.notes_app.Models.users;
import com.example.notes_app.R;
import com.example.notes_app.adapter.UserAdapter;
import com.example.notes_app.database.UserDatabase;
import com.example.notes_app.listeners.UserListener;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

public class admin_page extends AppCompatActivity {

    private RecyclerView userRecyclerView;
    private UserAdapter userAdapter;
    private List<users> userList;
    private LineChart lineChart;
    private BarChart barChart;

    private TextView totalUsersText, mostActiveUsersText;
    private SharedPref sharedPreferences;
    private boolean isReceiverRegistered = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPreferences = new SharedPref(this);

        if (sharedPreferences.loadNightModeState()) {
            setTheme(R.style.Dark);
        } else {
            setTheme(R.style.Light);
        }
        setContentView(R.layout.activity_admin_page);

        totalUsersText = findViewById(R.id.totalUsersText);
        mostActiveUsersText = findViewById(R.id.mostActiveUsersText);

        setupRecyclerView();
        setupCharts();
        loadUsers();
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
        loadUsers();
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

    private void setupRecyclerView() {
        userRecyclerView = findViewById(R.id.adminview);
        userRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        userList = new ArrayList<>();
        userAdapter = new UserAdapter(new UserListener() {
            @Override
            public void onResetPasswordClicked(users user) {
                resetUserPassword(user);
            }

            @Override
            public void onDeleteUserClicked(users user) {
                deleteUser(user);
            }
        });
        userRecyclerView.setAdapter(userAdapter);
    }

    private void setupCharts() {
        lineChart = findViewById(R.id.lineChart);
        barChart = findViewById(R.id.barChart);
    }

    private void loadUsers() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<users> list = UserDatabase.getInstance(this).userDao().getAllUsers();
            runOnUiThread(() -> {
                userList.clear();
                userList.addAll(list);
                userAdapter.setUsers(userList);
                totalUsersText.setText("Total Users: " + userList.size());
                loadUserAnalytics(list);
                fetchMostActiveUsers();
            });
        });
    }

    private void loadUserAnalytics(List<users> usersList) {
        List<Entry> lineEntries = new ArrayList<>();
        List<BarEntry> barEntries = new ArrayList<>();
        Map<String, Integer> activityMap = new HashMap<>();

        for (users user : usersList) {
            long time = user.getLastLogin() > 0 ? user.getLastLogin() : user.getJoinedDate();
            if (time > 0) {
                String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date(time));
                activityMap.put(date, activityMap.getOrDefault(date, 0) + 1);
            }
        }

        int index = 0;
        for (Map.Entry<String, Integer> entry : activityMap.entrySet()) {
            lineEntries.add(new Entry(index, entry.getValue()));
            barEntries.add(new BarEntry(index, entry.getValue()));
            index++;
        }

        if (!lineEntries.isEmpty()) {
            LineDataSet lineDataSet = new LineDataSet(lineEntries, "User Activity");
            LineData lineData = new LineData(lineDataSet);
            lineChart.setData(lineData);
            lineChart.invalidate();

            BarDataSet barDataSet = new BarDataSet(barEntries, "Active Users");
            BarData barData = new BarData(barDataSet);
            barChart.setData(barData);
            barChart.invalidate();
        }
    }

    private void fetchMostActiveUsers() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<users> activeList = UserDatabase.getInstance(this).userDao().getMostActiveUsers(3);
            runOnUiThread(() -> {
                StringBuilder activeUsers = new StringBuilder();
                for (users u : activeList) {
                    activeUsers.append(u.getUsername()).append("\n");
                }
                if (activeUsers.length() > 0) {
                    mostActiveUsersText.setText("Most Active Users:\n" + activeUsers);
                } else {
                    mostActiveUsersText.setText("Most Active Users: No data");
                }
            });
        });
    }

    private void resetUserPassword(users user) {
        final EditText input = new EditText(this);
        input.setHint("Enter new password");

        new AlertDialog.Builder(this)
                .setTitle("Reset Password for " + user.getUsername())
                .setMessage("Enter new password:")
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
                        runOnUiThread(() -> Toast.makeText(this, "Password reset successfully", Toast.LENGTH_SHORT).show());
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteUser(users user) {
        new AlertDialog.Builder(this)
                .setTitle("Delete User")
                .setMessage("Are you sure you want to delete user " + user.getUsername() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    Executors.newSingleThreadExecutor().execute(() -> {
                        UserDatabase.getInstance(this).userDao().delete(user);
                        runOnUiThread(() -> {
                            userList.remove(user);
                            userAdapter.setUsers(userList);
                            totalUsersText.setText("Total Users: " + userList.size());
                            Toast.makeText(this, "User deleted", Toast.LENGTH_SHORT).show();
                        });
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}