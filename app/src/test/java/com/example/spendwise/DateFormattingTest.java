//Showmick's tests
package com.example.spendwise;

import static org.junit.Assert.*;
import org.junit.Test;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateFormattingTest {

    @Test
    public void testDateFormat() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
        Date testDate = new Date();

        String formattedDate = dateFormat.format(testDate);

        assertTrue("Date should match pattern MM/dd/yyyy",
                formattedDate.matches("\\d{2}/\\d{2}/\\d{4}"));
    }

    @Test
    public void testDateParsing() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
        String dateString = "10/21/2025";

        try {
            Date parsedDate = dateFormat.parse(dateString);
            assertNotNull("Parsed date should not be null", parsedDate);
        } catch (Exception e) {
            fail("Date parsing should not throw exception");
        }
    }
}
