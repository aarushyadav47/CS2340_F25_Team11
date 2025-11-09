package com.example.spendwise;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import com.example.spendwise.model.SavingCircleInvitation;

import org.junit.Test;

/**
 * Unit tests for {@link SavingCircleInvitation}.
 */
public class SavingCircleInvitationTest {

    @Test
    public void creationShouldPopulateDefaultFields() {
        long beforeCreation = System.currentTimeMillis();
        SavingCircleInvitation invitation = new SavingCircleInvitation(
                "circle-123",
                "Rainy Day Fund",
                "Save $500",
                "alice@example.com",
                "bob@example.com",
                500.0,
                "Weekly"
        );
        long afterCreation = System.currentTimeMillis();

        assertNotNull("Invitation id should be generated", invitation.getInvitationId());
        assertEquals("Circle id should be preserved", "circle-123", invitation.getCircleId());
        assertEquals("Circle name should be preserved", "Rainy Day Fund", invitation.getCircleName());
        assertEquals("Challenge title should be preserved", "Save $500", invitation.getChallengeTitle());
        assertEquals("Inviter email should be preserved", "alice@example.com", invitation.getInviterEmail());
        assertEquals("Invitee email should be preserved", "bob@example.com", invitation.getInviteeEmail());
        assertEquals("Status should default to pending", "pending", invitation.getStatus());
        assertTrue("isPending should be true for new invitations", invitation.isPending());
        assertFalse("isAccepted should be false for new invitations", invitation.isAccepted());
        assertFalse("isDeclined should be false for new invitations", invitation.isDeclined());
        assertEquals("Response timestamp should default to zero", 0L, invitation.getRespondedAt());
        assertEquals("Goal amount should be preserved", 500.0, invitation.getGoalAmount(), 0.0001);
        assertEquals("Frequency should be preserved", "Weekly", invitation.getFrequency());
        assertTrue("Sent timestamp should be within creation window",
                invitation.getSentAt() >= beforeCreation && invitation.getSentAt() <= afterCreation);
    }

    @Test
    public void statusHelpersShouldReflectUpdates() {
        SavingCircleInvitation invitation = new SavingCircleInvitation(
                "circle-456",
                "Vacation Fund",
                "Save $1000",
                "carol@example.com",
                "dave@example.com",
                1000.0,
                "Monthly"
        );

        long acceptedAt = 1_700_000_000_000L;
        invitation.setStatus("accepted");
        invitation.setRespondedAt(acceptedAt);

        assertTrue("Invitation should report accepted", invitation.isAccepted());
        assertFalse("Invitation should not report pending", invitation.isPending());
        assertFalse("Invitation should not report declined", invitation.isDeclined());
        assertEquals("Responded timestamp should update", acceptedAt, invitation.getRespondedAt());

        invitation.setStatus("declined");

        assertTrue("Invitation should report declined after update", invitation.isDeclined());
        assertFalse("Invitation should not report accepted after decline", invitation.isAccepted());

        invitation.setStatus("pending");
        invitation.setRespondedAt(0L);
        invitation.setCircleName("Updated Circle");
        invitation.setGoalAmount(250.0);
        invitation.setFrequency("Biweekly");
        invitation.setCircleId("circle-updated");

        assertTrue("Invitation should return to pending", invitation.isPending());
        assertEquals("Responded timestamp should reset", 0L, invitation.getRespondedAt());
        assertEquals("Circle name should update", "Updated Circle", invitation.getCircleName());
        assertEquals("Circle id should update", "circle-updated", invitation.getCircleId());
        assertEquals("Goal amount should update", 250.0, invitation.getGoalAmount(), 0.0001);
        assertEquals("Frequency should update", "Biweekly", invitation.getFrequency());
    }
}

