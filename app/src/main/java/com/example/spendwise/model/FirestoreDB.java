package com.example.spendwise.model;

import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Singleton class to manage Firestore database instance
 * Replaces Firebase Realtime Database with Firestore
 */
public class FirestoreDB {

    // Volatile ensures visibility across threads
    private static volatile FirebaseFirestore firestoreInstance;

    // Private constructor prevents instantiation
    private FirestoreDB() { }

    /**
     * Get Firestore database instance using double-checked locking for thread safety
     * @return FirebaseFirestore instance
     */
    public static FirebaseFirestore getInstance() {
        if (firestoreInstance == null) {
            synchronized (FirestoreDB.class) {
                if (firestoreInstance == null) {
                    firestoreInstance = FirebaseFirestore.getInstance();
                    // Firestore enables offline persistence by default
                }
            }
        }
        return firestoreInstance;
    }

    /**
     * Get Firestore database instance (convenience method)
     * @return FirebaseFirestore instance
     */
    public static FirebaseFirestore getDatabase() {
        return getInstance();
    }
}

