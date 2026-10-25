package com.saikalyan.jobmate.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val JobMateLight = lightColorScheme(
    primary = Color(0xFF315F52), onPrimary = Color.White,
    primaryContainer = Color(0xFFD5E9DF), onPrimaryContainer = Color(0xFF153A30),
    secondary = Color(0xFF8B5E34), onSecondary = Color.White,
    secondaryContainer = Color(0xFFF4DFC9),
    background = Color(0xFFF6F7F3), onBackground = Color(0xFF1D2723),
    surface = Color(0xFFFFFEFC), onSurface = Color(0xFF1D2723),
    surfaceVariant = Color(0xFFE9EEEA), onSurfaceVariant = Color(0xFF4B5A53),
    outline = Color(0xFF7D8B83), error = Color(0xFFB3261E),
)

private val JobMateDark = darkColorScheme(
    primary = Color(0xFFA8D2BF), onPrimary = Color(0xFF12392D),
    primaryContainer = Color(0xFF315F52), onPrimaryContainer = Color(0xFFD5E9DF),
    secondary = Color(0xFFE8BC91),
    background = Color(0xFF121815), surface = Color(0xFF1B2420),
    surfaceVariant = Color(0xFF2B3731), onSurface = Color(0xFFE4ECE7),
    onSurfaceVariant = Color(0xFFC1CCC5),
)

private val JobMateShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun JobMateTheme(
    darkTheme: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val useDarkTheme = darkTheme ?: isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (useDarkTheme) JobMateDark else JobMateLight,
        shapes = JobMateShapes,
        content = content,
    )
}
