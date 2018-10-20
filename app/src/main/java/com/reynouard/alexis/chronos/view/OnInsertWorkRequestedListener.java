package com.reynouard.alexis.chronos.view;

import com.reynouard.alexis.chronos.model.Task;

import java.util.Date;

public interface OnInsertWorkRequestedListener {

    void onInsertWorkRequested(Task task, Date time);
}
