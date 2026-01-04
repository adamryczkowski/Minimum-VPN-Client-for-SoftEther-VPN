package kittoku.mvc.adapter

import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kittoku.mvc.databinding.ItemSplitTunnelAppBinding
import kittoku.mvc.splittunnel.AppInfo

/**
 * Adapter for displaying installed apps in the split tunnel configuration screen.
 *
 * Uses ListAdapter with DiffUtil for efficient updates.
 *
 * @param packageManager Used to load app icons
 * @param onAppToggle Callback when an app's selection state changes
 */
class SplitTunnelAppAdapter(
    private val packageManager: PackageManager,
    private val onAppToggle: (AppInfo, Boolean) -> Unit,
) : ListAdapter<SplitTunnelAppAdapter.AppItem, SplitTunnelAppAdapter.AppViewHolder>(AppDiffCallback()) {
    /**
     * Data class combining app info with selection state.
     */
    data class AppItem(
        val appInfo: AppInfo,
        val isSelected: Boolean,
    )

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): AppViewHolder {
        val binding =
            ItemSplitTunnelAppBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            )
        return AppViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: AppViewHolder,
        position: Int,
    ) {
        holder.bind(getItem(position))
    }

    inner class AppViewHolder(
        private val binding: ItemSplitTunnelAppBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: AppItem) {
            binding.apply {
                appName.text = item.appInfo.appName
                packageName.text = item.appInfo.packageName

                // Show system app badge if applicable
                systemAppBadge.visibility =
                    if (item.appInfo.isSystemApp) View.VISIBLE else View.GONE

                // Set checkbox state
                appCheckbox.isChecked = item.isSelected
                appCard.isChecked = item.isSelected

                // Load app icon
                try {
                    val icon = packageManager.getApplicationIcon(item.appInfo.packageName)
                    appIcon.setImageDrawable(icon)
                } catch (e: PackageManager.NameNotFoundException) {
                    // Use default icon if app not found
                    appIcon.setImageResource(android.R.drawable.sym_def_app_icon)
                }

                // Handle click on card
                appCard.setOnClickListener {
                    val newState = !item.isSelected
                    onAppToggle(item.appInfo, newState)
                }
            }
        }
    }

    /**
     * Update the list with new apps and their selection states.
     *
     * @param apps List of apps to display
     * @param selectedPackages Set of package names that are selected
     */
    fun updateApps(
        apps: List<AppInfo>,
        selectedPackages: Set<String>,
    ) {
        val items =
            apps.map { appInfo ->
                AppItem(
                    appInfo = appInfo,
                    isSelected = selectedPackages.contains(appInfo.packageName),
                )
            }
        submitList(items)
    }

    private class AppDiffCallback : DiffUtil.ItemCallback<AppItem>() {
        override fun areItemsTheSame(
            oldItem: AppItem,
            newItem: AppItem,
        ): Boolean =
            oldItem.appInfo.packageName == newItem.appInfo.packageName

        override fun areContentsTheSame(
            oldItem: AppItem,
            newItem: AppItem,
        ): Boolean = oldItem == newItem
    }
}
