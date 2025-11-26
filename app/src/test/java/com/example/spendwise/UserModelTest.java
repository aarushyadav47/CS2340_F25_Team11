package com.example.spendwise;

import static org.junit.Assert.assertEquals;

import com.example.spendwise.model.User;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * Unit tests for {@link User}.
 */
public class UserModelTest {

    @Test
    public void testUserConstructorAndGetters() {
        User user = new User("1", "John Doe", "john@example.com", "secret");

        assertEquals("1", user.getId());
        assertEquals("John Doe", user.getName());
        assertEquals("john@example.com", user.getEmail());
        assertEquals("secret", user.getPassword());
    }

    @Test
    public void testSetters() {
        User user = new User("1", "John", "john@mail.com", "pass");

        user.setId("2");
        user.setName("Jane");
        user.setEmail("jane@mail.com");
        user.setPassword("newpass");

        assertEquals("2", user.getId());
        assertEquals("Jane", user.getName());
        assertEquals("jane@mail.com", user.getEmail());
        assertEquals("newpass", user.getPassword());
    }

    @Test
    public void testPrintUserInfo() {
        User user = new User("1", "Alice", "alice@mail.com", "pwd");

        // Capture console output
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));

        user.printUserInfo();

        String output = out.toString().trim();
        assertEquals("User: Alice, Email: alice@mail.com", output);
    }
}
