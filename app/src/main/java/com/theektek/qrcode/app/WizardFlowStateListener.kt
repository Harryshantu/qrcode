package com.theektek.qrcode.app

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.sap.cloud.mobile.flowv2.ext.FlowStateListener
import com.sap.cloud.mobile.foundation.model.AppConfig
import android.widget.Toast
import com.sap.cloud.mobile.foundation.authentication.AppLifecycleCallbackHandler
import com.sap.cloud.mobile.foundation.settings.policies.LogPolicy
import ch.qos.logback.classic.Level
import com.theektek.qrcode.R
import com.theektek.qrcode.repository.SharedPreferenceRepository
import com.sap.cloud.mobile.flowv2.model.FlowType
import com.sap.cloud.mobile.foundation.common.addUniqueInterceptor
import com.sap.cloud.mobile.foundation.mobileservices.ApplicationStates
import com.sap.cloud.mobile.foundation.networking.BlockedUserInterceptor
import com.sap.cloud.mobile.foundation.networking.BlockedUserInterceptor.BlockType
import com.sap.cloud.mobile.foundation.networking.LastConnectionTimeInterceptor
import com.sap.cloud.mobile.foundation.settings.policies.ClientPolicies
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

import org.slf4j.LoggerFactory
import okhttp3.OkHttpClient
import java.util.Date

class WizardFlowStateListener(private val application: SAPWizardApplication) :
    FlowStateListener() {

    private var sharedPreferences: SharedPreferences? = null
    private var userSwitchFlag = false

    override fun onAppConfigRetrieved(appConfig: AppConfig) {
        logger.debug("onAppConfigRetrieved: $appConfig")
        application.initializeServiceManager(appConfig)
    }

    override fun onApplicationReset() {
        this.application.resetApplication()
    }

    override fun onApplicationLocked() {
        super.onApplicationLocked()
        application.isApplicationUnlocked = false
    }

    override fun onOkHttpClientReady(httpClient: OkHttpClient) {
        val lastConnectionTimeInterceptor = LastConnectionTimeInterceptor(object :
            LastConnectionTimeInterceptor.LastConnectionTimeCallback {
            override fun updateLastConnectionTime() {
                sharedPreferences = application.getSharedPreferences(
                    KEY_LOCK_WIPE_POLICY_PARAMETERS_PREFERENCE, Context.MODE_PRIVATE
                )
                sharedPreferences?.edit()?.apply {
                    val dateTimeNow = Date().time
                    val dateTime = Date(dateTimeNow)
                    logger.info("sharedPreferences save data", "Saving lastConnection: $dateTime")
                    putLong(LAST_VALID_CONNECTION_TIME, dateTimeNow)
                }?.apply()
            }
        })

        val blockedUserInterceptor = BlockedUserInterceptor(object :
            BlockedUserInterceptor.BlockedUserCallback {
            override fun handleBlockedUser(blockType: BlockType) {
                if (blockType == BlockType.REGISTRATION_WIPED || blockType == BlockType.REGISTRATION_LOCKED) {
                    application.applyLockOrWipeByServer(blockType)
                }
            }
        })

        httpClient.addUniqueInterceptor(
            interceptor = lastConnectionTimeInterceptor,
            save = true
        ).addUniqueInterceptor(
            interceptor = blockedUserInterceptor,
            save = true
        )
    }

    override fun onFlowFinished(flowName: String?) {
        flowName?.let{
            application.isApplicationUnlocked = true
        }

        if (userSwitchFlag) {
            Intent(application, MainBusinessActivity::class.java).also {
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                application.startActivity(it)
            }
        }

        if(FlowType.TIMEOUT_UNLOCK.toString() == flowName && !ApplicationStates.isNetworkAvailable) {
            application.applyLockWipePolicy()
        }
    }

    override fun onClientPolicyRetrieved(policies: ClientPolicies) {
        policies.logPolicy?.also { logSettings ->
            val sharedPreferenceRepository = SharedPreferenceRepository(application)
            CoroutineScope(Dispatchers.IO).launch {
                val currentSettings =
                    sharedPreferenceRepository.userPreferencesFlow.first().logSetting

                if (currentSettings.logLevel != logSettings.logLevel) {
                    sharedPreferenceRepository.updateLogLevel(LogPolicy.getLogLevel(logSettings))

                    AppLifecycleCallbackHandler.getInstance().activity?.let {
                        it.runOnUiThread {
                            val logString = when (LogPolicy.getLogLevel(logSettings)) {
                                Level.ALL -> application.getString(R.string.log_level_path)
                                Level.INFO -> application.getString(R.string.log_level_info)
                                Level.WARN -> application.getString(R.string.log_level_warning)
                                Level.ERROR -> application.getString(R.string.log_level_error)
                                Level.OFF -> application.getString(R.string.log_level_none)
                                else -> application.getString(R.string.log_level_debug)
                            }
                            Toast.makeText(
                                application,
                                String.format(
                                    application.getString(R.string.log_level_changed),
                                    logString
                                ),
                                Toast.LENGTH_SHORT
                            ).show()
                            logger.info(
                                String.format(
                                    application.getString(R.string.log_level_changed),
                                    logString
                                )
                            )
                        }
                    }
                }
            }
        }

        policies.blockWipingPolicy?.also { blockWipingPolicy ->
            sharedPreferences = application.getSharedPreferences(
                KEY_LOCK_WIPE_POLICY_PARAMETERS_PREFERENCE,
                Context.MODE_PRIVATE
            )
            sharedPreferences?.edit()?.apply {
                logger.info("Save data to sharedPreferences", "enable: ${blockWipingPolicy.blockWipeEnabled}, lockDays: ${blockWipingPolicy.blockDisconnectedPeriod}, wipeDays: ${blockWipingPolicy.wipeDisconnectedPeriod}")
                putBoolean(LOCKWIPE_ENABLED, blockWipingPolicy.blockWipeEnabled)
                putInt(LOCKWIPE_BLOCK_PERIOD, blockWipingPolicy.blockDisconnectedPeriod)
                putInt(LOCKWIPE_WIPE_PERIOD, blockWipingPolicy.wipeDisconnectedPeriod)
            }?.apply()
        }
    }


    companion object {
        private val logger = LoggerFactory.getLogger(WizardFlowStateListener::class.java)

        const val KEY_LOCK_WIPE_POLICY_PARAMETERS_PREFERENCE = "key.lock.wipe.parameters.preference"
        const val LAST_VALID_CONNECTION_TIME = "last_valid_connection_time"
        const val LOCKWIPE_ENABLED = "blockWipeEnabled"
        const val LOCKWIPE_BLOCK_PERIOD = "blockDisconnectedPeriod"
        const val LOCKWIPE_WIPE_PERIOD = "wipeDisconnectedPeriod"
    }
}
