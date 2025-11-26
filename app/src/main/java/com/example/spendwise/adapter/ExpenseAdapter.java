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
import java.util.Locale;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder> {

    private List<Expense> expenses = new ArrayList<>();
    private OnItemClickListener clickListener;

    // Set click listener for item clicks
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.clickListener = listener;
    }

    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.expense_item, parent, false);
        return new ExpenseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
        Expense expense = expenses.get(position);
        holder.bind(expense);
    }

    @Override
    public int getItemCount() {
        return expenses.size();
    }

    // Update the expense list
    public void setExpenses(List<Expense> expenses) {
        this.expenses = expenses;
        notifyDataSetChanged();
    }

    // Get expense at specific position
    public Expense getExpenseAt(int position) {
        return expenses.get(position);
    }

    public interface OnItemClickListener {
        void onItemClick(Expense expense);
    }

    class ExpenseViewHolder extends RecyclerView.ViewHolder {

        private TextView textViewName;
        private TextView textViewAmount;
        private TextView textViewCategory;
        private TextView textViewDate;

        // Initialize view holder with item view
        public ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.text_view_name);
            textViewAmount = itemView.findViewById(R.id.text_view_amount);
            textViewCategory = itemView.findViewById(R.id.text_view_category);
            textViewDate = itemView.findViewById(R.id.text_view_date);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (clickListener != null && position != RecyclerView.NO_POSITION) {
                    clickListener.onItemClick(expenses.get(position));
                }
            });
        }

        // Bind expense data to views
        public void bind(Expense expense) {
            textViewName.setText(expense.getName());
            textViewAmount.setText(String.format(Locale.US, "$%.2f", expense.getAmount()));
            textViewCategory.setText(expense.getCategory().getDisplayName());
            textViewDate.setText(expense.getDate());
        }
    }
}