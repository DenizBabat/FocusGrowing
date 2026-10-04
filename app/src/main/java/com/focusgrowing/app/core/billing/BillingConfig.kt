package com.focusgrowing.app.core.billing

/**
 * Must match what you create in Play Console → Monetize with Play → Products → Subscriptions.
 * See docs/billing/BILLING_SETUP.md.
 */
object BillingConfig {
    /** Subscription product id. */
    const val PREMIUM_SUBSCRIPTION_ID = "premium"

    /** Base plan ids inside that subscription. */
    const val BASE_PLAN_MONTHLY = "monthly"
    const val BASE_PLAN_YEARLY = "yearly"

    /**
     * Play Console → Monetize with Play → Monetization setup → Licensing → "Base64-encoded RSA public key".
     * When set, every purchase signature is checked on the device. Leave empty only while testing.
     */
    const val PLAY_LICENSE_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAu5p1Mbvnbvv7oXi7HHcovzjb3D/O6vxsgCHr0qporUlEbmbajT9tBCLt7eRjVPqWic0fWKZ8dIma4ai7rjXVxx8yim0/GXdB4vtIb0raI6T76iZ7+jB9OXXoN/ubeCN9RA3MfZFNHrOP27a21/8o7ZlqopKZB8wu2LfrQLDVw6gNVKBi3J3s4QunOnndO1mPITe50iqp8zH8vtYxhg48cZI7EmUC43QPaJaxzoHEL276ZeONE1LngH3QJe6cchVDFDjwHlsH193DKoHDeCelqPmexga5WxfToj2s+p8oO/+x4Fpm6XtWM58hsOFh62dOlK1Rhj4njeiNAJpuAlcmfwIDAQAB"
}
