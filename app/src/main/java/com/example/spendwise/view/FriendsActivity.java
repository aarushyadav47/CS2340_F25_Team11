package com.example.spendwise.view;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.spendwise.R;
import com.example.spendwise.util.ThemeManager;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Friends Activity for managing friend system
 * Extra credit: Friend system integration
 */
public class FriendsActivity extends AppCompatActivity {
    private static final String TAG = "FriendsActivity";

    private RecyclerView friendsRecyclerView;
    private RecyclerView requestsRecyclerView;
    private FriendsAdapter friendsAdapter;
    private FriendRequestsAdapter requestsAdapter;
    private Button addFriendButton;
    
    private FirebaseAuth auth;
    private FirebaseDatabase database;
    private FirebaseStorage storage;
    private DatabaseReference friendsRef;
    private DatabaseReference requestsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeManager.loadTheme(this);
        setContentView(R.layout.activity_friends);

        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        storage = FirebaseStorage.getInstance();

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            finish();
            return;
        }

        String uid = currentUser.getUid();
        friendsRef = database.getReference("users").child(uid).child("friends");
        requestsRef = database.getReference("users").child(uid).child("friendRequests");

        initializeViews();
        setupRecyclerViews();
        loadFriends();
        loadFriendRequests();

        findViewById(R.id.friends_back_button).setOnClickListener(v -> finish());
    }

    private void initializeViews() {
        friendsRecyclerView = findViewById(R.id.friends_recycler);
        requestsRecyclerView = findViewById(R.id.friend_requests_recycler);
        addFriendButton = findViewById(R.id.add_friend_button);

        addFriendButton.setOnClickListener(v -> showAddFriendDialog());
    }

    private void setupRecyclerViews() {
        friendsAdapter = new FriendsAdapter(new ArrayList<>());
        friendsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        friendsRecyclerView.setAdapter(friendsAdapter);

        requestsAdapter = new FriendRequestsAdapter(new ArrayList<>());
        requestsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        requestsRecyclerView.setAdapter(requestsAdapter);
    }

    private void showAddFriendDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_friend, null);
        EditText emailInput = dialogView.findViewById(R.id.friend_email_input);
        Button sendButton = dialogView.findViewById(R.id.send_request_button);

        AlertDialog dialog = builder.setView(dialogView).create();

        sendButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            if (TextUtils.isEmpty(email)) {
                emailInput.setError("Email cannot be empty");
                return;
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailInput.setError("Invalid email address");
                return;
            }

            sendFriendRequest(email);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void sendFriendRequest(String friendEmail) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

        // Find user by email
        database.getReference("users").orderByChild("email").equalTo(friendEmail)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            Toast.makeText(FriendsActivity.this, 
                                    "User not found", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        DataSnapshot userSnapshot = snapshot.getChildren().iterator().next();
                        String friendUid = userSnapshot.getKey();
                        String currentEmail = currentUser.getEmail();

                        if (friendUid == null || friendUid.equals(currentUser.getUid())) {
                            Toast.makeText(FriendsActivity.this, 
                                    "Cannot add yourself", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Send request to friend
                        DatabaseReference friendRequestRef = database.getReference("users")
                                .child(friendUid)
                                .child("friendRequests")
                                .child(currentUser.getUid());
                        
                        Map<String, Object> requestData = new HashMap<>();
                        requestData.put("fromEmail", currentEmail);
                        requestData.put("fromUid", currentUser.getUid());
                        requestData.put("status", "pending");
                        requestData.put("timestamp", System.currentTimeMillis());

                        friendRequestRef.setValue(requestData)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(FriendsActivity.this, 
                                            "Friend request sent", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Failed to send friend request", e);
                                    Toast.makeText(FriendsActivity.this, 
                                            "Failed to send request", Toast.LENGTH_SHORT).show();
                                });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error finding user", error.toException());
                    }
                });
    }

    private void loadFriends() {
        friendsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Friend> friends = new ArrayList<>();
                for (DataSnapshot friendSnapshot : snapshot.getChildren()) {
                    String friendUid = friendSnapshot.getKey();
                    if (friendSnapshot.child("status").getValue(String.class) != null &&
                            friendSnapshot.child("status").getValue(String.class).equals("accepted")) {
                        loadFriendDetails(friendUid, friends);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading friends", error.toException());
            }
        });
    }

    private void loadFriendDetails(String friendUid, List<Friend> friendsList) {
        database.getReference("users").child(friendUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String name = snapshot.child("name").getValue(String.class);
                        String email = snapshot.child("email").getValue(String.class);
                        if (name != null && email != null) {
                            friendsList.add(new Friend(friendUid, name, email));
                            friendsAdapter.updateFriends(new ArrayList<>(friendsList));
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
    }

    private void loadFriendRequests() {
        requestsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<FriendRequest> requests = new ArrayList<>();
                for (DataSnapshot requestSnapshot : snapshot.getChildren()) {
                    String fromUid = requestSnapshot.getKey();
                    String status = requestSnapshot.child("status").getValue(String.class);
                    if ("pending".equals(status)) {
                        String fromEmail = requestSnapshot.child("fromEmail").getValue(String.class);
                        requests.add(new FriendRequest(fromUid, fromEmail));
                    }
                }
                requestsAdapter.updateRequests(requests);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading friend requests", error.toException());
            }
        });
    }

    private void acceptFriendRequest(String friendUid, String friendEmail) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

        String currentUid = currentUser.getUid();
        String currentEmail = currentUser.getEmail();

        // Update request status
        requestsRef.child(friendUid).child("status").setValue("accepted");

        // Add to friends list (bidirectional)
        Map<String, Object> friendData = new HashMap<>();
        friendData.put("status", "accepted");
        friendData.put("addedAt", System.currentTimeMillis());

        friendsRef.child(friendUid).setValue(friendData);
        database.getReference("users").child(friendUid).child("friends")
                .child(currentUid).setValue(friendData);

        Toast.makeText(this, "Friend added", Toast.LENGTH_SHORT).show();
    }

    private void rejectFriendRequest(String friendUid) {
        requestsRef.child(friendUid).removeValue();
        Toast.makeText(this, "Friend request rejected", Toast.LENGTH_SHORT).show();
    }

    // Data classes
    private static class Friend {
        String uid;
        String name;
        String email;

        Friend(String uid, String name, String email) {
            this.uid = uid;
            this.name = name;
            this.email = email;
        }
    }

    private static class FriendRequest {
        String uid;
        String email;

        FriendRequest(String uid, String email) {
            this.uid = uid;
            this.email = email;
        }
    }

    // Adapters
    private class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.ViewHolder> {
        private List<Friend> friends;

        FriendsAdapter(List<Friend> friends) {
            this.friends = friends;
        }

        void updateFriends(List<Friend> newFriends) {
            this.friends = newFriends;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_friend, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Friend friend = friends.get(position);
            holder.nameText.setText(friend.name);
            holder.emailText.setText(friend.email);
            
            // Load profile photo
            StorageReference photoRef = storage.getReference()
                    .child("profile_photos")
                    .child(friend.uid + ".jpg");
            photoRef.getDownloadUrl().addOnSuccessListener(uri -> {
                Glide.with(FriendsActivity.this)
                        .load(uri)
                        .circleCrop()
                        .placeholder(R.drawable.ic_person_add)
                        .into(holder.photoView);
            });
        }

        @Override
        public int getItemCount() {
            return friends.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView photoView;
            TextView nameText;
            TextView emailText;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                photoView = itemView.findViewById(R.id.friend_photo);
                nameText = itemView.findViewById(R.id.friend_name);
                emailText = itemView.findViewById(R.id.friend_email);
            }
        }
    }

    private class FriendRequestsAdapter extends RecyclerView.Adapter<FriendRequestsAdapter.ViewHolder> {
        private List<FriendRequest> requests;

        FriendRequestsAdapter(List<FriendRequest> requests) {
            this.requests = requests;
        }

        void updateRequests(List<FriendRequest> newRequests) {
            this.requests = newRequests;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_friend_request, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            FriendRequest request = requests.get(position);
            holder.emailText.setText(request.email);
            
            holder.acceptButton.setOnClickListener(v -> 
                acceptFriendRequest(request.uid, request.email));
            holder.rejectButton.setOnClickListener(v -> 
                rejectFriendRequest(request.uid));
        }

        @Override
        public int getItemCount() {
            return requests.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView emailText;
            Button acceptButton;
            Button rejectButton;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                emailText = itemView.findViewById(R.id.request_email);
                acceptButton = itemView.findViewById(R.id.accept_button);
                rejectButton = itemView.findViewById(R.id.reject_button);
            }
        }
    }
}

