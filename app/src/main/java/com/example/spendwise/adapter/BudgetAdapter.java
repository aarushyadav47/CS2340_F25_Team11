package com.example.spendwise.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.spendwise.R;
import com.example.spendwise.model.Budget;

import java.util.ArrayList;
import java.util.List;

public class BudgetAdapter extends RecyclerView.Adapter<BudgetAdapter.BudgetViewHolder> {

    public interface OnItemClickListener { void onItemClick(Budget budget); }
    private final List<Budget> budgets = new ArrayList<>();
    private OnItemClickListener onItemClickListener;

    public void setOnItemClickListener(OnItemClickListener listener) { this.onItemClickListener = listener; }

    public void setBudgets(List<Budget> newBudgets) {
        budgets.clear();
        if (newBudgets != null) budgets.addAll(newBudgets);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BudgetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.budget_item, parent, false);
        return new BudgetViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull BudgetViewHolder holder, int position) {
        Budget b = budgets.get(position);
        holder.name.setText(b.getName());
        holder.amount.setText(String.format("$%.2f", b.getAmount()));
        holder.meta.setText(b.getCategory() + " • " + b.getFrequency());

        // Set status indicator color (simple heuristic: no spending yet = green)
        int color = 0xFF00C853; // green default
        holder.itemView.findViewById(R.id.status_indicator).setBackgroundColor(color);

        holder.itemView.setOnClickListener(v -> { if (onItemClickListener != null) onItemClickListener.onItemClick(b); });
    }

    @Override
    public int getItemCount() { return budgets.size(); }

    static class BudgetViewHolder extends RecyclerView.ViewHolder {
        TextView name; TextView amount; TextView meta;
        BudgetViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.budget_name);
            amount = itemView.findViewById(R.id.budget_amount);
            meta = itemView.findViewById(R.id.budget_meta);
        }
    }
}


