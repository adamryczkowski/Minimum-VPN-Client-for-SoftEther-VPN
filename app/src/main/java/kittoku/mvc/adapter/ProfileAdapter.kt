package kittoku.mvc.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kittoku.mvc.R
import kittoku.mvc.databinding.ItemProfileBinding
import kittoku.mvc.model.VpnProfile

/**
 * Adapter for displaying VPN profiles in a RecyclerView.
 *
 * Uses ListAdapter with DiffUtil for efficient updates.
 */
class ProfileAdapter(
    private val onProfileClick: (VpnProfile) -> Unit,
    private val onEditClick: (VpnProfile) -> Unit,
    private val onDeleteClick: (VpnProfile) -> Unit,
    private val onConnectClick: (VpnProfile) -> Unit,
) : ListAdapter<VpnProfile, ProfileAdapter.ProfileViewHolder>(ProfileDiffCallback()) {
    private var selectedProfileId: Long? = null

    /**
     * Set the currently selected profile (for highlighting).
     */
    fun setSelectedProfile(profileId: Long?) {
        val oldSelectedId = selectedProfileId
        selectedProfileId = profileId

        // Update the old and new selected items
        if (oldSelectedId != null) {
            val oldPosition = currentList.indexOfFirst { it.id == oldSelectedId }
            if (oldPosition >= 0) notifyItemChanged(oldPosition)
        }
        if (profileId != null) {
            val newPosition = currentList.indexOfFirst { it.id == profileId }
            if (newPosition >= 0) notifyItemChanged(newPosition)
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ProfileViewHolder {
        val binding =
            ItemProfileBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            )
        return ProfileViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: ProfileViewHolder,
        position: Int,
    ) {
        holder.bind(getItem(position))
    }

    inner class ProfileViewHolder(
        private val binding: ItemProfileBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(profile: VpnProfile) {
            binding.apply {
                profileName.text = profile.name
                profileServer.text = "${profile.serverAddress}:${profile.port}"
                profileUsername.text = "${profile.username}@${profile.hubName}"

                // Set card checked state for selection
                profileCard.isChecked = profile.id == selectedProfileId

                // Click on card to select profile
                profileCard.setOnClickListener {
                    onProfileClick(profile)
                }

                // Long click to connect
                profileCard.setOnLongClickListener {
                    onConnectClick(profile)
                    true
                }

                // Menu button
                profileMenuButton.setOnClickListener { view ->
                    val popup = PopupMenu(view.context, view)
                    popup.menuInflater.inflate(R.menu.profile_item_menu, popup.menu)
                    popup.setOnMenuItemClickListener { menuItem ->
                        when (menuItem.itemId) {
                            R.id.action_connect -> {
                                onConnectClick(profile)
                                true
                            }
                            R.id.action_edit -> {
                                onEditClick(profile)
                                true
                            }
                            R.id.action_delete -> {
                                onDeleteClick(profile)
                                true
                            }
                            else -> false
                        }
                    }
                    popup.show()
                }
            }
        }
    }

    /**
     * DiffUtil callback for efficient list updates.
     */
    class ProfileDiffCallback : DiffUtil.ItemCallback<VpnProfile>() {
        override fun areItemsTheSame(
            oldItem: VpnProfile,
            newItem: VpnProfile,
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: VpnProfile,
            newItem: VpnProfile,
        ): Boolean {
            return oldItem == newItem
        }
    }
}
