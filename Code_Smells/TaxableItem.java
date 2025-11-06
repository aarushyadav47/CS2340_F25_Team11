public class TaxableItem extends Item {
    private double taxRate = 7;

    public TaxableItem(String name, double price, int quantity, Discount discount) {
        super(name, price, quantity, discount);
    }

    @Override
    public double calculateFinalPrice() {
        // Apply discount first
        double discountedPrice = super.calculateFinalPrice();
        
        // Calculate tax on ORIGINAL price
        double tax = getPrice() * (taxRate / 100.0);
        
        // Return: (discounted price + tax) × quantity
        return (discountedPrice + tax) * getQuantity();
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
