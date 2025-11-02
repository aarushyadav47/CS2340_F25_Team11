package com.example.spendwise.viewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.spendwise.model.SavingCircle;
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

public class SavingCircleViewModel extends ViewModel {
    private static final String TAG = "SavingCircleViewModel";

    private MutableLiveData<String> statusMessage;
    private MutableLiveData<List<SavingCircle>> savingCircles;
    private FirebaseDatabase database;
    private DatabaseReference savingCirclesRef; // references to the savingCircles collection
    // Firebase is a json so points to that node
    private FirebaseAuth auth;

    public SavingCircleViewModel() {
        savingCircles = new MutableLiveData<>(new ArrayList<>());
        statusMessage = new MutableLiveData<>();

        // Initialize Firebase
        database = Firebase.getDatabase(); // gets it from package.json
        auth = FirebaseAuth.getInstance(); // gets user info

        // Setup user specific path for the proper structure in database tree,
        // and correct retrieval later
        setupUserSavingCirclesReference();
        // Load saving circles from Firebase when ViewModel is created (a function)
        loadSavingCirclesFromFirebase();
    }

    // Setup reference based on current user
    private void setupUserSavingCirclesReference() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            // Path: users/{uid}/savingCircles
            savingCirclesRef = database.getReference("users").child(uid)
                    .child("savingCircles");
            Log.d(TAG, "SavingCircles reference set for user: " + uid);
        } else {
            Log.e(TAG, "No user logged in!");
            statusMessage.setValue("Please log in to manage saving circles");
        }
    }

    public LiveData<List<SavingCircle>> getSavingCircles() {
        return savingCircles;
    }

    // Add new saving circle to Firebase
    public void addSavingCircle(String groupName, String creatorEmail, String challengeTitle,
                                double goalAmount, String frequency, String notes) {
        if (savingCirclesRef == null) {
            Log.e(TAG, "savingCirclesRef is null! Cannot add saving circle.");
            statusMessage.setValue("Error: User not logged in");
            return;
        }

        SavingCircle savingCircle = new SavingCircle(groupName, creatorEmail, challengeTitle,
                goalAmount, frequency, notes);
        // Push to Firebase (auto-generates ID)
        // creates a new child location in the database tree with unique id
        DatabaseReference newSavingCircleRef = savingCirclesRef.push();
        // gets the key associated with the pointer ie /savingCircles/mkdfjos
        String firebaseId = newSavingCircleRef.getKey();
        // ensures the key in database and of object is aligned
        savingCircle.setId(firebaseId);

        Log.d(TAG, "Adding saving circle to Firebase: " + savingCircle);

        newSavingCircleRef.setValue(savingCircle)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Saving circle added successfully: " + savingCircle);
                    statusMessage.setValue("Saving circle created!");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding saving circle", e);
                    statusMessage.setValue("Error: " + e.getMessage());
                });
        // setValue in that pointer location that holds no data
        // with serialized (json formatted) data
    }

    public LiveData<String> getStatusMessage() {
        return statusMessage;
    }

    // Update existing saving circle in Firebase
    public void updateSavingCircle(String id, String groupName, String creatorEmail,
                                   String challengeTitle, double goalAmount,
                                   String frequency, String notes) {
        if (savingCirclesRef == null) {
            statusMessage.setValue("User not authenticated");
            return;
        }

        SavingCircle savingCircle = new SavingCircle(groupName, creatorEmail, challengeTitle,
                goalAmount, frequency, notes);
        savingCircle.setId(id);

        savingCirclesRef.child(id).setValue(savingCircle)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Saving circle updated successfully");
                    statusMessage.setValue("Saving circle updated!");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating saving circle", e);
                    statusMessage.setValue("Error: " + e.getMessage());
                });
    }

    // Load saving circles from Firebase
    private void loadSavingCirclesFromFirebase() {
        if (savingCirclesRef == null) {
            Log.e(TAG, "savingCirclesRef is null! Cannot load saving circles.");
            return;
        }

        savingCirclesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<SavingCircle> savingCircleList = new ArrayList<>();

                for (DataSnapshot savingCircleSnapshot : snapshot.getChildren()) {
                    try {
                        // Get the saving circle data and parse it correctly
                        String id = savingCircleSnapshot.getKey();
                        String groupName = savingCircleSnapshot.child("groupName")
                                .getValue(String.class);
                        String creatorEmail = savingCircleSnapshot.child("creatorEmail")
                                .getValue(String.class);
                        String challengeTitle = savingCircleSnapshot.child("challengeTitle")
                                .getValue(String.class);
                        Double goalAmount = savingCircleSnapshot.child("goalAmount")
                                .getValue(Double.class);
                        String frequency = savingCircleSnapshot.child("frequency")
                                .getValue(String.class);
                        String notes = savingCircleSnapshot.child("notes")
                                .getValue(String.class);
                        Long createdAt = savingCircleSnapshot.child("createdAt")
                                .getValue(Long.class);

                        // Create saving circle object
                        if (groupName != null && creatorEmail != null && challengeTitle != null
                                && goalAmount != null && frequency != null) {
                            SavingCircle savingCircle = new SavingCircle(
                                    id,
                                    groupName,
                                    creatorEmail,
                                    challengeTitle,
                                    goalAmount,
                                    frequency,
                                    notes != null ? notes : "",
                                    createdAt != null ? createdAt : System.currentTimeMillis()
                            );
                            savingCircleList.add(savingCircle);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing saving circle", e);
                    }
                }

                savingCircles.setValue(savingCircleList);
                Log.d(TAG, "Loaded " + savingCircleList.size()
                        + " saving circles from Firebase");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase error: " + error.getMessage());
                statusMessage.setValue("Error loading saving circles: "
                        + error.getMessage());
            }
        });
    }

    // Delete saving circle from Firebase
    public void deleteSavingCircle(String id) {
        if (savingCirclesRef == null) {
            statusMessage.setValue("User not authenticated");
            return;
        }

        savingCirclesRef.child(id).removeValue()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Saving circle deleted successfully");
                    statusMessage.setValue("Saving circle deleted!");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting saving circle", e);
                    statusMessage.setValue("Error: " + e.getMessage());
                });
    }
}