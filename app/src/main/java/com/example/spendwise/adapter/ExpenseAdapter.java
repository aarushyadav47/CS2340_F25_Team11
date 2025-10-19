package com.example.spendwise.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.spendwise.R;
import com.example.spendwise.model.Expense;

import java.util.ArrayList;
import java.util.List;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(Expense expense);
    }

    private final List<Expense> expenses = new ArrayList<>();
    private OnItemClickListener onItemClickListener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    public void setExpenses(List<Expense> newExpenses) {
        expenses.clear();
        if (newExpenses != null)
            expenses.addAll(newExpenses);
        notifyDataSetChanged();
    }

    public Expense getExpenseAt(int position) {
        return expenses.get(position);
    }

    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.expense_item, parent, false);
        return new ExpenseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
        Expense e = expenses.get(position);
        holder.name.setText(e.getName());
        holder.amount.setText(String.format("$%.2f", e.getAmount()));
        holder.category.setText(e.getCategory().getDisplayName());
        holder.date.setText(e.getDate());

        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null)
                onItemClickListener.onItemClick(e);
        });
    }

    @Override
    public int getItemCount() {
        return expenses.size();
    }

    static class ExpenseViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        TextView amount;
        TextView category;
        TextView date;

        ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.item_name);
            amount = itemView.findViewById(R.id.item_amount);
            category = itemView.findViewById(R.id.item_category);
            date = itemView.findViewById(R.id.item_date);
        }
    }
}
