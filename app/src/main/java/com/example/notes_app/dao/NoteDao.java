package com.example.notes_app.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import com.example.notes_app.entities.NoteEntity;

import java.util.List;

@Dao
public interface NoteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(NoteEntity note);

    @Insert
    void insertNote(NoteEntity note);

    @Update
    void update(NoteEntity note);

    @Delete
    void delete(NoteEntity note);

    @Query("DELETE FROM notes")
    void deleteAllNotes();

    @Query("DELETE FROM notes WHERE userId = :userId")
    void deleteAllNotesForUser(String userId);

    @Query("SELECT * FROM notes")
    LiveData<List<NoteEntity>> getAllNotes();

    @Query("SELECT * FROM notes WHERE id = :noteId LIMIT 1")
    NoteEntity getNoteByIdDirect(String noteId);

    @Query("SELECT * FROM notes WHERE id = :noteId")
    LiveData<NoteEntity> getNoteById(String noteId);

    @Query("SELECT * FROM notes WHERE userId = :userId")
    LiveData<List<NoteEntity>> getNotesByUserId(String userId);

    @Query("SELECT * FROM notes WHERE userId = :userId AND deleted = 0 ORDER BY isPinned DESC, id DESC")
    LiveData<List<NoteEntity>> getActiveNotesByUserId(String userId);

    @Query("SELECT * FROM notes WHERE userId = :userId AND deleted = 1 ORDER BY id DESC")
    LiveData<List<NoteEntity>> getDeletedNotesByUserId(String userId);

    @Query("SELECT * FROM notes WHERE userId = :userId AND deleted = 0 ORDER BY isPinned DESC, id DESC")
    List<NoteEntity> getActiveNotesListForUser(String userId);

    @Query("SELECT * FROM notes WHERE userId = :userId AND deleted = 1 ORDER BY id DESC")
    List<NoteEntity> getDeletedNotesListForUser(String userId);

    @Query("SELECT * FROM notes WHERE deleted = :includeDeleted")
    LiveData<List<NoteEntity>> getNotesByDeletionStatus(boolean includeDeleted);

    @Query("SELECT * FROM notes WHERE (title LIKE '%' || :searchQuery || '%' OR subtitle LIKE '%' || :searchQuery || '%' OR note LIKE '%' || :searchQuery || '%') AND deleted = 0 ORDER BY isPinned DESC, id DESC")
    LiveData<List<NoteEntity>> searchNotes(String searchQuery);

    @Query("SELECT * FROM notes WHERE userId = :userId AND (title LIKE '%' || :searchQuery || '%' OR subtitle LIKE '%' || :searchQuery || '%' OR note LIKE '%' || :searchQuery || '%') AND deleted = 0 ORDER BY isPinned DESC, id DESC")
    LiveData<List<NoteEntity>> searchNotesForUser(String userId, String searchQuery);

    // Pagination and sorting
    @Query("SELECT * FROM notes ORDER BY title ASC LIMIT :limit OFFSET :offset")
    LiveData<List<NoteEntity>> getNotesWithPagination(int limit, int offset);

    // Count notes by userId
    @Query("SELECT COUNT(*) FROM notes WHERE userId = :userId AND deleted = 0")
    LiveData<Integer> countNotesByUserId(String userId);

    @Query("SELECT COUNT(*) FROM notes WHERE userId = :userId AND deleted = 0")
    int countNotesForUserSync(String userId);

    // Count all notes
    @Query("SELECT COUNT(*) FROM notes")
    LiveData<Integer> countAllNotes();

    @Query("DELETE FROM notes WHERE id = :noteId")
    void deleteNoteById(String noteId);

    @Query("DELETE FROM notes WHERE userId = :userId AND deleted = 1")
    void deleteDeletedNotesForUser(String userId);

    @Query("SELECT * FROM notes WHERE deleted = 1 AND (title LIKE '%' || :searchQuery || '%' OR note LIKE '%' || :searchQuery || '%')")
    LiveData<List<NoteEntity>> searchDeletedNotes(String searchQuery);

    @Query("SELECT * FROM notes WHERE userId = :userId AND deleted = 1 AND (title LIKE '%' || :searchQuery || '%' OR note LIKE '%' || :searchQuery || '%')")
    LiveData<List<NoteEntity>> searchDeletedNotesForUser(String userId, String searchQuery);

    @Query("DELETE FROM notes WHERE id IN (:noteIds)")
    void deleteNotesByIds(List<String> noteIds);
}
