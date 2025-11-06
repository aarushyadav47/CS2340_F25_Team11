package com.example.spendwise.view;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import androidx.lifecycle.ViewModelProvider;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.spendwise.R;
import com.example.spendwise.databinding.ExpenselogBinding;
import com.example.spendwise.model.Category;
import com.example.spendwise.model.Expense;

import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Locale;
import android.app.DatePickerDialog;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;
import com.example.spendwise.adapter.ExpenseAdapter;

import com.example.spendwise.viewModel.ExpenseViewModel;
import com.google.android.material.textfield.TextInputEditText;

import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import java.util.Collections;
import java.util.List;
import java.util.Comparator;
import java.util.Date;
import java.util.ArrayList;

public class ExpenseLog extends AppCompatActivity {
    
}