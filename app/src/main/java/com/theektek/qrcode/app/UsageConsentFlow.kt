package com.theektek.qrcode.app

import android.app.Application
import com.sap.cloud.mobile.flowv2.R
import com.sap.cloud.mobile.flowv2.core.Flow
import com.sap.cloud.mobile.flowv2.steps.UsageConsentFragment

/**
 * A custom flow for usage consent in settings page
 */
class UsageConsentFlow(application: Application) : Flow(application) {
    init {
        addSingleStep(R.id.stepUsageConsent, UsageConsentFragment::class)
    }
}
