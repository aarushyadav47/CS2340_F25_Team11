package com.example.spendwise.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

/**
 * Base repository class for Firestore operations
 * Provides common functionality for all repositories
 */
public abstract class FirestoreRepository<T> {

    protected FirebaseFirestore db;
    protected FirebaseAuth auth;
    protected Map<String, ListenerRegistration> activeListeners;

    public FirestoreRepository() {
        this.db = com.example.spendwise.model.FirestoreDB.getInstance();
        this.auth = FirebaseAuth.getInstance();
        this.activeListeners = new HashMap<>();
    }

    /**
     * Get current user ID
     * @return User ID or null if not logged in
     */
    protected String getCurrentUserId() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    /**
     * Get current user email
     * @return User email or null if not logged in
     */
    protected String getCurrentUserEmail() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getEmail() : null;
    }

    /**
     * Get collection reference for a user's data
     * @param collectionName Name of the collection
     * @return CollectionReference
     */
    protected CollectionReference getUserCollection(String collectionName) {
        String userId = getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("User must be logged in to access Firestore");
        }
        return db.collection("users").document(userId).collection(collectionName);
    }

    /**
     * Get document reference for a user's document
     * @param collectionName Name of the collection
     * @param documentId Document ID
     * @return DocumentReference
     */
    protected DocumentReference getUserDocument(String collectionName, String documentId) {
        String userId = getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("User must be logged in to access Firestore");
        }
        return db.collection("users").document(userId).collection(collectionName).document(documentId);
    }

    /**
     * Remove a listener by key
     * @param key Key for the listener
     */
    protected void removeListener(String key) {
        ListenerRegistration registration = activeListeners.remove(key);
        if (registration != null) {
            registration.remove();
        }
    }

    /**
     * Remove all active listeners
     */
    public void removeAllListeners() {
        for (ListenerRegistration registration : activeListeners.values()) {
            registration.remove();
        }
        activeListeners.clear();
    }

    /**
     * Convert Firestore document to model object
     * @param documentId Document ID
     * @param data Document data
     * @return Model object
     */
    protected abstract T documentToModel(String documentId, Map<String, Object> data);

    /**
     * Convert model object to Firestore document
     * @param model Model object
     * @return Map of document data
     */
    protected abstract Map<String, Object> modelToDocument(T model);
}

