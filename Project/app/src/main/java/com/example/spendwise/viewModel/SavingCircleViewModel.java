package com.example.spendwise.viewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.spendwise.model.SavingCircle;
import com.example.spendwise.model.SavingCircleMember;
import com.example.spendwise.model.MemberCycle;
import com.example.spendwise.model.Firebase;
import com.example.spendwise.model.SavingCircleInvitation;

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
import java.util.Calendar;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class SavingCircleViewModel extends ViewModel {
    private static final String TAG = "SavingCircleViewModel";

    private MutableLiveData<String> statusMessage;
    private MutableLiveData<List<SavingCircle>> savingCircles;
    private MutableLiveData<String> currentUserEmail;
    private MutableLiveData<List<SavingCircleInvitation>> invitations;
    private FirebaseDatabase database;
    private DatabaseReference savingCirclesRef;
    private DatabaseReference invitationsRootRef;
    private FirebaseAuth auth;
    private ValueEventListener invitationsListener;

    public SavingCircleViewModel() {
        savingCircles = new MutableLiveData<>(new ArrayList<>());
        statusMessage = new MutableLiveData<>();
        currentUserEmail = new MutableLiveData<>();
        invitations = new MutableLiveData<>(new ArrayList<>());

        // Initialize Firebase
        database = Firebase.getDatabase();
        auth = FirebaseAuth.getInstance();
        invitationsRootRef = database.getReference("invitations");

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
            attachInvitationListener(email);
        } else {
            Log.e(TAG, "No user logged in!");
            currentUserEmail.setValue("");
        }
    }

    public LiveData<String> getCurrentUserEmail() {
        return currentUserEmail;
    }

    // Setup reference based on current user
    private void setupUserSavingCirclesReference() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            savingCirclesRef = database.getReference("users").child(uid)
                    .child("savingCircles");
            Log.d(TAG, "SavingCircles reference set for user: " + uid);
        } else {
            Log.e(TAG, "No user logged in!");
            statusMessage.setValue("Please log in to manage saving circles");
        }
    }

    private void attachInvitationListener(String email) {
        if (email == null || email.isEmpty() || invitationsRootRef == null) {
            return;
        }

        String sanitizedEmail = sanitizeEmail(email);

        if (invitationsListener != null) {
            invitationsRootRef.child(sanitizedEmail).removeEventListener(invitationsListener);
        }

        invitationsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<SavingCircleInvitation> invitationList = new ArrayList<>();
                for (DataSnapshot invitationSnapshot : snapshot.getChildren()) {
                    SavingCircleInvitation invitation = invitationSnapshot.getValue(SavingCircleInvitation.class);
                    if (invitation != null) {
                        invitationList.add(invitation);
                    }
                }
                invitations.setValue(invitationList);
                Log.d(TAG, "Loaded " + invitationList.size() + " invitations for " + email);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading invitations: " + error.getMessage());
            }
        };

        invitationsRootRef.child(sanitizedEmail).addValueEventListener(invitationsListener);
    }

    public LiveData<List<SavingCircleInvitation>> getInvitations() {
        return invitations;
    }

    public LiveData<List<SavingCircle>> getSavingCircles() {
        return savingCircles;
    }

    // Add new saving circle to Firebase AND add creator as first member
    public void addSavingCircle(String groupName, String creatorEmail, String challengeTitle,
                                double goalAmount, String frequency, String notes,
                                double personalAllocation, long dashboardTimestamp) {
        if (savingCirclesRef == null) {
            Log.e(TAG, "savingCirclesRef is null! Cannot add saving circle.");
            statusMessage.setValue("Error: User not logged in");
            return;
        }

        SavingCircle savingCircle = new SavingCircle(groupName, creatorEmail, challengeTitle,
                goalAmount, frequency, notes, dashboardTimestamp);

        DatabaseReference newSavingCircleRef = savingCirclesRef.push();
        String firebaseId = newSavingCircleRef.getKey();
        savingCircle.setId(firebaseId);

        Log.d(TAG, "Adding saving circle to Firebase: " + savingCircle);

        newSavingCircleRef.setValue(savingCircle)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Saving circle added successfully: " + savingCircle);

                    // Add the creator as the first member with cycle initialization
                    addMemberToCircle(firebaseId, creatorEmail, personalAllocation,
                            dashboardTimestamp, frequency);

                    statusMessage.setValue("Saving circle created!");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding saving circle", e);
                    statusMessage.setValue("Error: " + e.getMessage());
                });
    }

    // UPDATED: Add a member to a saving circle with cycle initialization
    public void addMemberToCircle(String circleId, String memberEmail,
                                  double personalAllocation, long joinTimestamp,
                                  String frequency) {
        if (savingCirclesRef == null) {
            Log.e(TAG, "savingCirclesRef is null!");
            return;
        }

        SavingCircleMember member = new SavingCircleMember(memberEmail, personalAllocation, joinTimestamp);
        String sanitizedEmail = sanitizeEmail(memberEmail);

        savingCirclesRef.child(circleId)
                .child("members")
                .child(sanitizedEmail)
                .setValue(member)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Member added to circle: " + memberEmail);

                    // Initialize first cycle for this member
                    initializeMemberCycle(circleId, memberEmail, joinTimestamp,
                            frequency, personalAllocation);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding member to circle", e);
                });
    }

    // ==================== INVITATION METHODS ====================

    public void sendInvitation(String circleId, String inviteeEmail, OnInvitationSentListener listener) {
        if (savingCirclesRef == null || invitationsRootRef == null) {
            if (listener != null) listener.onError("Database reference not initialized");
            return;
        }

        savingCirclesRef.child(circleId).get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        if (listener != null) listener.onError("Saving circle not found");
                        return;
                    }

                    String groupName = snapshot.child("groupName").getValue(String.class);
                    String challengeTitle = snapshot.child("challengeTitle").getValue(String.class);
                    Double goalAmount = snapshot.child("goalAmount").getValue(Double.class);
                    String frequency = snapshot.child("frequency").getValue(String.class);
                    String inviterEmail = currentUserEmail.getValue();

                    if (groupName == null || challengeTitle == null || goalAmount == null) {
                        if (listener != null) listener.onError("Circle data incomplete");
                        return;
                    }

                    SavingCircleInvitation invitation = new SavingCircleInvitation(
                            circleId,
                            groupName,
                            challengeTitle,
                            inviterEmail != null ? inviterEmail : "",
                            inviteeEmail,
                            goalAmount,
                            frequency != null ? frequency : "Monthly"
                    );

                    String sanitizedInvitee = sanitizeEmail(inviteeEmail);
                    invitationsRootRef.child(sanitizedInvitee)
                            .child(invitation.getInvitationId())
                            .setValue(invitation)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Invitation sent to " + inviteeEmail);
                                if (listener != null) listener.onInvitationSent();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error sending invitation", e);
                                if (listener != null) listener.onError(e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching circle for invitation", e);
                    if (listener != null) listener.onError(e.getMessage());
                });
    }

    public void acceptInvitation(SavingCircleInvitation invitation, OnInvitationActionListener listener) {
        if (invitation == null) {
            if (listener != null) listener.onError("Invitation not found");
            return;
        }

        long responseTimestamp = System.currentTimeMillis();
        double allocation = invitation.getGoalAmount() > 0 ? invitation.getGoalAmount() : 0;
        String frequency = invitation.getFrequency() != null ? invitation.getFrequency() : "Monthly";

        addMemberToCircle(invitation.getCircleId(),
                invitation.getInviteeEmail(),
                allocation,
                responseTimestamp,
                frequency);

        updateInvitationStatus(invitation, "accepted", responseTimestamp, listener);
    }

    public void declineInvitation(SavingCircleInvitation invitation, OnInvitationActionListener listener) {
        if (invitation == null) {
            if (listener != null) listener.onError("Invitation not found");
            return;
        }

        long responseTimestamp = System.currentTimeMillis();
        updateInvitationStatus(invitation, "declined", responseTimestamp, listener);
    }

    private void updateInvitationStatus(SavingCircleInvitation invitation,
                                        String status,
                                        long respondedAt,
                                        OnInvitationActionListener listener) {
        if (invitationsRootRef == null) {
            if (listener != null) listener.onError("Database reference not initialized");
            return;
        }

        String sanitizedInvitee = sanitizeEmail(invitation.getInviteeEmail());
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);
        updates.put("respondedAt", respondedAt);

        invitationsRootRef.child(sanitizedInvitee)
                .child(invitation.getInvitationId())
                .updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Invitation " + status + " for " + invitation.getInviteeEmail());
                    if (listener != null) listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating invitation status", e);
                    if (listener != null) listener.onError(e.getMessage());
                });
    }

    // ==================== CYCLE MANAGEMENT METHODS ====================

    /**
     * Initialize first cycle when member joins
     */
    public void initializeMemberCycle(String circleId, String memberEmail,
                                      long joinDate, String frequency,
                                      double startAmount) {
        if (savingCirclesRef == null) return;

        // Calculate cycle end date
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(joinDate);

        if ("Weekly".equals(frequency)) {
            calendar.add(Calendar.WEEK_OF_YEAR, 1);
        } else { // Monthly
            calendar.add(Calendar.MONTH, 1);
        }

        long endDate = calendar.getTimeInMillis();

        // Create first cycle
        MemberCycle firstCycle = new MemberCycle(joinDate, endDate, startAmount);

        String sanitizedEmail = sanitizeEmail(memberEmail);

        savingCirclesRef.child(circleId)
                .child("members")
                .child(sanitizedEmail)
                .child("cycles")
                .child(firstCycle.getCycleId())
                .setValue(firstCycle)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Initial cycle created for member: " + memberEmail);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating initial cycle", e);
                });
    }

    /**
     * Get current active cycle for a member
     */
    public void getCurrentCycle(String circleId, String memberEmail,
                                OnCycleLoadedListener listener) {
        if (savingCirclesRef == null) return;

        String sanitizedEmail = sanitizeEmail(memberEmail);

        savingCirclesRef.child(circleId)
                .child("members")
                .child(sanitizedEmail)
                .child("cycles")
                .get()
                .addOnSuccessListener(dataSnapshot -> {
                    MemberCycle currentCycle = null;
                    long currentTime = System.currentTimeMillis();

                    // Find the cycle that contains current time
                    for (DataSnapshot cycleSnapshot : dataSnapshot.getChildren()) {
                        MemberCycle cycle = cycleSnapshot.getValue(MemberCycle.class);
                        if (cycle != null && cycle.isDateInCycle(currentTime)) {
                            currentCycle = cycle;
                            break;
                        }
                    }

                    if (currentCycle != null) {
                        listener.onCycleLoaded(currentCycle);
                    } else {
                        listener.onCycleNotFound();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading current cycle", e);
                    listener.onError(e.getMessage());
                });
    }

    /**
     * Check if member needs a new cycle and create it
     */
    public void checkAndCreateNextCycle(String circleId, String memberEmail, String frequency) {
        if (savingCirclesRef == null) return;

        getCurrentCycle(circleId, memberEmail, new OnCycleLoadedListener() {
            @Override
            public void onCycleLoaded(MemberCycle cycle) {
                // Current cycle exists, check if it should be complete
                if (cycle.shouldBeComplete() && !cycle.isComplete()) {
                    // Complete the current cycle
                    completeCycle(circleId, memberEmail, cycle);

                    // Create next cycle
                    createNextCycle(circleId, memberEmail, cycle, frequency);
                }
            }

            @Override
            public void onCycleNotFound() {
                Log.w(TAG, "No active cycle found for member: " + memberEmail);
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "Error checking cycle: " + message);
            }
        });
    }

    /**
     * Complete a cycle and mark it as finished
     */
    private void completeCycle(String circleId, String memberEmail, MemberCycle cycle) {
        String sanitizedEmail = sanitizeEmail(memberEmail);

        cycle.setComplete(true);

        savingCirclesRef.child(circleId)
                .child("members")
                .child(sanitizedEmail)
                .child("cycles")
                .child(cycle.getCycleId())
                .setValue(cycle)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Cycle completed: " + cycle.getCycleId());
                });
    }

    /**
     * Create the next cycle for a member
     */
    private void createNextCycle(String circleId, String memberEmail,
                                 MemberCycle previousCycle, String frequency) {
        String sanitizedEmail = sanitizeEmail(memberEmail);

        MemberCycle nextCycle = MemberCycle.createNextCycle(previousCycle, frequency);

        savingCirclesRef.child(circleId)
                .child("members")
                .child(sanitizedEmail)
                .child("cycles")
                .child(nextCycle.getCycleId())
                .setValue(nextCycle)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Next cycle created: " + nextCycle.getCycleId());
                });
    }

    /**
     * Record an expense in the member's current cycle
     */
    public void recordExpenseInCycle(String circleId, String memberEmail, double amount) {
        getCurrentCycle(circleId, memberEmail, new OnCycleLoadedListener() {
            @Override
            public void onCycleLoaded(MemberCycle cycle) {
                cycle.recordExpense(amount);

                String sanitizedEmail = sanitizeEmail(memberEmail);

                savingCirclesRef.child(circleId)
                        .child("members")
                        .child(sanitizedEmail)
                        .child("cycles")
                        .child(cycle.getCycleId())
                        .setValue(cycle);
            }

            @Override
            public void onCycleNotFound() {
                Log.e(TAG, "Cannot record expense - no active cycle");
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "Error recording expense: " + message);
            }
        });
    }

    public void restoreExpenseInCycle(String circleId, String memberEmail, double amount) {
        getCurrentCycle(circleId, memberEmail, new OnCycleLoadedListener() {
            @Override
            public void onCycleLoaded(MemberCycle cycle) {
                cycle.restoreExpense(amount);

                String sanitizedEmail = sanitizeEmail(memberEmail);

                savingCirclesRef.child(circleId)
                        .child("members")
                        .child(sanitizedEmail)
                        .child("cycles")
                        .child(cycle.getCycleId())
                        .setValue(cycle);
            }

            @Override
            public void onCycleNotFound() {
                Log.w(TAG, "Cannot restore expense - no active cycle");
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "Error restoring expense: " + message);
            }
        });
    }

    /**
     * Get all cycles for a member (for history view)
     */
    public LiveData<List<MemberCycle>> getMemberCycleHistory(String circleId, String memberEmail) {
        MutableLiveData<List<MemberCycle>> cycles = new MutableLiveData<>(new ArrayList<>());

        if (savingCirclesRef == null) return cycles;

        String sanitizedEmail = sanitizeEmail(memberEmail);

        savingCirclesRef.child(circleId)
                .child("members")
                .child(sanitizedEmail)
                .child("cycles")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        List<MemberCycle> cycleList = new ArrayList<>();

                        for (DataSnapshot cycleSnapshot : dataSnapshot.getChildren()) {
                            MemberCycle cycle = cycleSnapshot.getValue(MemberCycle.class);
                            if (cycle != null) {
                                cycleList.add(cycle);
                            }
                        }

                        // Sort by start date (newest first)
                        cycleList.sort((c1, c2) -> Long.compare(c2.getStartDate(), c1.getStartDate()));

                        cycles.setValue(cycleList);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error loading cycle history: " + error.getMessage());
                    }
                });

        return cycles;
    }

    // Callback interface
    public interface OnCycleLoadedListener {
        void onCycleLoaded(MemberCycle cycle);
        void onCycleNotFound();
        void onError(String message);
    }

    // ==================== EXISTING METHODS (UPDATED) ====================

    // UPDATED: Deduct expense from member's current amount AND record in cycle
    public void deductExpenseFromMember(String circleId, String memberEmail, double expenseAmount) {
        if (savingCirclesRef == null) {
            Log.e(TAG, "savingCirclesRef is null!");
            return;
        }

        String sanitizedEmail = sanitizeEmail(memberEmail);

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
                    recordExpenseInCycle(circleId, memberEmail, expenseAmount);
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

    // Add expense amount back (if user deletes an expense)
    public void addBackExpenseToMember(String circleId, String memberEmail, double expenseAmount) {
        if (savingCirclesRef == null) return;

        String sanitizedEmail = sanitizeEmail(memberEmail);

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
                    restoreExpenseInCycle(circleId, memberEmail, expenseAmount);
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

    // Update member's current amount directly (for manual deposits/contributions)
    public void updateMemberCurrentAmount(String circleId, String memberEmail, double newAmount) {
        if (savingCirclesRef == null) return;

        String sanitizedEmail = sanitizeEmail(memberEmail);

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

    // Update existing saving circle in Firebase
    public void updateSavingCircle(String id, String groupName, String creatorEmail,
                                   String challengeTitle, double goalAmount,
                                   String frequency, String notes, long createdAtTimestamp) {
        if (savingCirclesRef == null) {
            statusMessage.setValue("User not authenticated");
            return;
        }

        SavingCircle savingCircle = new SavingCircle(groupName, creatorEmail, challengeTitle,
                goalAmount, frequency, notes, createdAtTimestamp);
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
                                    notes != null ? notes : "",
                                    createdAt != null ? createdAt : System.currentTimeMillis()
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

    // Add this to SavingCircleViewModel.java in the CYCLE MANAGEMENT METHODS section

    /**
     * Get the cycle that was active at a specific date (for viewing historical data)
     * Different from getCurrentCycle which uses System.currentTimeMillis()
     */
    public void getCycleAtDate(String circleId, String memberEmail, long targetDate,
                               OnCycleLoadedListener listener) {
        if (savingCirclesRef == null) {
            Log.e(TAG, "savingCirclesRef is null!");
            listener.onError("Database reference is null");
            return;
        }

        String sanitizedEmail = sanitizeEmail(memberEmail);

        savingCirclesRef.child(circleId)
                .child("members")
                .child(sanitizedEmail)
                .child("cycles")
                .get()
                .addOnSuccessListener(dataSnapshot -> {
                    MemberCycle targetCycle = null;

                    Log.d(TAG, "Searching for cycle at date: " + new java.util.Date(targetDate)
                            + " for member: " + memberEmail);

                    // Find the cycle that contains the target date
                    for (DataSnapshot cycleSnapshot : dataSnapshot.getChildren()) {
                        MemberCycle cycle = cycleSnapshot.getValue(MemberCycle.class);
                        if (cycle != null) {
                            Log.d(TAG, "Checking cycle: " + cycle.getCycleId()
                                    + " [" + new java.util.Date(cycle.getStartDate())
                                    + " to " + new java.util.Date(cycle.getEndDate()) + "]");

                            if (cycle.isDateInCycle(targetDate)) {
                                targetCycle = cycle;
                                Log.d(TAG, "Found matching cycle: " + cycle.getCycleId());
                                break;
                            }
                        }
                    }

                    if (targetCycle != null) {
                        listener.onCycleLoaded(targetCycle);
                    } else {
                        Log.w(TAG, "No cycle found for date: " + new java.util.Date(targetDate));
                        listener.onCycleNotFound();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading cycle at date", e);
                    listener.onError(e.getMessage());
                });
    }

    /**
     * Manually create a specific cycle for a member
     */
    public void createCycle(String circleId, String memberEmail, MemberCycle cycle,
                            OnCycleCreatedListener listener) {
        if (savingCirclesRef == null) {
            Log.e(TAG, "savingCirclesRef is null!");
            if (listener != null) listener.onError("Database reference is null");
            return;
        }

        String sanitizedEmail = sanitizeEmail(memberEmail);

        savingCirclesRef.child(circleId)
                .child("members")
                .child(sanitizedEmail)
                .child("cycles")
                .child(cycle.getCycleId())
                .setValue(cycle)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Cycle created: " + cycle.getCycleId());
                    if (listener != null) listener.onCycleCreated(cycle);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating cycle", e);
                    if (listener != null) listener.onError(e.getMessage());
                });
    }

    // Add this callback interface at the end of the ViewModel class
    public interface OnCycleCreatedListener {
        void onCycleCreated(MemberCycle cycle);
        void onError(String message);
    }

    public interface OnInvitationSentListener {
        void onInvitationSent();
        void onError(String message);
    }

    public interface OnInvitationActionListener {
        void onSuccess();
        void onError(String message);
    }

    // ==================== NEW METHODS FOR SavingCircleDetailActivity ====================

    // Fetch a single SavingCircle by ID
    public LiveData<SavingCircle> getSavingCircleById(String circleId) {
        MutableLiveData<SavingCircle> circleLiveData = new MutableLiveData<>();

        if (savingCirclesRef == null) {
            Log.e(TAG, "savingCirclesRef is null! Cannot load circle by ID.");
            return circleLiveData;
        }

        savingCirclesRef.child(circleId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    try {
                        String id = snapshot.getKey();
                        String groupName = snapshot.child("groupName").getValue(String.class);
                        String creatorEmail = snapshot.child("creatorEmail").getValue(String.class);
                        String challengeTitle = snapshot.child("challengeTitle").getValue(String.class);
                        Double goalAmount = snapshot.child("goalAmount").getValue(Double.class);
                        String frequency = snapshot.child("frequency").getValue(String.class);
                        String notes = snapshot.child("notes").getValue(String.class);
                        Long createdAt = snapshot.child("createdAt").getValue(Long.class);

                        if (groupName != null && creatorEmail != null && challengeTitle != null && goalAmount != null) {
                            SavingCircle savingCircle = new SavingCircle(
                                    groupName,
                                    creatorEmail,
                                    challengeTitle,
                                    goalAmount,
                                    frequency != null ? frequency : "Monthly",
                                    notes != null ? notes : "",
                                    createdAt != null ? createdAt : System.currentTimeMillis()
                            );
                            savingCircle.setId(id);
                            circleLiveData.setValue(savingCircle);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing circle by ID", e);
                    }
                } else {
                    Log.w(TAG, "No SavingCircle found for ID: " + circleId);
                    circleLiveData.setValue(null);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading circle by ID: " + error.getMessage());
            }
        });

        return circleLiveData;
    }


    // Fetch all members for a specific SavingCircle
    public LiveData<List<SavingCircleMember>> getSavingCircleMembers(String circleId) {
        MutableLiveData<List<SavingCircleMember>> membersLiveData = new MutableLiveData<>(new ArrayList<>());

        if (savingCirclesRef == null) {
            Log.e(TAG, "savingCirclesRef is null! Cannot load members.");
            return membersLiveData;
        }

        savingCirclesRef.child(circleId).child("members").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<SavingCircleMember> members = new ArrayList<>();
                for (DataSnapshot memberSnapshot : snapshot.getChildren()) {
                    SavingCircleMember member = memberSnapshot.getValue(SavingCircleMember.class);
                    if (member != null) {
                        members.add(member);
                    }
                }
                membersLiveData.setValue(members);
                Log.d(TAG, "Loaded " + members.size() + " members for circle " + circleId);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading members for circle " + circleId + ": " + error.getMessage());
            }
        });

        return membersLiveData;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (invitationsListener != null && invitationsRootRef != null) {
            String email = currentUserEmail.getValue();
            if (email != null && !email.isEmpty()) {
                invitationsRootRef.child(sanitizeEmail(email)).removeEventListener(invitationsListener);
            }
        }
    }

    private String sanitizeEmail(String email) {
        if (email == null) {
            return "";
        }
        return email.replace(".", "_").replace("@", "_at_");
    }
}