package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = BrandBluePrimaryDark,
    onPrimary = BrandBlueOnPrimaryDark,
    primaryContainer = BrandBlueContainerDark,
    onPrimaryContainer = BrandBlueOnContainerDark,
    secondary = BrandCyanSecondaryDark,
    onSecondary = BrandCyanOnSecondaryDark,
    secondaryContainer = BrandCyanContainerDark,
    onSecondaryContainer = BrandCyanOnContainerDark,
    tertiary = BrandAccentTertiary
)

private val LightColorScheme = lightColorScheme(
    primary = BrandBluePrimary,
    onPrimary = BrandBlueOnPrimary,
    primaryContainer = BrandBlueContainer,
    onPrimaryContainer = BrandBlueOnContainer,
    secondary = BrandCyanSecondary,
    onSecondary = BrandCyanOnSecondary,
    secondaryContainer = BrandCyanContainer,
    onSecondaryContainer = BrandCyanOnContainer,
    tertiary = BrandAccentTertiary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
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
        content = content
    )
}
