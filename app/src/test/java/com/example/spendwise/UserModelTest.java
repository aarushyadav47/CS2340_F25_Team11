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

    private final PrintStream originalOut = System.out;
    private ByteArrayOutputStream outContent;

    @Before
    public void setUpStreams() {
        outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
    }

    @After
    public void restoreStreams() {
        System.setOut(originalOut);
    }

    @Test
    public void testUserConstructorAndGetters() {
        User user = new User("123", "Alice", "alice@example.com", "secret");

        assertEquals("ID should match constructor value", "123", user.getId());
        assertEquals("Name should match constructor value", "Alice", user.getName());
        assertEquals("Email should match constructor value", "alice@example.com", user.getEmail());
        assertEquals("Password should match constructor value", "secret", user.getPassword());
    }

    @Test
    public void testSettersAndPrintUserInfo() {
        User user = new User("123", "Alice", "alice@example.com", "secret");

        user.setId("456");
        user.setName("Bob");
        user.setEmail("bob@example.com");
        user.setPassword("new-secret");

        user.printUserInfo();

        assertEquals("User: Bob, Email: bob@example.com" + System.lineSeparator(),
                outContent.toString());
        assertEquals("ID should update via setter", "456", user.getId());
        assertEquals("Name should update via setter", "Bob", user.getName());
        assertEquals("Email should update via setter", "bob@example.com", user.getEmail());
        assertEquals("Password should update via setter", "new-secret", user.getPassword());
    }
}

