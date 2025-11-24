package com.example.spendwise.viewModel;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.spendwise.model.Firebase;
import com.example.spendwise.model.MemberCycle;
import com.example.spendwise.model.SavingCircle;
import com.example.spendwise.model.SavingCircleInvitation;
import com.example.spendwise.model.SavingCircleMember;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SavingCircleViewModel extends ViewModel {
    private static final String TAG = "SavingCircleViewModel";

    private final MutableLiveData<String> statusMessage;
    private final MutableLiveData<List<SavingCircle>> savingCircles;
    private final MutableLiveData<String> currentUserEmail;
    private final MutableLiveData<List<SavingCircleInvitation>> invitations;
    private final FirebaseDatabase database;
    private DatabaseReference savingCirclesRef;
    private final DatabaseReference invitationsRootRef;
    private final FirebaseAuth auth;
    private ValueEventListener invitationsListener;

    public SavingCircleViewModel() {
        savingCircles = new MutableLiveData<>(new ArrayList<>());
        statusMessage = new MutableLiveData<>();
        currentUserEmail = new MutableLiveData<>();
        invitations = new MutableLiveData<>(new ArrayList<>());

        database = Firebase.getDatabase();
        auth = FirebaseAuth.getInstance();
        invitationsRootRef = database.getReference("invitations");

        loadCurrentUserEmail();
        setupUserSavingCirclesReference();
        loadSavingCirclesFromFirebase();
    }

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

    public void addSavingCircle(String groupName, String creatorEmail, String challengeTitle,
                                double goalAmount, String frequency, String notes,
                                double personalAllocation, long dashboardTimestamp) {
        if (savingCirclesRef == null) {
            Log.e(TAG, "savingCirclesRef is null! Cannot add saving circle.");
            statusMessage.setValue("Error: User not logged in");
            return;
        }

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            statusMessage.setValue("Error: User not authenticated");
            return;
        }
        String creatorUid = currentUser.getUid();

        SavingCircle savingCircle = new SavingCircle(groupName, creatorEmail, challengeTitle,
                goalAmount, frequency, notes, dashboardTimestamp);
        savingCircle.setCreatorUid(creatorUid);

        DatabaseReference newSavingCircleRef = savingCirclesRef.push();
        String firebaseId = newSavingCircleRef.getKey();
        savingCircle.setId(firebaseId);

        Log.d(TAG, "Adding saving circle to Firebase: " + savingCircle);

        newSavingCircleRef.setValue(savingCircle)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Saving circle added successfully: " + savingCircle);

                    addMemberToCircle(firebaseId, creatorEmail, personalAllocation,
                            dashboardTimestamp, frequency);

                    statusMessage.setValue("Saving circle created!");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding saving circle", e);
                    statusMessage.setValue("Error: " + e.getMessage());
                });
    }

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

                    initializeMemberCycleInPath(savingCirclesRef, circleId, memberEmail, joinTimestamp,
                            frequency, personalAllocation);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding member to circle", e);
                });
    }

    private void addMemberToCircleInCreatorPath(String creatorUid, String circleId, String memberEmail,
                                                double personalAllocation, long joinTimestamp,
                                                String frequency) {
        DatabaseReference creatorCirclesRef = database.getReference("users").child(creatorUid)
                .child("savingCircles");

        SavingCircleMember member = new SavingCircleMember(memberEmail, personalAllocation, joinTimestamp);
        String sanitizedEmail = sanitizeEmail(memberEmail);

        creatorCirclesRef.child(circleId)
                .child("members")
                .child(sanitizedEmail)
                .setValue(member)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Member added to circle in creator's path: " + memberEmail + " with personal allocation: $" + personalAllocation);

                    initializeMemberCycleInPath(creatorCirclesRef, circleId, memberEmail, joinTimestamp,
                            frequency, personalAllocation);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding member to circle in creator's path", e);
                });
    }

    private void addCircleToInviteePath(String inviteeUid, SavingCircle circle) {
        DatabaseReference inviteeCirclesRef = database.getReference("users").child(inviteeUid)
                .child("savingCircles");

        // Ensure creatorUid is set in the circle before saving to invitee's path
        if (circle.getCreatorUid() == null || circle.getCreatorUid().isEmpty()) {
            Log.w(TAG, "Circle missing creatorUid when adding to invitee path");
        }

        inviteeCirclesRef.child(circle.getId()).setValue(circle)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Circle added to invitee's path: " + circle.getId() + " for user: " + inviteeUid);
                    // The ValueEventListener in loadSavingCirclesFromFirebase should automatically pick up this change
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding circle to invitee's path", e);
                });
    }

    public void sendInvitation(String circleId, String inviteeEmail, OnInvitationSentListener listener) {
        if (savingCirclesRef == null || invitationsRootRef == null) {
            if (listener != null) listener.onError("Database reference not initialized");
            return;
        }

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            if (listener != null) listener.onError("User not authenticated");
            return;
        }
        String creatorUid = currentUser.getUid();

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
                            frequency != null ? frequency : "Monthly",
                            creatorUid
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

    public void acceptInvitation(SavingCircleInvitation invitation, double personalAllocation, OnInvitationActionListener listener) {
        if (invitation == null) {
            if (listener != null) listener.onError("Invitation not found");
            return;
        }

        if (personalAllocation < 0) {
            if (listener != null) listener.onError("Personal allocation must be non-negative");
            return;
        }

        long responseTimestamp = System.currentTimeMillis();
        double allocation = personalAllocation;
        String frequency = invitation.getFrequency() != null ? invitation.getFrequency() : "Monthly";
        String circleId = invitation.getCircleId();
        String inviteeEmail = invitation.getInviteeEmail();
        String creatorUid = invitation.getCreatorUid();

        // Get the invitee's UID
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            if (listener != null) listener.onError("User not authenticated");
            return;
        }
        String inviteeUid = currentUser.getUid();

        // If creatorUid is not in the invitation (for backwards compatibility), search for it
        if (creatorUid == null || creatorUid.isEmpty()) {
            // Fallback: search for the circle
            database.getReference("users").get()
                    .addOnSuccessListener(usersSnapshot -> {
                        String foundCreatorUid = null;
                        for (DataSnapshot userSnapshot : usersSnapshot.getChildren()) {
                            if (userSnapshot.child("savingCircles").child(circleId).exists()) {
                                foundCreatorUid = userSnapshot.getKey();
                                break;
                            }
                        }
                        if (foundCreatorUid != null) {
                            proceedWithAcceptance(foundCreatorUid, circleId, inviteeEmail, inviteeUid,
                                    allocation, responseTimestamp, frequency, invitation, listener);
                        } else {
                            if (listener != null) listener.onError("Circle not found");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error finding circle", e);
                        if (listener != null) listener.onError("Error accepting invitation: " + e.getMessage());
                    });
        } else {
            // Use the creatorUid from the invitation
            proceedWithAcceptance(creatorUid, circleId, inviteeEmail, inviteeUid,
                    allocation, responseTimestamp, frequency, invitation, listener);
        }
    }

    private void proceedWithAcceptance(String creatorUid, String circleId, String inviteeEmail,
                                       String inviteeUid, double allocation, long responseTimestamp,
                                       String frequency, SavingCircleInvitation invitation,
                                       OnInvitationActionListener listener) {
        // Fetch the circle from the creator's path
        DatabaseReference creatorCirclesRef = database.getReference("users").child(creatorUid)
                .child("savingCircles");

        creatorCirclesRef.child(circleId).get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        Log.e(TAG, "Circle not found in creator's path: " + circleId);
                        if (listener != null) listener.onError("Circle not found");
                        return;
                    }

                    try {
                        String id = snapshot.getKey();
                        String groupName = snapshot.child("groupName").getValue(String.class);
                        String creatorEmail = snapshot.child("creatorEmail").getValue(String.class);
                        String challengeTitle = snapshot.child("challengeTitle").getValue(String.class);
                        Double goalAmount = snapshot.child("goalAmount").getValue(Double.class);
                        String freq = snapshot.child("frequency").getValue(String.class);
                        String notes = snapshot.child("notes").getValue(String.class);
                        Long createdAt = snapshot.child("createdAt").getValue(Long.class);

                        if (groupName == null || creatorEmail == null || challengeTitle == null || goalAmount == null) {
                            if (listener != null) listener.onError("Circle data incomplete");
                            return;
                        }

                        SavingCircle circle = new SavingCircle(groupName, creatorEmail, challengeTitle,
                                goalAmount, freq != null ? freq : "Monthly",
                                notes != null ? notes : "",
                                createdAt != null ? createdAt : System.currentTimeMillis());
                        circle.setId(id);
                        circle.setCreatorUid(creatorUid); // Store creator UID in the circle

                        // Step 1: Add member to the circle in the creator's path
                        addMemberToCircleInCreatorPath(creatorUid, circleId, inviteeEmail, allocation,
                                responseTimestamp, frequency);

                        // Step 2: Add the circle to the invitee's path so it shows in their list
                        addCircleToInviteePath(inviteeUid, circle);

                        // Step 3: Update invitation status
                        updateInvitationStatus(invitation, "accepted", responseTimestamp, listener);
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing circle", e);
                        if (listener != null) listener.onError("Error parsing circle data");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching circle from creator's path", e);
                    if (listener != null) listener.onError("Error accepting invitation: " + e.getMessage());
                });
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

    public void initializeMemberCycle(String circleId, String memberEmail,
                                      long joinDate, String frequency,
                                      double startAmount) {
        if (savingCirclesRef == null) return;
        initializeMemberCycleInPath(savingCirclesRef, circleId, memberEmail, joinDate, frequency, startAmount);
    }

    private void initializeMemberCycleInPath(DatabaseReference circlesRef, String circleId, String memberEmail,
                                             long joinDate, String frequency,
                                             double startAmount) {
        if (circlesRef == null) return;

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(joinDate);

        long cycleStartDate;
        long cycleEndDate;

        if ("Weekly".equals(frequency)) {
            // Weekly: Cycle starts on acceptance date (Day 1 of 7-day period)
            // Cycle ends 7 days after start date
            cycleStartDate = joinDate;
            calendar.setTimeInMillis(joinDate);
            calendar.add(Calendar.DAY_OF_YEAR, 7);
            cycleEndDate = calendar.getTimeInMillis();
        } else {
            // Monthly: Cycle starts on Day 1 of the month in which acceptance occurred
            // Cycle ends at the end of that month
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            cycleStartDate = calendar.getTimeInMillis();

            // Move to last day of month
            calendar.add(Calendar.MONTH, 1);
            calendar.add(Calendar.DAY_OF_MONTH, -1);
            calendar.set(Calendar.HOUR_OF_DAY, 23);
            calendar.set(Calendar.MINUTE, 59);
            calendar.set(Calendar.SECOND, 59);
            calendar.set(Calendar.MILLISECOND, 999);
            cycleEndDate = calendar.getTimeInMillis();
        }

        MemberCycle firstCycle = new MemberCycle(cycleStartDate, cycleEndDate, startAmount);

        String sanitizedEmail = sanitizeEmail(memberEmail);

        // Use the new centralized helper method
        saveCycleToDatabase(circlesRef, circleId, memberEmail, firstCycle, new OnCycleCreatedListener() {
            @Override
            public void onCycleCreated(MemberCycle cycle) {
                // Success from helper
                Log.d(TAG, "Initial cycle created for member: " + memberEmail);
            }

            @Override
            public void onError(String message) {
                // Error from helper
                Log.e(TAG, "Error creating initial cycle: " + message);
            }
        });
    }

    public void getCurrentCycle(String circleId, String memberEmail,
                                OnCycleLoadedListener listener) {
        if (savingCirclesRef == null) return;

        // Find the circle to get creator UID, then read from creator's path
        getCircleCreatorUid(circleId, creatorUid -> {
            if (creatorUid == null) {
                listener.onError("Cannot find circle creator");
                return;
            }

            DatabaseReference circlesRef = database.getReference("users")
                    .child(creatorUid).child("savingCircles");
            String sanitizedEmail = sanitizeEmail(memberEmail);

            circlesRef.child(circleId)
                    .child("members")
                    .child(sanitizedEmail)
                    .child("cycles")
                    .get()
                    .addOnSuccessListener(dataSnapshot -> {
                        MemberCycle currentCycle = null;
                        long currentTime = System.currentTimeMillis();

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
        });
    }

    public void checkAndCreateNextCycle(String circleId, String memberEmail, String frequency) {
        if (savingCirclesRef == null) return;

        getCurrentCycle(circleId, memberEmail, new OnCycleLoadedListener() {
            @Override
            public void onCycleLoaded(MemberCycle cycle) {
                if (cycle.shouldBeComplete() && !cycle.isComplete()) {
                    completeCycle(circleId, memberEmail, cycle);
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

    private void completeCycle(String circleId, String memberEmail, MemberCycle cycle) {
        // Find creator UID and write to creator's path
        getCircleCreatorUid(circleId, creatorUid -> {
            if (creatorUid == null) {
                Log.e(TAG, "Cannot find circle creator for completing cycle");
                return;
            }

            DatabaseReference circlesRef = database.getReference("users")
                    .child(creatorUid).child("savingCircles");
            String sanitizedEmail = sanitizeEmail(memberEmail);

            cycle.setComplete(true);

            circlesRef.child(circleId)
                    .child("members")
                    .child(sanitizedEmail)
                    .child("cycles")
                    .child(cycle.getCycleId())
                    .setValue(cycle)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Cycle completed: " + cycle.getCycleId());
                    });
        });
    }

    private void createNextCycle(String circleId, String memberEmail,
                                 MemberCycle previousCycle, String frequency) {
        // Find creator UID and write to creator's path
        getCircleCreatorUid(circleId, creatorUid -> {
            if (creatorUid == null) {
                Log.e(TAG, "Cannot find circle creator for creating next cycle");
                return;
            }

            DatabaseReference circlesRef = database.getReference("users")
                    .child(creatorUid).child("savingCircles");
            String sanitizedEmail = sanitizeEmail(memberEmail);

            MemberCycle nextCycle = MemberCycle.createNextCycle(previousCycle, frequency);

            // Use the new centralized helper method
            saveCycleToDatabase(circlesRef, circleId, memberEmail, nextCycle, new OnCycleCreatedListener() {
                @Override
                public void onCycleCreated(MemberCycle cycle) {
                    // Success from helper
                    Log.d(TAG, "Next cycle created: " + nextCycle.getCycleId());
                }

                @Override
                public void onError(String message) {
                    // Error handling (you may want to add logging here)
                    Log.e(TAG, "Error creating next cycle: " + message);
                }
            });
        });
    }

    public void recordExpenseInCycle(String circleId, String memberEmail, double amount) {
        // Use current time for backwards compatibility
        recordExpenseInCycleAtDate(circleId, memberEmail, amount, System.currentTimeMillis());
    }

    // Record expense in cycle for a specific date (respects Dashboard date selector)
    public void recordExpenseInCycleAtDate(String circleId, String memberEmail, double amount, long expenseDate) {
        getCycleAtDate(circleId, memberEmail, expenseDate, new OnCycleLoadedListener() {
            @Override
            public void onCycleLoaded(MemberCycle cycle) {
                double oldEndAmount = cycle.getEndAmount();
                cycle.recordExpense(amount);
                double newEndAmount = cycle.getEndAmount();

                // Find creator UID and write to creator's path
                getCircleCreatorUid(circleId, creatorUid -> {
                    if (creatorUid == null) {
                        Log.e(TAG, "Cannot find circle creator for recording expense");
                        return;
                    }

                    DatabaseReference circlesRef = database.getReference("users")
                            .child(creatorUid).child("savingCircles");
                    String sanitizedEmail = sanitizeEmail(memberEmail);

                    circlesRef.child(circleId)
                            .child("members")
                            .child(sanitizedEmail)
                            .child("cycles")
                            .child(cycle.getCycleId())
                            .setValue(cycle)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Expense recorded in cycle: " + cycle.getCycleId() + " for date: " + new java.util.Date(expenseDate));

                                // Sync currentAmount with the cycle's endAmount if this is the current cycle
                                // Check if this cycle contains the current date
                                long currentTime = System.currentTimeMillis();
                                if (cycle.isDateInCycle(currentTime)) {
                                    circlesRef.child(circleId)
                                            .child("members")
                                            .child(sanitizedEmail)
                                            .child("currentAmount")
                                            .setValue(newEndAmount)
                                            .addOnSuccessListener(aVoid2 -> {
                                                Log.d(TAG, "Synced currentAmount with cycle endAmount: " + newEndAmount);
                                            });
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error recording expense in cycle", e);
                            });
                });
            }

            @Override
            public void onCycleNotFound() {
                Log.e(TAG, "Cannot record expense - no cycle found for date: " + new java.util.Date(expenseDate));
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "Error recording expense: " + message);
            }
        });
    }

    public void restoreExpenseInCycle(String circleId, String memberEmail, double amount) {
        // Use current time for backwards compatibility
        restoreExpenseInCycleAtDate(circleId, memberEmail, amount, System.currentTimeMillis());
    }

    // Restore expense in cycle for a specific date
    public void restoreExpenseInCycleAtDate(String circleId, String memberEmail, double amount, long expenseDate) {
        getCycleAtDate(circleId, memberEmail, expenseDate, new OnCycleLoadedListener() {
            @Override
            public void onCycleLoaded(MemberCycle cycle) {
                double oldEndAmount = cycle.getEndAmount();
                cycle.restoreExpense(amount);
                double newEndAmount = cycle.getEndAmount();

                // Find creator UID and write to creator's path
                getCircleCreatorUid(circleId, creatorUid -> {
                    if (creatorUid == null) {
                        Log.e(TAG, "Cannot find circle creator for restoring expense");
                        return;
                    }

                    DatabaseReference circlesRef = database.getReference("users")
                            .child(creatorUid).child("savingCircles");
                    String sanitizedEmail = sanitizeEmail(memberEmail);

                    circlesRef.child(circleId)
                            .child("members")
                            .child(sanitizedEmail)
                            .child("cycles")
                            .child(cycle.getCycleId())
                            .setValue(cycle)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Expense restored in cycle: " + cycle.getCycleId());

                                // Sync currentAmount if this is the current cycle
                                long currentTime = System.currentTimeMillis();
                                if (cycle.isDateInCycle(currentTime)) {
                                    circlesRef.child(circleId)
                                            .child("members")
                                            .child(sanitizedEmail)
                                            .child("currentAmount")
                                            .setValue(newEndAmount)
                                            .addOnSuccessListener(aVoid2 -> {
                                                Log.d(TAG, "Synced currentAmount after restore: " + newEndAmount);
                                            });
                                }
                            });
                });
            }

            @Override
            public void onCycleNotFound() {
                Log.w(TAG, "Cannot restore expense - no cycle found for date: " + new java.util.Date(expenseDate));
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "Error restoring expense: " + message);
            }
        });
    }

    public LiveData<List<MemberCycle>> getMemberCycleHistory(String circleId, String memberEmail) {
        MutableLiveData<List<MemberCycle>> cycles = new MutableLiveData<>(new ArrayList<>());

        if (savingCirclesRef == null) return cycles;

        // Find the circle to get creator UID, then read from creator's path
        getCircleCreatorUid(circleId, creatorUid -> {
            if (creatorUid == null) {
                Log.e(TAG, "Cannot find circle creator for cycle history");
                return;
            }

            DatabaseReference circlesRef = database.getReference("users")
                    .child(creatorUid).child("savingCircles");
            String sanitizedEmail = sanitizeEmail(memberEmail);

            circlesRef.child(circleId)
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

                            cycleList.sort((c1, c2) -> Long.compare(c2.getStartDate(), c1.getStartDate()));

                            cycles.setValue(cycleList);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e(TAG, "Error loading cycle history: " + error.getMessage());
                        }
                    });
        });

        return cycles;
    }

    public interface OnCycleLoadedListener {
        void onCycleLoaded(MemberCycle cycle);

        void onCycleNotFound();

        void onError(String message);
    }

    // Overload for backwards compatibility (uses current time)
    public void deductExpenseFromMember(String circleId, String memberEmail, double expenseAmount) {
        deductExpenseFromMemberAtDate(circleId, memberEmail, expenseAmount, System.currentTimeMillis());
    }

    // Deduct expense from member at a specific date (respects Dashboard date selector)
    public void deductExpenseFromMemberAtDate(String circleId, String memberEmail, double expenseAmount, long expenseDate) {
        if (savingCirclesRef == null) {
            Log.e(TAG, "savingCirclesRef is null!");
            return;
        }

        // Record the expense in the cycle for the expense date
        // This will automatically sync currentAmount if the expense is in the current cycle
        recordExpenseInCycleAtDate(circleId, memberEmail, expenseAmount, expenseDate);
        Log.d(TAG, "Deducting expense from savings circle: " + circleId + ", amount: " + expenseAmount + ", date: " + new java.util.Date(expenseDate));
    }

    // Overload for backwards compatibility
    public void addBackExpenseToMember(String circleId, String memberEmail, double expenseAmount) {
        addBackExpenseToMemberAtDate(circleId, memberEmail, expenseAmount, System.currentTimeMillis());
    }

    // Add back expense to member at a specific date (for expense deletion)
    public void addBackExpenseToMemberAtDate(String circleId, String memberEmail, double expenseAmount, long expenseDate) {
        if (savingCirclesRef == null) return;

        // Restore the expense in the cycle for the expense date
        // This will automatically sync currentAmount if the expense is in the current cycle
        restoreExpenseInCycleAtDate(circleId, memberEmail, expenseAmount, expenseDate);
        Log.d(TAG, "Restoring expense to savings circle: " + circleId + ", amount: " + expenseAmount + ", date: " + new java.util.Date(expenseDate));
    }

    public void updateMemberCurrentAmount(String circleId, String memberEmail, double newAmount) {
        if (savingCirclesRef == null) return;

        // Find creator UID and operate on creator's path
        getCircleCreatorUid(circleId, creatorUid -> {
            if (creatorUid == null) {
                Log.e(TAG, "Cannot find circle creator for updating member amount");
                statusMessage.setValue("Error: Cannot find circle");
                return;
            }

            DatabaseReference circlesRef = database.getReference("users")
                    .child(creatorUid).child("savingCircles");
            String sanitizedEmail = sanitizeEmail(memberEmail);

            circlesRef.child(circleId)
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
        });
    }

    public LiveData<String> getStatusMessage() {
        return statusMessage;
    }

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
                            // Set creatorUid - if not stored, assume current user is creator (backwards compatibility)
                            String creatorUid = savingCircleSnapshot.child("creatorUid").getValue(String.class);
                            if (creatorUid == null || creatorUid.isEmpty()) {
                                FirebaseUser currentUser = auth.getCurrentUser();
                                creatorUid = currentUser != null ? currentUser.getUid() : null;
                            }
                            if (creatorUid != null) {
                                savingCircle.setCreatorUid(creatorUid);
                            }
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

    public void getCycleAtDate(String circleId, String memberEmail, long targetDate,
                               OnCycleLoadedListener listener) {
        if (savingCirclesRef == null) {
            Log.e(TAG, "savingCirclesRef is null!");
            listener.onError("Database reference is null");
            return;
        }

        // Find the circle to get creator UID, then read from creator's path
        getCircleCreatorUid(circleId, creatorUid -> {
            if (creatorUid == null) {
                listener.onError("Cannot find circle creator");
                return;
            }

            DatabaseReference circlesRef = database.getReference("users")
                    .child(creatorUid).child("savingCircles");
            String sanitizedEmail = sanitizeEmail(memberEmail);

            circlesRef.child(circleId)
                    .child("members")
                    .child(sanitizedEmail)
                    .child("cycles")
                    .get()
                    .addOnSuccessListener(dataSnapshot -> {
                        MemberCycle targetCycle = null;

                        Log.d(TAG, "Searching for cycle at date: " + new java.util.Date(targetDate)
                                + " for member: " + memberEmail);

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
        });
    }

    private void getCircleCreatorUid(String circleId, CreatorUidCallback callback) {
        // First try current user's path
        savingCirclesRef.child(circleId).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        String creatorUid = snapshot.child("creatorUid").getValue(String.class);
                        if (creatorUid != null && !creatorUid.isEmpty()) {
                            callback.onUidFound(creatorUid);
                        } else {
                            // Backwards compatibility: assume current user is creator
                            String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
                            callback.onUidFound(uid);
                        }
                    } else {
                        // Search in other users' paths
                        database.getReference("users").get()
                                .addOnSuccessListener(usersSnapshot -> {
                                    for (DataSnapshot userSnapshot : usersSnapshot.getChildren()) {
                                        DataSnapshot circleSnapshot = userSnapshot.child("savingCircles").child(circleId);
                                        if (circleSnapshot.exists()) {
                                            String creatorUid = circleSnapshot.child("creatorUid").getValue(String.class);
                                            callback.onUidFound(creatorUid != null ? creatorUid : userSnapshot.getKey());
                                            return;
                                        }
                                    }
                                    callback.onUidFound(null);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error searching for circle creator", e);
                                    callback.onUidFound(null);
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading circle", e);
                    callback.onUidFound(null);
                });
    }

    private interface CreatorUidCallback {
        void onUidFound(String creatorUid);
    }

    public void createCycle(String circleId, String memberEmail, MemberCycle cycle,
                            OnCycleCreatedListener listener) {
        if (savingCirclesRef == null) {
            Log.e(TAG, "savingCirclesRef is null!");
            if (listener != null) listener.onError("Database reference is null");
            return;
        }

        // Find the circle to get creator UID, then write to creator's path
        getCircleCreatorUid(circleId, creatorUid -> {
            if (creatorUid == null) {
                if (listener != null) listener.onError("Cannot find circle creator");
                return;
            }

            DatabaseReference circlesRef = database.getReference("users")
                    .child(creatorUid).child("savingCircles");
            String sanitizedEmail = sanitizeEmail(memberEmail);

            circlesRef.child(circleId)
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
        });
    }

    public interface OnCycleCreatedListener {
        void onCycleCreated(MemberCycle cycle);

        void onError(String message);
    }

    public LiveData<SavingCircle> getSavingCircleById(String circleId) {
        MutableLiveData<SavingCircle> circleLiveData = new MutableLiveData<>();

        if (savingCirclesRef == null) {
            Log.e(TAG, "savingCirclesRef is null! Cannot load circle by ID.");
            return circleLiveData;
        }

        // First try current user's path
        savingCirclesRef.child(circleId).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        parseAndSetCircle(snapshot, circleLiveData);
                    } else {
                        // Circle not in current user's path, search for it
                        findCircleById(circleId, circleLiveData);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading circle from current user path: " + e.getMessage());
                    findCircleById(circleId, circleLiveData);
                });

        return circleLiveData;
    }

    private void findCircleById(String circleId, MutableLiveData<SavingCircle> circleLiveData) {
        database.getReference("users").get()
                .addOnSuccessListener(usersSnapshot -> {
                    for (DataSnapshot userSnapshot : usersSnapshot.getChildren()) {
                        DataSnapshot circleSnapshot = userSnapshot.child("savingCircles").child(circleId);
                        if (circleSnapshot.exists()) {
                            parseAndSetCircle(circleSnapshot, circleLiveData);
                            // Also set up a listener for real-time updates from the creator's path
                            String creatorUid = userSnapshot.getKey();
                            DatabaseReference creatorCirclesRef = database.getReference("users")
                                    .child(creatorUid).child("savingCircles");
                            creatorCirclesRef.child(circleId).addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    if (snapshot.exists()) {
                                        parseAndSetCircle(snapshot, circleLiveData);
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Log.e(TAG, "Error listening to circle updates: " + error.getMessage());
                                }
                            });
                            return;
                        }
                    }
                    Log.w(TAG, "No SavingCircle found for ID: " + circleId);
                    circleLiveData.setValue(null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error searching for circle: " + e.getMessage());
                    circleLiveData.setValue(null);
                });
    }

    private void parseAndSetCircle(DataSnapshot snapshot, MutableLiveData<SavingCircle> circleLiveData) {
        try {
            String id = snapshot.getKey();
            String groupName = snapshot.child("groupName").getValue(String.class);
            String creatorEmail = snapshot.child("creatorEmail").getValue(String.class);
            String creatorUid = snapshot.child("creatorUid").getValue(String.class);
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
                if (creatorUid != null) {
                    savingCircle.setCreatorUid(creatorUid);
                }
                circleLiveData.setValue(savingCircle);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing circle by ID", e);
        }
    }

    public LiveData<List<SavingCircleMember>> getSavingCircleMembers(String circleId) {
        MutableLiveData<List<SavingCircleMember>> membersLiveData = new MutableLiveData<>(new ArrayList<>());

        if (savingCirclesRef == null) {
            Log.e(TAG, "savingCirclesRef is null! Cannot load members.");
            return membersLiveData;
        }

        // First, get the circle to find the creator's UID
        savingCirclesRef.child(circleId).get()
                .addOnSuccessListener(circleSnapshot -> {
                    if (!circleSnapshot.exists()) {
                        Log.e(TAG, "Circle not found in current user's path: " + circleId);
                        // Try to find it in other users' paths (fallback for backwards compatibility)
                        findCircleAndLoadMembers(circleId, membersLiveData);
                        return;
                    }

                    String creatorUid = circleSnapshot.child("creatorUid").getValue(String.class);
                    if (creatorUid == null || creatorUid.isEmpty()) {
                        // Backwards compatibility: if creatorUid is not set, assume current user is creator
                        creatorUid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
                    }

                    if (creatorUid != null) {
                        // Read members from the creator's path
                        DatabaseReference creatorCirclesRef = database.getReference("users")
                                .child(creatorUid).child("savingCircles");
                        loadMembersFromPath(creatorCirclesRef, circleId, membersLiveData);
                    } else {
                        Log.e(TAG, "Cannot determine creator UID for circle: " + circleId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error finding circle: " + e.getMessage());
                    // Fallback: try to find circle in other paths
                    findCircleAndLoadMembers(circleId, membersLiveData);
                });

        return membersLiveData;
    }

    private void findCircleAndLoadMembers(String circleId, MutableLiveData<List<SavingCircleMember>> membersLiveData) {
        database.getReference("users").get()
                .addOnSuccessListener(usersSnapshot -> {
                    for (DataSnapshot userSnapshot : usersSnapshot.getChildren()) {
                        DataSnapshot circleSnapshot = userSnapshot.child("savingCircles").child(circleId);
                        if (circleSnapshot.exists()) {
                            String creatorUid = userSnapshot.getKey();
                            DatabaseReference creatorCirclesRef = database.getReference("users")
                                    .child(creatorUid).child("savingCircles");
                            loadMembersFromPath(creatorCirclesRef, circleId, membersLiveData);
                            return;
                        }
                    }
                    Log.e(TAG, "Circle not found in any user path: " + circleId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error searching for circle: " + e.getMessage());
                });
    }

    private void loadMembersFromPath(DatabaseReference circlesRef, String circleId,
                                     MutableLiveData<List<SavingCircleMember>> membersLiveData) {
        circlesRef.child(circleId).child("members").addValueEventListener(new ValueEventListener() {
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

    public interface OnInvitationSentListener {
        void onInvitationSent();

        void onError(String message);
    }

    public interface OnInvitationActionListener {
        void onSuccess();

        void onError(String message);
    }

    /**
     * The core private helper method for saving/updating a MemberCycle object in Firebase.
     * This ensures all cycle database writes are centralized and consistent.
     */
    private void saveCycleToDatabase(DatabaseReference circlesRef, String circleId, String memberEmail,
                                     MemberCycle cycle, OnCycleCreatedListener listener) {
        if (circlesRef == null) {
            if (listener != null) listener.onError("Database reference not initialized");
            return;
        }

        String sanitizedEmail = sanitizeEmail(memberEmail);

        circlesRef.child(circleId)
                .child("members")
                .child(sanitizedEmail)
                .child("cycles")
                .child(cycle.getCycleId())
                .setValue(cycle)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Cycle saved/updated: " + cycle.getCycleId());
                    if (listener != null) listener.onCycleCreated(cycle);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving cycle", e);
                    if (listener != null) listener.onError(e.getMessage());
                });
    }

    /** Finds the MemberCycle in the list with the latest end date. */
    private MemberCycle findLastCycle(List<MemberCycle> cycles) {
        if (cycles == null || cycles.isEmpty()) return null;
        MemberCycle lastCycle = null;
        for (MemberCycle cycle : cycles) {
            if (lastCycle == null || cycle.getEndDate() > lastCycle.getEndDate()) {
                lastCycle = cycle;
            }
        }
        return lastCycle;
    }

    private void createNextCycleAndReturn(String circleId, String memberEmail,
                                          MemberCycle previousCycle, String frequency,
                                          OnCycleLoadedListener finalListener) {

        if (!previousCycle.isComplete()) {
            // Ensure the previous cycle is marked complete first
            completeCycleAndCreateNext(circleId, memberEmail, previousCycle, frequency, finalListener);
        } else {
            // Previous cycle already complete, just create next one
            getCircleCreatorUid(circleId, creatorUid -> {
                if (creatorUid == null) {
                    finalListener.onError("Cannot find circle creator for next cycle.");
                    return;
                }

                DatabaseReference circlesRef = database.getReference("users")
                        .child(creatorUid).child("savingCircles");

                MemberCycle nextCycle = MemberCycle.createNextCycle(previousCycle, frequency);

                saveCycleToDatabase(circlesRef, circleId, memberEmail, nextCycle, new OnCycleCreatedListener() {
                    @Override
                    public void onCycleCreated(MemberCycle cycle) {
                        Log.d(TAG, "Next cycle created: " + cycle.getCycleId());
                        finalListener.onCycleLoaded(cycle);
                    }

                    @Override
                    public void onError(String message) {
                        Log.e(TAG, "Error creating next cycle: " + message);
                        finalListener.onError(message);
                    }
                });
            });
        }
    }

    /**
     * Public high-level command to ensure a cycle exists and is active for the given date.
     * If the cycle is missing or expired, it handles completion of the previous cycle
     * and creation of the next one up to the check date.
     */
    public void ensureCycleIsActiveForDate(String circleId, String memberEmail, long checkDate,
                                           String frequency, double allocation,
                                           OnCycleLoadedListener listener) {
        // 1. Try to get the existing cycle for the date
        getCycleAtDate(circleId, memberEmail, checkDate, new OnCycleLoadedListener() {
            @Override
            public void onCycleLoaded(MemberCycle cycle) {
                // Case 1: Cycle exists.
                if (cycle.shouldBeComplete() && !cycle.isComplete()) {
                    // Cycle should be complete but isn't marked as such. Complete it first.
                    completeCycleAndCreateNext(circleId, memberEmail, cycle, frequency, listener);
                } else {
                    // Cycle is valid for the date. Return it.
                    listener.onCycleLoaded(cycle);
                }
            }

            @Override
            public void onCycleNotFound() {
                // Case 2: No cycle found for the checkDate. Use one-time get instead of observe
                getMemberCycleHistoryOnce(circleId, memberEmail, cycles -> {
                    MemberCycle lastCycle = findLastCycle(cycles);

                    if (cycles == null || cycles.isEmpty() || lastCycle == null) {
                        // Case 2A: No cycles ever. Create the first cycle
                        initializeMemberCycle(circleId, memberEmail, checkDate, frequency, allocation);
                        // Re-run getCycleAtDate to fetch the newly created cycle and return it
                        // Add a small delay to ensure the write completes
                        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                            getCycleAtDate(circleId, memberEmail, checkDate, listener);
                        }, 500);

                    } else if (checkDate > lastCycle.getEndDate()) {
                        // Case 2B: Gap detected. Complete the last cycle and create next ones until we reach checkDate
                        fillCycleGap(circleId, memberEmail, lastCycle, checkDate, frequency, listener);

                    } else {
                        // This shouldn't happen, but handle it
                        listener.onError("Cycle logic inconsistency: Cycle history exists, but none found for the date.");
                    }
                });
            }

            @Override
            public void onError(String message) {
                listener.onError("Error checking cycle existence: " + message);
            }
        });
    }

    // New helper method to get cycle history once (not observe)
    public void getMemberCycleHistoryOnce(String circleId, String memberEmail, CycleHistoryCallback callback) {
        if (savingCirclesRef == null) {
            callback.onHistoryLoaded(new ArrayList<>());
            return;
        }

        getCircleCreatorUid(circleId, creatorUid -> {
            if (creatorUid == null) {
                Log.e(TAG, "Cannot find circle creator for cycle history");
                callback.onHistoryLoaded(new ArrayList<>());
                return;
            }

            DatabaseReference circlesRef = database.getReference("users")
                    .child(creatorUid).child("savingCircles");
            String sanitizedEmail = sanitizeEmail(memberEmail);

            circlesRef.child(circleId)
                    .child("members")
                    .child(sanitizedEmail)
                    .child("cycles")
                    .get()
                    .addOnSuccessListener(dataSnapshot -> {
                        List<MemberCycle> cycleList = new ArrayList<>();
                        for (DataSnapshot cycleSnapshot : dataSnapshot.getChildren()) {
                            MemberCycle cycle = cycleSnapshot.getValue(MemberCycle.class);
                            if (cycle != null) {
                                cycleList.add(cycle);
                            }
                        }
                        cycleList.sort((c1, c2) -> Long.compare(c1.getStartDate(), c2.getStartDate()));
                        callback.onHistoryLoaded(cycleList);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error loading cycle history: " + e.getMessage());
                        callback.onHistoryLoaded(new ArrayList<>());
                    });
        });
    }

    public interface CycleHistoryCallback {
        void onHistoryLoaded(List<MemberCycle> cycles);
    }

    // New method to complete a cycle and create the next one sequentially
    private void completeCycleAndCreateNext(String circleId, String memberEmail,
                                            MemberCycle cycle, String frequency,
                                            OnCycleLoadedListener finalListener) {
        getCircleCreatorUid(circleId, creatorUid -> {
            if (creatorUid == null) {
                finalListener.onError("Cannot find circle creator");
                return;
            }

            DatabaseReference circlesRef = database.getReference("users")
                    .child(creatorUid).child("savingCircles");
            String sanitizedEmail = sanitizeEmail(memberEmail);

            // Step 1: Mark cycle as complete
            cycle.setComplete(true);

            circlesRef.child(circleId)
                    .child("members")
                    .child(sanitizedEmail)
                    .child("cycles")
                    .child(cycle.getCycleId())
                    .setValue(cycle)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Cycle marked as complete: " + cycle.getCycleId());

                        // Step 2: Create next cycle only after completion is saved
                        MemberCycle nextCycle = MemberCycle.createNextCycle(cycle, frequency);

                        saveCycleToDatabase(circlesRef, circleId, memberEmail, nextCycle, new OnCycleCreatedListener() {
                            @Override
                            public void onCycleCreated(MemberCycle newCycle) {
                                Log.d(TAG, "Next cycle created after completion: " + newCycle.getCycleId());
                                finalListener.onCycleLoaded(newCycle);
                            }

                            @Override
                            public void onError(String message) {
                                finalListener.onError("Error creating next cycle: " + message);
                            }
                        });
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error completing cycle", e);
                        finalListener.onError("Error completing cycle: " + e.getMessage());
                    });
        });
    }

    // New method to fill gaps by creating multiple cycles sequentially
    private void fillCycleGap(String circleId, String memberEmail, MemberCycle lastCycle,
                              long targetDate, String frequency, OnCycleLoadedListener finalListener) {
        // First, ensure the last cycle is marked complete
        if (!lastCycle.isComplete()) {
            getCircleCreatorUid(circleId, creatorUid -> {
                if (creatorUid == null) {
                    finalListener.onError("Cannot find circle creator");
                    return;
                }

                DatabaseReference circlesRef = database.getReference("users")
                        .child(creatorUid).child("savingCircles");
                String sanitizedEmail = sanitizeEmail(memberEmail);

                lastCycle.setComplete(true);

                circlesRef.child(circleId)
                        .child("members")
                        .child(sanitizedEmail)
                        .child("cycles")
                        .child(lastCycle.getCycleId())
                        .setValue(lastCycle)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Last cycle marked complete before filling gap: " + lastCycle.getCycleId());
                            createCyclesUntilDate(circleId, memberEmail, lastCycle, targetDate, frequency, finalListener);
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error marking last cycle complete", e);
                            finalListener.onError("Error completing last cycle: " + e.getMessage());
                        });
            });
        } else {
            createCyclesUntilDate(circleId, memberEmail, lastCycle, targetDate, frequency, finalListener);
        }
    }

    // Recursively create cycles until we have one that contains the target date
    private void createCyclesUntilDate(String circleId, String memberEmail, MemberCycle previousCycle,
                                       long targetDate, String frequency, OnCycleLoadedListener finalListener) {
        MemberCycle nextCycle = MemberCycle.createNextCycle(previousCycle, frequency);

        getCircleCreatorUid(circleId, creatorUid -> {
            if (creatorUid == null) {
                finalListener.onError("Cannot find circle creator");
                return;
            }

            DatabaseReference circlesRef = database.getReference("users")
                    .child(creatorUid).child("savingCircles");

            saveCycleToDatabase(circlesRef, circleId, memberEmail, nextCycle, new OnCycleCreatedListener() {
                @Override
                public void onCycleCreated(MemberCycle newCycle) {
                    Log.d(TAG, "Gap-filling cycle created: " + newCycle.getCycleId());

                    if (newCycle.isDateInCycle(targetDate)) {
                        // We've reached the cycle containing the target date
                        finalListener.onCycleLoaded(newCycle);
                    } else if (targetDate > newCycle.getEndDate()) {
                        // Need to create more cycles - mark this one complete and continue
                        newCycle.setComplete(true);
                        circlesRef.child(circleId)
                                .child("members")
                                .child(sanitizeEmail(memberEmail))
                                .child("cycles")
                                .child(newCycle.getCycleId())
                                .setValue(newCycle)
                                .addOnSuccessListener(aVoid -> {
                                    createCyclesUntilDate(circleId, memberEmail, newCycle, targetDate, frequency, finalListener);
                                })
                                .addOnFailureListener(e -> {
                                    finalListener.onError("Error completing intermediate cycle: " + e.getMessage());
                                });
                    } else {
                        // This shouldn't happen, but handle it
                        finalListener.onCycleLoaded(newCycle);
                    }
                }

                @Override
                public void onError(String message) {
                    finalListener.onError("Error creating gap-filling cycle: " + message);
                }
            });
        });
    }

    public interface MembersCallback {
        void onMembersLoaded(List<SavingCircleMember> members);
    }

    /**
     * Get circle members with one-time read (not LiveData observer)
     */
    public void getSavingCircleMembersOnce(String circleId, MembersCallback callback) {
        if (savingCirclesRef == null) {
            Log.e(TAG, "savingCirclesRef is null! Cannot load members.");
            callback.onMembersLoaded(new ArrayList<>());
            return;
        }

        // First, get the circle to find the creator's UID
        savingCirclesRef.child(circleId).get()
                .addOnSuccessListener(circleSnapshot -> {
                    if (!circleSnapshot.exists()) {
                        Log.e(TAG, "Circle not found in current user's path: " + circleId);
                        // Try to find it in other users' paths (fallback)
                        findCircleAndLoadMembersOnce(circleId, callback);
                        return;
                    }

                    String creatorUid = circleSnapshot.child("creatorUid").getValue(String.class);
                    if (creatorUid == null || creatorUid.isEmpty()) {
                        // Backwards compatibility: if creatorUid is not set, assume current user is creator
                        creatorUid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
                    }

                    if (creatorUid != null) {
                        // Read members from the creator's path (ONE-TIME)
                        DatabaseReference creatorCirclesRef = database.getReference("users")
                                .child(creatorUid).child("savingCircles");
                        loadMembersFromPathOnce(creatorCirclesRef, circleId, callback);
                    } else {
                        Log.e(TAG, "Cannot determine creator UID for circle: " + circleId);
                        callback.onMembersLoaded(new ArrayList<>());
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error finding circle: " + e.getMessage());
                    findCircleAndLoadMembersOnce(circleId, callback);
                });
    }

    private void findCircleAndLoadMembersOnce(String circleId, MembersCallback callback) {
        database.getReference("users").get()
                .addOnSuccessListener(usersSnapshot -> {
                    for (DataSnapshot userSnapshot : usersSnapshot.getChildren()) {
                        DataSnapshot circleSnapshot = userSnapshot.child("savingCircles").child(circleId);
                        if (circleSnapshot.exists()) {
                            String creatorUid = userSnapshot.getKey();
                            DatabaseReference creatorCirclesRef = database.getReference("users")
                                    .child(creatorUid).child("savingCircles");
                            loadMembersFromPathOnce(creatorCirclesRef, circleId, callback);
                            return;
                        }
                    }
                    Log.e(TAG, "Circle not found in any user path: " + circleId);
                    callback.onMembersLoaded(new ArrayList<>());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error searching for circle: " + e.getMessage());
                    callback.onMembersLoaded(new ArrayList<>());
                });
    }

    private void loadMembersFromPathOnce(DatabaseReference circlesRef, String circleId, MembersCallback callback) {
        circlesRef.child(circleId).child("members").get()
                .addOnSuccessListener(snapshot -> {
                    List<SavingCircleMember> members = new ArrayList<>();
                    for (DataSnapshot memberSnapshot : snapshot.getChildren()) {
                        SavingCircleMember member = memberSnapshot.getValue(SavingCircleMember.class);
                        if (member != null) {
                            members.add(member);
                        }
                    }
                    Log.d(TAG, "Loaded " + members.size() + " members for circle " + circleId + " (one-time)");
                    callback.onMembersLoaded(members);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading members for circle " + circleId + ": " + e.getMessage());
                    callback.onMembersLoaded(new ArrayList<>());
                });
    }
}