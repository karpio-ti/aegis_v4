package com.example.cambio_precio_gondola

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix

class QRResultActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_result)

        // Obtener el número de código de barras del intent
        val barcodeNumber = intent.getStringExtra("barcode_number")

        // Generar y mostrar el código de barras
        val barcodeImage = findViewById<ImageView>(R.id.barcode_image)
        if (barcodeNumber != null) {
            generateBarcode(barcodeNumber, barcodeImage)
        }
    }

    private fun generateBarcode(data: String, imageView: ImageView) {
        val writer = MultiFormatWriter()
        try {
            // Generar un BitMatrix para el código EAN-13
            val bitMatrix: BitMatrix = writer.encode(data, BarcodeFormat.EAN_13, 600, 300)
            val width = bitMatrix.width
            val height = bitMatrix.height

            // Crear un bitmap
            val bmp = Bitmap.createBitmap(width, height + 50, Bitmap.Config.ARGB_8888) // Espacio para el texto

            // Crear un canvas para dibujar
            val canvas = Canvas(bmp)

            // Cambiar el color de las barras a gris
            val paint = Paint().apply {
                color = Color.rgb(128, 128, 128) // Color gris similar al de la imagen
                isAntiAlias = true
            }

            // Dibujar el código de barras
            for (x in 0 until width) {
                // La altura de la barra puede ser mayor o menor según el bitMatrix
                val barHeight = if (bitMatrix[x, 0]) height.toFloat() else 0f
                canvas.drawRect(x.toFloat(), (height - barHeight), (x + 1).toFloat(), height.toFloat(), paint)
            }

            // Cambiar el color del texto a gris más claro
            val textPaint = Paint().apply {
                color = Color.rgb(150, 150, 150) // Gris claro para el texto
                textSize = 40f // Ajusta el tamaño del texto para hacerlo similar
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }

            // Dibujar el texto centrado
            canvas.drawText(data, (width / 2).toFloat(), (height + 40).toFloat(), textPaint)

            // Establecer el bitmap en el ImageView
            imageView.setImageBitmap(bmp)
        } catch (e: WriterException) {
            e.printStackTrace()
        }
    }


    companion object {
        private const val BLACK = -0x1000000 // Color negro
        private const val WHITE = -0x1 // Color blanco
    }
}