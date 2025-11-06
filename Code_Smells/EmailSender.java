package codesmells;

import java.util.logging.Logger;

public class EmailSender {
    private static final Logger logger = Logger.getLogger(EmailSender.class.getName());
    
    private EmailSender() {
        // Private constructor to prevent instantiation
    }
    
    public static void sendEmail(String customerEmail, String subject, String message){
        logger.info("Email to: " + customerEmail);
        logger.info("Subject: " + subject);
        logger.info("Body: " + message);
    }
}