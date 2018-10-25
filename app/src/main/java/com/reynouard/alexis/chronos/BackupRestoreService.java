package com.reynouard.alexis.chronos;

import android.app.Service;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/*
private void showFileChooser() {
    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
    intent.setType("text/*");
    intent.addCategory(Intent.CATEGORY_OPENABLE);
    startA
}
*/

interface OnPartialBackupDoneListener {
    void onPartialBackupDone(BackupRestoreService.BackupPart partialBackupPart, Writer writer, Boolean ok);
}

interface OnRestorationDoneListener {
    void onRestorationDone(Boolean ok);
}

/**
 * Service to make human readable (csv) backups
 */
public class BackupRestoreService extends Service implements OnPartialBackupDoneListener, OnRestorationDoneListener {

    public static final ReentrantLock restorationLock = new ReentrantLock();
    /* Intent Action */
    public static final String ACTION_BACKUP = BackupRestoreService.class.getCanonicalName() + ".ACTION_BACKUP";
    public static final String ACTION_RESTORE = BackupRestoreService.class.getCanonicalName() + ".ACTION_RESTORE";
    static final String TAG = "backup";
    /**
     * START_REDELIVER_INTENT: Constant to return from onStartCommand(Intent, int, int): if this service's process is
     * killed while it is started (after returning from onStartCommand(Intent, int, int)), then it will be scheduled
     * for a restart and the last delivered Intent re-delivered to it again via onStartCommand(Intent, int, int).
     */
    private static final int SERVICE_TYPE = START_REDELIVER_INTENT;
    private static final String PROGRESS_NOTIFICATION_CHANNEL_ID = "backup_progress";
    /**
     * The number of command started from the beginning.
     * This is used to give to each command, in onStartCommand, a unique identifier held by mCommandCount.
     */
    private static int mCommandCounter = 0;
    /**
     * Current running command.
     * This is used to ensure that there is no more than one command running simultaneously, and to give to
     * notifications unique identifiers.
     */
    private static int mCommandCount = 0;
    /**
     * Where to write the backup files
     */
    private final File mDirectory = new File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "chronos/");
    /* Data */
    private ChronosDao mChronosDao;
    private LiveData<List<StatedTask>> mTasksLiveData = null;
    private LiveData<List<Work>> mWorksLiveData = null;

    private boolean mOk = true;
    private Integer mStartId;

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

    /**
     * Clients can not bind to the service.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Start a backup/restoration of the tasks and works, if there is not already one running and external storage is
     * writable.
     * <p>
     * The app must have WRITE_EXTERNAL_STORAGE permission.
     * <p>
     * Called by the system every time a client explicitly starts the service by calling Context.startService(Intent),
     * providing the arguments it supplied and a unique integer token representing the start request.
     * Do not call this method directly.
     * <p>
     * /!\ Called on the UI thread.
     *
     * @param intent  action must be ACTION_BACKUP or ACTION_RESTORE.
     *                If it is ACTION_RESTORE, the intent must provides the chosen content uri as data.
     * @param flags   ignored
     * @param startId A unique integer representing this specific request to start.
     * @return SERVICE_TYPE
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        synchronized (BackupRestoreService.class) {
            if (mCommandCount != 0) {
                Log.w(TAG, "onStartCommand ignored, already running");
                stopSelf(startId);
                return SERVICE_TYPE;
            } else {
                mCommandCount = ++mCommandCounter;
            }
        }

        mStartId = startId;

        if (ACTION_BACKUP.equals(intent.getAction())) {
            startBackup(startId);
        } else if (ACTION_RESTORE.equals(intent.getAction())) {
            startRestore(startId, intent.getData());
        } else {
            Log.e(TAG, "Bad intent action");
        }

        return SERVICE_TYPE;
    }

    /* Backup */

    private void startBackup(int startId) {
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            Log.w(TAG, "Media not writable");
            Toast.makeText(this, "Unable to write to external storage", Toast.LENGTH_LONG).show();
            mCommandCount = 0;
            stopSelf(startId);
            return;
        }
        if (!(mDirectory.exists() || mDirectory.mkdirs())) {
            Log.w(TAG, "Unable to create directory " + mDirectory.getAbsolutePath());
            Toast.makeText(this, "Unable to create directory", Toast.LENGTH_LONG).show();
            mCommandCount = 0;
            stopSelf(startId);
            return;
        }

        final NotificationCompat.Builder notificationBuilder = NotificationHelper
                .createNotificationBuilder(this, PROGRESS_NOTIFICATION_CHANNEL_ID)
                .setContentTitle("Backup")
                .setContentText("Chronos database backup running...")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setProgress(0, 0, true)
                .setOngoing(true);

        final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(mCommandCount, notificationBuilder.build());

        final String filename = String.format("chronos_backup_%s.csv", DateConverter.fromDate(new Date()));
        File taskFile = new File(mDirectory, filename);
        Writer writer = null;
        try {
            writer = new FileWriter(taskFile);
            backupTasks(writer);
        } catch (IOException e) {
            Log.e(TAG, "startBackup: the file exists but is a directory rather than a regular file, does not " +
                    "exist but cannot be created, or cannot be opened for any other reason");
            e.printStackTrace();
            Toast.makeText(this, String.format("Unable to write file %s (%s)", taskFile.getAbsolutePath(),
                    e.getLocalizedMessage()), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Backup tasks then call onPartialBackupDone
     */
    private void backupTasks(Writer writer) {
        mTasksLiveData = mChronosDao.getTasks();
        mTasksLiveData.observeForever(new BackupTasksAsyncTask(mTasksLiveData, writer, this));
    }

    /**
     * Backup works then call onPartialBackupDone
     */
    private void backupWorks(Writer writer) {
        mWorksLiveData = mChronosDao.getWorks();
        mWorksLiveData.observeForever(new BackupWorksAsyncTask(mWorksLiveData, writer, this));
    }

    @Override
    public void onPartialBackupDone(BackupPart part, @NonNull Writer writer, Boolean ok) {
        mOk = mOk && ok;
        switch (part) {
            case TASKS:
                mTasksLiveData = null;
                backupWorks(writer);
                break;
            case WORKS:
                mWorksLiveData = null;
                try {
                    writer.flush();
                    writer.close();
                } catch (IOException e) {
                    Log.e(TAG, "onPartialBackupDone: " + e.getMessage());
                    e.printStackTrace();
                    ok = false;
                } finally {
                    try {
                        writer.close();
                    } catch (IOException e) {
                        Log.e(TAG, e.getMessage());
                        e.printStackTrace();
                        ok = false;
                    }
                }
                if (ok) {
                    Toast.makeText(this, "Saved in " + mDirectory.getAbsolutePath(), Toast.LENGTH_LONG)
                            .show();
                } else {
                    Toast.makeText(this, "Error while saving in " + mDirectory.getAbsolutePath(),
                            Toast.LENGTH_LONG).show();
                }
                stop();
                break;
        }
    }

    /* Restore */

    private void startRestore(int startId, Uri source) {
        if (!(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                || Environment.MEDIA_MOUNTED_READ_ONLY.equals(Environment.getExternalStorageState()))) {
            Log.w(TAG, "Media not readable");
            Toast.makeText(this, "Unable to read from external storage", Toast.LENGTH_LONG).show();
            mCommandCount = 0;
            stopSelf(startId);
            return;
        }

        restorationLock.lock();
        try {
            InputStream is = getContentResolver().openInputStream(source);
            if (is == null) {
                throw new IOException();
            }
            else {
                new RestorationAsyncTask(this, is, mChronosDao).execute();
            }
        } catch (IOException e) {
            e.printStackTrace();
            restorationLock.unlock();
        }
    }

    @Override
    public void onRestorationDone(Boolean ok) {
        restorationLock.unlock();
        if (ok) {
            Toast.makeText(this, "Restored!", Toast.LENGTH_LONG).show();
        }
        else {
            Toast.makeText(this, "Unable to restore", Toast.LENGTH_LONG).show();
        }
    }

    private void stop() {
        Log.d(TAG, "done, stopping");
        synchronized (this) {
            // TODO: ensure that liveData observers were removed
            NotificationManagerCompat.from(this).cancel(mCommandCount);
            mCommandCount = 0;
            mTasksLiveData = null;
            mWorksLiveData = null;
            if (mStartId == null) {
                stopSelf();
            } else {
                stopSelf(mStartId);
            }
            mStartId = null;
        }
    }

    enum BackupPart {
        TASKS,
        WORKS,
    }

    private static class BackupTasksAsyncTask extends AsyncTask<StatedTask, Void, Boolean> implements
            Observer<List<StatedTask>> {

        @NonNull
        private final LiveData<List<StatedTask>> mObserved;
        @NonNull
        private final Writer mWriter;
        @NonNull
        private final OnPartialBackupDoneListener mOnDoneListener;

        BackupTasksAsyncTask(@NonNull LiveData<List<StatedTask>> observed,
                             @NonNull Writer writer,
                             @NonNull OnPartialBackupDoneListener onDoneListener) {
            mObserved = observed;
            mWriter = writer;
            mOnDoneListener = onDoneListener;
        }

        /**
         * Called when the data is changed.
         * The first call with non null parameter is used to perform the backup.
         *
         * @param statedTasks The new data
         */
        @Override
        public void onChanged(@Nullable List<StatedTask> statedTasks) {
            if (statedTasks == null) {
                Log.w(TAG, "receive null task list");
            } else {
                mObserved.removeObserver(this);
                this.execute(statedTasks.toArray(new StatedTask[statedTasks.size()]));
            }
        }

        /**
         * Performs Backup. Called by onChanged. Do not call directly.
         *
         * @param statedTasks The data to save
         * @return true if ok
         */
        @Override
        protected Boolean doInBackground(StatedTask... statedTasks) {
            boolean ok;
            synchronized (BackupTasksAsyncTask.class) {
                try {
                    for (Task task : statedTasks) {
                        mWriter.append(CsvAdapter.toCsv(task));
                        mWriter.append(CsvAdapter.RECORD_BREAK);
                    }
                    mWriter.append(CsvAdapter.RECORD_BREAK);
                    ok = true;
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage());
                    e.printStackTrace();
                    ok = false;
                }
            }
            return ok;
        }

        @Override
        protected void onPostExecute(Boolean ok) {
            mOnDoneListener.onPartialBackupDone(BackupPart.TASKS, mWriter, ok);
        }
    }

    private static class BackupWorksAsyncTask extends AsyncTask<Work, Void, Boolean> implements
            Observer<List<Work>> {

        @NonNull
        private final LiveData<List<Work>> mObserved;
        @NonNull
        private final Writer mWriter;
        @NonNull
        private final OnPartialBackupDoneListener mOnDoneListener;

        BackupWorksAsyncTask(@NonNull LiveData<List<Work>> observed,
                             @NonNull Writer writer,
                             @NonNull OnPartialBackupDoneListener onDoneListener) {
            mObserved = observed;
            mWriter = writer;
            mOnDoneListener = onDoneListener;
        }

        /**
         * Called when the data is changed.
         * The first call with non null parameter is used to perform the backup.
         *
         * @param works The new data
         */
        @Override
        public void onChanged(@Nullable List<Work> works) {
            if (works == null) {
                Log.w(TAG, "receive null work list");
            } else {
                mObserved.removeObserver(this);
                this.execute(works.toArray(new Work[works.size()]));
            }
        }

        /**
         * Performs Backup. Called by onChanged. Do not call directly.
         *
         * @param works The data to save
         * @return true if ok
         */
        @Override
        protected Boolean doInBackground(Work... works) {
            boolean ok;
            synchronized (BackupWorksAsyncTask.class) {
                try {
                    for (Work work : works) {
                        mWriter.append(CsvAdapter.toCsv(work));
                        mWriter.append(CsvAdapter.RECORD_BREAK);
                    }
                    mWriter.append(CsvAdapter.RECORD_BREAK);
                    ok = true;
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage());
                    e.printStackTrace();
                    ok = false;
                }
            }
            return ok;
        }

        @Override
        protected void onPostExecute(Boolean ok) {
            mOnDoneListener.onPartialBackupDone(BackupPart.WORKS, mWriter, ok);
        }
    }

    private static class RestorationAsyncTask extends AsyncTask<Void, Void, Boolean> {

        @NonNull
        private final OnRestorationDoneListener mOnDoneListener;
        @NonNull
        private final InputStream mInput;
        @NonNull
        private final ChronosDao mChronosDao;

        private RestorationAsyncTask(@NonNull OnRestorationDoneListener onDoneListener, @NonNull InputStream input, @NonNull ChronosDao chronosDao) {
            mOnDoneListener = onDoneListener;
            mInput = input;
            mChronosDao = chronosDao;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                mChronosDao.clearAll();
                BufferedReader reader = new BufferedReader(new InputStreamReader(mInput));
                Task task = CsvAdapter.toTask(reader);
                while (task != null) {
                    mChronosDao.insertTask(task);
                    task = CsvAdapter.toTask(reader);
                }
                Work work = CsvAdapter.toWork(reader);
                while (work != null) {
                    mChronosDao.insertWork(work);
                    work = CsvAdapter.toWork(reader);
                }
                reader.close();
                mInput.close();
                return true;
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean ok) {
            mOnDoneListener.onRestorationDone(ok);
        }

        @Override
        protected void onCancelled(Boolean ok) {
            super.onCancelled(ok);
            mOnDoneListener.onRestorationDone(ok);
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            mOnDoneListener.onRestorationDone(false);
        }
    }
}