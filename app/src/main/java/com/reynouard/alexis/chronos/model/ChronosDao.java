package com.reynouard.alexis.chronos.model;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Transaction;
import android.arch.persistence.room.Update;

import java.util.List;

import static android.arch.persistence.room.OnConflictStrategy.REPLACE;

/**
 * Do not use directly to edit: delete, insert nor update.
 * Use ChronosViewModel instead.
 */
@Dao
public abstract class ChronosDao {

    private static final String SQL_SECONDS_SINCE_REFERENCE_DATE = " (strftime('%s','now') - strftime('%s',W.mReferenceDate))";
    private static final String SQL_DAYS_SINCE_REFERENCE_DATE = " (" + SQL_SECONDS_SINCE_REFERENCE_DATE + "/ (24 * 60 * 60.0))";
    private static final String SQL_PERIOD_IN_SECOND = " ((T.mHyperPeriod * 24 * 60 * 60.0) / (T.mRepetition))";
    private static final String SQL_URGENCY = " CASE WHEN mRepetitive" +
            " THEN (0.0 +" + SQL_SECONDS_SINCE_REFERENCE_DATE + ") /" + SQL_PERIOD_IN_SECOND +
            " ELSE CASE WHEN" + SQL_SECONDS_SINCE_REFERENCE_DATE + " >= 0" +
                " THEN " + SQL_DAYS_SINCE_REFERENCE_DATE + " + 2" +
                " ELSE (1.0 / -(" + SQL_DAYS_SINCE_REFERENCE_DATE + " - 0.5)) END END";

    @Transaction
    public void clearAll() {
        clearAllWorks();
        clearAllTasks();
    }

    @Insert(onConflict = REPLACE)
    public abstract long insertTask(Task task);

    @Update
    public abstract void updateTask(Task task);

    @Delete
    public abstract void deleteTask(Task task);

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
            "  W.mReferenceDate as mReferenceDate" +
            " FROM" +
            "  Task as T" +
            "  LEFT OUTER JOIN (SELECT mTaskId, max(mDate) as mReferenceDate FROM Work GROUP BY mTaskId) as W" +
            "  ON (T.mId = W.mTaskId)" +
            " WHERE mId = :taskId" +
            " ORDER BY" +
            "  CASE WHEN mReferenceDate IS NULL THEN 0 ELSE 1 END," +
            "  " + SQL_URGENCY + " DESC")
    public abstract LiveData<StatedTask> getTask(long taskId);

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
            "  W.mReferenceDate as mReferenceDate" +
            " FROM" +
            "  Task as T" +
            "  LEFT OUTER JOIN (SELECT mTaskId, max(mDate) as mReferenceDate FROM Work GROUP BY mTaskId) as W" +
            "  ON (T.mId = W.mTaskId)" +
            " ORDER BY" +
            "  CASE WHEN mReferenceDate IS NULL THEN 0 ELSE 1 END," +
            "  " + SQL_URGENCY + " DESC")
    public abstract LiveData<List<StatedTask>> getTasks();

    @Query("DELETE FROM Task")
    public abstract void clearAllTasks();

    @Insert(onConflict = REPLACE)
    public abstract void insertWork(Work work);

    @Delete
    public abstract void deleteWork(Work work);

    @Query("DELETE FROM Work WHERE mTaskId = :taskId")
    public abstract void deleteWorks(Long taskId);

    @Query("SELECT * FROM Work ORDER BY mDate DESC")
    public abstract LiveData<List<Work>> getWorks();

    @Query("SELECT * FROM Work WHERE mTaskId = :taskId ORDER BY mDate DESC")
    public abstract LiveData<List<Work>> getWorksForTask(long taskId);

    @Query("SELECT" +
            "  SUM(T.mDuration)" +
            " FROM" +
            "  (SELECT mTaskId from Work" +
            "   WHERE datetime('now', 'start of day') <= mDate" +
            "     AND mDate < datetime('now', '+1 day', 'start of day')) as W" +
            "  INNER JOIN Task as T" +
            "  ON (W.mTaskId = T.mId)")
    public abstract LiveData<Integer> getDurationDoneToday();

    @Query("DELETE FROM Work")
    public abstract void clearAllWorks();
}
