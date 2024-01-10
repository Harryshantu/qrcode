package com.theektek.qrcode.mdui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.Preference
import com.theektek.qrcode.databinding.ActivitySettingsBinding
import com.theektek.qrcode.R
import com.theektek.qrcode.viewmodel.OperationResult
import com.theektek.qrcode.viewmodel.OperationType
import com.theektek.qrcode.viewmodel.OperationType.UPLOAD_LOG
import com.theektek.qrcode.viewmodel.OperationType.UPLOAD_USAGE_DATA
import com.theektek.qrcode.viewmodel.SettingsViewModel
import com.sap.cloud.mobile.flowv2.core.DialogHelper
import kotlinx.coroutines.launch

class SettingsActivity: AppCompatActivity() {
    lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settingsFragment = SettingsFragment()
        val binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val viewModel = ViewModelProvider(this)[SettingsViewModel::class.java]
        lifecycleScope?.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.operationUIState.collect{
                    val result = it.result
                    result?.let {
                        when (it) {
                            is OperationResult.OperationFail -> {
                                DialogHelper(this@SettingsActivity).showOKOnlyDialog(
                                    fragmentManager = supportFragmentManager,
                                    message = it.message
                                )
                                enablePreferenceStatus(it.operationType, settingsFragment)
                            }
                            is OperationResult.OperationSuccess -> {
                                Toast.makeText(this@SettingsActivity, it.message, Toast.LENGTH_LONG).show()
                                enablePreferenceStatus(it.operationType, settingsFragment)
                            }
                        }
                        viewModel.resetOperationState()
                    }
                    val inProgress = it.inProgress
                    inProgress?.let {
                        when {
                            it -> {
                                binding.indeterminateBar.visibility = View.VISIBLE
                            }
                            else -> {
                                binding.indeterminateBar.visibility = View.INVISIBLE
                            }
                        }
                    }
                }
            }
        }
        supportFragmentManager.beginTransaction().replace(binding.settingsContainer.id, settingsFragment).commit()
    }

    private fun enablePreferenceStatus(operationType: OperationType, settingsFragment: SettingsFragment) {
        when (operationType) {
            UPLOAD_LOG -> {
                var logUploadPreference: Preference? =
                    settingsFragment.findPreference(getString(R.string.upload_log))
                logUploadPreference?.apply {
                    isEnabled = true
                }
            }

            UPLOAD_USAGE_DATA -> {
                var usageUploadPreference: Preference? =
                    settingsFragment.findPreference(getString(R.string.upload_usage))
                usageUploadPreference?.apply {
                    isEnabled = true
                }
            }
        }
    }
}
