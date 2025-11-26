package com.example.spendwise.util;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.text.MessageFormat;

public class EmailSender {
    private static final Logger logger = Logger.getLogger(EmailSender.class.getName());

    // Private constructor to prevent instantiation
    private EmailSender() {
        throw new IllegalStateException("Utility class");
    }

    // Send email method (logs to console for now)
    public static void sendEmail(String customerEmail, String subject, String message) {
        if (logger.isLoggable(Level.INFO)) {
            logger.info(MessageFormat.format("Email to: {0}", customerEmail));
            logger.info(MessageFormat.format("Subject: {0}", subject));
            logger.info(MessageFormat.format("Body: {0}", message));
        }
    }
}

