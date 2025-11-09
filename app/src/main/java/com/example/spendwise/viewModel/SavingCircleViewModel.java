package com.example.spendwise.viewModel;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.spendwise.model.MemberCycle;
import com.example.spendwise.model.SavingCircle;
import com.example.spendwise.model.SavingCircleInvitation;
import com.example.spendwise.model.SavingCircleMember;
import com.example.spendwise.repository.SavingCircleRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * ViewModel for SavingCircle operations using Firestore
 * Migrated from Firebase Realtime Database to Firestore
 */
public class SavingCircleViewModel extends ViewModel {
    private static final String TAG = "SavingCircleViewModel";

    private final MutableLiveData<String> statusMessage;
    private final MutableLiveData<String> currentUserEmail;
    private final SavingCircleRepository repository;
    private final FirebaseAuth auth;

    // MediatorLiveData to combine repository LiveData
    private final MediatorLiveData<List<SavingCircle>> savingCircles;
    private final MediatorLiveData<List<SavingCircleInvitation>> invitations;

    public SavingCircleViewModel() {
        savingCircles = new MediatorLiveData<>(new ArrayList<>());
        statusMessage = new MutableLiveData<>();
        currentUserEmail = new MutableLiveData<>();
        invitations = new MediatorLiveData<>(new ArrayList<>());

        repository = new SavingCircleRepository();
        auth = FirebaseAuth.getInstance();

        loadCurrentUserEmail();
        initializeListeners();
    }

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

    private void initializeListeners() {
        // Observe saving circles from repository
        savingCircles.addSource(repository.getSavingCircles(), circles -> {
            if (circles != null) {
                savingCircles.setValue(circles);
            }
        });

        // Observe invitations from repository
        String email = currentUserEmail.getValue();
        if (email != null && !email.isEmpty()) {
            invitations.addSource(repository.getInvitations(email), invs -> {
                if (invs != null) {
                    invitations.setValue(invs);
                }
            });
        }
    }

    public LiveData<String> getCurrentUserEmail() {
        return currentUserEmail;
    }

    public LiveData<List<SavingCircleInvitation>> getInvitations() {
        return invitations;
    }

    public LiveData<List<SavingCircle>> getSavingCircles() {
        return savingCircles;
    }

    public LiveData<String> getStatusMessage() {
        return statusMessage;
    }

    /**
     * Add a new saving circle
     */
    public void addSavingCircle(String groupName, String creatorEmail, String challengeTitle,
                                double goalAmount, String frequency, String notes,
                                double personalAllocation, long dashboardTimestamp) {
        SavingCircle savingCircle = new SavingCircle(groupName, creatorEmail, challengeTitle,
                goalAmount, frequency, notes, dashboardTimestamp);

        repository.addSavingCircle(savingCircle, new SavingCircleRepository.RepositoryCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Saving circle added successfully: " + savingCircle);
                
                // Add creator as first member
                addMemberToCircle(savingCircle.getId(), creatorEmail, personalAllocation,
                        dashboardTimestamp, frequency);
                
                statusMessage.setValue("Saving circle created!");
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error adding saving circle: " + error);
                statusMessage.setValue("Error: " + error);
            }
        });
    }

    /**
     * Add member to a saving circle
     */
    public void addMemberToCircle(String circleId, String memberEmail,
                                  double personalAllocation, long joinTimestamp,
                                  String frequency) {
        SavingCircleMember member = new SavingCircleMember(memberEmail, personalAllocation, joinTimestamp);

        repository.addMember(circleId, member, new SavingCircleRepository.RepositoryCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Member added to circle: " + memberEmail);
                initializeMemberCycle(circleId, memberEmail, joinTimestamp, frequency, personalAllocation);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error adding member to circle: " + error);
            }
        });
    }

    /**
     * Send invitation
     */
    public void sendInvitation(String circleId, String inviteeEmail, OnInvitationSentListener listener) {
        repository.getSavingCircleForInvitation(circleId, new SavingCircleRepository.OnCircleLoadedListener() {
            @Override
            public void onCircleLoaded(SavingCircle circle) {
                String inviterEmail = currentUserEmail.getValue();
                if (inviterEmail == null) {
                    if (listener != null) listener.onError("User not logged in");
                    return;
                }

                SavingCircleInvitation invitation = new SavingCircleInvitation(
                        circleId,
                        circle.getGroupName(),
                        circle.getChallengeTitle(),
                        inviterEmail,
                        inviteeEmail,
                        circle.getGoalAmount(),
                        circle.getFrequency()
                );

                repository.sendInvitation(invitation, new SavingCircleRepository.RepositoryCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Invitation sent to " + inviteeEmail);
                        if (listener != null) listener.onInvitationSent();
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Error sending invitation: " + error);
                        if (listener != null) listener.onError(error);
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error fetching circle for invitation: " + error);
                if (listener != null) listener.onError(error);
            }
        });
    }

    /**
     * Accept invitation
     */
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

    /**
     * Decline invitation
     */
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
        repository.updateInvitationStatus(invitation.getInviteeEmail(), 
                invitation.getInvitationId(), status, respondedAt,
                new SavingCircleRepository.RepositoryCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Invitation " + status + " for " + invitation.getInviteeEmail());
                        if (listener != null) listener.onSuccess();
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Error updating invitation status: " + error);
                        if (listener != null) listener.onError(error);
                    }
                });
    }

    /**
     * Initialize member cycle
     */
    public void initializeMemberCycle(String circleId, String memberEmail,
                                      long joinDate, String frequency,
                                      double startAmount) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(joinDate);

        if ("Weekly".equals(frequency)) {
            calendar.add(Calendar.WEEK_OF_YEAR, 1);
        } else {
            calendar.add(Calendar.MONTH, 1);
        }

        long endDate = calendar.getTimeInMillis();
        MemberCycle firstCycle = new MemberCycle(joinDate, endDate, startAmount);

        repository.saveMemberCycle(circleId, memberEmail, firstCycle,
                new SavingCircleRepository.RepositoryCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Initial cycle created for member: " + memberEmail);
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Error creating initial cycle: " + error);
                    }
                });
    }

    /**
     * Get current cycle for a member
     */
    public void getCurrentCycle(String circleId, String memberEmail,
                                OnCycleLoadedListener listener) {
        LiveData<List<MemberCycle>> cyclesLiveData = repository.getMemberCycles(circleId, memberEmail);
        List<MemberCycle> cycles = cyclesLiveData.getValue();
        
        if (cycles == null || cycles.isEmpty()) {
            // If no value yet, observe once
            androidx.lifecycle.Observer<List<MemberCycle>> observer = new androidx.lifecycle.Observer<List<MemberCycle>>() {
                @Override
                public void onChanged(List<MemberCycle> cycles) {
                    cyclesLiveData.removeObserver(this);
                    processCycleList(cycles, listener);
                }
            };
            cyclesLiveData.observeForever(observer);
        } else {
            processCycleList(cycles, listener);
        }
    }
    
    private void processCycleList(List<MemberCycle> cycles, OnCycleLoadedListener listener) {
        if (cycles == null || cycles.isEmpty()) {
            listener.onCycleNotFound();
            return;
        }

        long currentTime = System.currentTimeMillis();
        MemberCycle currentCycle = null;

        for (MemberCycle cycle : cycles) {
            if (cycle.isDateInCycle(currentTime)) {
                currentCycle = cycle;
                break;
            }
        }

        if (currentCycle != null) {
            listener.onCycleLoaded(currentCycle);
        } else {
            listener.onCycleNotFound();
        }
    }

    /**
     * Check and create next cycle if needed
     */
    public void checkAndCreateNextCycle(String circleId, String memberEmail, String frequency) {
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
        cycle.setComplete(true);
        repository.saveMemberCycle(circleId, memberEmail, cycle,
                new SavingCircleRepository.RepositoryCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Cycle completed: " + cycle.getCycleId());
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Error completing cycle: " + error);
                    }
                });
    }

    private void createNextCycle(String circleId, String memberEmail,
                                 MemberCycle previousCycle, String frequency) {
        MemberCycle nextCycle = MemberCycle.createNextCycle(previousCycle, frequency);
        repository.saveMemberCycle(circleId, memberEmail, nextCycle,
                new SavingCircleRepository.RepositoryCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Next cycle created: " + nextCycle.getCycleId());
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Error creating next cycle: " + error);
                    }
                });
    }

    /**
     * Record expense in cycle
     */
    public void recordExpenseInCycle(String circleId, String memberEmail, double amount) {
        getCurrentCycle(circleId, memberEmail, new OnCycleLoadedListener() {
            @Override
            public void onCycleLoaded(MemberCycle cycle) {
                cycle.recordExpense(amount);
                repository.saveMemberCycle(circleId, memberEmail, cycle,
                        new SavingCircleRepository.RepositoryCallback() {
                            @Override
                            public void onSuccess() {
                                Log.d(TAG, "Expense recorded in cycle");
                            }

                            @Override
                            public void onError(String error) {
                                Log.e(TAG, "Error recording expense: " + error);
                            }
                        });
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

    /**
     * Restore expense in cycle
     */
    public void restoreExpenseInCycle(String circleId, String memberEmail, double amount) {
        getCurrentCycle(circleId, memberEmail, new OnCycleLoadedListener() {
            @Override
            public void onCycleLoaded(MemberCycle cycle) {
                cycle.restoreExpense(amount);
                repository.saveMemberCycle(circleId, memberEmail, cycle,
                        new SavingCircleRepository.RepositoryCallback() {
                            @Override
                            public void onSuccess() {
                                Log.d(TAG, "Expense restored in cycle");
                            }

                            @Override
                            public void onError(String error) {
                                Log.e(TAG, "Error restoring expense: " + error);
                            }
                        });
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
     * Get member cycle history
     */
    public LiveData<List<MemberCycle>> getMemberCycleHistory(String circleId, String memberEmail) {
        return repository.getMemberCycles(circleId, memberEmail);
    }

    /**
     * Deduct expense from member
     */
    public void deductExpenseFromMember(String circleId, String memberEmail, double expenseAmount) {
        LiveData<List<SavingCircleMember>> membersLiveData = repository.getMembers(circleId);
        List<SavingCircleMember> members = membersLiveData.getValue();
        
        if (members == null) {
            // Observe once to get current members
            androidx.lifecycle.Observer<List<SavingCircleMember>> observer = new androidx.lifecycle.Observer<List<SavingCircleMember>>() {
                @Override
                public void onChanged(List<SavingCircleMember> members) {
                    membersLiveData.removeObserver(this);
                    processDeductExpense(members, circleId, memberEmail, expenseAmount);
                }
            };
            membersLiveData.observeForever(observer);
        } else {
            processDeductExpense(members, circleId, memberEmail, expenseAmount);
        }
    }
    
    private void processDeductExpense(List<SavingCircleMember> members, String circleId, 
                                     String memberEmail, double expenseAmount) {
        if (members == null) return;

        for (SavingCircleMember member : members) {
            if (member.getEmail().equals(memberEmail)) {
                double newAmount = member.getCurrentAmount() - expenseAmount;
                if (newAmount < 0) newAmount = 0;

                repository.updateMemberAmount(circleId, memberEmail, newAmount,
                        new SavingCircleRepository.RepositoryCallback() {
                            @Override
                            public void onSuccess() {
                                Log.d(TAG, "Expense deducted from member's current amount");
                                recordExpenseInCycle(circleId, memberEmail, expenseAmount);
                            }

                            @Override
                            public void onError(String error) {
                                Log.e(TAG, "Error deducting expense: " + error);
                            }
                        });
                break;
            }
        }
    }

    /**
     * Add back expense to member
     */
    public void addBackExpenseToMember(String circleId, String memberEmail, double expenseAmount) {
        LiveData<List<SavingCircleMember>> membersLiveData = repository.getMembers(circleId);
        List<SavingCircleMember> members = membersLiveData.getValue();
        
        if (members == null) {
            androidx.lifecycle.Observer<List<SavingCircleMember>> observer = new androidx.lifecycle.Observer<List<SavingCircleMember>>() {
                @Override
                public void onChanged(List<SavingCircleMember> members) {
                    membersLiveData.removeObserver(this);
                    processAddBackExpense(members, circleId, memberEmail, expenseAmount);
                }
            };
            membersLiveData.observeForever(observer);
        } else {
            processAddBackExpense(members, circleId, memberEmail, expenseAmount);
        }
    }
    
    private void processAddBackExpense(List<SavingCircleMember> members, String circleId,
                                      String memberEmail, double expenseAmount) {
        if (members == null) return;

        for (SavingCircleMember member : members) {
            if (member.getEmail().equals(memberEmail)) {
                double newAmount = member.getCurrentAmount() + expenseAmount;

                repository.updateMemberAmount(circleId, memberEmail, newAmount,
                        new SavingCircleRepository.RepositoryCallback() {
                            @Override
                            public void onSuccess() {
                                Log.d(TAG, "Expense amount added back");
                                restoreExpenseInCycle(circleId, memberEmail, expenseAmount);
                            }

                            @Override
                            public void onError(String error) {
                                Log.e(TAG, "Error adding back expense: " + error);
                            }
                        });
                break;
            }
        }
    }

    /**
     * Update member current amount
     */
    public void updateMemberCurrentAmount(String circleId, String memberEmail, double newAmount) {
        repository.updateMemberAmount(circleId, memberEmail, newAmount,
                new SavingCircleRepository.RepositoryCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Member current amount updated");
                        statusMessage.setValue("Amount updated!");
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Error updating member amount: " + error);
                    }
                });
    }

    /**
     * Update saving circle
     */
    public void updateSavingCircle(String id, String groupName, String creatorEmail,
                                   String challengeTitle, double goalAmount,
                                   String frequency, String notes, long createdAtTimestamp) {
        SavingCircle savingCircle = new SavingCircle(groupName, creatorEmail, challengeTitle,
                goalAmount, frequency, notes, createdAtTimestamp);
        savingCircle.setId(id);

        repository.updateSavingCircle(savingCircle, new SavingCircleRepository.RepositoryCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Saving circle updated successfully");
                statusMessage.setValue("Saving circle updated!");
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error updating saving circle: " + error);
                statusMessage.setValue("Error: " + error);
            }
        });
    }

    /**
     * Delete saving circle
     */
    public void deleteSavingCircle(String id) {
        repository.deleteSavingCircle(id, new SavingCircleRepository.RepositoryCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Saving circle deleted successfully");
                statusMessage.setValue("Saving circle deleted!");
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error deleting saving circle: " + error);
                statusMessage.setValue("Error: " + error);
            }
        });
    }

    /**
     * Get cycle at specific date
     */
    public void getCycleAtDate(String circleId, String memberEmail, long targetDate,
                               OnCycleLoadedListener listener) {
        LiveData<List<MemberCycle>> cyclesLiveData = repository.getMemberCycles(circleId, memberEmail);
        List<MemberCycle> cycles = cyclesLiveData.getValue();
        
        if (cycles == null) {
            androidx.lifecycle.Observer<List<MemberCycle>> observer = new androidx.lifecycle.Observer<List<MemberCycle>>() {
                @Override
                public void onChanged(List<MemberCycle> cycles) {
                    cyclesLiveData.removeObserver(this);
                    processCycleAtDate(cycles, targetDate, listener);
                }
            };
            cyclesLiveData.observeForever(observer);
        } else {
            processCycleAtDate(cycles, targetDate, listener);
        }
    }
    
    private void processCycleAtDate(List<MemberCycle> cycles, long targetDate, OnCycleLoadedListener listener) {
        if (cycles == null || cycles.isEmpty()) {
            listener.onCycleNotFound();
            return;
        }

        MemberCycle targetCycle = null;
        for (MemberCycle cycle : cycles) {
            if (cycle.isDateInCycle(targetDate)) {
                targetCycle = cycle;
                break;
            }
        }

        if (targetCycle != null) {
            listener.onCycleLoaded(targetCycle);
        } else {
            listener.onCycleNotFound();
        }
    }

    /**
     * Create cycle
     */
    public void createCycle(String circleId, String memberEmail, MemberCycle cycle,
                            OnCycleCreatedListener listener) {
        repository.saveMemberCycle(circleId, memberEmail, cycle,
                new SavingCircleRepository.RepositoryCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Cycle created: " + cycle.getCycleId());
                        if (listener != null) listener.onCycleCreated(cycle);
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Error creating cycle: " + error);
                        if (listener != null) listener.onError(error);
                    }
                });
    }

    /**
     * Get saving circle by ID
     */
    public LiveData<SavingCircle> getSavingCircleById(String circleId) {
        return repository.getSavingCircleById(circleId);
    }

    /**
     * Get saving circle members
     */
    public LiveData<List<SavingCircleMember>> getSavingCircleMembers(String circleId) {
        return repository.getMembers(circleId);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.cleanup();
    }

    // Interfaces
    public interface OnCycleLoadedListener {
        void onCycleLoaded(MemberCycle cycle);
        void onCycleNotFound();
        void onError(String message);
    }

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
}

