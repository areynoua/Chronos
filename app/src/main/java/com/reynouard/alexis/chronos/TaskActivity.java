package com.reynouard.alexis.chronos;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;

import com.reynouard.alexis.chronos.model.StatedTask;
import com.reynouard.alexis.chronos.model.Task;
import com.reynouard.alexis.chronos.model.Work;
import com.reynouard.alexis.chronos.view.OnDeleteWorkRequestedListener;
import com.reynouard.alexis.chronos.view.WorkListAdapter;
import com.reynouard.alexis.chronos.viewModel.ChronosViewModel;

import java.util.Calendar;
import java.util.List;

import static android.content.DialogInterface.BUTTON_POSITIVE;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class TaskActivity extends AppCompatActivity implements View.OnClickListener, DialogInterface.OnClickListener, OnDeleteWorkRequestedListener, ChronosViewModel.OnTaskModifiedListener {

    public static final String EXTRA_ID = "com.reynouard.alexis.chronos.TaskActivity.id";

    private View mOkButton;
    private View mCancelButton;
    private View mAddLogEntryButton;
    private EditText mNameView;
    private EditText mDescriptionView;
    private EditText mRepetitionView;
    private EditText mHyperPeriodView;
    private EditText mDurationView;
    private CheckBox mFlexibleDurationView;
    private CheckBox mImportantTaskView;
    private CheckBox mRepetitiveTaskView;

    private ChronosViewModel mChronosViewModel;

    private Long mTaskId = 0L;

    private DatePicker mDatePicker = null;
    private TimePicker mTimePicker = null;
    private Calendar mDatePicked = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        setContentView(R.layout.activity_task);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mOkButton = findViewById(R.id.view_ok_button);
        mCancelButton = findViewById(R.id.view_cancel_button);
        mAddLogEntryButton = findViewById(R.id.view_add_log_entry_button);
        mNameView = findViewById(R.id.view_task_name);
        mDescriptionView = findViewById(R.id.view_task_description);
        mRepetitionView = findViewById(R.id.view_task_repetition);
        mHyperPeriodView = findViewById(R.id.view_task_hyper_period);
        mDurationView = findViewById(R.id.view_task_duration);
        mFlexibleDurationView = findViewById(R.id.view_task_flexible_duration);
        mImportantTaskView = findViewById(R.id.view_task_important);
        mRepetitiveTaskView = findViewById(R.id.view_task_repetitive);

        mChronosViewModel = ViewModelProviders.of(this).get(ChronosViewModel.class);

        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.getLong(EXTRA_ID, 0L) != 0) {
            mTaskId = extras.getLong(EXTRA_ID);
            mNameView.setText(R.string.loading);
            mChronosViewModel.getTask(mTaskId).observe(this, new Observer<StatedTask>() {
                @Override
                public void onChanged(@Nullable StatedTask task) {
                    if (task == null) throw new AssertionError(); // TODO
                    mNameView.setText(task.getName());
                    mDescriptionView.setText(task.getDescription());
                    mRepetitionView.setText(String.valueOf(task.getRepetition()));
                    mHyperPeriodView.setText(String.valueOf(task.getHyperPeriod()));
                    mDurationView.setText(String.valueOf(task.getDuration()));
                    mFlexibleDurationView.setChecked(task.isDurationFlexible());
                    mImportantTaskView.setChecked(task.isImportant());
                    mRepetitiveTaskView.setChecked(task.isRepetitive());

                    if (!task.isRepetitive()) {
                        if (task.getRepetition() != 1) throw new AssertionError(); // TODO
                        Integer dueDateDifference = task.getNaturalDaysCountToNextOccurrence();
                        if (dueDateDifference != null) {
                            mHyperPeriodView.setText(String.valueOf(dueDateDifference <= 0 ? 0 : dueDateDifference));
                        }
                    }

                    viewRepetitive(task.isRepetitive());
                }
            });

            RecyclerView listView = findViewById(R.id.view_work_list);
            final WorkListAdapter workListAdapter = new WorkListAdapter(this);

            listView.setAdapter(workListAdapter);
            listView.setLayoutManager(new LinearLayoutManager(this));
            mChronosViewModel.getWorksForTask(mTaskId).observe(this, new Observer<List<Work>>() {
                @Override
                public void onChanged(@Nullable List<Work> works) {
                    workListAdapter.setWorks(works);
                }
            });

            workListAdapter.setOnDeleteWorkRequestedListener(this);
        }
        else {
            mTaskId = 0L;
            mNameView.requestFocus();
            viewLog(false);
        }

        mRepetitiveTaskView.setOnClickListener(this);
        mOkButton.setOnClickListener(this);
        mCancelButton.setOnClickListener(this);
        mAddLogEntryButton.setOnClickListener(this);
    }

    private void viewRepetitive(boolean repetitive) {
        if (repetitive) {
            findViewById(R.id.view_every_times_sentence_start).setVisibility(VISIBLE);
            ((TextView) findViewById(R.id.view_every_times_sentence_middle)).setText(R.string.every_times_sentence_middle);
            ((TextView) findViewById(R.id.view_every_times_sentence_end)).setText(R.string.every_times_sentence_end);
            mRepetitionView.setVisibility(VISIBLE);
            mFlexibleDurationView.setVisibility(VISIBLE);

            viewLog(mTaskId != 0);
        }
        else {
            findViewById(R.id.view_every_times_sentence_start).setVisibility(GONE);
            ((TextView) findViewById(R.id.view_every_times_sentence_middle)).setText(R.string.to_do_within_d_days_start);
            ((TextView) findViewById(R.id.view_every_times_sentence_end)).setText(R.string.to_do_wihtin_d_days_end);
            mRepetitionView.setVisibility(GONE);
            mRepetitionView.setText("1");
            mFlexibleDurationView.setVisibility(GONE);
            mFlexibleDurationView.setChecked(false);

            viewLog(false);
        }
    }

    private void viewLog(boolean visible) {
        int visibility = visible ? VISIBLE : GONE;
        findViewById(R.id.view_add_log_entry_button).setVisibility(visibility);
        findViewById(R.id.view_work_list_title).setVisibility(visibility);
        findViewById(R.id.view_work_list).setVisibility(visibility);
    }

    @Override
    public void onBackPressed() {
        if (!saveAndFinishIfValidates()) {
            new AlertDialog.Builder(this)
                    .setMessage("Do you want to save?")
                    .setCancelable(true)
                    .setPositiveButton(android.R.string.yes, null)
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            TaskActivity.this.onClick(mCancelButton);
                        }
                    })
                    .create().show();
        }
    }

    @Override
    public void onClick(View view) {
        if (view == mOkButton) {
            saveAndFinishIfValidates();
        }
        if (view == mCancelButton) {
            setResult(RESULT_CANCELED);
            finish();
        }
        if (view == mAddLogEntryButton && mTaskId != 0) {
            insertWork();
        }
        if (view == mRepetitiveTaskView) {
            viewRepetitive(isRepetitive());
        }
    }

    private boolean saveAndFinishIfValidates() {
        if (validate()) {
            if (mTaskId == 0) {
                mChronosViewModel.insertTask(getTask(), this);
            }
            else {
                mChronosViewModel.updateTask(getTask());
                onTaskModified(mTaskId);
            }
            return true;
        }
        return false;
    }

    private Task getTask() {
        return new Task(mTaskId, getName(), getDescription(), getRepetition(), getHyperPeriod(), getDuration(),
                durationIsFlexible(), isImportant(), isRepetitive());
    }

    @Override
    public void onTaskInserted(Long taskId) {
        onTaskModified(taskId);
    }

    private void onTaskModified(Long taskId) {
        // If no repeat, dummy work to have an urgency
        if (!isRepetitive()) {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            mChronosViewModel.insertWork(new Work(0, taskId, calendar.getTime()));
        }
        setResult(RESULT_OK);
        finish();
    }

    private void insertWork() {
        mDatePicker = new DatePicker(this);
        new AlertDialog.Builder(this)
                .setPositiveButton("Next", this)
                .setCancelable(true)
                .setNegativeButton(android.R.string.cancel, this)
                .setView(mDatePicker)
                .setTitle("Choose date")
                .create().show();
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int button) {
        if (mDatePicker != null) {
            if (mDatePicked != null) throw new AssertionError();
            if (button == BUTTON_POSITIVE) {
                mDatePicked = Calendar.getInstance();
                mDatePicked.set(Calendar.YEAR, mDatePicker.getYear());
                mDatePicked.set(Calendar.MONTH, mDatePicker.getMonth());
                mDatePicked.set(Calendar.DAY_OF_MONTH, mDatePicker.getDayOfMonth());

                mTimePicker = new TimePicker(this);
                mTimePicker.setIs24HourView(android.text.format.DateFormat.is24HourFormat(this));
                new AlertDialog.Builder(this)
                        .setPositiveButton("Add", this)
                        .setCancelable(true)
                        .setNegativeButton(android.R.string.cancel, this)
                        .setView(mTimePicker)
                        .setTitle("Choose time")
                        .create().show();
            }
            mDatePicker = null;
        }
        else {
            if (mDatePicked == null) throw new AssertionError();
            if (mTimePicker == null) throw new AssertionError();
            if (button == BUTTON_POSITIVE) {
                mDatePicked.set(Calendar.HOUR_OF_DAY, mTimePicker.getCurrentHour());
                mDatePicked.set(Calendar.MINUTE, mTimePicker.getCurrentMinute());
                mChronosViewModel.insertWork(new Work(0, mTaskId, mDatePicked.getTime()));
            }
            mDatePicked = null;
            mTimePicker = null;
        }
    }

    @Override
    public void onDeleteWorkRequested(Work work) {
        mChronosViewModel.deleteWork(work);
    }

    private boolean validate() {
        boolean ok = true;
        if (getName().isEmpty()) {
            mNameView.setError("You must give the task a name");
            ok = false;
        }
        try {
            if (getRepetition() < 1) {
                mRepetitionView.setError("How many times?");
                ok = false;
            }
        }
        catch (NumberFormatException e) {
            mRepetitionView.setError("Must be a number");
            ok = false;
        }
        try {
            if (getHyperPeriod() < 1) {
                mHyperPeriodView.setError("Every how many days?");
                ok = false;
            }
        }
        catch (NumberFormatException e) {
            mHyperPeriodView.setError("Must be a number");
            ok = false;
        }
        if (ok && getRepetition() > getHyperPeriod()) {
            mRepetitionView.setError("Max frequency is once a day");
            mRepetitionView.setText(mHyperPeriodView.getText());
            ok = false;
        }
        return ok;
    }

    private String getName() {
        return mNameView.getText().toString();
    }

    private String getDescription() {
        return mDescriptionView.getText().toString();
    }

    private Integer getRepetition() throws NumberFormatException {
        return getIntegerFomEditText(mRepetitionView);
    }

    private Integer getHyperPeriod() throws NumberFormatException {
        return getIntegerFomEditText(mHyperPeriodView);
    }

    private Integer getDuration() {
        try {
            return getIntegerFomEditText(mDurationView);
        }
        catch (NumberFormatException e) {
            Log.d("selection", "bad duration");
            return 0;
        }
    }

    private boolean durationIsFlexible() {
        return mFlexibleDurationView.isChecked();
    }

    private boolean isImportant() {
        return mImportantTaskView.isChecked();
    }

    private boolean isRepetitive() {
        return mRepetitiveTaskView.isChecked();
    }

    private Integer getIntegerFomEditText(EditText editText) throws NumberFormatException {
        return Integer.valueOf(editText.getText().toString());
    }
}
