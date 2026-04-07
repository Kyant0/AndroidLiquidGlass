package com.kyant.backdrop.catalog.payments

import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.kyant.backdrop.catalog.R
import com.kyant.backdrop.catalog.network.ApiClient
import com.kyant.backdrop.catalog.network.models.PremiumCheckoutResponse
import com.razorpay.Checkout
import com.razorpay.PaymentData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

object PremiumCheckoutManager {
    private const val CHECKOUT_STATE_PREFS = "premium_checkout_state"
    private const val PENDING_ORDER_ID_KEY = "pending_order_id"

    private var pendingOrderId: String? = null
    private val _refreshSignal = MutableStateFlow(0L)
    val refreshSignal = _refreshSignal.asStateFlow()
    private val _celebrationSignal = MutableStateFlow(0L)
    val celebrationSignal = _celebrationSignal.asStateFlow()

    fun preload(context: Context) {
        Checkout.preload(context.applicationContext)
    }

    fun startCheckout(
        activity: ComponentActivity,
        checkoutSession: PremiumCheckoutResponse
    ) {
        val preparedCheckout = preparePremiumCheckoutSession(checkoutSession)

        if (preparedCheckout == null) {
            Toast.makeText(
                activity,
                "Premium checkout is missing its Razorpay setup.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        rememberPendingOrderId(activity, preparedCheckout.orderId)

        try {
            val checkout = Checkout().apply {
                setKeyID(preparedCheckout.keyId)
                setImage(R.mipmap.ic_launcher)
                setFullScreenDisable(true)
            }
            val options = JSONObject().apply {
                put("name", checkoutSession.title)
                put("description", checkoutSession.description)
                put("order_id", preparedCheckout.orderId)
                put("currency", checkoutSession.currency)
                put("amount", checkoutSession.amountMinor)
                put("theme.color", "#0F6BFF")
                put("prefill.name", checkoutSession.prefill.name.orEmpty())
                put("prefill.email", checkoutSession.prefill.email.orEmpty())
                put("send_sms_hash", true)
                put(
                    "retry",
                    JSONObject().apply {
                        put("enabled", true)
                        put("max_count", 4)
                    }
                )
            }

            checkout.open(activity, options)
        } catch (error: Exception) {
            clearPendingOrderId(activity)
            Toast.makeText(
                activity,
                error.message ?: "Unable to start premium checkout.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun handlePaymentSuccess(
        activity: ComponentActivity,
        razorpayPaymentId: String?,
        paymentData: PaymentData?
    ) {
        val verificationPreparation = preparePremiumVerificationRequest(
            trustedOrderId = getPendingOrderId(activity),
            callbackOrderId = paymentData?.orderId,
            callbackPaymentId = paymentData?.paymentId,
            fallbackPaymentId = razorpayPaymentId,
            callbackSignature = paymentData?.signature
        )

        if (verificationPreparation is PremiumVerificationPreparation.Invalid) {
            clearPendingOrderId(activity)
            Toast.makeText(
                activity,
                verificationPreparation.message,
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val verificationRequest =
            (verificationPreparation as PremiumVerificationPreparation.Ready).request

        activity.lifecycleScope.launch {
            val verificationResult = ApiClient.verifyPremiumCheckout(
                context = activity,
                request = verificationRequest
            )

            clearPendingOrderId(activity)

            verificationResult
                .onSuccess { response ->
                    Toast.makeText(
                        activity,
                        response.message.ifBlank { "Premium unlocked successfully." },
                        Toast.LENGTH_SHORT
                    ).show()
                    notifyPremiumStateChanged(triggerCelebration = true)
                }
                .onFailure { error ->
                    Toast.makeText(
                        activity,
                        error.message ?: "Payment verification failed.",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }

    fun notifyPremiumStateChanged(triggerCelebration: Boolean = false) {
        val now = System.currentTimeMillis()
        _refreshSignal.value = now
        if (triggerCelebration) {
            _celebrationSignal.value = now
        }
    }

    fun handlePaymentError(
        activity: ComponentActivity,
        code: Int,
        response: String?
    ) {
        clearPendingOrderId(activity)

        val message = resolvePremiumCheckoutErrorMessage(
            code = code,
            response = response
        ) ?: return

        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
    }

    private fun rememberPendingOrderId(context: Context, orderId: String) {
        pendingOrderId = orderId
        context.getSharedPreferences(CHECKOUT_STATE_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(PENDING_ORDER_ID_KEY, orderId)
            .apply()
    }

    private fun getPendingOrderId(context: Context): String? {
        val inMemoryOrderId = pendingOrderId?.trim().takeUnless { it.isNullOrBlank() }
        if (inMemoryOrderId != null) {
            return inMemoryOrderId
        }

        return context.getSharedPreferences(CHECKOUT_STATE_PREFS, Context.MODE_PRIVATE)
            .getString(PENDING_ORDER_ID_KEY, null)
            ?.trim()
            ?.takeUnless { it.isNullOrBlank() }
    }

    private fun clearPendingOrderId(context: Context) {
        pendingOrderId = null
        context.getSharedPreferences(CHECKOUT_STATE_PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(PENDING_ORDER_ID_KEY)
            .apply()
    }
}

fun Context.findComponentActivity(): ComponentActivity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is ComponentActivity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return null
}
