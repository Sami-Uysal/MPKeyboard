package com.samiuysal.keyboard.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme =
        darkColorScheme(
                primary = KeyboardBlue,
                secondary = KeyboardGray,
                tertiary = KeyboardGreen,
                background = KeyboardBlack,
                surface = KeyboardDarkGray,
                onPrimary = TextWhite,
                onSecondary = TextWhite,
                onTertiary = TextWhite,
                onBackground = TextWhite,
                onSurface = TextWhite
        )

private val LightColorScheme =
        lightColorScheme(
                primary = KeyboardBlue,
                secondary = KeyboardLightGray,
                tertiary = KeyboardGreen,
                background = KeyboardLightGray,
                surface = TextWhite,
                onPrimary = TextWhite,
                onSecondary = TextBlack,
                onTertiary = TextWhite,
                onBackground = TextBlack,
                onSurface = TextBlack
        )

@Composable
fun MPKeyboardTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(colorScheme = colorScheme, typography = KeyboardTypography, content = content)
}
