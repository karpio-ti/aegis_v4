package com.example.cambio_precio_gondola

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import kotlin.experimental.or

class ZPLConverter {

    private var compressHex = true
    private var blacknessLimitPercentage = 50

    fun setCompressHex(compress: Boolean) {
        compressHex = compress
    }

    fun setBlacknessLimitPercentage(percentage: Int) {
        blacknessLimitPercentage = percentage
    }

    fun convertFromImage(bitmap: Bitmap, addHeaderFooter: Boolean): String {
        val width = bitmap.width
        val height = bitmap.height
        val bytesPerRow = (width + 7) / 8
        val imageData = ByteArray(height * bytesPerRow)

        Log.d("ZPLConverter", "Bitmap dimensions: width=$width, height=$height")

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = bitmap.getPixel(x, y)
                val gray = (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3
                Log.d("ZPLConverter", "Pixel [$x, $y]: Gray=$gray")
                if (gray < 255 * (blacknessLimitPercentage / 100.0)) {
                    val byteIndex = y * bytesPerRow + x / 8
                    imageData[byteIndex] = imageData[byteIndex] or (1 shl (7 - (x % 8))).toByte()
                }
            }
        }

        val hexString = imageData.joinToString("") { "%02X".format(it) }
        Log.d("ZPLConverter", "Generated Hex String: $hexString")

        val builder = StringBuilder()
        if (addHeaderFooter) {
            builder.append("^XA\n")
            builder.append("^PW433\n") // Ancho de la etiqueta en puntos (55 mm = 433 puntos)
            builder.append("^LL400\n") // Largo de la etiqueta en puntos (50 mm = 400 puntos)
            builder.append("^FO0,0^GFA,${imageData.size},${imageData.size},${bytesPerRow},$hexString\n")

        }
        if (hexString.isNotEmpty()) {
            builder.append("^FO0,0^GFA,${imageData.size},${imageData.size},${bytesPerRow},$hexString\n")
        }
        if (addHeaderFooter) {
            builder.append("^XZ")
        }
        return builder.toString()
    }
}