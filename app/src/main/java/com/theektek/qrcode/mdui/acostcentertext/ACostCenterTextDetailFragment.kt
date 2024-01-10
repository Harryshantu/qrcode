package com.theektek.qrcode.mdui.acostcentertext

import android.content.Intent
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import com.theektek.qrcode.databinding.FragmentAcostcentertextDetailBinding
import com.theektek.qrcode.mdui.EntityKeyUtil
import com.theektek.qrcode.mdui.InterfacedFragment
import com.theektek.qrcode.mdui.UIConstants
import com.theektek.qrcode.repository.OperationResult
import com.theektek.qrcode.R
import com.theektek.qrcode.viewmodel.acostcentertexttype.ACostCenterTextTypeViewModel
import com.sap.cloud.android.odata.api_costcenter_srv_entities.API_COSTCENTER_SRV_EntitiesMetadata.EntitySets
import com.sap.cloud.android.odata.api_costcenter_srv_entities.ACostCenterTextType
import com.sap.cloud.mobile.fiori.`object`.ObjectHeader

import com.theektek.qrcode.mdui.acostcenter.ACostCenterActivity

/**
 * A fragment representing a single ACostCenterTextType detail screen.
 * This fragment is contained in an ACostCenterTextActivity.
 */
class ACostCenterTextDetailFragment : InterfacedFragment<ACostCenterTextType, FragmentAcostcentertextDetailBinding>() {

    /** ACostCenterTextType entity to be displayed */
    private lateinit var aCostCenterTextTypeEntity: ACostCenterTextType

    /** Fiori ObjectHeader component used when entity is to be displayed on phone */
    private var objectHeader: ObjectHeader? = null

    /** View model of the entity type that the displayed entity belongs to */
    private lateinit var viewModel: ACostCenterTextTypeViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        menu = R.menu.itemlist_view_options
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        fragmentBinding.handler = this
        return fragmentBinding.root
    }

    override  fun  initBinding(inflater: LayoutInflater, container: ViewGroup?) = FragmentAcostcentertextDetailBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.let {
            currentActivity = it
            viewModel = ViewModelProvider(it)[ACostCenterTextTypeViewModel::class.java]
            viewModel.deleteResult.observe(viewLifecycleOwner) { result ->
                onDeleteComplete(result)
            }

            viewModel.selectedEntity.observe(viewLifecycleOwner) { entity ->
                aCostCenterTextTypeEntity = entity
                fragmentBinding.aCostCenterTextType = entity
                setupObjectHeader()
            }
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.update_item -> {
                listener?.onFragmentStateChange(UIConstants.EVENT_EDIT_ITEM, aCostCenterTextTypeEntity)
                true
            }
            R.id.delete_item -> {
                listener?.onFragmentStateChange(UIConstants.EVENT_ASK_DELETE_CONFIRMATION,null)
                true
            }
            else -> super.onMenuItemSelected(menuItem)
        }
    }

    /**
     * Completion callback for delete operation
     *
     * @param [result] of the operation
     */
    private fun onDeleteComplete(result: OperationResult<ACostCenterTextType>) {
        progressBar?.let {
            it.visibility = View.INVISIBLE
        }
        viewModel.removeAllSelected()
        result.error?.let {
            showError(getString(R.string.delete_failed_detail))
            return
        }
        listener?.onFragmentStateChange(UIConstants.EVENT_DELETION_COMPLETED, aCostCenterTextTypeEntity)
    }


    @Suppress("UNUSED", "UNUSED_PARAMETER") // parameter is needed because of the xml binding
    fun onNavigationClickedToACostCenter_to_CostCenter(view: View) {
        val intent = Intent(currentActivity, ACostCenterActivity::class.java)
        intent.putExtra("parent", aCostCenterTextTypeEntity)
        intent.putExtra("navigation", "to_CostCenter")
        startActivity(intent)
    }

    /**
     * Set detail image of ObjectHeader.
     * When the entity does not provides picture, set the first character of the masterProperty.
     */
    private fun setDetailImage(objectHeader: ObjectHeader, aCostCenterTextTypeEntity: ACostCenterTextType) {
        if (aCostCenterTextTypeEntity.getOptionalValue(ACostCenterTextType.validityStartDate) != null && !aCostCenterTextTypeEntity.getOptionalValue(ACostCenterTextType.validityStartDate).toString().isEmpty()) {
            objectHeader.detailImageCharacter = aCostCenterTextTypeEntity.getOptionalValue(ACostCenterTextType.validityStartDate).toString().substring(0, 1)
        } else {
            objectHeader.detailImageCharacter = "?"
        }
    }

    /**
     * Setup ObjectHeader with an instance of aCostCenterTextTypeEntity
     */
    private fun setupObjectHeader() {
        val secondToolbar = currentActivity.findViewById<Toolbar>(R.id.secondaryToolbar)
        if (secondToolbar != null) {
            secondToolbar.title = aCostCenterTextTypeEntity.entityType.localName
        } else {
            currentActivity.title = aCostCenterTextTypeEntity.entityType.localName
        }

        // Object Header is not available in tablet mode
        objectHeader = currentActivity.findViewById(R.id.objectHeader)
        val dataValue = aCostCenterTextTypeEntity.getOptionalValue(ACostCenterTextType.validityStartDate)

        objectHeader?.let {
            it.apply {
                headline = dataValue?.toString()
                subheadline = EntityKeyUtil.getOptionalEntityKey(aCostCenterTextTypeEntity)
                body = "You can set the header body text here."
                footnote = "You can set the header footnote here."
                description = "You can add a detailed item description here."
            }
            it.setTag("#tag1", 0)
            it.setTag("#tag3", 2)
            it.setTag("#tag2", 1)

            setDetailImage(it, aCostCenterTextTypeEntity)
            it.visibility = View.VISIBLE
        }
    }
}
