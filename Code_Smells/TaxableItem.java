package codesmells;

public class TaxableItem extends Item {
    private double taxRate = 7;

    public TaxableItem(String name, double price, int quantity, Discount discount) {
        super(name, price, quantity, discount);
    }

    @Override
    public double calculateFinalPrice() {
        double basePrice = super.calculateFinalPrice();
        double tax = basePrice * (taxRate / 100.0);
        return basePrice + tax;
    }

    public double getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(double rate) {
        if (rate >= 0){
            taxRate = rate;
        }
    }
}
