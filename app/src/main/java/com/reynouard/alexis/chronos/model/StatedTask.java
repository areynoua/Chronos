package com.reynouard.alexis.chronos.model;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.TypeConverters;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.reynouard.alexis.chronos.utils.DateTimeUtils;

import java.util.Calendar;
import java.util.Date;

public class StatedTask extends Task {
    @Nullable
    @TypeConverters(DateConverter.class)
    @ColumnInfo(name = "mReferenceDate")
    private transient Date mReferenceDate; // if repetitive then lastDoneDate else dueDate

    public StatedTask(long id, @NonNull String name, String description, int repetition, int hyperPeriod, int
            duration, boolean durationFlexible, boolean important, boolean repetitive, @Nullable Date referenceDate) {
        super(id, name, description, repetition, hyperPeriod, duration, durationFlexible, important, repetitive);
        //if (!repetitive && referenceDate == null) throw new AssertionError(); // TODO
        mReferenceDate = referenceDate;
    }

    @Nullable
    public Date getReferenceDate() {
        return mReferenceDate;
    }

    @Nullable
    public Double getUrgency() {
        if (isRepetitive()) {
            return getWaited() == null
                    ? null
                    : getWaited() / getPeriod();
        } else {
            double days = getDaysToNextOccurrence();
            if (days < 0) {
                return (-days) + 2;
            } else {
                return (1.0 / (days + 1)) + 1;
            }
        }
    }

    @Nullable
    private Double getDaysSinceReferenceDate() {
        return mReferenceDate == null
                ? null
                : (double) (new Date().getTime() - mReferenceDate.getTime()) / (24 * 60 * 60 * 1000);
    }

    @Nullable
    public Double getWaited() {
        if (!isRepetitive()) {
            Log.w("tasks", "getWaited: called on non-repetitive task");
            return null;
        } else {
            return getDaysSinceReferenceDate();
        }
    }

    @Nullable
    public Double getDaysToNextOccurrence() {
        if (isRepetitive()) {
            return getWaited() == null
                    ? null
                    : getPeriod() - getWaited();
        } else {
            return -getDaysSinceReferenceDate();
        }
    }

    @Nullable
    public Integer getNaturalDaysCountToNextOccurrence() {
        Double toNext = getDaysToNextOccurrence();
        if (toNext == null) {
            return null;
        } else {
            Calendar next = Calendar.getInstance();
            next.add(Calendar.SECOND, (int) (toNext * 24 * 60 * 60));
            return DateTimeUtils.naturalDaysDelta(next);
        }
    }

}
