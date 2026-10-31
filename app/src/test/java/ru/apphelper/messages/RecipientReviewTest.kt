package ru.apphelper.messages

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipientReviewTest {
    @Test fun notificationNameIsNeverVerifiedByDefault() {
        val review = RecipientReview("n1", "MAX", "Сергей")
        assertFalse(review.confirmedForDisplay)
    }

    @Test fun displayConfirmationDoesNotProvideDeliveryIdentity() {
        val review = RecipientReview("n1", "MAX", "Сергей").confirmDisplay()
        assertTrue(review.confirmedForDisplay)
        // No recipient account ID, channel address or send method exists on this model.
    }

    @Test fun missingSenderCannotBeConfirmed() {
        val review = RecipientReview("n1", "MAX", "").confirmDisplay()
        assertFalse(review.confirmedForDisplay)
    }

    @Test fun confirmationCanBeRevoked() {
        assertFalse(RecipientReview("n1", "MAX", "Сергей").confirmDisplay().invalidate().confirmedForDisplay)
    }
}
