package com.theektek.qrcode.mdui.acostcenter

import android.content.Intent
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import com.theektek.qrcode.databinding.FragmentAcostcenterDetailBinding
import com.theektek.qrcode.mdui.EntityKeyUtil
import com.theektek.qrcode.mdui.InterfacedFragment
import com.theektek.qrcode.mdui.UIConstants
import com.theektek.qrcode.repository.OperationResult
import com.theektek.qrcode.R
import com.theektek.qrcode.viewmodel.acostcentertype.ACostCenterTypeViewModel
import com.sap.cloud.android.odata.api_costcenter_srv_entities.API_COSTCENTER_SRV_EntitiesMetadata.EntitySets
import com.sap.cloud.android.odata.api_costcenter_srv_entities.ACostCenterType
import com.sap.cloud.mobile.fiori.`object`.ObjectHeader

import com.theektek.qrcode.mdui.acostcentertext.ACostCenterTextActivity

/**
 * A fragment representing a single ACostCenterType detail screen.
 * This fragment is contained in an ACostCenterActivity.
 */
class ACostCenterDetailFragment : InterfacedFragment<ACostCenterType, FragmentAcostcenterDetailBinding>() {

    /** ACostCenterType entity to be displayed */
    private lateinit var aCostCenterTypeEntity: ACostCenterType

    /** Fiori ObjectHeader component used when entity is to be displayed on phone */
    private var objectHeader: ObjectHeader? = null

    /** View model of the entity type that the displayed entity belongs to */
    private lateinit var viewModel: ACostCenterTypeViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        menu = R.menu.itemlist_view_options
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        fragmentBinding.handler = this
        return fragmentBinding.root
    }

    override  fun  initBinding(inflater: LayoutInflater, container: ViewGroup?) = FragmentAcostcenterDetailBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.let {
            currentActivity = it
            viewModel = ViewModelProvider(it)[ACostCenterTypeViewModel::class.java]
            viewModel.deleteResult.observe(viewLifecycleOwner) { result ->
                onDeleteComplete(result)
            }

            viewModel.selectedEntity.observe(viewLifecycleOwner) { entity ->
                aCostCenterTypeEntity = entity
                fragmentBinding.aCostCenterType = entity
                setupObjectHeader()
            }
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.update_item -> {
                listener?.onFragmentStateChange(UIConstants.EVENT_EDIT_ITEM, aCostCenterTypeEntity)
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
    private fun onDeleteComplete(result: OperationResult<ACostCenterType>) {
        progressBar?.let {
            it.visibility = View.INVISIBLE
        }
        viewModel.removeAllSelected()
        result.error?.let {
            showError(getString(R.string.delete_failed_detail))
            return
        }
        listener?.onFragmentStateChange(UIConstants.EVENT_DELETION_COMPLETED, aCostCenterTypeEntity)
    }


    @Suppress("UNUSED", "UNUSED_PARAMETER") // parameter is needed because of the xml binding
    fun onNavigationClickedToACostCenterText_to_Text(view: View) {
        val intent = Intent(currentActivity, ACostCenterTextActivity::class.java)
        intent.putExtra("parent", aCostCenterTypeEntity)
        intent.putExtra("navigation", "to_Text")
        startActivity(intent)
    }

    /**
     * Set detail image of ObjectHeader.
     * When the entity does not provides picture, set the first character of the masterProperty.
     */
    private fun setDetailImage(objectHeader: ObjectHeader, aCostCenterTypeEntity: ACostCenterType) {
        if (aCostCenterTypeEntity.getOptionalValue(ACostCenterType.validityStartDate) != null && !aCostCenterTypeEntity.getOptionalValue(ACostCenterType.validityStartDate).toString().isEmpty()) {
            objectHeader.detailImageCharacter = aCostCenterTypeEntity.getOptionalValue(ACostCenterType.validityStartDate).toString().substring(0, 1)
        } else {
            objectHeader.detailImageCharacter = "?"
        }
    }

    /**
     * Setup ObjectHeader with an instance of aCostCenterTypeEntity
     */
    private fun setupObjectHeader() {
        val secondToolbar = currentActivity.findViewById<Toolbar>(R.id.secondaryToolbar)
        if (secondToolbar != null) {
            secondToolbar.title = aCostCenterTypeEntity.entityType.localName
        } else {
            currentActivity.title = aCostCenterTypeEntity.entityType.localName
        }

        // Object Header is not available in tablet mode
        objectHeader = currentActivity.findViewById(R.id.objectHeader)
        val dataValue = aCostCenterTypeEntity.getOptionalValue(ACostCenterType.validityStartDate)

        objectHeader?.let {
            it.apply {
                headline = dataValue?.toString()
                subheadline = EntityKeyUtil.getOptionalEntityKey(aCostCenterTypeEntity)
                body = "You can set the header body text here."
                footnote = "You can set the header footnote here."
                description = "You can add a detailed item description here."
            }
            it.setTag("#tag1", 0)
            it.setTag("#tag3", 2)
            it.setTag("#tag2", 1)

            setDetailImage(it, aCostCenterTypeEntity)
            it.visibility = View.VISIBLE
        }
    }
}
