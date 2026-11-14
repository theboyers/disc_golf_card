package com.discgolf.card.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class AppExtraColors(
    val success: Color,
    val successContainer: Color,
    val onSuccessContainer: Color
)

private val LightExtraColors = AppExtraColors(
    success = Color(0xFF15803D),
    successContainer = Color(0xFFDCFCE7),
    onSuccessContainer = Color(0xFF14532D)
)

private val DarkExtraColors = AppExtraColors(
    success = Color(0xFF4ADE80),
    successContainer = Color(0xFF14351F),
    onSuccessContainer = Color(0xFFDCFCE7)
)

val LocalAppExtraColors = staticCompositionLocalOf { LightExtraColors }

val MaterialTheme.appColors: AppExtraColors
    @Composable
    @ReadOnlyComposable
    get() = LocalAppExtraColors.current

@Composable
fun DiscGolfCardTheme(darkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme(), content: @Composable () -> Unit) {
    androidx.compose.runtime.CompositionLocalProvider(
        LocalAppExtraColors provides if (darkTheme) DarkExtraColors else LightExtraColors
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}
