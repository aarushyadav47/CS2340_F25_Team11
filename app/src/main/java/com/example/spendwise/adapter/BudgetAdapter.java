package com.example.spendwise.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.spendwise.R;
import com.example.spendwise.model.Budget;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BudgetAdapter extends RecyclerView.Adapter<BudgetAdapter.ViewHolder> {

    private List<Budget> budgets;

    public BudgetAdapter() {
        this.budgets = new ArrayList<>();
    }

    public BudgetAdapter(List<Budget> budgets) {
        this.budgets = budgets;
    }

    public Budget getBudgetAt(int position) {
        if (budgets != null && position >= 0 && position < budgets.size()) {
            return budgets.get(position);
        }
        return null;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.budget_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Budget budget = budgets.get(position);

        double remaining = budget.getAmount();
        double original = budget.getOriginalAmount();

        holder.getNameText().setText(budget.getName());
        holder.getCategoryText().setText(
                budget.getCategory().getDisplayName() + " · " + budget.getfreq()
        );
        holder.getAmountText().setText(String.format(Locale.US, "$%.2f", remaining));
        holder.getDateText().setText(budget.getDate());

        // Color logic based on remaining amount
        if (remaining < 0) {
            holder.getAmountText().setTextColor(ContextCompat.getColor(
                    holder.itemView.getContext(), android.R.color.holo_red_dark));
        } else if (remaining <= 0.3 * original) {
            holder.getAmountText().setTextColor(
                    ContextCompat.getColor(holder.itemView.getContext(),
                            android.R.color.holo_orange_light));
        } else {
            holder.getAmountText().setTextColor(Color.parseColor("#4CAF50"));
        }
    }

    @Override
    public int getItemCount() {
        return budgets != null ? budgets.size() : 0;
    }

    // Optional helper if you want to update the list dynamically
    public void setBudgets(List<Budget> newBudgets) {
        this.budgets = newBudgets;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView nameText;
        private TextView categoryText;
        private TextView amountText;
        private TextView dateText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.text_view_budget_name);
            categoryText = itemView.findViewById(R.id.text_view_budget_category);
            amountText = itemView.findViewById(R.id.text_view_budget_amount);
            dateText = itemView.findViewById(R.id.text_view_budget_date);
        }

        public TextView getNameText() {
            return nameText;
        }

        public TextView getCategoryText() {
            return categoryText;
        }

        public TextView getAmountText() {
            return amountText;
        }

        public TextView getDateText() {
            return dateText;
        }
    }
}