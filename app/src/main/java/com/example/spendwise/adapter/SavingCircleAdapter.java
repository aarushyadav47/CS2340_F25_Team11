package com.example.spendwise.adapter;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.spendwise.R;
import com.example.spendwise.model.SavingCircle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SavingCircleAdapter extends RecyclerView.Adapter<SavingCircleAdapter.SavingCircleViewHolder> {

    private List<SavingCircle> savingCircles = new ArrayList<>();
    private Map<String, CircleProgress> progressMap = new HashMap<>();
    private OnItemClickListener clickListener;

    /** Data class to hold progress information */
    public static class CircleProgress {
        public double currentAmount;
        public double goalAmount;
        public int percentage;

        public CircleProgress(double currentAmount, double goalAmount) {
            this.currentAmount = currentAmount;
            this.goalAmount = goalAmount;
            this.percentage = goalAmount > 0 ? (int) ((currentAmount / goalAmount) * 100) : 0;
        }
    }

    /** Listener for item clicks */
    public interface OnItemClickListener {
        void onItemClick(SavingCircle savingCircle);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.clickListener = listener;
    }

    @NonNull
    @Override
    public SavingCircleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.savingcircle_item, parent, false);
        return new SavingCircleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SavingCircleViewHolder holder, int position) {
        SavingCircle circle = savingCircles.get(position);
        CircleProgress progress = progressMap.get(circle.getId());
        holder.bind(circle, progress);
    }

    @Override
    public int getItemCount() {
        return savingCircles != null ? savingCircles.size() : 0;
    }

    /** Update list efficiently using DiffUtil */
    public void setSavingCircles(List<SavingCircle> newList) {
        if (newList == null) return;

        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return savingCircles.size();
            }

            @Override
            public int getNewListSize() {
                return newList.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return savingCircles.get(oldItemPosition).getId()
                        .equals(newList.get(newItemPosition).getId());
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                SavingCircle oldCircle = savingCircles.get(oldItemPosition);
                SavingCircle newCircle = newList.get(newItemPosition);
                return oldCircle.equals(newCircle);
            }
        });

        savingCircles = new ArrayList<>(newList);
        diffResult.dispatchUpdatesTo(this);
    }

    /** Update progress for a specific circle */
    public void setCircleProgress(String circleId, double currentAmount, double goalAmount) {
        progressMap.put(circleId, new CircleProgress(currentAmount, goalAmount));

        // Find the position and notify
        for (int i = 0; i < savingCircles.size(); i++) {
            if (savingCircles.get(i).getId().equals(circleId)) {
                notifyItemChanged(i);
                break;
            }
        }
    }

    /** Retrieve a circle for swipe-to-delete */
    public SavingCircle getSavingCircleAt(int position) {
        return savingCircles.get(position);
    }

    /** ------------------------ ViewHolder ------------------------ */
    class SavingCircleViewHolder extends RecyclerView.ViewHolder {

        private final TextView textViewGroupName;
        private final TextView textViewChallengeTitle;
        private final TextView textViewGoalAmount;
        private final TextView textViewFrequency;
        private final TextView textViewProgress;
        private final ProgressBar progressBar;

        @SuppressLint("DefaultLocale")
        public void bind(SavingCircle savingCircle, CircleProgress progress) {
            textViewGroupName.setText(savingCircle.getGroupName());
            textViewChallengeTitle.setText(savingCircle.getChallengeTitle());
            textViewGoalAmount.setText(String.format(Locale.US, "$%.2f", savingCircle.getGoalAmount()));
            textViewFrequency.setText(savingCircle.getFrequency());

            if (progress != null) {
                textViewProgress.setText(String.format(Locale.US, "$%.2f / $%.2f (%d%%)",
                        progress.currentAmount, progress.goalAmount, progress.percentage));
                progressBar.setProgress(Math.min(progress.percentage, 100));
                textViewProgress.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.VISIBLE);
            } else {
                textViewProgress.setText("Loading...");
                progressBar.setProgress(0);
                textViewProgress.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.VISIBLE);
            }
        }

        public SavingCircleViewHolder(@NonNull View itemView) {
            super(itemView);

            textViewGroupName = itemView.findViewById(R.id.text_view_group_name);
            textViewChallengeTitle = itemView.findViewById(R.id.text_view_challenge_title);
            textViewGoalAmount = itemView.findViewById(R.id.text_view_goal_amount);
            textViewFrequency = itemView.findViewById(R.id.text_view_frequency);
            textViewProgress = itemView.findViewById(R.id.text_view_progress);
            progressBar = itemView.findViewById(R.id.progress_bar);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (clickListener != null && position != RecyclerView.NO_POSITION) {
                    clickListener.onItemClick(savingCircles.get(position));
                }
            });
        }
    }
}