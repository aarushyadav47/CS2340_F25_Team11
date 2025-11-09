package com.example.spendwise.adapter;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.spendwise.R;
import com.example.spendwise.model.SavingCircle;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SavingCircleAdapter extends RecyclerView.Adapter<SavingCircleAdapter.SavingCircleViewHolder> {

    private List<SavingCircle> savingCircles = new ArrayList<>();
    private OnItemClickListener clickListener;

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
        holder.bind(savingCircles.get(position));
    }

    @Override
    public int getItemCount() {
        return savingCircles != null ? savingCircles.size() : 0;
    }

    /** 🔄 Update list efficiently using DiffUtil */
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

        @SuppressLint("DefaultLocale")
        public void bind(SavingCircle savingCircle) {
            textViewGroupName.setText(savingCircle.getGroupName());
            textViewChallengeTitle.setText(savingCircle.getChallengeTitle());
            textViewGoalAmount.setText(String.format(Locale.US, "$%.2f", savingCircle.getGoalAmount()));
            textViewFrequency.setText(savingCircle.getFrequency());
        }

        public SavingCircleViewHolder(@NonNull View itemView) {
            super(itemView);

            textViewGroupName = itemView.findViewById(R.id.text_view_group_name);
            textViewChallengeTitle = itemView.findViewById(R.id.text_view_challenge_title);
            textViewGoalAmount = itemView.findViewById(R.id.text_view_goal_amount);
            textViewFrequency = itemView.findViewById(R.id.text_view_frequency);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (clickListener != null && position != RecyclerView.NO_POSITION) {
                    clickListener.onItemClick(savingCircles.get(position));
                }
            });
        }
    }
}
