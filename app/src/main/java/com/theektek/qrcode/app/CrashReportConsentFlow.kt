package com.theektek.qrcode.app

import android.app.Application
import com.sap.cloud.mobile.flowv2.R
import com.sap.cloud.mobile.flowv2.core.Flow
import com.sap.cloud.mobile.flowv2.steps.CrashReportConsentFragment

/**
 * A custom flow for crash report consent in settings page
 */
class CrashReportConsentFlow(application: Application) : Flow(application) {
    init {
        addSingleStep(R.id.stepCrashReportConsent, CrashReportConsentFragment::class)
    }
}
