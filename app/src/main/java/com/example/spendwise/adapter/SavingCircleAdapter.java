package com.example.spendwise.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.spendwise.R;
import com.example.spendwise.model.SavingCircle;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SavingCircleAdapter extends RecyclerView.Adapter<SavingCircleAdapter.SavingCircleViewHolder> {

    private List<SavingCircle> savingCircles = new ArrayList<>();
    private OnItemClickListener clickListener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.clickListener = listener;
    }

    @NonNull
    @Override
    public SavingCircleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.saving_circle_item, parent, false);
        return new SavingCircleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SavingCircleViewHolder holder, int position) {
        SavingCircle savingCircle = savingCircles.get(position);
        holder.bind(savingCircle);
    }

    @Override
    public int getItemCount() {
        return savingCircles.size();
    }

    public void setSavingCircles(List<SavingCircle> savingCircles) {
        this.savingCircles = savingCircles;
        notifyDataSetChanged();
    }

    public SavingCircle getSavingCircleAt(int position) {
        return savingCircles.get(position);
    }

    public interface OnItemClickListener {
        void onItemClick(SavingCircle savingCircle);
    }

    class SavingCircleViewHolder extends RecyclerView.ViewHolder {

        private TextView textViewGroupName;
        private TextView textViewChallengeTitle;
        private TextView textViewGoalAmount;
        private TextView textViewFrequency;

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

        public void bind(SavingCircle savingCircle) {
            textViewGroupName.setText(savingCircle.getGroupName());
            textViewChallengeTitle.setText(savingCircle.getChallengeTitle());
            textViewGoalAmount.setText(String.format(Locale.US, "$%.2f", savingCircle.getGoalAmount()));
            textViewFrequency.setText(savingCircle.getFrequency());
        }
    }
}