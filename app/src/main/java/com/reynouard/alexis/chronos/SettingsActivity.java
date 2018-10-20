package com.reynouard.alexis.chronos;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import com.reynouard.alexis.chronos.utils.DateTimeUtils;

import java.util.concurrent.TimeUnit;

public class SettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String PREF_TIME_BY_DAY_PREFIX = "pref_free_time_";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        initTimeByDayPreferences(sharedPreferences);
        initVacanciesByYear(sharedPreferences);
    }

    private void initVacanciesByYear(SharedPreferences sharedPreferences) {
        final String[] keys = new String[]{"pref_holiday_complete_weeks", "pref_holiday_weekends"};
        for (String key : keys) {
            findPreference(key).setSummary(String.valueOf(sharedPreferences.getInt(key, 0)));
        }
    }

    private void initTimeByDayPreferences(SharedPreferences sharedPreferences) {
        for (String dayName : DateTimeUtils.dayNames()) {
            String key = PREF_TIME_BY_DAY_PREFIX + dayName;
            updateTimeByDaySummary(sharedPreferences, key);
            ((EditTextPreference) findPreference(key)).setDialogTitle(String.format(getString(R.string.time_for_day), dayName));
        }
    }

    private void updateTimeByDaySummary(SharedPreferences sharedPreferences, String key) {
        findPreference(key).setSummary(DateTimeUtils.durationString(this, sharedPreferences.getInt(key, 0), TimeUnit.MINUTES));
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.startsWith(PREF_TIME_BY_DAY_PREFIX)) {
            updateTimeByDaySummary(sharedPreferences, key);
        }
    }
}
