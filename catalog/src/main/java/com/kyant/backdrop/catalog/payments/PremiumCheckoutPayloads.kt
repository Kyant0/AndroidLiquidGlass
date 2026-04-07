package com.kyant.backdrop.catalog.payments

import com.kyant.backdrop.catalog.network.models.PremiumCheckoutResponse
import com.kyant.backdrop.catalog.network.models.PremiumVerifyRequest
import com.razorpay.Checkout

internal data class PreparedPremiumCheckoutSession(
    val keyId: String,
    val orderId: String
)

internal sealed interface PremiumVerificationPreparation {
    data class Ready(val request: PremiumVerifyRequest) : PremiumVerificationPreparation
    data class Invalid(val message: String) : PremiumVerificationPreparation
}

internal fun preparePremiumCheckoutSession(
    checkoutSession: PremiumCheckoutResponse
): PreparedPremiumCheckoutSession? {
    val keyId = checkoutSession.keyId?.trim().orEmpty()
    val orderId = checkoutSession.orderId.trim()

    if (keyId.isBlank() || orderId.isBlank()) {
        return null
    }

    return PreparedPremiumCheckoutSession(
        keyId = keyId,
        orderId = orderId
    )
}

internal fun preparePremiumVerificationRequest(
    trustedOrderId: String?,
    callbackOrderId: String?,
    callbackPaymentId: String?,
    fallbackPaymentId: String?,
    callbackSignature: String?
): PremiumVerificationPreparation {
    val orderId = trustedOrderId?.trim().orEmpty()
    if (orderId.isBlank()) {
        return PremiumVerificationPreparation.Invalid(
            "Payment finished, but the secure checkout order could not be confirmed."
        )
    }

    val checkoutOrderId = callbackOrderId?.trim()
    if (!checkoutOrderId.isNullOrBlank() && checkoutOrderId != orderId) {
        return PremiumVerificationPreparation.Invalid(
            "Payment details did not match the secure checkout session."
        )
    }

    val paymentId = callbackPaymentId?.trim().takeUnless { it.isNullOrBlank() }
        ?: fallbackPaymentId?.trim().takeUnless { it.isNullOrBlank() }
        ?: return PremiumVerificationPreparation.Invalid(
            "Payment finished, but the payment id was missing."
        )

    val signature = callbackSignature?.trim()
        ?: return PremiumVerificationPreparation.Invalid(
            "Payment finished, but the verification signature was missing."
        )

    if (signature.isBlank()) {
        return PremiumVerificationPreparation.Invalid(
            "Payment finished, but the verification signature was missing."
        )
    }

    return PremiumVerificationPreparation.Ready(
        PremiumVerifyRequest(
            razorpayOrderId = orderId,
            razorpayPaymentId = paymentId,
            razorpaySignature = signature
        )
    )
}

internal fun resolvePremiumCheckoutErrorMessage(
    code: Int,
    response: String?
): String? {
    val normalizedResponse = response?.trim().orEmpty()
    if (
        code == Checkout.PAYMENT_CANCELED ||
        normalizedResponse.contains("cancel", ignoreCase = true) ||
        normalizedResponse.contains("dismiss", ignoreCase = true) ||
        normalizedResponse.contains("backpress", ignoreCase = true) ||
        normalizedResponse.contains("back press", ignoreCase = true)
    ) {
        return null
    }

    return when {
        code == Checkout.NETWORK_ERROR ||
            normalizedResponse.contains("network", ignoreCase = true) -> {
            "Network issue while opening Razorpay. Please try again."
        }

        code == Checkout.INVALID_OPTIONS -> {
            "Premium checkout is temporarily unavailable. Please try again."
        }

        code == Checkout.TLS_ERROR -> {
            "Secure payment setup failed on this device. Please try again."
        }

        code == Checkout.WEBVIEW_CREATION_FAILED -> {
            "Premium checkout could not open on this device. Please try again."
        }

        else -> "Payment could not be completed. Please try again."
    }
}
