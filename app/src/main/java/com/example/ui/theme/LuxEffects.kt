package com.example.ui.theme

import android.graphics.Bitmap
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import java.util.Random

object LuxEffects {
    private var cachedGrainBitmap: ImageBitmap? = null

    private fun getGrainBitmap(): ImageBitmap {
        if (cachedGrainBitmap == null) {
            val width = 256
            val height = 256
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val pixels = IntArray(width * height)
            val random = Random(42) // Fixed seed for stable appearance
            for (i in pixels.indices) {
                // Subtle noise: vary between black and white with very low alpha
                val isLight = random.nextBoolean()
                val alpha = random.nextInt(12) // Very subtle: 0 to 11
                if (isLight) {
                    pixels[i] = android.graphics.Color.argb(alpha, 255, 255, 255)
                } else {
                    pixels[i] = android.graphics.Color.argb(alpha, 0, 0, 0)
                }
            }
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            cachedGrainBitmap = bitmap.asImageBitmap()
        }
        return cachedGrainBitmap!!
    }

    val grainBrush: Brush = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.01f),
            Color.Transparent,
            Color.Black.copy(alpha = 0.02f)
        )
    )
}

fun Modifier.grainEffect() = drawWithContent {
    drawContent()
    drawRect(
        brush = LuxEffects.grainBrush,
        size = size
    )
}

fun Modifier.luxBorder(shape: Shape): Modifier = this.border(
    width = 1.dp,
    brush = Brush.linearGradient(
        0.0f to Color.White.copy(alpha = 0.25f),
        0.2f to Color.White.copy(alpha = 0.05f),
        0.8f to Color.Transparent,
        1.0f to Color.White.copy(alpha = 0.1f)
    ),
    shape = shape
)
