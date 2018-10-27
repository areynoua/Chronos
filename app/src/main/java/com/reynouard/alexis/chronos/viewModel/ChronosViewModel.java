package com.reynouard.alexis.chronos.viewModel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;

import com.reynouard.alexis.chronos.BackupRestoreService;
import com.reynouard.alexis.chronos.model.ChronosDao;
import com.reynouard.alexis.chronos.model.ChronosRoom;
import com.reynouard.alexis.chronos.model.StatedTask;
import com.reynouard.alexis.chronos.model.Task;
import com.reynouard.alexis.chronos.model.Work;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Never pass context into ViewModel instances. Do not store Activity, Fragment, or View instances or their Context
// in the ViewModel.
//
// For example, an Activity can be destroyed and created many times during the lifecycle of a ViewModel as the device
// is rotated. If you store a reference to the Activity in the ViewModel, you end up with references that point to
// the destroyed Activity. This is a memory leak.

// ViewModel is not a replacement for the onSaveInstanceState() method, because the ViewModel does not survive
// a process shutdown.

public class ChronosViewModel extends AndroidViewModel {

    private final ChronosDao mChronosDao;
    @NonNull
    private final LiveData<List<StatedTask>> mTasks;
    @NonNull
    private final LiveData<Integer> mDurationDoneToday;
    @NonNull
    private final Map<Long, LiveData<List<Work>>> mWorksByTask = new HashMap<>();

    public ChronosViewModel(Application application) {
        super(application);
        mChronosDao = ChronosRoom.getDatabase(application).getChronosDao();
        mTasks = mChronosDao.getTasks();
        mDurationDoneToday = mChronosDao.getDurationDoneToday();
    }

    @NonNull
    public LiveData<List<StatedTask>> getTasks() {
        return mTasks;
    }

    @NonNull
    public LiveData<Integer> getDurationDoneToday() {
        return mDurationDoneToday;
    }

    @NonNull
    public LiveData<List<Work>> getWorksForTask(long taskId) {
        if (!mWorksByTask.containsKey(taskId)) {
            mWorksByTask.put(taskId, mChronosDao.getWorksForTask(taskId));
        }
        return mWorksByTask.get(taskId);
    }

    private static void ensureNoRestorationRunning() {
        synchronized(BackupRestoreService.restorationLock){}
    }

    public void updateTask(Task task) {
        new UpdateTaskAsyncTask(mChronosDao).execute(task);
    }

    public void deleteTask(Task task) {
        new DeleteTaskAsyncTask(mChronosDao).execute(task);
    }

    public void insertWork(Work work) {
        Log.d("work", "ChronosViewModel insert Work " + work.getId() + " " + work.getTaskId() + " " + work.getDate());
        new InsertWorkAsyncTask(mChronosDao).execute(work);
    }

    public void deleteWork(Work work) {
        new DeleteWorkAsyncTask(mChronosDao).execute(work);
    }

    public void deleteWorks(Long taskId) {
        new DeleteWorksAsyncTask(mChronosDao).execute(taskId);
    }

    public LiveData<StatedTask> getTask(long taskId) {
        return mChronosDao.getTask(taskId);
    }

    public void insertTask(Task task, @NonNull OnTaskModifiedListener listener) {
        new InsertTaskAsyncTask(mChronosDao, listener).execute(task);
    }

    public interface OnTaskModifiedListener {
        void onTaskInserted(Long taskId);
    }

    private static class InsertTaskAsyncTask extends AsyncTask<Task, Void, Long> {
        private final ChronosDao mChronosDao;
        private final OnTaskModifiedListener mListener;

        InsertTaskAsyncTask(ChronosDao chronosDao, @NonNull OnTaskModifiedListener listener) {
            mChronosDao = chronosDao;
            mListener = listener;
        }

        @Override
        protected Long doInBackground(final Task... tasks) {
            ensureNoRestorationRunning();
            return mChronosDao.insertTask(tasks[0]);
        }

        @Override
        protected void onPostExecute(Long taskId) {
            super.onPostExecute(taskId);
            mListener.onTaskInserted(taskId);
        }
    }

    private static class UpdateTaskAsyncTask extends AsyncTask<Task, Void, Void> {
        private final ChronosDao mChronosDao;

        UpdateTaskAsyncTask(ChronosDao chronosDao) {
            mChronosDao = chronosDao;
        }

        @Override
        protected Void doInBackground(final Task... tasks) {
            ensureNoRestorationRunning();
            mChronosDao.updateTask(tasks[0]);
            return null;
        }
    }

    private static class InsertWorkAsyncTask extends AsyncTask<Work, Void, Void> {
        private final ChronosDao mChronosDao;

        InsertWorkAsyncTask(ChronosDao chronosDao) {
            mChronosDao = chronosDao;
        }

        @Override
        protected Void doInBackground(Work... works) {
            ensureNoRestorationRunning();
            mChronosDao.insertWork(works[0]);
            return null;
        }
    }

    private static class DeleteTaskAsyncTask extends AsyncTask<Task, Void, Void> {
        private final ChronosDao mChronosDao;

        DeleteTaskAsyncTask(ChronosDao chronosDao) {
            mChronosDao = chronosDao;
        }

        @Override
        protected Void doInBackground(final Task... tasks) {
            ensureNoRestorationRunning();
            mChronosDao.deleteTask(tasks[0]);
            return null;
        }
    }

    private static class DeleteWorkAsyncTask extends AsyncTask<Work, Void, Void> {
        private final ChronosDao mChronosDao;

        DeleteWorkAsyncTask(ChronosDao chronosDao) {
            mChronosDao = chronosDao;
        }

        @Override
        protected Void doInBackground(Work... works) {
            ensureNoRestorationRunning();
            mChronosDao.deleteWork(works[0]);
            return null;
        }
    }

    private static class DeleteWorksAsyncTask extends AsyncTask<Long, Void, Void> {
        private final ChronosDao mChronosDao;

        DeleteWorksAsyncTask(ChronosDao chronosDao) {
            mChronosDao = chronosDao;
        }

        @Override
        protected Void doInBackground(Long... taskIds) {
            ensureNoRestorationRunning();
            mChronosDao.deleteWorks(taskIds[0]);
            return null;
        }
    }
}
