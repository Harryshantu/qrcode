package com.theektek.qrcode.app

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.sap.cloud.mobile.foundation.model.AppConfig
import com.theektek.qrcode.service.SAPServiceManager
import com.theektek.qrcode.repository.RepositoryFactory
import com.sap.cloud.mobile.flowv2.core.Flow
import com.sap.cloud.mobile.flowv2.core.FlowContextRegistry
import com.sap.cloud.mobile.flowv2.ext.FlowOptions
import com.sap.cloud.mobile.flowv2.model.FlowType
import com.sap.cloud.mobile.foundation.authentication.AppLifecycleCallbackHandler
import com.sap.cloud.mobile.foundation.mobileservices.MobileService
import com.sap.cloud.mobile.foundation.mobileservices.SDKInitializer
import com.sap.cloud.mobile.foundation.networking.LockWipePolicy
import com.sap.cloud.mobile.foundation.logging.LoggingService
import com.sap.cloud.mobile.foundation.settings.policies.LogPolicy
import com.sap.cloud.mobile.foundation.networking.BlockedUserInterceptor.BlockType
import com.sap.cloud.mobile.foundation.usage.UsageService
import com.sap.cloud.mobile.foundation.crash.CrashService
import com.sap.cloud.mobile.foundation.theme.ThemeDownloadService

import org.slf4j.LoggerFactory
import java.util.Date

class SAPWizardApplication: Application() {

    internal var isApplicationUnlocked = false
    lateinit var preferenceManager: SharedPreferences

    /**
     * Manages and provides access to OData stores providing data for the app.
     */
    internal var sapServiceManager: SAPServiceManager? = null
    /**
     * Application-wide RepositoryFactory
     */
    lateinit var repositoryFactory: RepositoryFactory
        private set

    override fun onCreate() {
        super.onCreate()
        preferenceManager = PreferenceManager.getDefaultSharedPreferences(this)
        initServices()
    }

    /**
     * Initialize service manager with application configuration
     *
     * @param appConfig the application configuration
     */
    fun initializeServiceManager(appConfig: AppConfig) {
        sapServiceManager = SAPServiceManager(appConfig)
        repositoryFactory =
            RepositoryFactory(sapServiceManager)
    }

    /**
     * Clears all user-specific data and configuration from the application, essentially resetting it to its initial
     * state.
     *
     * If client code wants to handle the reset logic of a service, here is an example:
     *
     *   SDKInitializer.resetServices { service ->
     *       return@resetServices if( service is PushService ) {
     *           PushService.unregisterPushSync(object: CallbackListener {
     *               override fun onSuccess() {
     *               }
     *
     *               override fun onError(p0: Throwable) {
     *               }
     *           })
     *           true
     *       } else {
     *           false
     *       }
     *   }
     */
    fun resetApplication() {
        preferenceManager.also {
            it.edit().clear().apply()
        }
        isApplicationUnlocked = false
        repositoryFactory.reset()
        SDKInitializer.resetServices()

    }

    private fun initServices() {
        val services = mutableListOf<MobileService>()
        services.add(LoggingService(autoUpload = false).apply {
            policy = LogPolicy(logLevel = "WARN", entryExpiry = 0, maxFileNumber = 4)
            logToConsole = true
        })
        services.add(UsageService())
        services.add(CrashService(true))
        services.add(ThemeDownloadService(this))

        SDKInitializer.start(this, * services.toTypedArray())
    }

    fun applyLockWipePolicy() {
        var sharedPreferences = this.getSharedPreferences(
            WizardFlowStateListener.KEY_LOCK_WIPE_POLICY_PARAMETERS_PREFERENCE,
            Context.MODE_PRIVATE
        )

        val lockWipePolicy = retrieveLockWipeEnabledStatus(sharedPreferences)?.let {
            retrieveLockDays(sharedPreferences)?.let { lockDays ->
                retrieveWipeDays(sharedPreferences)?.let { wipeDays ->
                    logger.info(
                        LOCK_WIPE_POLICY_TAG,
                        "Construct LockWipePolicy and define two callbacks"
                    )
                    LockWipePolicy(
                        it,
                        lockDays,
                        wipeDays,
                        {
                            startLockFlow()
                        },
                        {
                            startWipeFlow()
                        }
                    )
                }
            }
        }
        retrieveLastConnectionTime(sharedPreferences)?.let {
            logger.info(LOCK_WIPE_POLICY_TAG, "Apply LockWipePolicy")
            lockWipePolicy?.apply(it)
        }
    }

    fun applyLockOrWipeByServer(blockType: BlockType) {
        val activity = AppLifecycleCallbackHandler.getInstance().activity!!
        when (blockType) {
            BlockType.REGISTRATION_LOCKED -> {
                startLockFlow()
            }
            BlockType.REGISTRATION_WIPED -> {
                startWipeFlow()
            }
            else -> {
                TODO("Will do something if needed")
            }
        }
    }

    private fun startLockFlow() {
        val activity = AppLifecycleCallbackHandler.getInstance().activity!!
        logger.info(LOCK_CALL_BACK_TAG, "LockCallback is called")
        Flow.start(
            activity, FlowContextRegistry.flowContext.copy(
                flowType = FlowType.LOGOUT,
                flowOptions = FlowOptions(needConfirmWhenLogout = false)
            )
        ) { _, resultCode, _ ->
            if (resultCode == AppCompatActivity.RESULT_OK) {
                Intent(
                    this,
                    WelcomeActivity::class.java
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    this@SAPWizardApplication.startActivity(this)
                }
            }
        }
    }

    private fun startWipeFlow() {
        val activity = AppLifecycleCallbackHandler.getInstance().activity!!
        logger.info(WIPE_CALL_BACK_TAG, "WipeCallback is called")
        preferenceManager.also {
            it.edit().clear().apply()
        }
        isApplicationUnlocked = false
        repositoryFactory.reset()
        Flow.start(
            activity, FlowContextRegistry.flowContext.copy(
                flowType = FlowType.DEL_REGISTRATION
            )
        ) { _, resultCode, _ ->
            if (resultCode == AppCompatActivity.RESULT_OK) {
                Intent(this, WelcomeActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    startActivity(this)
                }
            }
        }
    }

    private fun retrieveLastConnectionTime(sharedPreferencesPara: SharedPreferences?): Date? {
        val dateString = sharedPreferencesPara?.getLong(WizardFlowStateListener.LAST_VALID_CONNECTION_TIME, Date().time)
        val date = dateString?.let { Date(it) }
        logger.info("retrieveLastConnectionTime", "LastConnectionTime: $date")
        return date
    }

    private fun retrieveLockWipeEnabledStatus(sharedPreferencesPara: SharedPreferences?): Boolean? {
        val enabled =sharedPreferencesPara?.getBoolean(WizardFlowStateListener.LOCKWIPE_ENABLED, false)
        logger.info("retrieveLockWipeEnabledStatus", "LockWipe is enabled on server: ${enabled.toString()}")
        return enabled;
    }

    private fun retrieveLockDays(sharedPreferencesPara: SharedPreferences?): Int? {
        val lockDays = sharedPreferencesPara?.getInt(WizardFlowStateListener.LOCKWIPE_BLOCK_PERIOD, -1)
        logger.info("retrieveLockDays", "lockDays for LockWipe: ${lockDays.toString()}")
        return lockDays;
    }

    private fun retrieveWipeDays(sharedPreferencesPara: SharedPreferences?): Int? {
        val wipeDays = sharedPreferencesPara?.getInt(WizardFlowStateListener.LOCKWIPE_WIPE_PERIOD, -1)
        logger.info("retrieveWipeDays", "wipeDays for LockWipe: ${wipeDays.toString()}")
        return wipeDays;
    }

    companion object {
        const val KEY_LOG_SETTING_PREFERENCE = "key.log.settings.preference"
        const val LOCK_WIPE_POLICY_TAG = "LockWipePolicy"
        const val LOCK_CALL_BACK_TAG = "LockCallback"
        const val WIPE_CALL_BACK_TAG = "WipeCallback"
        private val logger = LoggerFactory.getLogger(SAPWizardApplication::class.java)
    }
}
