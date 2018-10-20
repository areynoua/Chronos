package com.reynouard.alexis.chronos.view;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.reynouard.alexis.chronos.R;
import com.reynouard.alexis.chronos.model.Work;

import java.text.DateFormat;
import java.util.List;

public class WorkListAdapter extends RecyclerView.Adapter<WorkListAdapter.WorkViewHolder> {

    private static final DateFormat DATE_FORMAT = DateFormat.getDateInstance();
    private final LayoutInflater mLayoutInflater;
    private List<Work> mWorks;
    private OnDeleteWorkRequestedListener mOnDeleteWorkRequestedListener;

    public WorkListAdapter(Context context) {
        mLayoutInflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public WorkViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = mLayoutInflater.inflate(R.layout.work_item, parent, false);
        return new WorkListAdapter.WorkViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull WorkViewHolder holder, int position) {
        if (mWorks == null) {
            holder.mWorkDateView.setText("Nothing here");
        } else {
            Work current = mWorks.get(holder.getAdapterPosition());
            holder.update(current);
        }
    }

    @Override
    public int getItemCount() {
        return mWorks == null ? 0 : mWorks.size();
    }

    public void setWorks(List<Work> works) {
        mWorks = works;
        notifyDataSetChanged();
    }

    public void setOnDeleteWorkRequestedListener(OnDeleteWorkRequestedListener onDeleteWorkRequestedListener) {
        mOnDeleteWorkRequestedListener = onDeleteWorkRequestedListener;
    }

    class WorkViewHolder extends RecyclerView.ViewHolder {
        private final TextView mWorkDateView;
        private Work mWork;

        WorkViewHolder(@NonNull View itemView) {
            super(itemView);

            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    if (mOnDeleteWorkRequestedListener != null && mWork != null) {
                        mOnDeleteWorkRequestedListener.onDeleteWorkRequested(mWork);
                        return true;
                    }
                    return false;
                }
            });
            mWorkDateView = itemView.findViewById(R.id.view_work_date);
        }

        void update(Work work) {
            mWork = work;
            // mWorkDateView.setText(DATE_FORMAT.format(work.getDate())); // TODO: use this one
            mWorkDateView.setText(DateFormat.getDateTimeInstance().format(work.getDate()));
            Log.d("WorkViewHolder", "Update: " + work.getDate());
        }
    }
}
