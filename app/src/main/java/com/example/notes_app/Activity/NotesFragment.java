package com.example.notes_app.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.example.notes_app.Models.Notes;
import com.example.notes_app.R;
import com.example.notes_app.SessionManager;
import com.example.notes_app.adapter.recayclerviewAdapter;
import com.example.notes_app.bottomSheetFragment.CreateTaskBottomSheetFragment;
import com.example.notes_app.dao.NoteDao;
import com.example.notes_app.database.NoteDatabase;
import com.example.notes_app.databinding.ActivityNotesFragmentBinding;
import com.example.notes_app.entities.NoteEntity;
import com.example.notes_app.listeners.NotesListner;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class NotesFragment extends Fragment implements NotesListner {

    private recayclerviewAdapter notesAdapter;
    private NoteDao noteDao;
    private ActivityNotesFragmentBinding binding;
    private GestureDetector gestureDetector;
    private SwipeRefreshLayout swipeRefreshLayout;
    private List<Notes> notesList = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = ActivityNotesFragmentBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeRoomDatabase();
        setupRecyclerView();
        setupSearchFunctionality();
        loadNotesFromRoom();

        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(() -> {
                loadNotesFromRoom();
                swipeRefreshLayout.setRefreshing(false);
            });
        }

        FloatingActionButton optionOne = binding.optionOne;
        FloatingActionButton optionTwo = binding.optionTwo;
        FloatingActionsMenu fabMenu = binding.layoutquickactiontodo;

        optionOne.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), CreatingNoteActivity.class);
            startActivity(intent);
            fabMenu.collapse();
        });

        optionTwo.setOnClickListener(v -> {
            CreateTaskBottomSheetFragment fragment = new CreateTaskBottomSheetFragment();
            fragment.show(getChildFragmentManager(), "CreateTaskBottomSheet");
            fabMenu.collapse();
        });

        Glide.with(this)
                .load(R.drawable.add_first_note)
                .into(binding.noDataImagenotes);
    }

    private void initializeRoomDatabase() {
        NoteDatabase appDatabase = NoteDatabase.getInstance(getContext());
        noteDao = appDatabase.noteDao();
    }

    private void setupRecyclerView() {
        binding.noteRecyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, LinearLayoutManager.VERTICAL));
        notesList = new ArrayList<>();
        notesAdapter = new recayclerviewAdapter(this, this);
        binding.noteRecyclerView.setAdapter(notesAdapter);
    }

    private void setupSearchFunctionality() {
        binding.searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                performSearch(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().isEmpty()) {
                    loadNotesFromRoom();
                }
            }
        });
    }

    private void loadNotesFromRoom() {
        if (getContext() == null || noteDao == null) return;
        String userId = SessionManager.getUserId(requireContext());

        noteDao.getActiveNotesByUserId(userId).observe(getViewLifecycleOwner(), noteEntities -> {
            List<Notes> notes = convertNoteEntitiesToNotes(noteEntities);
            updateNotesAdapter(notes);

            if (binding != null && binding.noDataImagenotes != null) {
                if (notes.isEmpty()) {
                    binding.noDataImagenotes.setVisibility(View.VISIBLE);
                } else {
                    binding.noDataImagenotes.setVisibility(View.GONE);
                }
            }
        });
    }

    private void performSearch(String searchQuery) {
        if (getContext() == null || noteDao == null) return;
        String userId = SessionManager.getUserId(requireContext());

        LiveData<List<NoteEntity>> liveDataNotes = noteDao.searchNotesForUser(userId, searchQuery);
        liveDataNotes.observe(getViewLifecycleOwner(), noteEntities -> {
            List<Notes> notes = convertNoteEntitiesToNotes(noteEntities);
            updateNotesAdapter(notes);
        });
    }

    private List<Notes> convertNoteEntitiesToNotes(List<NoteEntity> noteEntities) {
        List<Notes> notes = new ArrayList<>();
        if (noteEntities != null) {
            for (NoteEntity noteEntity : noteEntities) {
                Notes note = new Notes(noteEntity.getId(), noteEntity.getTitle(), noteEntity.getSubtitle(),
                        noteEntity.getNote(), noteEntity.getDate(), noteEntity.getColor(),
                        noteEntity.getImageUrl(), noteEntity.getUserId(), noteEntity.isDeleted());
                note.setPinned(noteEntity.isPinned());
                notes.add(note);
            }
        }
        return notes;
    }

    private void updateNotesAdapter(List<Notes> notesList) {
        this.notesList = notesList;
        notesAdapter.setNotes(notesList);
        notesAdapter.notifyDataSetChanged();
    }

    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onNoteClicked(Notes note, int position) {
        Intent intent = new Intent(getContext(), CreatingNoteActivity.class);
        intent.putExtra("note", note);
        intent.putExtra("isViewOrUpdate", true);
        startActivity(intent);
    }

    @Override
    public void onNoteLongClicked(Notes note, int position) {
    }

    @Override
    public void onRequestDeleteNotes(List<Notes> selectedNotes) {
        if (selectedNotes.isEmpty()) return;
        showDeleteConfirmationDialog(new ArrayList<>(selectedNotes));
    }

    private void showDeleteConfirmationDialog(List<Notes> selectedNotes) {
        View customView = LayoutInflater.from(getContext()).inflate(R.layout.custom_dialog, null);
        android.widget.TextView dialogMessage = customView.findViewById(R.id.dialog_message);
        if (dialogMessage != null) {
            dialogMessage.setText(R.string.are_you_sure_you_want_to_delete_this_note);
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.AppTheme_Dialog)
                .setView(customView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        View positiveButton = customView.findViewById(R.id.positive_button);
        View negativeButton = customView.findViewById(R.id.negative_button);

        if (positiveButton != null) {
            positiveButton.setOnClickListener(v -> {
                deleteSelectedNotes(selectedNotes);
                dialog.dismiss();
            });
        }

        if (negativeButton != null) {
            negativeButton.setOnClickListener(v -> dialog.cancel());
        }

        dialog.show();
    }

    private void deleteSelectedNotes(List<Notes> selectedNotes) {
        if (selectedNotes.isEmpty()) return;

        Executors.newSingleThreadExecutor().execute(() -> {
            for (Notes note : selectedNotes) {
                NoteEntity entity = noteDao.getNoteByIdDirect(note.getId());
                if (entity != null) {
                    entity.setDeleted(true);
                    noteDao.update(entity);
                }
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    notesAdapter.removeSelectedNotes(selectedNotes);
                    showToast("Notes moved to bin");
                });
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
