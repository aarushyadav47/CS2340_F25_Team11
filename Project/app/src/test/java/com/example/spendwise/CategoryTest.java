//Rayeed's tests

package com.example.spendwise;

import static org.junit.Assert.*;
import com.example.spendwise.model.Category;
import org.junit.Test;
import java.util.HashSet;
import java.util.Set;

public class CategoryTest {

    /**
     * Test 2: Display names are unique across all categories
     */
    @Test
    public void testUniqueDisplayNames() {
        Set<String> seenNames = new HashSet<>();
        for (Category category : Category.values()) {
            String displayName = category.getDisplayName();
            assertFalse("Duplicate display name found: " + displayName,
                    seenNames.contains(displayName));
            seenNames.add(displayName);
        }
    }

    @Test
    public void testCategoryDisplayName() {
        String foodDisplay = Category.FOOD.getDisplayName();
        String transportDisplay = Category.TRANSPORT.getDisplayName();

        assertNotNull("Food display name should not be null", foodDisplay);
        assertNotNull("Transport display name should not be null", transportDisplay);
        assertFalse("Display name should not be empty", foodDisplay.isEmpty());
    }
}