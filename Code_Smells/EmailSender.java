import java.util.logging.Logger;
import java.text.MessageFormat;

public class EmailSender {
    private static final Logger logger = Logger.getLogger(EmailSender.class.getName());
    
    private EmailSender() {
        throw new IllegalStateException("Utility class");
    }
    
    public static void sendEmail(String customerEmail, String subject, String message){
        logger.info(MessageFormat.format("Email to: {0}", customerEmail));
        logger.info(MessageFormat.format("Subject: {0}", subject));
        logger.info(MessageFormat.format("Body: {0}", message));
    }
}
