package com.reynouard.alexis.chronos;

import android.app.Service;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.widget.Toast;

import com.reynouard.alexis.chronos.model.ChronosDao;
import com.reynouard.alexis.chronos.model.ChronosRoom;
import com.reynouard.alexis.chronos.model.DateConverter;
import com.reynouard.alexis.chronos.model.StatedTask;
import com.reynouard.alexis.chronos.model.Task;
import com.reynouard.alexis.chronos.model.Work;
import com.reynouard.alexis.chronos.model.csv.CsvAdapter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.List;

interface OnPartialBackupDoneListener {
    void onPartialBackupDone(int partialBackupPart, Boolean ok);
}

public class BackupService extends Service implements OnPartialBackupDoneListener {

    /** Partial backup flags */
    private static final int NO_BACKUP   = 0b00;
    private static final int TASK_BACKUP = 0b01;
    private static final int WORK_BACKUP = 0b10;
    private static final int ALL_BACKUP  = 0b11;

    /**
     * START_STICKY: if this service's process is killed while it is started (after returning from onStartCommand
     * (Intent, int, int)), then leave it in the started state but don't retain this delivered intent. Later the
     * system will try to re-create the service. Because it is in the started state, it will guarantee to call
     * onStartCommand(Intent, int, int) after creating the new service instance; if there are not any pending start
     * commands to be delivered to the service, it will be called with a null intent object.
     */
    private static final int SERVICE_TYPE = START_STICKY;

    private static final String PROGRESS_NOTIFICATION_CHANNEL_ID = "backup_progress";

    /**
     * The number of command started from the beginning.
     * This is used to give to each command, in onStartCommand, a unique identifier held by mCommandCount.
     */
    private static int mCommandCounter = 0;

    /** Where to write the backup files */
    private final File mDirectory = new File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "chronos/");

    private ChronosDao mChronosDao;
    private LiveData<List<StatedTask>> mTasksLiveData = null;
    private LiveData<List<Work>> mWorksLiveData = null;

    /**
     * Current running command.
     * This is used to ensure that there is no more than one command running simultaneously, and to give notifications
     * unique identifiers.
     */
    private int mCommandCount = 0;

    /** A command start many partial backups as async tasks. It stop itself when all are done. */
    private int mPartialBackupsDone = 0x0;

    private boolean mOk = true;

    /**
     * Called by the system when the service is first created. Do not call this method directly.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        mChronosDao = ChronosRoom.getDatabase(getApplicationContext()).getChronosDao();
        NotificationHelper.createNotificationChannel(this, PROGRESS_NOTIFICATION_CHANNEL_ID,
                getString(R.string.backup_progress_channel_name),
                getString(R.string.backup_progress_channel_description),
                NotificationHelper.IMPORTANCE_LOW);
    }

    /** Clients can not bind to the service. */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Start a backup of the tasks and works, if there is not already one running and external storage is writable.
     *
     * The app must have WRITE_EXTERNAL_STORAGE permission.
     *
     * Called by the system every time a client explicitly starts the service by calling Context.startService(Intent),
     * providing the arguments it supplied and a unique integer token representing the start request.
     * Do not call this method directly.
     *
     * /!\ Called on the UI thread.
     *
     * @param intent ignored
     * @param flags ignored
     * @param startId A unique integer representing this specific request to start.
     * @return SERVICE_TYPE
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        synchronized (this) {
            if (mCommandCount != 0) {
                Log.w("backup", "onStartCommand ignored, already running");
                stopSelf(startId);
                return SERVICE_TYPE;
            } else {
                mCommandCount = ++mCommandCounter;
                if (mPartialBackupsDone != NO_BACKUP) {
                    mPartialBackupsDone = NO_BACKUP;
                    Log.e("backup", "backup marked as (partially?) done before started.");
                }
            }
        }

        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            Log.w("backup", "Media not writable");
            Toast.makeText(this, "Unable to write to external storage", Toast.LENGTH_LONG).show();
            mCommandCount = 0;
            stopSelf(startId);
            return SERVICE_TYPE;
        }
        if (!(mDirectory.exists() || mDirectory.mkdirs())) {
            Log.w("backup", "Unable to create directory " + mDirectory.getAbsolutePath());
            Toast.makeText(this, "Unable to create directory", Toast.LENGTH_LONG).show();
            mCommandCount = 0;
            stopSelf(startId);
            return SERVICE_TYPE;
        }

//        Intent notificationIntent = new Intent(this, MainActivity.class);
//        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        NotificationCompat.Builder notificationBuilder = createNotificationBuilder()
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Backup")
                .setContentText("Chronos database backup running...")
                .setPriority(NotificationCompat.PRIORITY_LOW)
//                .setContentIntent(pendingIntent)
                .setProgress(0, 0, true)
                .setOngoing(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(mCommandCount, notificationBuilder.build());

        backupTasks();
        backupWorks();

        return SERVICE_TYPE;
    }

    private void backupTasks() {
        synchronized (this) {
            if (mTasksLiveData != null) {
                Log.e("backup", "mTasksLiveData is not null, backupTasks aborts.");
                return;
            }
            mTasksLiveData = mChronosDao.getTasks();
        }

        mTasksLiveData.observeForever(new BackupTasksAsyncTask(mTasksLiveData, mDirectory, this));
    }

    private void backupWorks() {
        synchronized (this) {
            if (mWorksLiveData != null) {
                Log.e("backup", "mWorksLiveData is not null, backupWorks aborts.");
                return;
            }
            mWorksLiveData = mChronosDao.getWorks();
        }

        mWorksLiveData.observeForever(new BackupWorksAsyncTask(mWorksLiveData, mDirectory, this));
    }

    @Override
    public void onPartialBackupDone(int part, Boolean ok) {
        switch (part) {
            case TASK_BACKUP:
                mTasksLiveData = null;
                break;
            case WORK_BACKUP:
                mWorksLiveData = null;
                break;
        }
        synchronized (this) {
            mOk = mOk && ok;
            mPartialBackupsDone |= part;
            if (mPartialBackupsDone == ALL_BACKUP) {
                if (ok) {
                    Toast.makeText(this, "Saved in " + mDirectory.getAbsolutePath(), Toast.LENGTH_LONG)
                            .show();
                }
                else {
                    Toast.makeText(this, "Error while saving in " + mDirectory.getAbsolutePath(),
                            Toast.LENGTH_LONG).show();
                }
                stop(null);
            }
        }
    }

    private void stop(Integer startId) {
        Log.d("backup", "done, stopping");
        synchronized (this) {
            // TODO: ensure that liveData observers were removed
            NotificationManagerCompat.from(this).cancel(mCommandCount);
            mCommandCount = 0;
            mPartialBackupsDone = NO_BACKUP;
            if (startId == null) {
                stopSelf();
            } else {
                stopSelf(startId);
            }
        }
    }

    private NotificationCompat.Builder createNotificationBuilder() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return new NotificationCompat.Builder(this, PROGRESS_NOTIFICATION_CHANNEL_ID);
        } else {
            return new NotificationCompat.Builder(this);
        }
    }


    private static class BackupTasksAsyncTask extends AsyncTask<StatedTask, Void, Boolean> implements
            Observer<List<StatedTask>> {

        private final LiveData<List<StatedTask>> mObserved;
        private final File mDirectory;
        private final OnPartialBackupDoneListener mOnDoneListener;

        BackupTasksAsyncTask(@NonNull LiveData<List<StatedTask>> observed, @NonNull File directory,
                                    @NonNull OnPartialBackupDoneListener onDoneListener) {
            mObserved = observed;
            mDirectory = directory;
            mOnDoneListener = onDoneListener;
        }

        /**
         * Called when the data is changed.
         * The first call with non null parameter is used to performs the backup.
         *
         * @param statedTasks The new data
         */
        @Override
        public void onChanged(@Nullable List<StatedTask> statedTasks) {
            if (statedTasks == null) {
                Log.w("backup", "receive null task list");
            }
            else {
                mObserved.removeObserver(this);
                this.execute(statedTasks.toArray(new StatedTask[statedTasks.size()]));
            }
        }

        /**
         * Performs Backup. Called by onChanged. Do not call directly.
         * @param statedTasks The data to save
         * @return true if ok
         */
        @Override
        protected Boolean doInBackground(StatedTask... statedTasks) {
            boolean ok;
            FileWriter writer = null;
            synchronized (BackupTasksAsyncTask.class) {
                try {
                    File taskFile = new File(mDirectory, "tasks_" + DateConverter.fromDate(new Date()) + ".csv");
                    writer = new FileWriter(taskFile);
                    for (Task task : statedTasks) {
                        writer.append(CsvAdapter.toCsv(task));
                        writer.append("\r\n");
                    }
                    writer.flush();
                    writer.close();
                    ok = true;
                } catch (IOException e) {
                    Log.e("backup", e.getMessage());
                    e.printStackTrace();
                    ok = false;
                } finally {
                    if (writer != null) {
                        try {
                            writer.close();
                        } catch (IOException e) {
                            Log.e("backup", e.getMessage());
                            e.printStackTrace();
                            ok = false;
                        }
                    }
                }
            }
            return ok;
        }

        @Override
        protected void onPostExecute(Boolean ok) {
            mOnDoneListener.onPartialBackupDone(TASK_BACKUP, ok);
        }
    }

    private static class BackupWorksAsyncTask extends AsyncTask<Work, Void, Boolean> implements
            Observer<List<Work>> {

        private final LiveData<List<Work>> mObserved;
        private final File mDirectory;
        private final OnPartialBackupDoneListener mOnDoneListener;

        BackupWorksAsyncTask(@NonNull LiveData<List<Work>> observed, @NonNull File directory,
                             @NonNull OnPartialBackupDoneListener onDoneListener) {
            mObserved = observed;
            mDirectory = directory;
            mOnDoneListener = onDoneListener;
        }

        /**
         * Called when the data is changed.
         * The first call with non null parameter is used to performs the backup.
         *
         * @param works The new data
         */
        @Override
        public void onChanged(@Nullable List<Work> works) {
            if (works == null) {
                Log.w("backup", "receive null work list");
            }
            else {
                mObserved.removeObserver(this);
                this.execute(works.toArray(new Work[works.size()]));
            }
        }

        /**
         * Performs Backup. Called by onChanged. Do not call directly.
         * @param works The data to save
         * @return true if ok
         */
        @Override
        protected Boolean doInBackground(Work... works) {
            boolean ok;
            FileWriter writer = null;
            synchronized (BackupWorksAsyncTask.class) {
                try {
                    File file = new File(mDirectory, "works_" + DateConverter.fromDate(new Date()) + ".csv");
                    writer = new FileWriter(file);
                    for (Work work : works) {
                        writer.append(CsvAdapter.toCsv(work));
                        writer.append("\r\n");
                    }
                    writer.flush();
                    writer.close();
                    ok = true;
                } catch (IOException e) {
                    Log.e("backup", e.getMessage());
                    e.printStackTrace();
                    ok = false;
                } finally {
                    if (writer != null) {
                        try {
                            writer.close();
                        } catch (IOException e) {
                            Log.e("backup", e.getMessage());
                            e.printStackTrace();
                            ok = false;
                        }
                    }
                }
            }
            return ok;
        }

        @Override
        protected void onPostExecute(Boolean ok) {
            mOnDoneListener.onPartialBackupDone(WORK_BACKUP, ok);
        }
    }
}