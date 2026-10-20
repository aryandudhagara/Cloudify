package com.example.notes_app.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.example.notes_app.R;
import com.example.notes_app.Models.Notes;
import com.example.notes_app.SessionManager;
import com.example.notes_app.adapter.BinAdapter;
import com.example.notes_app.dao.NoteDao;
import com.example.notes_app.database.NoteDatabase;
import com.example.notes_app.entities.NoteEntity;
import com.example.notes_app.listeners.BinNotesListener;
import com.google.android.material.bottomappbar.BottomAppBar;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class Bin_fragment extends Fragment implements BinNotesListener {

    private RecyclerView binRecyclerView;
    private BinAdapter binAdapter;
    private List<Notes> deletedNotes = new ArrayList<>();
    private NoteDao noteDao;

    private ImageView ClearNotes;
    private EditText searchEditText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_bin_fragment, container, false);

        setHasOptionsMenu(true);

        NoteDatabase appDatabase = NoteDatabase.getInstance(requireContext());
        noteDao = appDatabase.noteDao();

        binRecyclerView = view.findViewById(R.id.binRecyclerView);
        binRecyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, LinearLayoutManager.VERTICAL));
        binAdapter = new BinAdapter(this);
        binRecyclerView.setAdapter(binAdapter);
        ClearNotes = view.findViewById(R.id.clearnotes);

        searchEditText = view.findViewById(R.id.searchBinEditText);
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                performSearch(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().isEmpty()) {
                    loadDeletedNotesFromRoom();
                }
            }
        });

        if (ClearNotes != null) {
            ClearNotes.setOnClickListener(v -> clearAllDeletedNotes());
        }

        loadDeletedNotesFromRoom();

        return view;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.bottom_nav_bin, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.clear_notes) {
            clearAllDeletedNotes();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void clearAllDeletedNotes() {
        if (getContext() == null || noteDao == null) return;
        String userId = SessionManager.getUserId(requireContext());

        Executors.newSingleThreadExecutor().execute(() -> {
            noteDao.deleteDeletedNotesForUser(userId);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "All deleted notes cleared", Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void loadDeletedNotesFromRoom() {
        if (getContext() == null || noteDao == null) return;
        String userId = SessionManager.getUserId(requireContext());

        noteDao.getDeletedNotesByUserId(userId).observe(getViewLifecycleOwner(), noteEntities -> {
            List<Notes> notes = convertNoteEntitiesToNotes(noteEntities);
            updateDeletedNotes(notes);
        });
    }

    private void updateDeletedNotes(List<Notes> newDeletedNotes) {
        deletedNotes.clear();
        deletedNotes.addAll(newDeletedNotes);
        binAdapter.setDeletedNotes(deletedNotes);
        binAdapter.notifyDataSetChanged();
    }

    @Override
    public void onNoteClicked(Notes note, int position) {
    }

    @Override
    public void onRestoreClicked(Notes note, int position) {
        restoreNoteFromRoom(note);
    }

    @Override
    public void onDeleteClicked(Notes note, int position) {
        permanentlyDeleteNoteFromRoom(note);
    }

    private void restoreNoteFromRoom(Notes note) {
        Executors.newSingleThreadExecutor().execute(() -> {
            NoteEntity entity = noteDao.getNoteByIdDirect(note.getId());
            if (entity != null) {
                entity.setDeleted(false);
                noteDao.update(entity);
            }
            if (getActivity() != null) {
                getActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Note restored", Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void permanentlyDeleteNoteFromRoom(Notes note) {
        Executors.newSingleThreadExecutor().execute(() -> {
            noteDao.deleteNoteById(note.getId());
            if (getActivity() != null) {
                getActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Note deleted permanently", Toast.LENGTH_SHORT).show()
                );
            }
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

    private void performSearch(String searchQuery) {
        if (getContext() == null || noteDao == null) return;
        String userId = SessionManager.getUserId(requireContext());

        LiveData<List<NoteEntity>> liveDataNotes = noteDao.searchDeletedNotesForUser(userId, searchQuery);
        liveDataNotes.observe(getViewLifecycleOwner(), noteEntities -> {
            List<Notes> notes = convertNoteEntitiesToNotes(noteEntities);
            updateDeletedNotes(notes);
        });
    }
}
