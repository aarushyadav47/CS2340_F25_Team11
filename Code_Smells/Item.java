package codesmells;

class Item {
    private String name;
    private double price;
    private int quantity;
    private Discount discount;

    public Item(String name, double price, int quantity, Discount discount) {
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.discount = discount;
    }

    public double calculateFinalPrice() {
        double discountedPrice = discount.applyDiscount(price);
        return discountedPrice * quantity;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }

    public int getQuantity() {
        return quantity;
    }
}
