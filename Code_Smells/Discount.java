package codesmells;

//Fix 1: No more switch statement, instead interface implementation
//Fix 5: Discount is centralized to one interface, to ensure changes do not ripple
public interface Discount {
    double applyDiscount(double price);
}