package com.petcare.app.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.widget.ImageView
import androidx.core.net.toUri

object PetImageUtils {

    fun getSpeciesEmoji(species: String?): String {
        return when (species?.trim()?.lowercase()) {
            "dog", "puppy", "canine" -> "🐶"
            "cat", "kitten", "feline" -> "🐱"
            "rabbit", "bunny" -> "🐰"
            "hamster", "guinea pig", "rodent" -> "🐹"
            "bird", "parrot" -> "🐦"
            "fish", "goldfish" -> "🐠"
            "turtle", "tortoise" -> "🐢"
            "reptile", "lizard", "snake" -> "🦎"
            else -> "🐾"
        }
    }

    fun createEmojiBitmap(emoji: String, sizeDp: Int = 80, density: Float = 2.0f): Bitmap {
        val sizePx = (sizeDp * density).toInt().coerceAtLeast(100)
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Subtle soft background tint
        val bgPaint = Paint().apply {
            color = Color.parseColor("#273229") // Surface elevated
            isAntiAlias = true
        }
        canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, bgPaint)

        // Draw emoji centered
        val textPaint = Paint().apply {
            textSize = sizePx * 0.5f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        val fontMetrics = textPaint.fontMetrics
        val yOffset = sizePx / 2f - (fontMetrics.ascent + fontMetrics.descent) / 2f
        canvas.drawText(emoji, sizePx / 2f, yOffset, textPaint)

        return bitmap
    }

    fun loadPetAvatar(imageView: ImageView, imageUriString: String?, species: String?) {
        val density = imageView.resources.displayMetrics.density
        val emoji = getSpeciesEmoji(species)
        val fallbackBitmap = createEmojiBitmap(emoji, sizeDp = 80, density = density)

        if (!imageUriString.isNullOrEmpty()) {
            try {
                val uri = imageUriString.toUri()
                imageView.setImageURI(null) // Clear previous
                imageView.setImageURI(uri)
                if (imageView.drawable == null) {
                    imageView.setImageBitmap(fallbackBitmap)
                }
            } catch (e: Exception) {
                imageView.setImageBitmap(fallbackBitmap)
            }
        } else {
            imageView.setImageBitmap(fallbackBitmap)
        }
    }

    fun loadUserAvatar(imageView: ImageView, imageUriString: String?) {
        val density = imageView.resources.displayMetrics.density
        val fallbackBitmap = createEmojiBitmap("👤", sizeDp = 80, density = density)

        if (!imageUriString.isNullOrEmpty()) {
            try {
                val uri = imageUriString.toUri()
                imageView.setImageURI(null)
                imageView.setImageURI(uri)
                if (imageView.drawable == null) {
                    imageView.setImageBitmap(fallbackBitmap)
                }
            } catch (e: Exception) {
                imageView.setImageBitmap(fallbackBitmap)
            }
        } else {
            imageView.setImageBitmap(fallbackBitmap)
        }
    }
}