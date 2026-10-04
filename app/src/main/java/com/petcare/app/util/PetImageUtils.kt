package com.petcare.app.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import java.io.File
import java.util.UUID
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.petcare.app.R

object PetImageUtils {

    /**
     * Copies a picked image into app storage and returns a file:// URI string that stays
     * readable after restarts (content:// URIs from the picker lose their permission).
     */
    fun copyToAppStorage(context: Context, source: Uri): String? = try {
        val file = newImageFile(context)
        context.contentResolver.openInputStream(source)?.use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
        Uri.fromFile(file).toString()
    } catch (e: Exception) {
        null
    }

    /** Saves a camera preview bitmap into app storage and returns its file:// URI string. */
    fun saveBitmapToAppStorage(context: Context, bitmap: Bitmap): String? = try {
        val file = newImageFile(context)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        Uri.fromFile(file).toString()
    } catch (e: Exception) {
        null
    }

    private fun newImageFile(context: Context): File {
        val dir = File(context.filesDir, "images").apply { mkdirs() }
        return File(dir, "${UUID.randomUUID()}.jpg")
    }

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

    fun createEmojiBitmap(emoji: String, sizeDp: Int = 80, density: Float = 2.0f, backgroundColor: Int = Color.WHITE): Bitmap {
        val sizePx = (sizeDp * density).toInt().coerceAtLeast(100)
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Fill the square; the ImageView's shape (rounded or circular) clips it.
        canvas.drawColor(backgroundColor)

        // Draw emoji centered
        val textPaint = Paint().apply {
            textSize = sizePx * 0.55f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        val fontMetrics = textPaint.fontMetrics
        val yOffset = sizePx / 2f - (fontMetrics.ascent + fontMetrics.descent) / 2f
        canvas.drawText(emoji, sizePx / 2f, yOffset, textPaint)

        return bitmap
    }

    fun loadPetAvatar(imageView: ImageView, imageUriString: String?, species: String?, sizeDp: Int = 80) {
        val density = imageView.resources.displayMetrics.density
        val emoji = getSpeciesEmoji(species)
        val fallbackBitmap = createEmojiBitmap(emoji, sizeDp = sizeDp, density = density,
            backgroundColor = ContextCompat.getColor(imageView.context, R.color.primary_container))

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
        val fallbackBitmap = createEmojiBitmap("👤", sizeDp = 80, density = density,
            backgroundColor = ContextCompat.getColor(imageView.context, R.color.secondary_container))

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