package kittoku.mvc.preference.custom

import android.content.Context
import android.content.SharedPreferences
import android.text.InputType
import android.util.AttributeSet
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.preference.ListPreference
import kittoku.mvc.preference.MvcPreference
import kittoku.mvc.preference.accessor.getIntPrefValue
import kittoku.mvc.preference.accessor.getStringPrefValue
import kittoku.mvc.preference.accessor.setIntPrefValue
import kittoku.mvc.preference.accessor.setStringPrefValue

internal class PortSelectionPreference(
    context: Context,
    attrs: AttributeSet,
) : ListPreference(context, attrs) {
    companion object {
        private const val PORT_443 = "443"
        private const val PORT_992 = "992"
        private const val PORT_CUSTOM = "Custom"
    }

    private val listener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == MvcPreference.SSL_PORT.name || key == MvcPreference.SSL_PORT_SELECTION.name) {
                updateSummaryFromPreferences()
            }
        }

    override fun onAttached() {
        super.onAttached()

        title = "Port Number"
        entries = arrayOf(PORT_443, PORT_992, PORT_CUSTOM)
        entryValues = arrayOf(PORT_443, PORT_992, PORT_CUSTOM)

        // Set initial value from preferences
        sharedPreferences?.let { prefs ->
            val selection = getStringPrefValue(MvcPreference.SSL_PORT_SELECTION, prefs)
            value = selection
        }

        updateSummaryFromPreferences()
        sharedPreferences?.registerOnSharedPreferenceChangeListener(listener)
    }

    override fun onDetached() {
        super.onDetached()
        sharedPreferences?.unregisterOnSharedPreferenceChangeListener(listener)
    }

    override fun callChangeListener(newValue: Any?): Boolean {
        val selectedValue = newValue as? String ?: return false

        if (selectedValue == PORT_CUSTOM) {
            showCustomPortDialog()
            return false // Don't change value yet, dialog will handle it
        }

        // Update preferences for standard ports
        sharedPreferences?.let { prefs ->
            setStringPrefValue(selectedValue, MvcPreference.SSL_PORT_SELECTION, prefs)
            val port = selectedValue.toIntOrNull() ?: 443
            setIntPrefValue(port, MvcPreference.SSL_PORT, prefs)
        }

        return super.callChangeListener(newValue)
    }

    private fun showCustomPortDialog() {
        val editText =
            EditText(context).apply {
                inputType = InputType.TYPE_CLASS_NUMBER
                hint = "Enter port number (1-65535)"
                sharedPreferences?.let { prefs ->
                    val currentPort = getIntPrefValue(MvcPreference.SSL_PORT, prefs)
                    setText(currentPort.toString())
                }
            }

        val container =
            LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                val padding = (16 * context.resources.displayMetrics.density).toInt()
                setPadding(padding, padding, padding, padding)
                addView(editText)
            }

        AlertDialog.Builder(context)
            .setTitle("Custom Port")
            .setView(container)
            .setPositiveButton("OK") { _, _ ->
                val portText = editText.text.toString()
                val port = portText.toIntOrNull()
                if (port != null && port in 1..65535) {
                    sharedPreferences?.let { prefs ->
                        setStringPrefValue(PORT_CUSTOM, MvcPreference.SSL_PORT_SELECTION, prefs)
                        setIntPrefValue(port, MvcPreference.SSL_CUSTOM_PORT, prefs)
                        setIntPrefValue(port, MvcPreference.SSL_PORT, prefs)
                    }
                    value = PORT_CUSTOM
                    updateSummaryFromPreferences()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateSummaryFromPreferences() {
        sharedPreferences?.let { prefs ->
            val selection = getStringPrefValue(MvcPreference.SSL_PORT_SELECTION, prefs)
            val port = getIntPrefValue(MvcPreference.SSL_PORT, prefs)

            summary =
                when (selection) {
                    PORT_CUSTOM -> "Custom: $port"
                    else -> port.toString()
                }
        }
    }
}
