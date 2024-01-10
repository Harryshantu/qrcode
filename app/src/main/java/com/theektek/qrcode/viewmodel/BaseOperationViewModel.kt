package com.theektek.qrcode.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class OperationType {
    UPLOAD_LOG, UPLOAD_USAGE_DATA
}

sealed class OperationResult() {
    data class OperationSuccess(val message: String, val operationType: OperationType) : OperationResult()
    data class OperationFail(val message: String, val operationType: OperationType) : OperationResult()
}

data class OperationUIState(
    val inProgress: Boolean = false,
    val progress: Float? = null,
    val result: OperationResult? = null,
)

open class BaseOperationViewModel(application: Application) : AndroidViewModel(application){
    protected val _operationUIState = MutableStateFlow(OperationUIState())
    val operationUIState = _operationUIState.asStateFlow()

    fun resetOperationState() {
        _operationUIState.update { OperationUIState() }
    }

    fun operationFinished(result: OperationResult) {
        _operationUIState.update { OperationUIState(result = result) }
    }

    fun operationStart() {
        _operationUIState.update { OperationUIState(inProgress = true) }
    }
}
