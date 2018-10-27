package com.reynouard.alexis.chronos.view;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.reynouard.alexis.chronos.R;
import com.reynouard.alexis.chronos.TaskSelector;
import com.reynouard.alexis.chronos.model.StatedTask;
import com.reynouard.alexis.chronos.model.Task;

import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TaskListAdapter extends RecyclerView.Adapter<TaskListAdapter.TaskViewHolder> {

    private final LayoutInflater mLayoutInflater;
    private final TaskSelector mSelector;
    private final Set<TaskListAdapter.TaskViewHolder> mViewHolders = new HashSet<>();
    private List<StatedTask> mTasks; // Cache
    private OnInsertWorkRequestedListener mOnInsertWorkRequestedListener;
    private OnEditTaskRequestedListener mOnEditTaskRequestedListener;
    private OnDeleteTaskRequestedListener mOnDeleteTaskRequestedListener;
    private int mDurationToDo = 120;

    public TaskListAdapter(Context context, TaskSelector taskSelector) {
        mLayoutInflater = LayoutInflater.from(context);
        mSelector = taskSelector;
    }

    public static int urgencyColor(Double urgency) {
        if (urgency == null || urgency > 2) {
            urgency = 2.0;
        }
        else if (urgency < 0) {
            urgency = 0.0;
        }
        return Color.HSVToColor(new float[]{(float) (240.0-120.0*urgency), 0.5f, 1f});
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = mLayoutInflater.inflate(R.layout.task_item, parent, false);
        TaskViewHolder viewHolder = new TaskViewHolder(itemView);
        mViewHolders.add(viewHolder);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        if (mTasks == null) {
            holder.mTaskNameView.setText(R.string.loading);
        } else {
            StatedTask current = mTasks.get(holder.getAdapterPosition());
            holder.update(current);
        }
    }

    public void setTasks(List<StatedTask> tasks) {
        mTasks = tasks;
        if (mTasks != null) {
            mSelector.select(mTasks, mDurationToDo);
            notifyDataSetChanged();
        }
    }

    public void setDurationToDo(int duration) {
        mDurationToDo = duration;
        if (mTasks != null) {
            mSelector.select(mTasks, mDurationToDo);
            notifyDataSetChanged();
        }
    }

    @Override
    public int getItemCount() {
        return mTasks == null ? 0 : mTasks.size();
    }

    public void setOnInsertWorkRequestedListener(OnInsertWorkRequestedListener onInsertWorkRequestedListener) {
        mOnInsertWorkRequestedListener = onInsertWorkRequestedListener;
    }

    public void setOnEditTaskRequestedListener(OnEditTaskRequestedListener onEditTaskRequestedListener) {
        mOnEditTaskRequestedListener = onEditTaskRequestedListener;
    }

    public void setOnDeleteTaskRequestedListener(OnDeleteTaskRequestedListener onDeleteTaskRequestedListener) {
        mOnDeleteTaskRequestedListener = onDeleteTaskRequestedListener;
    }

    private void collapseAll() {
        for (TaskViewHolder viewHolder : mViewHolders) {
            viewHolder.setExpanded(false);
        }
    }

    class TaskViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final TextView mTaskNameView;
        private final TextView mDueDateView;
        private final ImageView mTodoIconView;
        private final ViewGroup mSimpleTimePicker;
        private boolean mExpanded = true;
        private Task mTask = null;

        private TaskViewHolder(View itemView) {
            super(itemView);
            mTaskNameView = itemView.findViewById(R.id.view_task_name);
            mDueDateView = itemView.findViewById(R.id.view_due_date);
            mTodoIconView = itemView.findViewById(R.id.view_todo_icon);
            mSimpleTimePicker = itemView.findViewById(R.id.view_simple_time_picker);


            LayoutInflater inflater = ((LayoutInflater) itemView.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE));
            if (inflater == null) {
                throw new AssertionError(); // TODO
            }
            else {
                inflater.inflate(R.layout.small_icon_edit_button, mSimpleTimePicker, true);
            }

            itemView.findViewById(R.id.view_morning_button).setOnClickListener(this);
            itemView.findViewById(R.id.view_noon_button).setOnClickListener(this);
            itemView.findViewById(R.id.view_evening_button).setOnClickListener(this);
            itemView.findViewById(R.id.view_now_button).setOnClickListener(this);
            itemView.findViewById(R.id.view_edit_button).setOnClickListener(this);

            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    if (mOnDeleteTaskRequestedListener != null && mTask != null) {
                        mOnDeleteTaskRequestedListener.onDeleteTaskRequested(mTask);
                        return true;
                    }
                    return false;
                }
            });
        }

        private void update(StatedTask task) {
            mTask = task;

            mTaskNameView.setText(task.getName());
            setDueDateViewText(task.getNaturalDaysCountToNextOccurrence(), task.getUrgency());
            Log.d("tasks", "task: " + task.getName() + "; urgency: " + task.getUrgency());
            mTodoIconView.setImageDrawable(mTodoIconView.getContext().getDrawable(mSelector.isSelected(task)
                    ? R.drawable.ic_radio_button_checked_fg_24dp
                    : R.drawable.ic_radio_button_unchecked_fg_24dp));

            setExpanded(false);
        }

        private void setDueDateViewText(@Nullable Integer days, Double urgency) {
            if (days == null) {
                mDueDateView.setText("");
            } else if (days == 0) {
                mDueDateView.setText(R.string.due_today);
            } else if (days == -1) {
                mDueDateView.setText(R.string.due_yesterday);
            } else if (days == 1) {
                mDueDateView.setText(R.string.due_tomorrow);
            } else if (days < 1) {
                mDueDateView.setText(String.format(
                        mDueDateView.getContext().getResources().getString(R.string.due_d_days_ago), -days));
            } else if (days > 1) {
                mDueDateView.setText(String.format(
                        mDueDateView.getContext().getResources().getString(R.string.due_in_days), days));
            }
            mDueDateView.setTextColor(urgencyColor(urgency));
        }

        @Override
        public void onClick(View view) {
            if (mTask == null) throw new AssertionError(); // TODO

            Calendar calendar = Calendar.getInstance();
            int viewId = view.getId();
            switch (viewId) {
                case R.id.view_morning_button :
                case R.id.view_noon_button :
                case R.id.view_evening_button :
                    calendar.set(Calendar.HOUR_OF_DAY,
                            viewId == R.id.view_morning_button ? 10 : (viewId == R.id.view_noon_button ? 15 : 20));
                case R.id.view_now_button:
                    mOnInsertWorkRequestedListener.onInsertWorkRequested(mTask, calendar.getTime());
                    break;
                case R.id.view_edit_button :
                    mOnEditTaskRequestedListener.onEditTaskRequested(this.mTask);
                    break;
                default:
                    toggleExpanded();
            }
        }

        private void toggleExpanded() {
            setExpanded(!mExpanded);
        }

        private void setExpanded(final boolean expand) {
            if (expand) {
                collapseAll();
            }
            if (expand != mExpanded) {
                mExpanded = expand;
                int visibility = mExpanded ? View.VISIBLE : View.GONE;
                mSimpleTimePicker.setVisibility(visibility);
            }
        }
    }
}
