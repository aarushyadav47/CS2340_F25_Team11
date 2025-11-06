import java.util.logging.Logger;

public class EmailSender {
    private static final Logger logger = Logger.getLogger(EmailSender.class.getName());
    
    private EmailSender() {
        throw new IllegalStateException("Utility class");
    }
    
    public static void sendEmail(String customerEmail, String subject, String message){
        logger.info("Email to: {0}", customerEmail);
        logger.info("Subject: {0}", subject);
        logger.info("Body: {0}", message);
    }
}
