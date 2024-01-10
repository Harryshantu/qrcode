package com.theektek.qrcode.service

import com.sap.cloud.mobile.foundation.model.AppConfig
import com.sap.cloud.android.odata.api_costcenter_srv_entities.API_COSTCENTER_SRV_Entities
import com.sap.cloud.mobile.foundation.common.ClientProvider
import com.sap.cloud.mobile.odata.OnlineODataProvider
import com.sap.cloud.mobile.odata.http.OKHttpHandler

class SAPServiceManager(private val appConfig: AppConfig) {

    var serviceRoot: String = ""
        private set
        get() {
            return (aPI_COSTCENTER_SRV_Entities?.provider as OnlineODataProvider).serviceRoot
        }

    var aPI_COSTCENTER_SRV_Entities: API_COSTCENTER_SRV_Entities? = null
        private set
        get() {
            return field ?: throw IllegalStateException("SAPServiceManager was not initialized")
        }

    fun openODataStore(callback: () -> Unit) {
        if( appConfig != null ) {
            appConfig.serviceUrl?.let { _serviceURL ->
                aPI_COSTCENTER_SRV_Entities = API_COSTCENTER_SRV_Entities (
                    OnlineODataProvider("SAPService", _serviceURL + CONNECTION_ID_API_COSTCENTER_SRV_ENTITIES).apply {
                        networkOptions.httpHandler = OKHttpHandler(ClientProvider.get())
                        serviceOptions.checkVersion = false
                        serviceOptions.requiresType = true
                        serviceOptions.cacheMetadata = false
                    }
                )
            } ?: run {
                throw IllegalStateException("ServiceURL of Configuration Data is not initialized")
            }
        }
        callback.invoke()
    }

    companion object {
        const val CONNECTION_ID_API_COSTCENTER_SRV_ENTITIES: String = "CC_COSTCENTRE"
    }
}
