package com.reynouard.alexis.chronos.model;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

@Entity
public class Task {
    @PrimaryKey(autoGenerate = true)
    private long mId;
    @NonNull
    private String mName;
    private String mDescription;
    private int mRepetition;
    private int mHyperPeriod;
    private int mDuration;
    private boolean mDurationFlexible;
    private boolean mImportant;
    private boolean mRepetitive;

    public Task(long id, @NonNull String name, String description, int repetition, int hyperPeriod,
                int duration, boolean durationFlexible, boolean important, boolean repetitive) {
        if (repetition < 1) throw new AssertionError(); // TODO
        if (hyperPeriod < 1) throw new AssertionError(); // TODO
        if (!repetitive && repetition != 1) throw new AssertionError(); // TODO
        mId = id;
        mName = name;
        mDescription = description;
        mRepetition = repetition;
        mHyperPeriod = hyperPeriod;
        mDuration = duration;
        mDurationFlexible = durationFlexible;
        mImportant = important;
        mRepetitive = repetitive;
    }

    public long getId() {
        return mId;
    }

    @NonNull
    public String getName() {
        return mName;
    }

    public String getDescription() {
        return mDescription;
    }

    public int getRepetition() {
        return mRepetition;
    }

    public int getHyperPeriod() {
        return mHyperPeriod;
    }

    public int getDuration() {
        return mDuration;
    }

    public boolean isDurationFlexible() {
        return mDurationFlexible;
    }

    public boolean isImportant() {
        return mImportant;
    }

    public boolean isRepetitive() {
        return mRepetitive;
    }

    public double getPeriod() {
        return ((double) getHyperPeriod()) / getRepetition();
    }

    public double getFrequency() {
        return ((double) getRepetition()) / getHyperPeriod();
    }
}
