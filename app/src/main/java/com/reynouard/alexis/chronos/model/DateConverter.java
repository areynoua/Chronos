package com.reynouard.alexis.chronos.model;

import android.annotation.SuppressLint;
import android.arch.persistence.room.TypeConverter;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateConverter {
    @SuppressLint("SimpleDateFormat")
    private static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    @TypeConverter
    public static synchronized Date fromIsoFormat(String iso) {
        if (iso == null) {
            return null;
        }
        try {
            return format.parse(iso);
        } catch (ParseException e) {
            Log.e(Date.class.getSimpleName(), "Unable to convert String to date " + iso);
            return null;
        }

    }

    @TypeConverter
    public static synchronized String fromDate(Date date) {
        return date ==  null
                ? null
                : format.format(date);
    }

    /*
    @TypeConverter
    public static synchronized Date fromTime(long time) {
        return new Date(time);
    }

    @TypeConverter
    public static synchronized long fromDate(Date date) {
        return date.getTime();
    }
    */
}
