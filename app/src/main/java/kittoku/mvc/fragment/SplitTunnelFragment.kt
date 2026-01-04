package kittoku.mvc.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import kittoku.mvc.R
import kittoku.mvc.adapter.SplitTunnelAppAdapter
import kittoku.mvc.databinding.FragmentSplitTunnelBinding
import kittoku.mvc.splittunnel.SplitTunnelMode
import kittoku.mvc.viewmodel.SplitTunnelUiState
import kittoku.mvc.viewmodel.SplitTunnelViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Fragment for configuring split tunneling.
 *
 * This fragment allows users to:
 * - Enable/disable split tunneling
 * - Choose between include/exclude mode
 * - Search and filter installed apps
 * - Select which apps should use or bypass the VPN
 */
class SplitTunnelFragment : Fragment() {
    private var _binding: FragmentSplitTunnelBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SplitTunnelViewModel by viewModel()

    private lateinit var appAdapter: SplitTunnelAppAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSplitTunnelBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        setupClickListeners()
        setupTextWatchers()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            // Navigate back
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        appAdapter =
            SplitTunnelAppAdapter(
                packageManager = requireContext().packageManager,
                onAppToggle = { appInfo, _ ->
                    viewModel.toggleAppSelection(appInfo.packageName)
                },
            )

        binding.appRecyclerView.adapter = appAdapter
    }

    private fun setupClickListeners() {
        // Enable/disable switch
        binding.splitTunnelSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setEnabled(isChecked)
        }

        // Mode radio buttons
        binding.modeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val mode =
                when (checkedId) {
                    R.id.excludeRadioButton -> SplitTunnelMode.EXCLUDE
                    R.id.includeRadioButton -> SplitTunnelMode.INCLUDE
                    else -> SplitTunnelMode.EXCLUDE
                }
            viewModel.setMode(mode)
        }

        // Show system apps checkbox
        binding.showSystemAppsCheckbox.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setShowSystemApps(isChecked)
        }

        // Select/deselect all buttons
        binding.selectAllButton.setOnClickListener {
            viewModel.selectAll()
        }

        binding.deselectAllButton.setOnClickListener {
            viewModel.deselectAll()
        }
    }

    private fun setupTextWatchers() {
        binding.searchEditText.doAfterTextChanged { text ->
            viewModel.setSearchQuery(text?.toString() ?: "")
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateUi(state)
                }
            }
        }
    }

    private fun updateUi(state: SplitTunnelUiState) {
        // Update switch (without triggering listener)
        binding.splitTunnelSwitch.setOnCheckedChangeListener(null)
        binding.splitTunnelSwitch.isChecked = state.isEnabled
        binding.splitTunnelSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setEnabled(isChecked)
        }

        // Update summary
        binding.splitTunnelSummary.text = state.summary

        // Update mode selection (without triggering listener)
        binding.modeRadioGroup.setOnCheckedChangeListener(null)
        when (state.mode) {
            SplitTunnelMode.EXCLUDE -> binding.excludeRadioButton.isChecked = true
            SplitTunnelMode.INCLUDE -> binding.includeRadioButton.isChecked = true
        }
        binding.modeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val mode =
                when (checkedId) {
                    R.id.excludeRadioButton -> SplitTunnelMode.EXCLUDE
                    R.id.includeRadioButton -> SplitTunnelMode.INCLUDE
                    else -> SplitTunnelMode.EXCLUDE
                }
            viewModel.setMode(mode)
        }

        // Update show system apps checkbox (without triggering listener)
        binding.showSystemAppsCheckbox.setOnCheckedChangeListener(null)
        binding.showSystemAppsCheckbox.isChecked = state.showSystemApps
        binding.showSystemAppsCheckbox.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setShowSystemApps(isChecked)
        }

        // Enable/disable mode and app selection based on split tunnel enabled state
        binding.modeCard.alpha = if (state.isEnabled) 1.0f else 0.5f
        binding.searchCard.alpha = if (state.isEnabled) 1.0f else 0.5f
        binding.appListHeader.alpha = if (state.isEnabled) 1.0f else 0.5f
        binding.appRecyclerView.alpha = if (state.isEnabled) 1.0f else 0.5f

        // Disable interactions when split tunnel is disabled
        setModeCardEnabled(state.isEnabled)
        setSearchCardEnabled(state.isEnabled)
        binding.appRecyclerView.isEnabled = state.isEnabled

        // Update loading state
        binding.loadingIndicator.isVisible = state.isLoading

        // Update app list
        if (!state.isLoading) {
            appAdapter.updateApps(state.filteredApps, state.selectedApps)
        }

        // Update empty state
        binding.emptyState.isVisible =
            !state.isLoading && state.filteredApps.isEmpty() && state.isEnabled

        // Update app list visibility
        binding.appRecyclerView.isVisible =
            !state.isLoading && state.filteredApps.isNotEmpty()
        binding.appListHeader.isVisible =
            !state.isLoading && state.filteredApps.isNotEmpty()

        // Handle error
        state.error?.let { error ->
            showError(error)
            viewModel.dismissError()
        }
    }

    private fun setModeCardEnabled(enabled: Boolean) {
        binding.excludeRadioButton.isEnabled = enabled
        binding.includeRadioButton.isEnabled = enabled
    }

    private fun setSearchCardEnabled(enabled: Boolean) {
        binding.searchEditText.isEnabled = enabled
        binding.showSystemAppsCheckbox.isEnabled = enabled
        binding.selectAllButton.isEnabled = enabled
        binding.deselectAllButton.isEnabled = enabled
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(
                requireContext().getColor(
                    com.google.android.material.R.color.design_default_color_error,
                ),
            )
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): SplitTunnelFragment = SplitTunnelFragment()
    }
}
