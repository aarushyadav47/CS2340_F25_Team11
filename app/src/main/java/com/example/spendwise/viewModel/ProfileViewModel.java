package com.example.spendwise.viewModel;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.spendwise.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

public class ProfileViewModel extends ViewModel {

    private final MutableLiveData<User> userProfile = new MutableLiveData<>();
    private final MutableLiveData<String> statusMessage = new MutableLiveData<>();
    private final MutableLiveData<Integer> expensesCount = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> budgetsCount = new MutableLiveData<>(0);
    private final MutableLiveData<List<String>> friendEmails = new MutableLiveData<>(new ArrayList<>());

    private final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final StorageReference storageRef = FirebaseStorage.getInstance().getReference();

    public LiveData<User> getUserProfile() {
        return userProfile;
    }

    public LiveData<String> getStatusMessage() {
        return statusMessage;
    }

    public LiveData<Integer> getExpensesCount() {
        return expensesCount;
    }

    public LiveData<Integer> getBudgetsCount() {
        return budgetsCount;
    }

    public LiveData<List<String>> getFriendEmails() {
        return friendEmails;
    }

    public void loadUserProfile() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

        String uid = currentUser.getUid();
        dbRef.child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    userProfile.setValue(user);
                    loadFriends(user.getFriends());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                statusMessage.setValue("Failed to load profile: " + error.getMessage());
            }
        });

        // Load Stats
        dbRef.child("users").child(uid).child("expenses").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                expensesCount.setValue((int) snapshot.getChildrenCount());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        dbRef.child("users").child(uid).child("budgets").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                budgetsCount.setValue((int) snapshot.getChildrenCount());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadFriends(List<String> friendIds) {
        if (friendIds == null || friendIds.isEmpty()) {
            friendEmails.setValue(new ArrayList<>());
            return;
        }

        List<String> emails = new ArrayList<>();
        for (String friendId : friendIds) {
            dbRef.child("users").child(friendId).child("email").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String email = snapshot.getValue(String.class);
                    if (email != null) {
                        emails.add(email);
                        friendEmails.setValue(new ArrayList<>(emails)); // Update LiveData
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        }
    }

    public void uploadProfileImage(Uri imageUri) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

        String uid = currentUser.getUid();
        StorageReference profileRef = storageRef.child("profile_images/" + uid + ".jpg");

        profileRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> profileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String downloadUrl = uri.toString();
                    dbRef.child("users").child(uid).child("profileImageUrl").setValue(downloadUrl)
                            .addOnSuccessListener(aVoid -> {
                                statusMessage.setValue("Profile picture updated!");
                                loadUserProfile(); // Reload to update UI
                            });
                }))
                .addOnFailureListener(e -> statusMessage.setValue("Image upload failed: " + e.getMessage()));
    }

    public void addFriend(String email) {
        if (email == null || email.isEmpty()) {
            statusMessage.setValue("Please enter an email");
            return;
        }

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;
        
        if (email.equals(currentUser.getEmail())) {
            statusMessage.setValue("You cannot add yourself as a friend");
            return;
        }

        // Find user by email
        dbRef.child("users").orderByChild("email").equalTo(email).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        String friendId = child.getKey();
                        String currentUserId = currentUser.getUid();

                        // Add to current user's friend list
                        addFriendToUser(currentUserId, friendId);
                        // Add current user to friend's friend list (Mutual)
                        addFriendToUser(friendId, currentUserId);
                        
                        statusMessage.setValue("Friend added!");
                        loadUserProfile(); // Reload
                        return;
                    }
                } else {
                    statusMessage.setValue("User not found");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                statusMessage.setValue("Error finding user: " + error.getMessage());
            }
        });
    }

    private void addFriendToUser(String userId, String friendId) {
        DatabaseReference userFriendsRef = dbRef.child("users").child(userId).child("friends");
        userFriendsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> friends = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    friends.add(child.getValue(String.class));
                }
                if (!friends.contains(friendId)) {
                    friends.add(friendId);
                    userFriendsRef.setValue(friends);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public void removeFriend(String email) {
        // Find ID by email first (inefficient but consistent with display)
        // Or better, store ID in adapter. But adapter has emails.
        // I'll query for ID again.
        
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

        dbRef.child("users").orderByChild("email").equalTo(email).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        String friendId = child.getKey();
                        String currentUserId = currentUser.getUid();

                        removeFriendFromUser(currentUserId, friendId);
                        removeFriendFromUser(friendId, currentUserId);
                        
                        statusMessage.setValue("Friend removed");
                        loadUserProfile();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void removeFriendFromUser(String userId, String friendId) {
        DatabaseReference userFriendsRef = dbRef.child("users").child(userId).child("friends");
        userFriendsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> friends = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    friends.add(child.getValue(String.class));
                }
                if (friends.contains(friendId)) {
                    friends.remove(friendId);
                    userFriendsRef.setValue(friends);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
