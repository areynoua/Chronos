package com.reynouard.alexis.chronos.utils;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.SparseArray;

import com.reynouard.alexis.chronos.R;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static java.util.Calendar.FRIDAY;
import static java.util.Calendar.MONDAY;
import static java.util.Calendar.SATURDAY;
import static java.util.Calendar.SUNDAY;
import static java.util.Calendar.THURSDAY;
import static java.util.Calendar.TUESDAY;
import static java.util.Calendar.WEDNESDAY;

public final class DateTimeUtils {

    public static final SparseArray<String> DAY_OF_WEEK_NAMES = new SparseArray<>();
    public static final Map<String, Integer> DAY_OF_WEEK_VALUES = new HashMap<>();

    static {
        DAY_OF_WEEK_NAMES.put(SUNDAY, "sunday");
        DAY_OF_WEEK_NAMES.put(MONDAY, "monday");
        DAY_OF_WEEK_NAMES.put(TUESDAY, "tuesday");
        DAY_OF_WEEK_NAMES.put(WEDNESDAY, "wednesday");
        DAY_OF_WEEK_NAMES.put(THURSDAY, "thursday");
        DAY_OF_WEEK_NAMES.put(FRIDAY, "friday");
        DAY_OF_WEEK_NAMES.put(SATURDAY, "saturday");

        DAY_OF_WEEK_VALUES.put("sunday", SUNDAY);
        DAY_OF_WEEK_VALUES.put("monday", MONDAY);
        DAY_OF_WEEK_VALUES.put("tuesday", TUESDAY);
        DAY_OF_WEEK_VALUES.put("wednesday", WEDNESDAY);
        DAY_OF_WEEK_VALUES.put("thursday", THURSDAY);
        DAY_OF_WEEK_VALUES.put("friday", FRIDAY);
        DAY_OF_WEEK_VALUES.put("saturday", SATURDAY);
    }

    public static Calendar midnight(@NonNull final Calendar date) {
        Calendar res = (Calendar) date.clone();
        res.set(Calendar.HOUR_OF_DAY, 0);
        res.set(Calendar.MINUTE, 0);
        res.set(Calendar.SECOND, 0);
        res.set(Calendar.MILLISECOND, 0);
        return res;
    }

    public static int naturalDaysDelta(@NonNull final Calendar first, @NonNull final Calendar second) {
        return (int) TimeUnit.MILLISECONDS.toDays(
                midnight(second).getTimeInMillis() - midnight(first).getTimeInMillis());
    }

    public static int naturalDaysDelta(@NonNull final Calendar date) {
        return naturalDaysDelta(Calendar.getInstance(), date);
    }

    public static String durationString(Context context, long value, TimeUnit timeUnit) {
        return String.format(
                Locale.getDefault(),
                context.getString(R.string.duration_format),
                timeUnit.toHours(value), timeUnit.toMinutes(value) % 60);
    }

    public static Set<String> dayNames() {
        return DAY_OF_WEEK_VALUES.keySet();
    }
}
