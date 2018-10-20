package com.reynouard.alexis.chronos.model;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.TypeConverters;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.Date;

import static android.arch.persistence.room.ForeignKey.CASCADE;

@Entity(
        foreignKeys = @ForeignKey(
                entity = Task.class,
                parentColumns = "mId",
                childColumns = "mTaskId",
                onDelete = CASCADE
        ),
        indices = {@Index("mTaskId")}
)
public final class Work {

    @PrimaryKey(autoGenerate = true)
    private final long mId;
    private final long mTaskId;
    @NonNull
    @TypeConverters(DateConverter.class)
    private Date mDate = new Date();

    public Work(long id, long taskId, @NonNull Date date) {
        mId = id;
        mTaskId = taskId;
        mDate = date;
        Log.d("Work","Work(" + mId + ", " + mTaskId + ", " + mDate + ")");
        if (!DateConverter.fromIsoFormat(DateConverter.fromDate(date)).equals(date))throw new AssertionError();
    }

    public long getId() {
        return mId;
    }

    public long getTaskId() {
        return mTaskId;
    }

    @NonNull
    public Date getDate() {
        return mDate;
    }
}
