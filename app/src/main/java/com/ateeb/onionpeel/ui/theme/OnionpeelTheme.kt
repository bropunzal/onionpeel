package com.ateeb.onionpeel.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// COROS x SATISFY APEX 4-inspired: titanium black canvas, burnt olive, khaki type, sage accents.
val OnionInk = Color(0xFF0E0E0C)
val OnionCream = Color(0xFFC9BFA3)
val OnionCreamMuted = Color(0x998A8270)
val OnionCoral = Color(0xFF6D6848)
val OnionMint = Color(0xFF9CB896)
val OnionSurface = Color(0xFF1A1916)
val OnionStroke = Color(0x335A5744)
val OnionTitanium = Color(0xFF2A2926)

private val OnionColorScheme = darkColorScheme(
    primary = OnionCoral,
    onPrimary = OnionInk,
    background = OnionInk,
    onBackground = OnionCream,
    surface = OnionSurface,
    onSurface = OnionCream,
    surfaceVariant = Color(0xFF22211C),
    onSurfaceVariant = OnionCreamMuted,
    outline = OnionStroke,
)

object OnionType {
    val mono = FontFamily.Monospace

    val hero = TextStyle(
        fontFamily = mono,
        fontSize = 44.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 4.sp,
        lineHeight = 48.sp,
    )
    val metric = TextStyle(
        fontFamily = mono,
        fontSize = 22.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 1.sp,
        lineHeight = 28.sp,
    )
    val section = TextStyle(
        fontFamily = mono,
        fontSize = 10.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 2.5.sp,
    )
    val body = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 20.sp,
    )
}

@Composable
fun OnionpeelTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = OnionColorScheme,
        content = content,
    )
}
