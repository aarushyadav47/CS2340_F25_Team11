import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmailSender {
    private static final Logger logger = LoggerFactory.getLogger(EmailSender.class);
    
    private EmailSender() {
        throw new IllegalStateException("Utility class");
    }
    
    public static void sendEmail(String customerEmail, String subject, String message){
        logger.info("Email to: {}", customerEmail);
        logger.info("Subject: {}", subject);
        logger.info("Body: {}", message);
    }
}