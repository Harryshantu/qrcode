package com.theektek.qrcode.mdui.acostcentertext

import android.os.Build
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.view.*
import com.theektek.qrcode.R
import com.theektek.qrcode.databinding.FragmentAcostcentertextCreateBinding
import com.theektek.qrcode.mdui.BundleKeys
import com.theektek.qrcode.mdui.InterfacedFragment
import com.theektek.qrcode.mdui.UIConstants
import com.theektek.qrcode.repository.OperationResult
import com.theektek.qrcode.viewmodel.acostcentertexttype.ACostCenterTextTypeViewModel
import com.sap.cloud.android.odata.api_costcenter_srv_entities.ACostCenterTextType
import com.sap.cloud.android.odata.api_costcenter_srv_entities.API_COSTCENTER_SRV_EntitiesMetadata.EntityTypes
import com.sap.cloud.mobile.fiori.formcell.SimplePropertyFormCell
import com.sap.cloud.mobile.fiori.`object`.ObjectHeader
import com.sap.cloud.mobile.odata.Property
import org.slf4j.LoggerFactory

/**
 * A fragment that is used for both update and create for users to enter values for the properties. When used for
 * update, an instance of the entity is required. In the case of create, a new instance of the entity with defaults will
 * be created. The default values may not be acceptable for the OData service.
 * This fragment is either contained in a [ACostCenterTextListActivity] in two-pane mode (on tablets) or a
 * [ACostCenterTextDetailActivity] on handsets.
 *
 * Arguments: Operation: [OP_CREATE | OP_UPDATE]
 *            ACostCenterTextType if Operation is update
 */
class ACostCenterTextCreateFragment : InterfacedFragment<ACostCenterTextType, FragmentAcostcentertextCreateBinding>() {

    /** ACostCenterTextType object and it's copy: the modifications are done on the copied object. */
    private lateinit var aCostCenterTextTypeEntity: ACostCenterTextType
    private lateinit var aCostCenterTextTypeEntityCopy: ACostCenterTextType

    /** Indicate what operation to be performed */
    private lateinit var operation: String

    /** aCostCenterTextTypeEntity ViewModel */
    private lateinit var viewModel: ACostCenterTextTypeViewModel

    /** The update menu item */
    private lateinit var updateMenuItem: MenuItem

    private val isACostCenterTextTypeValid: Boolean
        get() {
            var isValid = true
            fragmentBinding.createUpdateAcostcentertexttype.let { linearLayout ->
                for (i in 0 until linearLayout.childCount) {
                    val simplePropertyFormCell = linearLayout.getChildAt(i) as SimplePropertyFormCell
                    val propertyName = simplePropertyFormCell.tag as String
                    val property = EntityTypes.aCostCenterTextType.getProperty(propertyName)
                    val value = simplePropertyFormCell.value.toString()
                    if (!isValidProperty(property, value)) {
                        simplePropertyFormCell.setTag(R.id.TAG_HAS_MANDATORY_ERROR, true)
                        val errorMessage = resources.getString(R.string.mandatory_warning)
                        simplePropertyFormCell.isErrorEnabled = true
                        simplePropertyFormCell.error = errorMessage
                        isValid = false
                    } else {
                        if (simplePropertyFormCell.isErrorEnabled) {
                            val hasMandatoryError = simplePropertyFormCell.getTag(R.id.TAG_HAS_MANDATORY_ERROR) as Boolean
                            if (!hasMandatoryError) {
                                isValid = false
                            } else {
                                simplePropertyFormCell.isErrorEnabled = false
                            }
                        }
                        simplePropertyFormCell.setTag(R.id.TAG_HAS_MANDATORY_ERROR, false)
                    }
                }
            }
            return isValid
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        menu = R.menu.itemlist_edit_options

        arguments?.let {
            (it.getString(BundleKeys.OPERATION))?.let { operationType ->
                operation = operationType
                activityTitle = when (operationType) {
                    UIConstants.OP_CREATE -> resources.getString(R.string.title_create_fragment, EntityTypes.aCostCenterTextType.localName)
                    else -> resources.getString(R.string.title_update_fragment) + " " + EntityTypes.aCostCenterTextType.localName

                }
            }
        }

        activity?.let {
            (it as ACostCenterTextActivity).isNavigationDisabled = true
            viewModel = ViewModelProvider(it)[ACostCenterTextTypeViewModel::class.java]
            viewModel.createResult.observe(this) { result -> onComplete(result) }
            viewModel.updateResult.observe(this) { result -> onComplete(result) }

            aCostCenterTextTypeEntity = if (operation == UIConstants.OP_CREATE) {
                createACostCenterTextType()
            } else {
                viewModel.selectedEntity.value!!
            }

            val workingCopy = when{ (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) -> {
                    savedInstanceState?.getParcelable<ACostCenterTextType>(KEY_WORKING_COPY, ACostCenterTextType::class.java)
                } else -> @Suppress("DEPRECATION") savedInstanceState?.getParcelable<ACostCenterTextType>(KEY_WORKING_COPY)
            }

            if (workingCopy == null) {
                aCostCenterTextTypeEntityCopy = aCostCenterTextTypeEntity.copy()
                aCostCenterTextTypeEntityCopy.entityTag = aCostCenterTextTypeEntity.entityTag
                aCostCenterTextTypeEntityCopy.oldEntity = aCostCenterTextTypeEntity
                aCostCenterTextTypeEntityCopy.editLink = aCostCenterTextTypeEntity.editLink
            } else {
                aCostCenterTextTypeEntityCopy = workingCopy
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        currentActivity.findViewById<ObjectHeader>(R.id.objectHeader)?.let {
            it.visibility = View.GONE
        }
        fragmentBinding.aCostCenterTextType = aCostCenterTextTypeEntityCopy
        return fragmentBinding.root
    }

    override  fun  initBinding(inflater: LayoutInflater, container: ViewGroup?) = FragmentAcostcentertextCreateBinding.inflate(inflater, container, false)

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.save_item -> {
                updateMenuItem = menuItem
                enableUpdateMenuItem(false)
                onSaveItem()
            }
            else -> super.onMenuItemSelected(menuItem)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if(secondaryToolbar != null) secondaryToolbar!!.title = activityTitle else activity?.title = activityTitle
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable(KEY_WORKING_COPY, aCostCenterTextTypeEntityCopy)
        super.onSaveInstanceState(outState)
    }

    /** Enables the update menu item based on [enable] */
    private fun enableUpdateMenuItem(enable : Boolean = true) {
        updateMenuItem.also {
            it.isEnabled = enable
            it.icon?.alpha = if(enable) 255 else 130
        }
    }

    /** Saves the entity */
    private fun onSaveItem(): Boolean {
        if (!isACostCenterTextTypeValid) {
            return false
        }
        (currentActivity as ACostCenterTextActivity).isNavigationDisabled = false
        progressBar?.visibility = View.VISIBLE
        when (operation) {
            UIConstants.OP_CREATE -> {
                viewModel.create(aCostCenterTextTypeEntityCopy)
            }
            UIConstants.OP_UPDATE -> viewModel.update(aCostCenterTextTypeEntityCopy)
        }
        return true
    }

    /**
     * Create a new ACostCenterTextType instance and initialize properties to its default values
     * Nullable property will remain null
     * @return new ACostCenterTextType instance
     */
    private fun createACostCenterTextType(): ACostCenterTextType {
        val entity = ACostCenterTextType(true)
        return entity
    }

    /** Callback function to complete processing when updateResult or createResult events fired */
    private fun onComplete(result: OperationResult<ACostCenterTextType>) {
        progressBar?.visibility = View.INVISIBLE
        enableUpdateMenuItem(true)
        if (result.error != null) {
            (currentActivity as ACostCenterTextActivity).isNavigationDisabled = true
            handleError(result)
        } else {
            if (operation == UIConstants.OP_UPDATE && !currentActivity.resources.getBoolean(R.bool.two_pane)) {
                viewModel.selectedEntity.value = aCostCenterTextTypeEntityCopy
            }
            (currentActivity as ACostCenterTextActivity).onBackPressedDispatcher.onBackPressed()
        }
    }

    /** Simple validation: checks the presence of mandatory fields. */
    private fun isValidProperty(property: Property, value: String): Boolean {
        return !(!property.isNullable && value.isEmpty())
    }

    /**
     * Notify user of error encountered while execution the operation
     *
     * @param [result] operation result with error
     */
    private fun handleError(result: OperationResult<ACostCenterTextType>) {
        val errorMessage = when (result.operation) {
            OperationResult.Operation.UPDATE -> getString(R.string.update_failed_detail)
            OperationResult.Operation.CREATE -> getString(R.string.create_failed_detail)
            else -> throw AssertionError()
        }
        showError(errorMessage)
    }


    companion object {
        private val KEY_WORKING_COPY = "WORKING_COPY"
        private val LOGGER = LoggerFactory.getLogger(ACostCenterTextActivity::class.java)
    }
}
