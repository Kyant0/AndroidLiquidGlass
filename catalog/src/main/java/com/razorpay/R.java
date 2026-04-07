package com.razorpay;

/**
 * Restores the resource class package that Razorpay's bytecode expects.
 * The locally patched AARs generate resources under com.razorpay.standardcore/corelib.
 */
public final class R {
    private R() {}

    public static final class bool {
        public static final int isTablet = com.razorpay.corelib.R.bool.isTablet;

        private bool() {}
    }

    public static final class drawable {
        public static final int ic_alert = com.razorpay.standardcore.R.drawable.ic_alert;
        public static final int ic_tick_mark = com.razorpay.standardcore.R.drawable.ic_tick_mark;
        public static final int rzp_logo = com.razorpay.corelib.R.drawable.rzp_logo;

        private drawable() {}
    }

    public static final class id {
        public static final int check_list = com.razorpay.standardcore.R.id.check_list;
        public static final int iv_check_mark = com.razorpay.standardcore.R.id.iv_check_mark;
        public static final int ll_loader = com.razorpay.corelib.R.id.ll_loader;
        public static final int progressBar = com.razorpay.corelib.R.id.progressBar;
        public static final int tv_sub_item = com.razorpay.standardcore.R.id.tv_sub_item;
        public static final int tv_title = com.razorpay.standardcore.R.id.tv_title;

        private id() {}
    }

    public static final class layout {
        public static final int rzp_loader = com.razorpay.corelib.R.layout.rzp_loader;
        public static final int sdk_integration_status = com.razorpay.standardcore.R.layout.sdk_integration_status;
        public static final int single_item = com.razorpay.standardcore.R.layout.single_item;

        private layout() {}
    }

    public static final class raw {
        public static final int otpelf = com.razorpay.corelib.R.raw.otpelf;
        public static final int rzp_config_checkout = com.razorpay.standardcore.R.raw.rzp_config_checkout;

        private raw() {}
    }
}
