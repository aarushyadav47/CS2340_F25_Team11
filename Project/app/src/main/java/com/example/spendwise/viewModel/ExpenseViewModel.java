package com.example.spendwise.viewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.spendwise.model.Category;

import com.example.spendwise.model.Expense;
import com.example.spendwise.model.Firebase;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import android.util.Log;
import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class ExpenseViewModel extends ViewModel {
    
}