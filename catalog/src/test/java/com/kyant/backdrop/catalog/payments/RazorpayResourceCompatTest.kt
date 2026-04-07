package com.kyant.backdrop.catalog.payments

import org.junit.Assert.assertEquals
import org.junit.Test

class RazorpayResourceCompatTest {
    @Test
    fun `compat resource class mirrors the patched Razorpay namespaces`() {
        assertEquals(com.razorpay.corelib.R.bool.isTablet, com.razorpay.R.bool.isTablet)

        assertEquals(com.razorpay.standardcore.R.drawable.ic_alert, com.razorpay.R.drawable.ic_alert)
        assertEquals(com.razorpay.standardcore.R.drawable.ic_tick_mark, com.razorpay.R.drawable.ic_tick_mark)
        assertEquals(com.razorpay.corelib.R.drawable.rzp_logo, com.razorpay.R.drawable.rzp_logo)

        assertEquals(com.razorpay.standardcore.R.id.check_list, com.razorpay.R.id.check_list)
        assertEquals(com.razorpay.standardcore.R.id.iv_check_mark, com.razorpay.R.id.iv_check_mark)
        assertEquals(com.razorpay.corelib.R.id.ll_loader, com.razorpay.R.id.ll_loader)
        assertEquals(com.razorpay.corelib.R.id.progressBar, com.razorpay.R.id.progressBar)
        assertEquals(com.razorpay.standardcore.R.id.tv_sub_item, com.razorpay.R.id.tv_sub_item)
        assertEquals(com.razorpay.standardcore.R.id.tv_title, com.razorpay.R.id.tv_title)

        assertEquals(com.razorpay.corelib.R.layout.rzp_loader, com.razorpay.R.layout.rzp_loader)
        assertEquals(
            com.razorpay.standardcore.R.layout.sdk_integration_status,
            com.razorpay.R.layout.sdk_integration_status
        )
        assertEquals(com.razorpay.standardcore.R.layout.single_item, com.razorpay.R.layout.single_item)

        assertEquals(com.razorpay.corelib.R.raw.otpelf, com.razorpay.R.raw.otpelf)
        assertEquals(
            com.razorpay.standardcore.R.raw.rzp_config_checkout,
            com.razorpay.R.raw.rzp_config_checkout
        )
    }
}
