import java.util.logging.Logger;
import java.text.MessageFormat;

public class OrderManager {
    private static final Logger logger = Logger.getLogger(OrderManager.class.getName());
    
    // FIX 6: Extracted printing responsibility
    public void printOrder(Order order) {
        logger.info("{0}", orderToString(order));
    }
    
    // FIX 6: Extracted email responsibility
    public void sendConfirmationEmail(Order order) {
        String message = MessageFormat.format("Thank you for your order, {0}!\n\n{1}", 
                                              order.getCustomerName(), 
                                              orderToString(order));
        EmailSender.sendEmail(order.getCustomerEmail(), "Order Confirmation", message);
    }
    
    public String orderToString(Order order) {
        StringBuilder str = new StringBuilder("Order Details:\n");
        for (Item item : order.getItems()) {
            str.append(item.getName()).append(" - $").append(item.getPrice()).append("\n");
        }
        str.append("Total: $").append(order.calculateTotalPrice());
        return str.toString();
    }
}
