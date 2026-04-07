package com.kyant.backdrop.catalog.payments

import com.kyant.backdrop.catalog.network.models.PremiumCheckoutPrefill
import com.kyant.backdrop.catalog.network.models.PremiumCheckoutResponse
import com.kyant.backdrop.catalog.network.models.PremiumVerifyRequest
import com.razorpay.Checkout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PremiumCheckoutPayloadsTest {
    @Test
    fun `preparePremiumCheckoutSession trims and returns secure launch values`() {
        val prepared = preparePremiumCheckoutSession(
            PremiumCheckoutResponse(
                keyId = "  rzp_test_123  ",
                orderId = "  order_123  ",
                amountMinor = 19900,
                currency = "INR",
                title = "Vormex Premium",
                description = "Unlock premium",
                prefill = PremiumCheckoutPrefill(name = "Alex", email = "alex@example.com")
            )
        )

        assertEquals(
            PreparedPremiumCheckoutSession(
                keyId = "rzp_test_123",
                orderId = "order_123"
            ),
            prepared
        )
    }

    @Test
    fun `preparePremiumCheckoutSession rejects missing secure Razorpay setup`() {
        val prepared = preparePremiumCheckoutSession(
            PremiumCheckoutResponse(
                keyId = " ",
                orderId = "order_123"
            )
        )

        assertEquals(null, prepared)
    }

    @Test
    fun `preparePremiumVerificationRequest uses the trusted order id from the server session`() {
        val prepared = preparePremiumVerificationRequest(
            trustedOrderId = "order_server_123",
            callbackOrderId = "order_server_123",
            callbackPaymentId = "pay_123",
            fallbackPaymentId = null,
            callbackSignature = "sig_123"
        )

        assertEquals(
            PremiumVerificationPreparation.Ready(
                PremiumVerifyRequest(
                    razorpayOrderId = "order_server_123",
                    razorpayPaymentId = "pay_123",
                    razorpaySignature = "sig_123"
                )
            ),
            prepared
        )
    }

    @Test
    fun `preparePremiumVerificationRequest rejects a mismatched callback order id`() {
        val prepared = preparePremiumVerificationRequest(
            trustedOrderId = "order_server_123",
            callbackOrderId = "order_other_123",
            callbackPaymentId = "pay_123",
            fallbackPaymentId = null,
            callbackSignature = "sig_123"
        )

        assertTrue(prepared is PremiumVerificationPreparation.Invalid)
        assertEquals(
            "Payment details did not match the secure checkout session.",
            (prepared as PremiumVerificationPreparation.Invalid).message
        )
    }

    @Test
    fun `preparePremiumVerificationRequest refuses to trust callback order id without a server order`() {
        val prepared = preparePremiumVerificationRequest(
            trustedOrderId = null,
            callbackOrderId = "order_callback_123",
            callbackPaymentId = "pay_123",
            fallbackPaymentId = null,
            callbackSignature = "sig_123"
        )

        assertTrue(prepared is PremiumVerificationPreparation.Invalid)
        assertEquals(
            "Payment finished, but the secure checkout order could not be confirmed.",
            (prepared as PremiumVerificationPreparation.Invalid).message
        )
    }

    @Test
    fun `resolvePremiumCheckoutErrorMessage stays quiet when the user cancels checkout`() {
        assertEquals(
            null,
            resolvePremiumCheckoutErrorMessage(
                code = Checkout.PAYMENT_CANCELED,
                response = "Payment cancelled by user."
            )
        )
    }

    @Test
    fun `resolvePremiumCheckoutErrorMessage converts technical errors into friendly copy`() {
        assertEquals(
            "Network issue while opening Razorpay. Please try again.",
            resolvePremiumCheckoutErrorMessage(
                code = Checkout.NETWORK_ERROR,
                response = "{\"error\":\"socket timeout\"}"
            )
        )
        assertEquals(
            "Payment could not be completed. Please try again.",
            resolvePremiumCheckoutErrorMessage(
                code = 999,
                response = "{\"error\":\"unexpected_state\"}"
            )
        )
    }
}
