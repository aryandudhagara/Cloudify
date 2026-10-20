package com.example.notes_app.Activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.notes_app.Models.Notes;
import com.example.notes_app.R;
import com.example.notes_app.SessionManager;
import com.example.notes_app.database.NoteDatabase;
import com.example.notes_app.entities.NoteEntity;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Executors;

public class CreatingNoteActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_STORAGE_PERMISSION = 1;
    private static final int REQUEST_CODE_SELECT_IMAGE = 2;
    private static final int REQUEST_CODE_CAMERA = 3;

    private ImageView imageBack, save, imagenote, imageShare, imagePin;
    private ImageView btnAttachment, btnColorPicker, btnMoreOptions;
    private TextView date, textFooterStatus;
    private EditText inNoteTitle, inNoteSubtitle, inNote;
    private View viewSubtitleIndicator;
    private Uri selectedImageUri;
    private String selectNoteColor = "#333333";
    private String localImagePath;
    private Notes existingNote;
    private SharedPref sharedPreferences;
    private boolean isReceiverRegistered = false;
    private boolean isNoteSaved = false;
    private boolean isPinned = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPreferences = new SharedPref(this);

        if (sharedPreferences.loadNightModeState()) {
            setTheme(R.style.Dark);
        } else {
            setTheme(R.style.Light);
        }

        setContentView(R.layout.activity_creating_note);

        initializeViews();
        setCurrentDateTime();

        if (getIntent().getBooleanExtra("isViewOrUpdate", false)) {
            existingNote = (Notes) getIntent().getSerializableExtra("note");
            setViewOrUpdateNote();
        } else {
            selectNoteColor = "#333333";
            setSubtitleIndicatorColor();
            updatePinIcon();
        }

        findViewById(R.id.imageRemoveImage).setOnClickListener(v -> {
            imagenote.setImageBitmap(null);
            imagenote.setVisibility(View.GONE);
            findViewById(R.id.imageRemoveImage).setVisibility(View.GONE);

            selectedImageUri = null;
            localImagePath = null;
            if (existingNote != null) {
                existingNote.setImageUrl("");
            }
        });

        save.setOnClickListener(view -> saveOrUpdateNote());
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

    @Override
    public void onBackPressed() {
        if (isNoteEmpty()) {
            super.onBackPressed();
        } else if (!isNoteSaved) {
            saveOrUpdateNote();
        } else {
            super.onBackPressed();
        }
    }

    private void applyTheme() {
        if (sharedPreferences.loadNightModeState()) {
            setTheme(R.style.Dark);
        } else {
            setTheme(R.style.Light);
        }
    }

    private boolean isNoteEmpty() {
        String title = inNoteTitle.getText().toString().trim();
        String subtitle = inNoteSubtitle.getText().toString().trim();
        String noteText = inNote.getText().toString().trim();

        return title.isEmpty() && subtitle.isEmpty() && noteText.isEmpty() && selectedImageUri == null && localImagePath == null;
    }

    private void initializeViews() {
        imageBack = findViewById(R.id.imageBack);
        save = findViewById(R.id.save);
        imagePin = findViewById(R.id.imagePin);
        imageShare = findViewById(R.id.imageShare);

        date = findViewById(R.id.textDateTime);
        textFooterStatus = findViewById(R.id.textFooterStatus);

        inNoteTitle = findViewById(R.id.inputNoteTitle);
        inNoteSubtitle = findViewById(R.id.inputNoteSubtitle);
        inNote = findViewById(R.id.inputNote);
        viewSubtitleIndicator = findViewById(R.id.viewSubtitleIndicator);
        imagenote = findViewById(R.id.imagenote);

        btnAttachment = findViewById(R.id.btnAttachment);
        btnColorPicker = findViewById(R.id.btnColorPicker);
        btnMoreOptions = findViewById(R.id.btnMoreOptions);

        imageBack.setOnClickListener(view -> onBackPressed());
        imagePin.setOnClickListener(v -> togglePin());
        imageShare.setOnClickListener(v -> shareNote());

        btnAttachment.setOnClickListener(v -> showAttachmentBottomSheet());
        btnColorPicker.setOnClickListener(v -> showColorPickerBottomSheet());
        btnMoreOptions.setOnClickListener(v -> showMoreOptionsBottomSheet());

        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateWordCount();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };
        inNote.addTextChangedListener(textWatcher);
        inNoteTitle.addTextChangedListener(textWatcher);
    }

    private void togglePin() {
        isPinned = !isPinned;
        updatePinIcon();
        Toast.makeText(this, isPinned ? "Note pinned" : "Note unpinned", Toast.LENGTH_SHORT).show();
    }

    private void updatePinIcon() {
        if (imagePin != null) {
            imagePin.setImageResource(isPinned ? R.drawable.ic_pin_filled : R.drawable.ic_pin);
            imagePin.setColorFilter(isPinned
                    ? ContextCompat.getColor(this, R.color.colorAccent)
                    : ContextCompat.getColor(this, R.color.black));
        }
    }

    private void updateWordCount() {
        String text = inNote != null ? inNote.getText().toString().trim() : "";
        int chars = text.length();
        int words = text.isEmpty() ? 0 : text.split("\\s+").length;
        if (textFooterStatus != null) {
            textFooterStatus.setText(words + " words | " + chars + " chars");
        }
    }

    @SuppressLint("SimpleDateFormat")
    private void setCurrentDateTime() {
        String currentDateTime = new SimpleDateFormat("EEEE, dd MMMM yyyy HH:mm a", Locale.getDefault()).format(new Date());
        date.setText(currentDateTime);
    }

    private void setViewOrUpdateNote() {
        if (existingNote != null) {
            inNoteTitle.setText(existingNote.getTitle());
            inNoteSubtitle.setText(existingNote.getSubtitle());
            inNote.setText(existingNote.getNote());

            selectNoteColor = existingNote.getColor();
            isPinned = existingNote.isPinned();

            setSubtitleIndicatorColor();
            updatePinIcon();
            updateWordCount();

            if (existingNote.getImageUrl() != null && !existingNote.getImageUrl().isEmpty()) {
                displaySelectedImage(Uri.parse(existingNote.getImageUrl()));
                localImagePath = existingNote.getImageUrl();
            }
        }
    }

    private void saveOrUpdateNote() {
        if (isNoteSaved) return;

        String title = inNoteTitle.getText().toString().trim();
        String subtitle = inNoteSubtitle.getText().toString().trim();
        String noteText = inNote.getText().toString().trim();

        if (title.isEmpty() && subtitle.isEmpty() && noteText.isEmpty() && selectedImageUri == null && localImagePath == null) {
            finishWithResult();
            return;
        }

        String currentDateTime = new SimpleDateFormat("EEEE, dd MMMM yyyy HH:mm a", Locale.getDefault()).format(new Date());
        String userId = SessionManager.getUserId(this);

        if (selectNoteColor == null || selectNoteColor.isEmpty()) {
            selectNoteColor = "#333333";
        }

        String noteId = (existingNote != null && existingNote.getId() != null)
                ? existingNote.getId()
                : UUID.randomUUID().toString();

        String imagePathToSave = localImagePath;
        if (selectedImageUri != null) {
            imagePathToSave = saveImageLocally(selectedImageUri);
        }

        NoteEntity noteEntity = new NoteEntity(
                noteId,
                title,
                subtitle,
                noteText,
                currentDateTime,
                selectNoteColor,
                imagePathToSave,
                userId,
                false
        );
        noteEntity.setLocalImagePath(imagePathToSave);
        noteEntity.setPinned(isPinned);

        isNoteSaved = true;

        Executors.newSingleThreadExecutor().execute(() -> {
            NoteDatabase.getInstance(CreatingNoteActivity.this).noteDao().insertOrUpdate(noteEntity);
            runOnUiThread(() -> {
                Toast.makeText(CreatingNoteActivity.this, "Note saved", Toast.LENGTH_SHORT).show();
                finishWithResult();
            });
        });
    }

    private void deleteNote() {
        if (existingNote != null) {
            Executors.newSingleThreadExecutor().execute(() -> {
                NoteDatabase db = NoteDatabase.getInstance(CreatingNoteActivity.this);
                NoteEntity noteEntity = db.noteDao().getNoteByIdDirect(existingNote.getId());
                if (noteEntity != null) {
                    noteEntity.setDeleted(true);
                    db.noteDao().update(noteEntity);
                } else {
                    db.noteDao().deleteNoteById(existingNote.getId());
                }
                runOnUiThread(() -> {
                    Toast.makeText(CreatingNoteActivity.this, "Note moved to bin", Toast.LENGTH_SHORT).show();
                    finishWithResult();
                });
            });
        } else {
            finish();
        }
    }

    private void confirmDeleteNote() {
        if (existingNote == null) {
            finish();
            return;
        }

        View customView = LayoutInflater.from(this).inflate(R.layout.custom_dialog, null);
        TextView titleView = customView.findViewById(R.id.dialog_title);
        TextView msgView = customView.findViewById(R.id.dialog_message);
        MaterialButton positiveBtn = customView.findViewById(R.id.positive_button);
        MaterialButton negativeBtn = customView.findViewById(R.id.negative_button);

        titleView.setText("Move to Bin?");
        msgView.setText("Are you sure you want to move this note to the recycle bin?");
        positiveBtn.setText("Move to Bin");
        positiveBtn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FF4842")));
        negativeBtn.setText("Cancel");

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.AppTheme_Dialog)
                .setView(customView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        positiveBtn.setOnClickListener(v -> {
            deleteNote();
            dialog.dismiss();
        });

        negativeBtn.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void shareNote() {
        String title = inNoteTitle.getText().toString().trim();
        String subtitle = inNoteSubtitle.getText().toString().trim();
        String noteText = inNote.getText().toString().trim();

        if (title.isEmpty() && subtitle.isEmpty() && noteText.isEmpty()) {
            Toast.makeText(this, "Note is empty!", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder sb = new StringBuilder();
        if (!title.isEmpty()) sb.append(title).append("\n\n");
        if (!subtitle.isEmpty()) sb.append(subtitle).append("\n\n");
        if (!noteText.isEmpty()) sb.append(noteText);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, title.isEmpty() ? "Note" : title);
        shareIntent.putExtra(Intent.EXTRA_TEXT, sb.toString());
        startActivity(Intent.createChooser(shareIntent, "Share Note via"));
    }

    private void copyNoteToClipboard() {
        String title = inNoteTitle.getText().toString().trim();
        String subtitle = inNoteSubtitle.getText().toString().trim();
        String noteText = inNote.getText().toString().trim();

        if (title.isEmpty() && subtitle.isEmpty() && noteText.isEmpty()) {
            Toast.makeText(this, "Nothing to copy!", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder sb = new StringBuilder();
        if (!title.isEmpty()) sb.append(title).append("\n\n");
        if (!subtitle.isEmpty()) sb.append(subtitle).append("\n\n");
        if (!noteText.isEmpty()) sb.append(noteText);

        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Note", sb.toString());
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Note text copied to clipboard!", Toast.LENGTH_SHORT).show();
        }
    }

    private void duplicateNote() {
        String title = inNoteTitle.getText().toString().trim();
        String subtitle = inNoteSubtitle.getText().toString().trim();
        String noteText = inNote.getText().toString().trim();

        String currentDateTime = new SimpleDateFormat("EEEE, dd MMMM yyyy HH:mm a", Locale.getDefault()).format(new Date());
        String userId = SessionManager.getUserId(this);

        NoteEntity copyEntity = new NoteEntity(
                UUID.randomUUID().toString(),
                title + " (Copy)",
                subtitle,
                noteText,
                currentDateTime,
                selectNoteColor != null ? selectNoteColor : "#333333",
                localImagePath,
                userId,
                false
        );
        copyEntity.setPinned(isPinned);

        Executors.newSingleThreadExecutor().execute(() -> {
            NoteDatabase.getInstance(this).noteDao().insertOrUpdate(copyEntity);
            runOnUiThread(() -> {
                Toast.makeText(this, "Note duplicated!", Toast.LENGTH_SHORT).show();
                finishWithResult();
            });
        });
    }

    private void showAttachmentBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_attachment, null);
        dialog.setContentView(view);

        view.findViewById(R.id.optionTakePhoto).setOnClickListener(v -> {
            dialog.dismiss();
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CODE_CAMERA);
            } else {
                launchCamera();
            }
        });

        view.findViewById(R.id.optionAddImage).setOnClickListener(v -> {
            dialog.dismiss();
            selectImage();
        });

        dialog.show();
    }

    private void showColorPickerBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_color_picker, null);
        dialog.setContentView(view);

        setupColorItem(view, R.id.btnColor1, R.id.tickColor1, "#333333", dialog, view);
        setupColorItem(view, R.id.btnColor2, R.id.tickColor2, "#FF4842", dialog, view);
        setupColorItem(view, R.id.btnColor3, R.id.tickColor3, "#f4d03f", dialog, view);
        setupColorItem(view, R.id.btnColor4, R.id.tickColor4, "#2196F3", dialog, view);
        setupColorItem(view, R.id.btnColor5, R.id.tickColor5, "#00008B", dialog, view);
        setupColorItem(view, R.id.btnColor6, R.id.tickColor6, "#DE3163", dialog, view);
        setupColorItem(view, R.id.btnColor7, R.id.tickColor7, "#CCCCFF", dialog, view);
        setupColorItem(view, R.id.btnColor8, R.id.tickColor8, "#8e44ad", dialog, view);

        updateColorTicksInSheet(view);
        dialog.show();
    }

    private void setupColorItem(View sheetView, int btnId, int tickId, String colorHex, BottomSheetDialog dialog, View rootSheet) {
        sheetView.findViewById(btnId).setOnClickListener(v -> {
            selectNoteColor = colorHex;
            setSubtitleIndicatorColor();
            updateColorTicksInSheet(rootSheet);
            dialog.dismiss();
        });
    }

    private void updateColorTicksInSheet(View sheetView) {
        int[] ticks = {
                R.id.tickColor1, R.id.tickColor2, R.id.tickColor3, R.id.tickColor4,
                R.id.tickColor5, R.id.tickColor6, R.id.tickColor7, R.id.tickColor8
        };
        String[] colors = {
                "#333333", "#FF4842", "#f4d03f", "#2196F3",
                "#00008B", "#DE3163", "#CCCCFF", "#8e44ad"
        };

        for (int i = 0; i < ticks.length; i++) {
            ImageView tick = sheetView.findViewById(ticks[i]);
            if (tick != null) {
                tick.setVisibility(colors[i].equalsIgnoreCase(selectNoteColor) ? View.VISIBLE : View.GONE);
            }
        }
    }

    private void showMoreOptionsBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_more_options, null);
        dialog.setContentView(view);

        TextView editedDate = view.findViewById(R.id.sheetTextEditedDate);
        if (editedDate != null && date != null) {
            editedDate.setText("Edited " + date.getText().toString());
        }

        view.findViewById(R.id.optionMakeCopy).setOnClickListener(v -> {
            dialog.dismiss();
            duplicateNote();
        });

        view.findViewById(R.id.optionCopyText).setOnClickListener(v -> {
            dialog.dismiss();
            copyNoteToClipboard();
        });

        view.findViewById(R.id.optionShare).setOnClickListener(v -> {
            dialog.dismiss();
            shareNote();
        });

        view.findViewById(R.id.optionDelete).setOnClickListener(v -> {
            dialog.dismiss();
            confirmDeleteNote();
        });

        dialog.show();
    }

    private void setSubtitleIndicatorColor() {
        if (selectNoteColor == null || selectNoteColor.isEmpty()) {
            selectNoteColor = "#333333";
        }
        if (viewSubtitleIndicator != null && viewSubtitleIndicator.getBackground() instanceof GradientDrawable) {
            GradientDrawable gradientDrawable = (GradientDrawable) viewSubtitleIndicator.getBackground();
            gradientDrawable.setColor(Color.parseColor(selectNoteColor));
        }
    }

    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_CODE_SELECT_IMAGE);
    }

    private void launchCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, REQUEST_CODE_CAMERA);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_CAMERA && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else if (requestCode == REQUEST_CODE_STORAGE_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            selectImage();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == REQUEST_CODE_SELECT_IMAGE) {
                selectedImageUri = data.getData();
                if (selectedImageUri != null) {
                    displaySelectedImage(selectedImageUri);
                }
            } else if (requestCode == REQUEST_CODE_CAMERA) {
                Bundle extras = data.getExtras();
                if (extras != null && extras.get("data") instanceof Bitmap) {
                    Bitmap photo = (Bitmap) extras.get("data");
                    saveBitmapLocally(photo);
                }
            }
        }
    }

    private void saveBitmapLocally(Bitmap bitmap) {
        try {
            File file = new File(getFilesDir(), "camera_note_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.flush();
            fos.close();
            localImagePath = file.getAbsolutePath();
            selectedImageUri = Uri.fromFile(file);
            displaySelectedImage(selectedImageUri);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void displaySelectedImage(Uri imageUri) {
        Glide.with(this)
                .load(imageUri.toString())
                .into(imagenote);
        imagenote.setVisibility(View.VISIBLE);
        findViewById(R.id.imageRemoveImage).setVisibility(View.VISIBLE);
    }

    private void finishWithResult() {
        Intent resultIntent = new Intent();
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    private String saveImageLocally(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            File file = new File(getFilesDir(), "note_image_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream outputStream = new FileOutputStream(file);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            outputStream.close();
            if (inputStream != null) {
                inputStream.close();
            }

            localImagePath = file.getAbsolutePath();
            return file.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            return imageUri.toString();
        }
    }
}
