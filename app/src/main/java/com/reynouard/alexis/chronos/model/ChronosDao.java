package com.reynouard.alexis.chronos.model;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

import static android.arch.persistence.room.OnConflictStrategy.REPLACE;

@Dao
public interface ChronosDao {

    @Insert(onConflict = REPLACE)
    long insertTask(Task task);

    @Update
    void updateTask(Task task);

    @Delete
    void deleteTask(Task task);

    @Query("SELECT" +
            "  T.mId as mId," +
            "  T.mName as mName," +
            "  T.mDescription as mDescription," +
            "  T.mRepetition as mRepetition," +
            "  T.mHyperPeriod as mHyperPeriod," +
            "  T.mDuration as mDuration," +
            "  T.mDurationFlexible as mDurationFlexible," +
            "  T.mImportant as mImportant," +
            "  T.mRepetitive as mRepetitive," +
            "  W.mLastDoneDate as mLastDoneDate" +
            " FROM" +
            "  Task as T" +
            "  LEFT OUTER JOIN (SELECT mTaskId, max(mDate) as mLastDoneDate FROM Work GROUP BY mTaskId) as W" +
            "  ON (T.mId = W.mTaskId)" +
            " WHERE mId = :taskId" +
            " ORDER BY" +
            "  CASE WHEN mLastDoneDate IS NULL THEN 0 ELSE 1 END," +
            "  (julianday('now') - julianday(W.mLastDoneDate)) / (T.mHyperPeriod/T.mRepetition) DESC")
    LiveData<StatedTask> getTask(long taskId);

    @Query("SELECT" +
            "  T.mId as mId," +
            "  T.mName as mName," +
            "  T.mDescription as mDescription," +
            "  T.mRepetition as mRepetition," +
            "  T.mHyperPeriod as mHyperPeriod," +
            "  T.mDuration as mDuration," +
            "  T.mDurationFlexible as mDurationFlexible," +
            "  T.mImportant as mImportant," +
            "  T.mRepetitive as mRepetitive," +
            "  W.mLastDoneDate as mLastDoneDate" +
            " FROM" +
            "  Task as T" +
            "  LEFT OUTER JOIN (SELECT mTaskId, max(mDate) as mLastDoneDate FROM Work GROUP BY mTaskId) as W" +
            "  ON (T.mId = W.mTaskId)" +
            " ORDER BY" +
            "  CASE WHEN mLastDoneDate IS NULL THEN 0 ELSE 1 END," +
            "  (julianday('now') - julianday(W.mLastDoneDate)) / (T.mHyperPeriod/T.mRepetition) DESC")
    LiveData<List<StatedTask>> getTasks();

    @Insert(onConflict = REPLACE)
    void insertWork(Work work);

    @Delete
    void deleteWork(Work work);

    @Query("SELECT * FROM Work ORDER BY mDate DESC")
    LiveData<List<Work>> getWorks();

    @Query("SELECT * FROM Work WHERE mTaskId = :taskId ORDER BY mDate DESC")
    LiveData<List<Work>> getWorksForTask(long taskId);

    @Query("SELECT" +
            "  SUM(T.mDuration)" +
            " FROM" +
            "  (SELECT mTaskId from Work" +
            "   WHERE datetime('now', 'start of day') <= mDate" +
            "     AND mDate < datetime('now', '+1 day', 'start of day')) as W" +
            "  INNER JOIN Task as T" +
            "  ON (W.mTaskId = T.mId)")
    LiveData<Integer> getDurationDoneToday();
}
