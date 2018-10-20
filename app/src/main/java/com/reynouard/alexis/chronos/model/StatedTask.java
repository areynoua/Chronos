package com.reynouard.alexis.chronos.model;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.TypeConverters;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.reynouard.alexis.chronos.utils.DateTimeUtils;

import java.util.Calendar;
import java.util.Date;

public class StatedTask extends Task {
    @Nullable
    @TypeConverters(DateConverter.class)
    @ColumnInfo(name = "mLastDoneDate")
    private transient Date mLastDoneDate;

    public StatedTask(long id, @NonNull String name, String description, int repetition, int hyperPeriod, int
            duration, boolean durationFlexible, boolean important, boolean repetitive, @Nullable Date lastDoneDate) {
        super(id, name, description, repetition, hyperPeriod, duration, durationFlexible, important, repetitive);
        mLastDoneDate = lastDoneDate;
    }

    @Nullable
    public Date getLastDoneDate() {
        return mLastDoneDate;
    }

    @Nullable
    public Double getUrgency() {
        return getWaited() == null
                ? null
                : getWaited() / getPeriod();
    }

    @Nullable
    public Double getWaited() {
        return mLastDoneDate == null
                ? null
                : (double) (new Date().getTime() - mLastDoneDate.getTime()) / (24*60*60*1000);
    }

    @Nullable
    public Double getDaysToNextOccurrence() {
        return getWaited() == null
                ? null
                : getPeriod() - getWaited();
    }

    @Nullable
    public Integer getNaturalDaysCountToNextOccurrence() {
        Double toNext = getDaysToNextOccurrence();
        if (toNext == null) {
            return null;
        }
        else {
            Calendar next = Calendar.getInstance();
            next.add(Calendar.SECOND, (int) (toNext * 24 * 60 * 60));
            return DateTimeUtils.naturalDaysDelta(next);
        }
    }

}
