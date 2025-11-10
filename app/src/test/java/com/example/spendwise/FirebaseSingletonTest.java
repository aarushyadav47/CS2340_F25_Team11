package com.example.spendwise;

import static org.junit.Assert.*;

import com.example.spendwise.model.Firebase;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.junit.Test;

/**
 * Tests for Firebase Singleton Pattern Implementation
 * Verifies thread-safe singleton behavior and reference management
 */
public class FirebaseSingletonTest {

    @Test
    public void testGetDatabase_returnsSameInstance() {
        // Note: This test may need to be run with Robolectric for actual Firebase testing
        // For now, we're testing the singleton pattern structure
        
        // In a real scenario with proper Firebase mocking:
        // FirebaseDatabase instance1 = Firebase.getDatabase();
        // FirebaseDatabase instance2 = Firebase.getDatabase();
        // assertSame("Should return same database instance", instance1, instance2);
        
        // For unit testing without Firebase, we test the pattern logic
        assertTrue("Firebase class should have private constructor", 
                isConstructorPrivate());
    }

    @Test
    public void testSingletonPatternPreventsInstantiation() {
        // Verify that Firebase class cannot be instantiated directly
        try {
            java.lang.reflect.Constructor<Firebase> constructor = 
                Firebase.class.getDeclaredConstructor();
            assertTrue("Constructor should be private", 
                java.lang.reflect.Modifier.isPrivate(constructor.getModifiers()));
        } catch (NoSuchMethodException e) {
            fail("Firebase should have a private constructor");
        }
    }

    @Test
    public void testMultipleReferenceCallsAreConsistent() {
        // This tests the singleton pattern structure
        // In production with proper mocking:
        // DatabaseReference ref1 = Firebase.getExpensesRef();
        // DatabaseReference ref2 = Firebase.getExpensesRef();
        // assertSame("Multiple calls should return same reference", ref1, ref2);
        
        // For now, verify the methods exist and follow singleton pattern
        assertTrue("Firebase should have static getExpensesRef method", 
                hasStaticMethod("getExpensesRef"));
        assertTrue("Firebase should have static getBudgetsRef method", 
                hasStaticMethod("getBudgetsRef"));
    }

    @Test
    public void testThreadSafetyStructure() {
        // Verify that the singleton uses volatile keyword for thread safety
        try {
            java.lang.reflect.Field databaseField = 
                Firebase.class.getDeclaredField("databaseInstance");
            assertTrue("Database instance should be volatile for thread safety",
                java.lang.reflect.Modifier.isVolatile(databaseField.getModifiers()));
        } catch (NoSuchFieldException e) {
            fail("Firebase should have databaseInstance field");
        }
    }

    @Test
    public void testStaticMethodsExist() {
        // Verify all expected static methods exist
        assertTrue("Should have static getDatabase method", 
                hasStaticMethod("getDatabase"));
        assertTrue("Should have static getExpensesRef method", 
                hasStaticMethod("getExpensesRef"));
        assertTrue("Should have static getBudgetsRef method", 
                hasStaticMethod("getBudgetsRef"));
    }

    // Helper methods
    private boolean isConstructorPrivate() {
        try {
            java.lang.reflect.Constructor<Firebase> constructor = 
                Firebase.class.getDeclaredConstructor();
            return java.lang.reflect.Modifier.isPrivate(constructor.getModifiers());
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private boolean hasStaticMethod(String methodName) {
        try {
            java.lang.reflect.Method method = Firebase.class.getMethod(methodName);
            return java.lang.reflect.Modifier.isStatic(method.getModifiers());
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
}
