package kittoku.mvc.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kittoku.mvc.R
import kittoku.mvc.adapter.ProfileAdapter
import kittoku.mvc.databinding.FragmentProfileListBinding
import kittoku.mvc.model.VpnProfile
import kittoku.mvc.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Fragment for displaying and managing VPN profiles.
 *
 * This fragment shows a list of VPN profiles and allows users to:
 * - View all saved profiles
 * - Add new profiles
 * - Edit existing profiles
 * - Delete profiles
 * - Select a profile for connection
 */
class ProfileListFragment : Fragment() {
    private var _binding: FragmentProfileListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModel()

    private lateinit var profileAdapter: ProfileAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentProfileListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        profileAdapter =
            ProfileAdapter(
                onProfileClick = { profile ->
                    viewModel.selectProfile(profile)
                },
                onEditClick = { profile ->
                    navigateToEditProfile(profile)
                },
                onDeleteClick = { profile ->
                    viewModel.requestDeleteProfile(profile)
                },
                onConnectClick = { profile ->
                    connectToProfile(profile)
                },
            )

        binding.profileRecyclerView.adapter = profileAdapter
    }

    private fun setupClickListeners() {
        binding.addProfileFab.setOnClickListener {
            navigateToAddProfile()
        }

        binding.addProfileButtonEmpty.setOnClickListener {
            navigateToAddProfile()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateUi(state.profiles, state.isLoading, state.selectedProfile)

                    // Handle delete confirmation
                    state.profileToDelete?.let { profile ->
                        showDeleteConfirmation(profile)
                    }

                    // Handle error messages
                    state.error?.let { error ->
                        showError(error)
                        viewModel.dismissError()
                    }

                    // Handle success messages
                    state.successMessage?.let { message ->
                        showSuccess(message)
                        viewModel.dismissSuccessMessage()
                    }
                }
            }
        }
    }

    private fun updateUi(
        profiles: List<VpnProfile>,
        isLoading: Boolean,
        selectedProfile: VpnProfile?,
    ) {
        binding.loadingIndicator.isVisible = isLoading
        binding.emptyState.isVisible = !isLoading && profiles.isEmpty()
        binding.profileRecyclerView.isVisible = !isLoading && profiles.isNotEmpty()

        profileAdapter.submitList(profiles)
        profileAdapter.setSelectedProfile(selectedProfile?.id)
    }

    private fun showDeleteConfirmation(profile: VpnProfile) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_profile_title)
            .setMessage(getString(R.string.delete_profile_message, profile.name))
            .setPositiveButton(R.string.delete_profile) { _, _ ->
                viewModel.deleteProfile(profile)
            }
            .setNegativeButton(R.string.cancel) { _, _ ->
                viewModel.dismissDeleteConfirmation()
            }
            .setOnCancelListener {
                viewModel.dismissDeleteConfirmation()
            }
            .show()
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(requireContext().getColor(com.google.android.material.R.color.design_default_color_error))
            .show()
    }

    private fun showSuccess(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun navigateToAddProfile() {
        // TODO: Navigate to add profile screen
        // For now, show a placeholder message
        Snackbar.make(binding.root, "Add profile feature coming soon", Snackbar.LENGTH_SHORT).show()
    }

    private fun navigateToEditProfile(profile: VpnProfile) {
        // TODO: Navigate to edit profile screen
        // For now, show a placeholder message
        Snackbar.make(binding.root, "Edit profile: ${profile.name}", Snackbar.LENGTH_SHORT).show()
    }

    private fun connectToProfile(profile: VpnProfile) {
        // TODO: Implement connection logic
        // For now, show a placeholder message
        Snackbar.make(binding.root, "Connecting to: ${profile.name}", Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): ProfileListFragment = ProfileListFragment()
    }
}
