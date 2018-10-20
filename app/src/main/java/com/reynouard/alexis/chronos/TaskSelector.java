package com.reynouard.alexis.chronos;

import com.reynouard.alexis.chronos.model.StatedTask;
import com.reynouard.alexis.chronos.model.Task;

import java.util.List;

public interface TaskSelector {
    void select(List<StatedTask> tasks, int duration);

    boolean isSelected(Task task);

    int minutesSelected();
}
