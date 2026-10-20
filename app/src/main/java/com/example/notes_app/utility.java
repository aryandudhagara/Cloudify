package com.example.notes_app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.EditText;

import com.example.notes_app.Activity.CreatingNoteActivity;
import com.example.notes_app.Activity.LoginPage;
import com.example.notes_app.bottomSheetFragment.CreateTaskBottomSheetFragment;
import com.example.notes_app.dao.NoteDao;
import com.example.notes_app.dao.TaskDao;
import com.google.android.material.bottomappbar.BottomAppBar;

public class utility {

    private static final String TAG = "Utility";
    private static final String DEFAULT_USERNAME = "User";

    public interface UsernameCallback {
        void onUsernameFetched(String username);
    }

    public static void fetchUsername(UsernameCallback callback) {
        callback.onUsernameFetched(DEFAULT_USERNAME);
    }

    public static void fetchUsername(Context context, UsernameCallback callback) {
        if (context != null) {
            String username = SessionManager.getUsername(context);
            callback.onUsernameFetched(username != null && !username.isEmpty() ? username : DEFAULT_USERNAME);
        } else {
            callback.onUsernameFetched(DEFAULT_USERNAME);
        }
    }

    public static void setupMainActivityBottomNavMenu(BottomAppBar bottomAppBar, Activity activity) {
        bottomAppBar.setOnMenuItemClickListener(item -> {
            Log.d("Utility", "Menu item clicked: " + item.getItemId());
            int id = item.getItemId();

            if (id == R.id.setting) {
                return true;
            } else if (id == R.id.theam) {
                return true;
            } else if (id == R.id.logout) {
                Log.d("Utility", "Logout clicked");

                // Clear offline session
                SessionManager.logout(activity);

                // Redirect to LoginPage
                Intent loginIntent = new Intent(activity, LoginPage.class);
                loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.startActivity(loginIntent);
                activity.finish();
                return true;
            } else if (id == R.id.notequickadd) {
                Log.d("Utility", "Note quick add clicked");
                Intent noteQuickAddIntent = new Intent(activity, CreatingNoteActivity.class);
                activity.startActivity(noteQuickAddIntent);
                return true;
            } else if (id == R.id.reminderquickadd) {
                Log.d("Utility", "Reminder quick add clicked");
                if (activity instanceof androidx.appcompat.app.AppCompatActivity) {
                    androidx.appcompat.app.AppCompatActivity appCompatActivity = (androidx.appcompat.app.AppCompatActivity) activity;
                    CreateTaskBottomSheetFragment fragment = new CreateTaskBottomSheetFragment();
                    fragment.show(appCompatActivity.getSupportFragmentManager(), "CreateTaskBottomSheet");
                }
                return true;
            }
            return false;
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    public static void setupPasswordVisibilityToggle(final EditText editText, boolean initialVisibility) {
        editText.setTransformationMethod(initialVisibility ? HideReturnsTransformationMethod.getInstance() : PasswordTransformationMethod.getInstance());

        editText.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                int drawableRightWidth = editText.getCompoundDrawables()[2] != null ? editText.getCompoundDrawables()[2].getBounds().width() : 0;
                int touchableAreaWidth = drawableRightWidth + editText.getPaddingEnd();
                if (event.getRawX() >= (editText.getRight() - touchableAreaWidth)) {
                    togglePasswordVisibility(editText);
                    return true;
                }
            }
            return false;
        });
    }

    private static void togglePasswordVisibility(EditText editText) {
        boolean isPasswordVisible = editText.getTransformationMethod() instanceof HideReturnsTransformationMethod;
        editText.setTransformationMethod(isPasswordVisible ? PasswordTransformationMethod.getInstance() : HideReturnsTransformationMethod.getInstance());
        editText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.baseline_lock_24, 0, isPasswordVisible ? R.drawable.baseline_visibility_off_24 : R.drawable.baseline_visibility_24, 0);
        editText.postInvalidate();
        editText.setSelection(editText.getText().length());
    }

    public static class TaskSyncManager {
        public TaskSyncManager(Object firestore, TaskDao taskDao) {}
        public void syncTasks() {}
    }

    public static class NoteSyncManager {
        public NoteSyncManager(Object firestore, NoteDao noteDao) {}
    }
}
