package com.example.spendwise.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.spendwise.model.MemberCycle;
import com.example.spendwise.model.SavingCircle;
import com.example.spendwise.model.SavingCircleInvitation;
import com.example.spendwise.model.SavingCircleMember;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository for SavingCircle operations using Firestore
 * Handles SavingCircles, Members, Cycles, and Invitations
 */
public class SavingCircleRepository extends FirestoreRepository<SavingCircle> {

    private static final String COLLECTION_NAME = "savingCircles";
    private static final String INVITATIONS_COLLECTION = "invitations";
    
    private MutableLiveData<List<SavingCircle>> savingCirclesLiveData;
    private MutableLiveData<List<SavingCircleInvitation>> invitationsLiveData;
    private ListenerRegistration savingCirclesListener;
    private ListenerRegistration invitationsListener;
    private String currentUserEmail;

    public SavingCircleRepository() {
        super();
        savingCirclesLiveData = new MutableLiveData<>(new ArrayList<>());
        invitationsLiveData = new MutableLiveData<>(new ArrayList<>());
        currentUserEmail = getCurrentUserEmail();
    }

    /**
     * Get LiveData for saving circles list
     */
    public LiveData<List<SavingCircle>> getSavingCircles() {
        if (savingCirclesListener == null) {
            attachSavingCirclesListener();
        }
        return savingCirclesLiveData;
    }

    /**
     * Get LiveData for invitations list
     */
    public LiveData<List<SavingCircleInvitation>> getInvitations(String userEmail) {
        if (invitationsListener == null && userEmail != null) {
            attachInvitationsListener(userEmail);
        }
        return invitationsLiveData;
    }

    /**
     * Attach real-time listener to saving circles collection
     */
    private void attachSavingCirclesListener() {
        String userId = getCurrentUserId();
        if (userId == null) {
            savingCirclesLiveData.setValue(new ArrayList<>());
            return;
        }

        CollectionReference circlesRef = getUserCollection(COLLECTION_NAME);
        
        savingCirclesListener = circlesRef.orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        savingCirclesLiveData.setValue(new ArrayList<>());
                        return;
                    }

                    if (snapshot != null) {
                        List<SavingCircle> circles = new ArrayList<>();
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            SavingCircle circle = documentToModel(doc.getId(), doc.getData());
                            if (circle != null) {
                                circles.add(circle);
                            }
                        }
                        savingCirclesLiveData.setValue(circles);
                    } else {
                        savingCirclesLiveData.setValue(new ArrayList<>());
                    }
                });

        activeListeners.put("savingCircles", savingCirclesListener);
    }

    /**
     * Attach real-time listener to invitations collection
     * Firestore structure: invitations/{sanitizedEmail}/invitations/{invitationId}
     */
    private void attachInvitationsListener(String userEmail) {
        if (userEmail == null || userEmail.isEmpty()) {
            return;
        }

        String sanitizedEmail = sanitizeEmail(userEmail);
        // Firestore structure: root collection -> document (user) -> subcollection (invitations)
        CollectionReference invitationsRef = db.collection(INVITATIONS_COLLECTION)
                .document(sanitizedEmail)
                .collection("invitations");

        invitationsListener = invitationsRef
                .whereEqualTo("status", "pending")
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        invitationsLiveData.setValue(new ArrayList<>());
                        return;
                    }

                    if (snapshot != null) {
                        List<SavingCircleInvitation> invitationList = new ArrayList<>();
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            SavingCircleInvitation invitation = parseInvitation(doc);
                            if (invitation != null) {
                                invitationList.add(invitation);
                            }
                        }
                        invitationsLiveData.setValue(invitationList);
                    } else {
                        invitationsLiveData.setValue(new ArrayList<>());
                    }
                });

        activeListeners.put("invitations", invitationsListener);
    }

    /**
     * Add a new saving circle
     */
    public void addSavingCircle(SavingCircle circle, RepositoryCallback callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            if (callback != null) callback.onError("User not logged in");
            return;
        }

        CollectionReference circlesRef = getUserCollection(COLLECTION_NAME);
        Map<String, Object> data = modelToDocument(circle);

        circlesRef.add(data)
                .addOnSuccessListener(documentReference -> {
                    circle.setId(documentReference.getId());
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    /**
     * Update an existing saving circle
     */
    public void updateSavingCircle(SavingCircle circle, RepositoryCallback callback) {
        if (circle.getId() == null || circle.getId().isEmpty()) {
            if (callback != null) callback.onError("Circle ID is required");
            return;
        }

        DocumentReference circleRef = getUserDocument(COLLECTION_NAME, circle.getId());
        Map<String, Object> data = modelToDocument(circle);

        circleRef.update(data)
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    /**
     * Delete a saving circle
     */
    public void deleteSavingCircle(String circleId, RepositoryCallback callback) {
        DocumentReference circleRef = getUserDocument(COLLECTION_NAME, circleId);

        circleRef.delete()
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    /**
     * Add member to a saving circle
     */
    public void addMember(String circleId, SavingCircleMember member, RepositoryCallback callback) {
        DocumentReference circleRef = getUserDocument(COLLECTION_NAME, circleId);
        String sanitizedEmail = sanitizeEmail(member.getEmail());
        Map<String, Object> memberData = memberToDocument(member);

        circleRef.collection("members")
                .document(sanitizedEmail)
                .set(memberData)
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    /**
     * Get saving circle members
     */
    public LiveData<List<SavingCircleMember>> getMembers(String circleId) {
        MutableLiveData<List<SavingCircleMember>> membersLiveData = new MutableLiveData<>(new ArrayList<>());
        
        DocumentReference circleRef = getUserDocument(COLLECTION_NAME, circleId);
        circleRef.collection("members")
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        membersLiveData.setValue(new ArrayList<>());
                        return;
                    }

                    if (snapshot != null) {
                        List<SavingCircleMember> members = new ArrayList<>();
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            SavingCircleMember member = parseMember(doc);
                            if (member != null) {
                                members.add(member);
                            }
                        }
                        membersLiveData.setValue(members);
                    }
                });

        return membersLiveData;
    }

    /**
     * Send invitation
     */
    public void sendInvitation(SavingCircleInvitation invitation, RepositoryCallback callback) {
        String sanitizedEmail = sanitizeEmail(invitation.getInviteeEmail());
        Map<String, Object> invitationData = invitationToDocument(invitation);

        db.collection(INVITATIONS_COLLECTION)
                .document(sanitizedEmail)
                .collection("invitations")
                .document(invitation.getInvitationId())
                .set(invitationData)
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    /**
     * Update invitation status
     */
    public void updateInvitationStatus(String inviteeEmail, String invitationId, 
                                      String status, long respondedAt, RepositoryCallback callback) {
        String sanitizedEmail = sanitizeEmail(inviteeEmail);
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);
        updates.put("respondedAt", respondedAt);

        db.collection(INVITATIONS_COLLECTION)
                .document(sanitizedEmail)
                .collection("invitations")
                .document(invitationId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    /**
     * Get saving circle by ID
     */
    public LiveData<SavingCircle> getSavingCircleById(String circleId) {
        MutableLiveData<SavingCircle> circleLiveData = new MutableLiveData<>();
        
        DocumentReference circleRef = getUserDocument(COLLECTION_NAME, circleId);
        circleRef.addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                circleLiveData.setValue(null);
                return;
            }

            if (snapshot != null && snapshot.exists()) {
                SavingCircle circle = documentToModel(snapshot.getId(), snapshot.getData());
                circleLiveData.setValue(circle);
            } else {
                circleLiveData.setValue(null);
            }
        });

        return circleLiveData;
    }

    // Helper methods for parsing and converting

    @Override
    protected SavingCircle documentToModel(String documentId, Map<String, Object> data) {
        try {
            String groupName = (String) data.get("groupName");
            String creatorEmail = (String) data.get("creatorEmail");
            String challengeTitle = (String) data.get("challengeTitle");
            Object goalAmountObj = data.get("goalAmount");
            double goalAmount = goalAmountObj instanceof Number ? ((Number) goalAmountObj).doubleValue() : 0.0;
            String frequency = (String) data.get("frequency");
            String notes = data.get("notes") != null ? (String) data.get("notes") : "";
            Object createdAtObj = data.get("createdAt");
            long createdAt = createdAtObj instanceof Number ? ((Number) createdAtObj).longValue() : System.currentTimeMillis();

            if (groupName == null || creatorEmail == null || challengeTitle == null) {
                return null;
            }

            SavingCircle circle = new SavingCircle(groupName, creatorEmail, challengeTitle, 
                    goalAmount, frequency != null ? frequency : "Monthly", notes, createdAt);
            circle.setId(documentId);
            return circle;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    protected Map<String, Object> modelToDocument(SavingCircle circle) {
        Map<String, Object> data = new HashMap<>();
        data.put("groupName", circle.getGroupName());
        data.put("creatorEmail", circle.getCreatorEmail());
        data.put("challengeTitle", circle.getChallengeTitle());
        data.put("goalAmount", circle.getGoalAmount());
        data.put("frequency", circle.getFrequency());
        data.put("notes", circle.getNotes());
        data.put("createdAt", circle.getCreatedAt());
        return data;
    }

    private SavingCircleMember parseMember(DocumentSnapshot doc) {
        try {
            Map<String, Object> data = doc.getData();
            if (data == null) return null;

            String email = (String) data.get("email");
            Object allocationObj = data.get("personalAllocation");
            double allocation = allocationObj instanceof Number ? ((Number) allocationObj).doubleValue() : 0.0;
            Object currentObj = data.get("currentAmount");
            double current = currentObj instanceof Number ? ((Number) currentObj).doubleValue() : allocation;
            Object joinedObj = data.get("joinedAt");
            long joinedAt = joinedObj instanceof Number ? ((Number) joinedObj).longValue() : System.currentTimeMillis();

            if (email == null) return null;

            SavingCircleMember member = new SavingCircleMember(email, allocation, joinedAt);
            member.setCurrentAmount(current);
            return member;
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> memberToDocument(SavingCircleMember member) {
        Map<String, Object> data = new HashMap<>();
        data.put("email", member.getEmail());
        data.put("personalAllocation", member.getPersonalAllocation());
        data.put("currentAmount", member.getCurrentAmount());
        data.put("joinedAt", member.getJoinedAt());
        return data;
    }

    private SavingCircleInvitation parseInvitation(DocumentSnapshot doc) {
        try {
            Map<String, Object> data = doc.getData();
            if (data == null) return null;

            String invitationId = doc.getId();
            String circleId = (String) data.get("circleId");
            String circleName = (String) data.get("circleName");
            String challengeTitle = (String) data.get("challengeTitle");
            String inviterEmail = (String) data.get("inviterEmail");
            String inviteeEmail = (String) data.get("inviteeEmail");
            String status = data.get("status") != null ? (String) data.get("status") : "pending";
            Object sentAtObj = data.get("sentAt");
            long sentAt = sentAtObj instanceof Number ? ((Number) sentAtObj).longValue() : System.currentTimeMillis();
            Object respondedAtObj = data.get("respondedAt");
            long respondedAt = respondedAtObj instanceof Number ? ((Number) respondedAtObj).longValue() : 0;
            Object goalAmountObj = data.get("goalAmount");
            double goalAmount = goalAmountObj instanceof Number ? ((Number) goalAmountObj).doubleValue() : 0.0;
            String frequency = (String) data.get("frequency");

            if (circleId == null || inviteeEmail == null) return null;

            SavingCircleInvitation invitation = new SavingCircleInvitation(
                    circleId, circleName, challengeTitle, inviterEmail, inviteeEmail, goalAmount, frequency);
            invitation.setInvitationId(invitationId);
            invitation.setStatus(status);
            invitation.setSentAt(sentAt);
            invitation.setRespondedAt(respondedAt);
            return invitation;
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> invitationToDocument(SavingCircleInvitation invitation) {
        Map<String, Object> data = new HashMap<>();
        data.put("circleId", invitation.getCircleId());
        data.put("circleName", invitation.getCircleName());
        data.put("challengeTitle", invitation.getChallengeTitle());
        data.put("inviterEmail", invitation.getInviterEmail());
        data.put("inviteeEmail", invitation.getInviteeEmail());
        data.put("status", invitation.getStatus());
        data.put("sentAt", invitation.getSentAt());
        data.put("respondedAt", invitation.getRespondedAt());
        data.put("goalAmount", invitation.getGoalAmount());
        data.put("frequency", invitation.getFrequency());
        return data;
    }

    /**
     * Update member's current amount
     */
    public void updateMemberAmount(String circleId, String memberEmail, double newAmount, RepositoryCallback callback) {
        DocumentReference circleRef = getUserDocument(COLLECTION_NAME, circleId);
        String sanitizedEmail = sanitizeEmail(memberEmail);

        circleRef.collection("members")
                .document(sanitizedEmail)
                .update("currentAmount", newAmount)
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    /**
     * Add or update member cycle
     */
    public void saveMemberCycle(String circleId, String memberEmail, MemberCycle cycle, RepositoryCallback callback) {
        DocumentReference circleRef = getUserDocument(COLLECTION_NAME, circleId);
        String sanitizedEmail = sanitizeEmail(memberEmail);
        Map<String, Object> cycleData = cycleToDocument(cycle);

        circleRef.collection("members")
                .document(sanitizedEmail)
                .collection("cycles")
                .document(cycle.getCycleId())
                .set(cycleData)
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    /**
     * Get member cycles
     */
    public LiveData<List<MemberCycle>> getMemberCycles(String circleId, String memberEmail) {
        MutableLiveData<List<MemberCycle>> cyclesLiveData = new MutableLiveData<>(new ArrayList<>());
        
        DocumentReference circleRef = getUserDocument(COLLECTION_NAME, circleId);
        String sanitizedEmail = sanitizeEmail(memberEmail);

        circleRef.collection("members")
                .document(sanitizedEmail)
                .collection("cycles")
                .orderBy("startDate", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        cyclesLiveData.setValue(new ArrayList<>());
                        return;
                    }

                    if (snapshot != null) {
                        List<MemberCycle> cycles = new ArrayList<>();
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            MemberCycle cycle = parseCycle(doc);
                            if (cycle != null) {
                                cycles.add(cycle);
                            }
                        }
                        cyclesLiveData.setValue(cycles);
                    }
                });

        return cyclesLiveData;
    }

    private MemberCycle parseCycle(DocumentSnapshot doc) {
        try {
            Map<String, Object> data = doc.getData();
            if (data == null) return null;

            String cycleId = doc.getId();
            Object startDateObj = data.get("startDate");
            long startDate = startDateObj instanceof Number ? ((Number) startDateObj).longValue() : 0;
            Object endDateObj = data.get("endDate");
            long endDate = endDateObj instanceof Number ? ((Number) endDateObj).longValue() : 0;
            Object startAmountObj = data.get("startAmount");
            double startAmount = startAmountObj instanceof Number ? ((Number) startAmountObj).doubleValue() : 0.0;
            Object endAmountObj = data.get("endAmount");
            double endAmount = endAmountObj instanceof Number ? ((Number) endAmountObj).doubleValue() : startAmount;
            Object spentObj = data.get("spent");
            double spent = spentObj instanceof Number ? ((Number) spentObj).doubleValue() : 0.0;
            Object contributedObj = data.get("contributed");
            double contributed = contributedObj instanceof Number ? ((Number) contributedObj).doubleValue() : 0.0;
            Object isCompleteObj = data.get("isComplete");
            boolean isComplete = isCompleteObj instanceof Boolean ? (Boolean) isCompleteObj : false;
            Object goalReachedObj = data.get("goalReached");
            boolean goalReached = goalReachedObj instanceof Boolean ? (Boolean) goalReachedObj : false;

            MemberCycle cycle = new MemberCycle(startDate, endDate, startAmount);
            cycle.setCycleId(cycleId);
            cycle.setEndAmount(endAmount);
            cycle.setSpent(spent);
            cycle.setContributed(contributed);
            cycle.setComplete(isComplete);
            cycle.setGoalReached(goalReached);
            return cycle;
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> cycleToDocument(MemberCycle cycle) {
        Map<String, Object> data = new HashMap<>();
        data.put("cycleId", cycle.getCycleId());
        data.put("startDate", cycle.getStartDate());
        data.put("endDate", cycle.getEndDate());
        data.put("startAmount", cycle.getStartAmount());
        data.put("endAmount", cycle.getEndAmount());
        data.put("spent", cycle.getSpent());
        data.put("contributed", cycle.getContributed());
        data.put("isComplete", cycle.isComplete());
        data.put("goalReached", cycle.isGoalReached());
        return data;
    }

    /**
     * Get saving circle data for invitation
     */
    public void getSavingCircleForInvitation(String circleId, OnCircleLoadedListener listener) {
        DocumentReference circleRef = getUserDocument(COLLECTION_NAME, circleId);
        circleRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        SavingCircle circle = documentToModel(documentSnapshot.getId(), documentSnapshot.getData());
                        if (listener != null) listener.onCircleLoaded(circle);
                    } else {
                        if (listener != null) listener.onError("Saving circle not found");
                    }
                })
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onError(e.getMessage());
                });
    }

    private String sanitizeEmail(String email) {
        if (email == null) return "";
        return email.replace(".", "_").replace("@", "_at_");
    }

    public void cleanup() {
        removeAllListeners();
        savingCirclesListener = null;
        invitationsListener = null;
    }

    public interface RepositoryCallback {
        void onSuccess();
        void onError(String error);
    }

    public interface OnCircleLoadedListener {
        void onCircleLoaded(SavingCircle circle);
        void onError(String error);
    }
}

