package com.theektek.qrcode.mdui

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.*

import com.theektek.qrcode.R
import com.theektek.qrcode.app.WelcomeActivity
import com.sap.cloud.mobile.flowv2.model.FlowType
import com.sap.cloud.mobile.flowv2.core.Flow.Companion.start
import com.sap.cloud.mobile.flowv2.model.FlowConstants
import com.sap.cloud.mobile.flowv2.core.FlowContextRegistry.flowContext

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.preference.ListPreference
import ch.qos.logback.classic.Level
import com.sap.cloud.mobile.foundation.settings.policies.LogPolicy
import com.sap.cloud.mobile.flowv2.ext.ConsentType
import com.sap.cloud.mobile.foundation.mobileservices.SDKInitializer
import com.theektek.qrcode.repository.SharedPreferenceRepository
import com.theektek.qrcode.viewmodel.SettingsViewModel
import com.theektek.qrcode.app.CrashReportConsentFlow
import com.sap.cloud.mobile.foundation.crash.CrashService
import com.sap.cloud.mobile.foundation.usage.UsageBroker
import com.theektek.qrcode.app.UsageConsentFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory


/** This fragment represents the settings screen. */
class SettingsFragment : PreferenceFragmentCompat() {
    private lateinit var logLevelPreference: ListPreference
    private lateinit var logUploadPreference: Preference
    private lateinit var viewModel: SettingsViewModel

    private var setUsagePermission: SwitchPreference? = null
    private lateinit var usageUploadPreference: Preference
    private var setCrashPermission: SwitchPreference? = null
    private var changePassCodePreference: Preference? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)

        activity?.let {
            viewModel = ViewModelProvider(it)[SettingsViewModel::class.java]
            logLevelPreference = findPreference(getString(R.string.log_level))!!
            prepareLogSetting(logLevelPreference, viewModel)
            // Upload log
            logUploadPreference = findPreference(getString(R.string.upload_log))!!
            logUploadPreference.setOnPreferenceClickListener {
                logUploadPreference.isEnabled = false
                if (viewModel.supportLogging) {
                    viewModel.uploadLog(viewLifecycleOwner)
                }
                false
            }
        }

        changePassCodePreference = findPreference(getString(R.string.manage_passcode))
        changePassCodePreference!!.setOnPreferenceClickListener {
            changePassCodePreference!!.isEnabled = false
            val flowContext =
                flowContext.copy(flowType = FlowType.CHANGEPASSCODE)
            start(requireActivity(), flowContext) { requestCode, _, _ ->
                if (requestCode == FlowConstants.FLOW_ACTIVITY_REQUEST_CODE) {
                    changePassCodePreference!!.isEnabled = true
                }
            }
            false
        }
        setUsagePermission = findPreference(getString(R.string.set_usage_consent))
        usageUploadPreference = findPreference(getString(R.string.upload_usage))!!
        prepareUsagePreference()
        prepareCrashReportPreference()
        // Reset App
        prepareResetAppPreference()

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        viewLifecycleOwner.lifecycleScope?.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.settingUIState.collect {
                    logLevelPreference = findPreference(getString(R.string.log_level))!!
                    logLevelPreference.summary = logStrings()[it.level]
                    logLevelPreference.value = it.level.toInt().toString()
                    setUsagePermission!!.isChecked = it.consentUsageCollection
                    usageUploadPreference.isEnabled = it.consentUsageCollection

                    setCrashPermission!!.isChecked = it.consentCrashReportCollection
                }
            }
        }
        return view
    }

    override fun onResume() {
        super.onResume()
        prepareLogSetting(logLevelPreference, viewModel)
        prepareUsagePreference()
        prepareCrashReportPreference()
    }

    private fun logStrings() = mapOf<Level, String>(
        Level.ALL to getString(R.string.log_level_path),
        Level.DEBUG to getString(R.string.log_level_debug),
        Level.INFO to getString(R.string.log_level_info),
        Level.WARN to getString(R.string.log_level_warning),
        Level.ERROR to getString(R.string.log_level_error),
        Level.OFF to getString(R.string.log_level_none)
    )

    private fun prepareLogSetting(
        logLevelPreference: ListPreference,
        viewModel: SettingsViewModel
    ) {
        activity?.let {
            val sharedPreferenceRepository = SharedPreferenceRepository(it.application)
            sharedPreferenceRepository.let {
                logLevelPreference.setOnPreferenceChangeListener { preference, newValue ->
                    val logLevel = Level.toLevel(Integer.valueOf(newValue as String))
                    CoroutineScope(Dispatchers.IO).launch {
                        val currentSettings = it.userPreferencesFlow.first().logSetting
                        LOGGER.debug(TAG, "old log settings: $currentSettings")
                        if (currentSettings.logLevel != logLevel.toString()) {
                            val newSettings =
                                currentSettings.copy(logLevel = LogPolicy.getLogLevelString(logLevel))
                            viewModel.updateLogLevel(LogPolicy.getLogLevel(newSettings))
                            LOGGER.debug(TAG, "new log settings: $newSettings")
                        }
                    }
                    preference.summary = logStrings()[logLevel]
                    true
                }
            }
        }

        logLevelPreference.summary = logStrings()[viewModel.settingUIState.value.level]
        logLevelPreference.value = viewModel.settingUIState.value.level.toInt().toString()
        logLevelPreference.entries = logStrings().values.toTypedArray()
        logLevelPreference.entryValues = arrayOf(
            Level.ALL.levelInt.toString(),
            Level.DEBUG.levelInt.toString(),
            Level.INFO.levelInt.toString(),
            Level.WARN.levelInt.toString(),
            Level.ERROR.levelInt.toString(),
            Level.OFF.levelInt.toString()
        )
    }

    private fun startUsageConsentFlow(application: Application, viewModel: SettingsViewModel) {
        val flowContext = flowContext.copy(
            flow = UsageConsentFlow(application)
        )
        start(requireActivity(), flowContext) { requestCode, _, _ ->
            if (requestCode == FlowConstants.FLOW_ACTIVITY_REQUEST_CODE) {
                viewModel.updateConsents(ConsentType.USAGE, UsageBroker.isStarted())
            }
        }
    }

    private fun prepareUsagePreference() {
        UsageBroker.isStarted().also {
            setUsagePermission!!.isChecked = it
            usageUploadPreference.isEnabled = it
        }
        setUsagePermission!!.setOnPreferenceClickListener {
            if (!(it as SwitchPreference).isChecked) {
                viewModel.updateConsents(ConsentType.USAGE, false)
            } else {
                startUsageConsentFlow(requireActivity().application, viewModel)
            }
            false
        }

        // Upload usage
        usageUploadPreference.setOnPreferenceClickListener {
            usageUploadPreference.isEnabled = false
            if (viewModel.supportUsage) {
                viewModel.uploadUsageData(this)
            }
            false
        }
    }

    private fun startCrashReportConsentFlow(
        application: Application,
        viewModel: SettingsViewModel
    ) {
        val flowContext = flowContext.copy(
            flow = CrashReportConsentFlow(application)
        )
        start(requireActivity(), flowContext) { requestCode, resultCode, data ->
            if (requestCode == FlowConstants.FLOW_ACTIVITY_REQUEST_CODE) {
                viewModel.updateConsents(
                    ConsentType.CRASH_REPORT,
                    SDKInitializer.getService(CrashService::class)!!.consented
                )
            }
        }
    }

    private fun prepareCrashReportPreference() {
        setCrashPermission = findPreference(getString(R.string.set_crash_report_consent))
        setCrashPermission!!.isChecked =
            viewModel.settingUIState.value.consentCrashReportCollection//SDKInitializer.getService(CrashService::class)!!.consented
        setCrashPermission!!.setOnPreferenceClickListener {
            if (viewModel.supportCrashReport) {
                if (!(it as SwitchPreference).isChecked) {
                    viewModel.updateConsents(ConsentType.CRASH_REPORT, false)
                } else {
                    startCrashReportConsentFlow(requireActivity().application, viewModel)
                }
            }
            false
        }
    }

    private fun startResetFlow() {
        start(
            activity = requireActivity(),
            flowContext = flowContext.copy(flowType = FlowType.RESET),
            flowActivityResultCallback = { _, resultCode, _ ->
                if (resultCode == Activity.RESULT_OK) {
                    Intent(requireActivity(), WelcomeActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        startActivity(this)
                    }
                }
            })
    }

    private fun prepareResetAppPreference() {
        val resetAppPreference: Preference = findPreference(getString(R.string.reset_app))!!
        resetAppPreference.setOnPreferenceClickListener {
            startResetFlow()
            false
        }
    }

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(SettingsFragment::class.java)
        private val TAG = SettingsFragment::class.simpleName
    }
}
