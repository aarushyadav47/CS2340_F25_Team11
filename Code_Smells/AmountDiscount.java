package codesmells;

public class AmountDiscount implements Discount {
    private double amount;

    public AmountDiscount(double amount) {
        this.amount = amount;
    }

    @Override
    public double applyDiscount(double price) {
        return price - amount;
    }
}