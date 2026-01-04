package kittoku.mvc.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// Light color scheme for SoftEther Connect.
private val LightColorScheme =
    lightColorScheme(
        primary = LightBlue40,
        onPrimary = Grey99,
        primaryContainer = LightBlue90,
        onPrimaryContainer = LightBlue10,
        secondary = Teal40,
        onSecondary = Grey99,
        secondaryContainer = Teal90,
        onSecondaryContainer = Teal10,
        tertiary = Amber40,
        onTertiary = Grey99,
        tertiaryContainer = Amber90,
        onTertiaryContainer = Amber10,
        error = Red40,
        onError = Grey99,
        errorContainer = Red90,
        onErrorContainer = Red10,
        background = Grey99,
        onBackground = Grey10,
        surface = Grey99,
        onSurface = Grey10,
        surfaceVariant = GreyVariant90,
        onSurfaceVariant = GreyVariant30,
        outline = GreyVariant40,
        outlineVariant = GreyVariant80,
        scrim = Grey10,
        inverseSurface = Grey20,
        inverseOnSurface = Grey95,
        inversePrimary = LightBlue80,
    )

// Dark color scheme for SoftEther Connect.
private val DarkColorScheme =
    darkColorScheme(
        primary = LightBlue80,
        onPrimary = LightBlue20,
        primaryContainer = LightBlue30,
        onPrimaryContainer = LightBlue90,
        secondary = Teal80,
        onSecondary = Teal20,
        secondaryContainer = Teal30,
        onSecondaryContainer = Teal90,
        tertiary = Amber80,
        onTertiary = Amber20,
        tertiaryContainer = Amber30,
        onTertiaryContainer = Amber90,
        error = Red80,
        onError = Red20,
        errorContainer = Red30,
        onErrorContainer = Red90,
        background = Grey10,
        onBackground = Grey90,
        surface = Grey10,
        onSurface = Grey90,
        surfaceVariant = GreyVariant30,
        onSurfaceVariant = GreyVariant80,
        outline = GreyVariant40,
        outlineVariant = GreyVariant30,
        scrim = Grey10,
        inverseSurface = Grey90,
        inverseOnSurface = Grey20,
        inversePrimary = LightBlue40,
    )

// SoftEther Connect Material 3 Theme.
// @param darkTheme Whether to use dark theme. Defaults to system setting.
// @param dynamicColor Whether to use dynamic color (Android 12+). Defaults to true.
// @param content The composable content to be themed.
@Composable
fun SoftEtherConnectTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
