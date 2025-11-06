package codesmells;

import java.util.List;

public class Order {
    private static final double GIFT_CARD_DISCOUNT = 10.0;
    private static final double BULK_DISCOUNT_THRESHOLD = 100.0;
    private static final double BULK_DISCOUNT_RATE = 0.9;

    private List<Item> items;
    private String customerName;
    private String customerEmail;

    public Order(List<Item> items, String customerName, String customerEmail) {
        this.items = items;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
    }

    // FIX 2: Simplified, focused method (no longer bloated)
    // FIX 1: No switch statement (polymorphism handles discounts)
    // FIX 4: No feature envy (Items calculate their own prices)
    public double calculateTotalPrice() {
        double itemsTotal = calculateItemsTotal();
        double giftCardDiscount = calculateGiftCardDiscount();
        double subtotal = itemsTotal - giftCardDiscount;
        return applyBulkDiscount(subtotal);
    }

    private double calculateItemsTotal() {
        double total = 0.0;
        for (Item item : items) {
            total += item.calculateFinalPrice();
        }
        return total;
    }

    private double calculateGiftCardDiscount() {
        return hasGiftCard() ? GIFT_CARD_DISCOUNT : 0.0;
    }

    private double applyBulkDiscount(double total) {
        return total > BULK_DISCOUNT_THRESHOLD ? total * BULK_DISCOUNT_RATE : total;
    }

    private boolean hasGiftCard() {
        for (Item item : items) {
            if (item instanceof GiftCardItem) {
                return true;
            }
        }
        return false;
    }

    public void addItem(Item item) {
        items.add(item);
    }

    public void removeItem(Item item) {
        items.remove(item);
    }

    public List<Item> getItems() {
        return items;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void addItemsFromAnotherOrder(Order otherOrder) {
        items.addAll(otherOrder.getItems());
   }
}