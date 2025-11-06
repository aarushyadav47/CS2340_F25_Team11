public class OrderManager {
    // FIX 6: Extracted printing responsibility
    public void printOrder(Order order) {
    }

    // FIX 6: Extracted email responsibility
    public void sendConfirmationEmail(Order order) {
        String message = "Thank you for your order, " + order.getCustomerName() + "!\n\n" + orderToString(order);
        EmailSender.sendEmail(order.getCustomerEmail(), "Order Confirmation", message);
    }

    public String orderToString(Order order) {
        String str= "Order Details:\n";
        for (Item item : order.getItems()) {
            str += item.getName() + " - $" + item.getPrice() + "\n";
        }
        str += "Total: $" + order.calculateTotalPrice();
        return str;
    }
}