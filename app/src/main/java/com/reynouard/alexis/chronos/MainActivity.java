package com.reynouard.alexis.chronos;

import android.Manifest;
import android.app.Dialog;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import com.reynouard.alexis.chronos.model.StatedTask;
import com.reynouard.alexis.chronos.model.Task;
import com.reynouard.alexis.chronos.model.Work;
import com.reynouard.alexis.chronos.utils.DateTimeUtils;
import com.reynouard.alexis.chronos.view.OnDeleteTaskRequestedListener;
import com.reynouard.alexis.chronos.view.OnEditTaskRequestedListener;
import com.reynouard.alexis.chronos.view.OnInsertWorkRequestedListener;
import com.reynouard.alexis.chronos.view.TaskListAdapter;
import com.reynouard.alexis.chronos.viewModel.ChronosViewModel;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements
        View.OnClickListener, OnInsertWorkRequestedListener, OnEditTaskRequestedListener, OnDeleteTaskRequestedListener {

    private static final int TASK_ACTIVITY_REQUEST_CODE = 1;
    private final TaskSelector mTaskSelector = new SimpleTaskSelector();
    private View mAddButton;
    private ChronosViewModel mChronosViewModel;
    private int mDurationToPlane = 120;
    private int mDurationDone = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        RecyclerView listView = findViewById(R.id.view_task_list);
        final TaskListAdapter taskListAdapter = new TaskListAdapter(this, mTaskSelector);

        listView.setAdapter(taskListAdapter);
        listView.setLayoutManager(new LinearLayoutManager(this));

        mChronosViewModel = ViewModelProviders.of(this).get(ChronosViewModel.class);
        mChronosViewModel.getTasks().observe(this, new Observer<List<StatedTask>>() {
            @Override
            public void onChanged(@Nullable List<StatedTask> tasks) {
                taskListAdapter.setTasks(tasks);
                updateDurationToPlane();
                updateProgressInfo();
            }
        });
        mChronosViewModel.getDurationDoneToday().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(@Nullable Integer durationDoneToday) {
                if (durationDoneToday == null) {
                    durationDoneToday = 0;
                }
                mDurationDone = durationDoneToday;
                updateDurationToPlane();
                taskListAdapter.setDurationToDo(mDurationToPlane - durationDoneToday);
                updateProgressInfo();
            }
        });

        taskListAdapter.setOnInsertWorkRequestedListener(this);
        taskListAdapter.setOnEditTaskRequestedListener(this);
        taskListAdapter.setOnDeleteTaskRequestedListener(this);

        mAddButton = findViewById(R.id.view_add_button);
        mAddButton.setOnClickListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        updateDurationToPlane();
    }

    private void updateDurationToPlane() {
        mDurationToPlane = PreferenceManager
                .getDefaultSharedPreferences(this)
                .getInt(SettingsActivity.PREF_TIME_BY_DAY_PREFIX + DateTimeUtils.DAY_OF_WEEK_NAMES.get(Calendar.getInstance().get(Calendar.DAY_OF_WEEK)), 120);
    }

    private void updateProgressInfo() {
        int total = mDurationDone + mTaskSelector.minutesSelected();
        if (total != 0) {
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setTitle(String.format(Locale.getDefault(),
                        "%s%s%s",
                        getResources().getString(R.string.app_name),
                        getString(R.string.hyphen_sep),
                        mTaskSelector.minutesSelected() == 0
                                ? getString(R.string.all_is_done)
                                : DateTimeUtils.durationString(this, mTaskSelector.minutesSelected(), TimeUnit.MINUTES)) + " left"
                );
            }

            int progress = Math.round(((float) mDurationDone * 100) / total);
            ((ProgressBar) findViewById(R.id.view_progress)).setProgress(progress);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_manage) {
            // TODO
        }
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        if (id == R.id.action_backup) {
            // TODO: wait for answer before start service
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            startService(new Intent(this, BackupService.class));
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {
        if (view == mAddButton) {
            startActivityForResult(new Intent(this, TaskActivity.class), TASK_ACTIVITY_REQUEST_CODE);
        }
    }

    @Override
    public void onInsertWorkRequested(Task task, Date date) {
        mChronosViewModel.insertWork(new Work(0, task.getId(), date));
        if (!task.isRepetitive()) {
            onDeleteTaskRequested(task);
        }
    }

    @Override
    public void onEditTaskRequested(Task task) {
        Intent intent = new Intent(this, TaskActivity.class);
        intent.putExtra(TaskActivity.EXTRA_ID, task.getId());
        startActivity(intent);
    }

    @Override
    public void onDeleteTaskRequested(final Task task) {
        Dialog confirmDialog = new AlertDialog.Builder(this)
                .setMessage(String.format("Do you want to delete task \"%s\"", task.getName()))
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        mChronosViewModel.deleteTask(task);
                    }
                })
                .setNegativeButton(R.string.keep, null)
                .create();
        confirmDialog.show();
    }
}
