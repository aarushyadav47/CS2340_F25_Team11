package com.example.spendwise.view;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.spendwise.R;
import com.example.spendwise.databinding.DashboardBinding;
import com.example.spendwise.model.Budget;
import com.example.spendwise.model.Expense;
import com.example.spendwise.viewModel.BudgetViewModel;
import com.example.spendwise.viewModel.ExpenseViewModel;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.example.spendwise.adapter.BudgetAdapter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ArrayList;

public class Dashboard extends AppCompatActivity {

}