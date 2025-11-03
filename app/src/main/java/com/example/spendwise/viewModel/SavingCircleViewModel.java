package com.example.spendwise.viewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.spendwise.model.SavingCircle;
import com.example.spendwise.model.SavingCircleMember;
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
    private MutableLiveData<String> currentUserEmail;
    private FirebaseDatabase database;
    private DatabaseReference savingCirclesRef;
    private FirebaseAuth auth;

    public SavingCircleViewModel() {
        savingCircles = new MutableLiveData<>(new ArrayList<>());
        statusMessage = new MutableLiveData<>();
        currentUserEmail = new MutableLiveData<>();

        // Initialize Firebase
        database = Firebase.getDatabase();
        auth = FirebaseAuth.getInstance();

        // Load current user email
        loadCurrentUserEmail();

        // Setup user specific path for the proper structure in database tree
        setupUserSavingCirclesReference();

        // Load saving circles from Firebase when ViewModel is created
        loadSavingCirclesFromFirebase();
    }

    // Load the current user's email from Firebase Auth
    private void loadCurrentUserEmail() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            String email = currentUser.getEmail();
            currentUserEmail.setValue(email);
            Log.d(TAG, "Current user email loaded: " + email);
        } else {
            Log.e(TAG, "No user logged in!");
            currentUserEmail.setValue("");
        }
    }

    /**
     * Get the current user's email as LiveData
     * @return LiveData containing the user's email
     */
    public LiveData<String> getCurrentUserEmail() {
        return currentUserEmail;
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

    // UPDATED: Add new saving circle to Firebase AND add creator as first member
    public void addSavingCircle(String groupName, String creatorEmail, String challengeTitle,
                                double goalAmount, String frequency, String notes,
                                double personalAllocation) {
        if (savingCirclesRef == null) {
            Log.e(TAG, "savingCirclesRef is null! Cannot add saving circle.");
            statusMessage.setValue("Error: User not logged in");
            return;
        }

        SavingCircle savingCircle = new SavingCircle(groupName, creatorEmail, challengeTitle,
                goalAmount, frequency, notes);
        // Push to Firebase (auto-generates ID)
        DatabaseReference newSavingCircleRef = savingCirclesRef.push();
        String firebaseId = newSavingCircleRef.getKey();
        savingCircle.setId(firebaseId);

        Log.d(TAG, "Adding saving circle to Firebase: " + savingCircle);

        newSavingCircleRef.setValue(savingCircle)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Saving circle added successfully: " + savingCircle);

                    // Now add the creator as the first member
                    addMemberToCircle(firebaseId, creatorEmail, personalAllocation);

                    statusMessage.setValue("Saving circle created!");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding saving circle", e);
                    statusMessage.setValue("Error: " + e.getMessage());
                });
    }

    // NEW: Add a member to a saving circle
    public void addMemberToCircle(String circleId, String memberEmail, double personalAllocation) {
        if (savingCirclesRef == null) {
            Log.e(TAG, "savingCirclesRef is null!");
            return;
        }

        SavingCircleMember member = new SavingCircleMember(memberEmail, personalAllocation);

        // Path: users/{uid}/savingCircles/{circleId}/members/{memberEmail-sanitized}
        // Sanitize email because Firebase keys can't contain . or @
        String sanitizedEmail = memberEmail.replace(".", "_").replace("@", "_at_");

        savingCirclesRef.child(circleId)
                .child("members")
                .child(sanitizedEmail)
                .setValue(member)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Member added to circle: " + memberEmail);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding member to circle", e);
                });
    }

    // NEW: Deduct expense from member's current amount
    public void deductExpenseFromMember(String circleId, String memberEmail, double expenseAmount) {
        if (savingCirclesRef == null) {
            Log.e(TAG, "savingCirclesRef is null!");
            return;
        }

        String sanitizedEmail = memberEmail.replace(".", "_").replace("@", "_at_");

        // Get current amount first
        savingCirclesRef.child(circleId)
                .child("members")
                .child(sanitizedEmail)
                .child("currentAmount")
                .get()
                .addOnSuccessListener(dataSnapshot -> {
                    Double currentAmount = dataSnapshot.getValue(Double.class);
                    if (currentAmount != null) {
                        double newAmount = currentAmount - expenseAmount;

                        // Don't let it go below 0
                        if (newAmount < 0) {
                            newAmount = 0;
                        }

                        // Update the current amount
                        savingCirclesRef.child(circleId)
                                .child("members")
                                .child(sanitizedEmail)
                                .child("currentAmount")
                                .setValue(newAmount)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Expense deducted from member's current amount");
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error deducting expense", e);
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching current amount", e);
                });
    }

    // NEW: Add expense amount back (if user deletes an expense)
    public void addBackExpenseToMember(String circleId, String memberEmail, double expenseAmount) {
        if (savingCirclesRef == null) return;

        String sanitizedEmail = memberEmail.replace(".", "_").replace("@", "_at_");

        savingCirclesRef.child(circleId)
                .child("members")
                .child(sanitizedEmail)
                .child("currentAmount")
                .get()
                .addOnSuccessListener(dataSnapshot -> {
                    Double currentAmount = dataSnapshot.getValue(Double.class);
                    if (currentAmount != null) {
                        double newAmount = currentAmount + expenseAmount;

                        // Update the current amount
                        savingCirclesRef.child(circleId)
                                .child("members")
                                .child(sanitizedEmail)
                                .child("currentAmount")
                                .setValue(newAmount)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Expense amount added back");
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error adding back expense", e);
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching current amount", e);
                });
    }

    // NEW: Update member's current amount directly (for manual deposits/contributions)
    public void updateMemberCurrentAmount(String circleId, String memberEmail, double newAmount) {
        if (savingCirclesRef == null) return;

        String sanitizedEmail = memberEmail.replace(".", "_").replace("@", "_at_");

        savingCirclesRef.child(circleId)
                .child("members")
                .child(sanitizedEmail)
                .child("currentAmount")
                .setValue(newAmount)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Member current amount updated");
                    statusMessage.setValue("Amount updated!");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating member amount", e);
                });
    }

    public LiveData<String> getStatusMessage() {
        return statusMessage;
    }

    // UPDATED: Update existing saving circle in Firebase
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
                                    groupName,
                                    creatorEmail,
                                    challengeTitle,
                                    goalAmount,
                                    frequency,
                                    notes != null ? notes : ""
                            );
                            savingCircle.setId(id);
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