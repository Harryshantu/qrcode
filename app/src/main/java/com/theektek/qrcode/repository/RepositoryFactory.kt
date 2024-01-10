package com.theektek.qrcode.repository
import com.theektek.qrcode.service.SAPServiceManager

import com.sap.cloud.android.odata.api_costcenter_srv_entities.API_COSTCENTER_SRV_EntitiesMetadata.EntitySets
import com.sap.cloud.android.odata.api_costcenter_srv_entities.ACostCenterType
import com.sap.cloud.android.odata.api_costcenter_srv_entities.ACostCenterTextType

import com.sap.cloud.mobile.odata.EntitySet
import com.sap.cloud.mobile.odata.EntityValue
import com.sap.cloud.mobile.odata.Property

import java.util.WeakHashMap

/*
 * Repository factory to construct repository for an entity set
 */
class RepositoryFactory
/**
 * Construct a RepositoryFactory instance. There should only be one repository factory and used
 * throughout the life of the application to avoid caching entities multiple times.
 * @param sapServiceManager - Service manager for interaction with OData service
 */
(private val sapServiceManager: SAPServiceManager?) {
    private val repositories: WeakHashMap<String, Repository<out EntityValue>> = WeakHashMap()

    /**
     * Construct or return an existing repository for the specified entity set
     * @param entitySet - entity set for which the repository is to be returned
     * @param orderByProperty - if specified, collection will be sorted ascending with this property
     * @return a repository for the entity set
     */
    fun getRepository(entitySet: EntitySet, orderByProperty: Property?): Repository<out EntityValue> {
        val aPI_COSTCENTER_SRV_Entities = sapServiceManager?.aPI_COSTCENTER_SRV_Entities
        val key = entitySet.localName
        var repository: Repository<out EntityValue>? = repositories[key]
        if (repository == null) {
            repository = when (key) {
                EntitySets.aCostCenter.localName -> Repository<ACostCenterType>(aPI_COSTCENTER_SRV_Entities!!, EntitySets.aCostCenter, orderByProperty)
                EntitySets.aCostCenterText.localName -> Repository<ACostCenterTextType>(aPI_COSTCENTER_SRV_Entities!!, EntitySets.aCostCenterText, orderByProperty)
                else -> throw AssertionError("Fatal error, entity set[$key] missing in generated code")
            }
            repositories[key] = repository
        }
        return repository
    }

    /**
     * Get rid of all cached repositories
     */
    fun reset() {
        repositories.clear()
    }
}
