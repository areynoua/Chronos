package com.reynouard.alexis.chronos;

import com.reynouard.alexis.chronos.model.StatedTask;
import com.reynouard.alexis.chronos.model.Task;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SimpleTaskSelector implements TaskSelector {

    private static final int UNDER_THRESHOLD = 4;
    private static final int OVER_THRESHOLD = 10;
    private final Set<Task> mSelection = new HashSet<>();
    private Integer mMinutesSelected = 0;

    @Override
    public synchronized void select(List<StatedTask> tasks, int duration) {
        mSelection.clear();
        mMinutesSelected = 0;

        // Important tasks
        for (int idx = 0; idx < tasks.size() && duration > UNDER_THRESHOLD; ++idx) {
            StatedTask task = tasks.get(idx);
            if (task.getDuration() <= duration + OVER_THRESHOLD
                    && task.isImportant()
                    && (task.getUrgency() == null || (task.getDaysToNextOccurrence() < 2.0 && task.getUrgency() > 0.5))) {
                duration = add(task, duration);
            }
        }
        // Other tasks
        for (int idx = 0; idx < tasks.size() && duration > UNDER_THRESHOLD; ++idx) {
            StatedTask task = tasks.get(idx);
            if (task.getDuration() <= duration + OVER_THRESHOLD
                    && !isSelected(task)
                    && (task.getUrgency() == null || task.getUrgency() > 1/7)) {
                duration = add(task, duration);
            }
        }
    }

    private int add(Task task, int duration) {
        mSelection.add(task);
        mMinutesSelected += task.getDuration();
        return duration - task.getDuration();
    }

    @Override
    public synchronized boolean isSelected(Task task) {
        return mSelection.contains(task);
    }

    @Override
    public synchronized int minutesSelected() {
        return mMinutesSelected;
    }
}
