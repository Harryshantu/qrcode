package com.theektek.qrcode.viewmodel.acostcentertype

import android.app.Application
import android.os.Parcelable

import com.theektek.qrcode.viewmodel.EntityViewModel
import com.sap.cloud.android.odata.api_costcenter_srv_entities.ACostCenterType
import com.sap.cloud.android.odata.api_costcenter_srv_entities.API_COSTCENTER_SRV_EntitiesMetadata.EntitySets

/*
 * Represents View model for ACostCenterType
 *
 * Having an entity view model for each <T> allows the ViewModelProvider to cache and return the view model of that
 * type. This is because the ViewModelStore of ViewModelProvider cannot not be able to tell the difference between
 * EntityViewModel<type1> and EntityViewModel<type2>.
 */
class ACostCenterTypeViewModel(application: Application): EntityViewModel<ACostCenterType>(application, EntitySets.aCostCenter, ACostCenterType.validityStartDate) {
    /**
     * Constructor for a specific view model with navigation data.
     * @param [navigationPropertyName] - name of the navigation property
     * @param [entityData] - parent entity (starting point of the navigation)
     */
    constructor(application: Application, navigationPropertyName: String, entityData: Parcelable): this(application) {
        EntityViewModel<ACostCenterType>(application, EntitySets.aCostCenter, ACostCenterType.validityStartDate, navigationPropertyName, entityData)
    }
}
