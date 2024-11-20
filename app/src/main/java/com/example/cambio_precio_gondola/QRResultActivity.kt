package com.example.cambio_precio_gondola

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.zebra.sdk.comm.BluetoothConnection
import com.zebra.sdk.comm.ConnectionException
import java.io.ByteArrayOutputStream

class QRResultActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_result)

        // Obtener datos del intent
        val resolucion = intent.getStringExtra("resolucion")?.replace("|", " ") ?: "No disponible"
        val nombreProd = intent.getStringExtra("nombre_prod") ?: "Producto no disponible"
        val itemNbr = intent.getStringExtra("item_nbr") ?: "No disponible"
        val peso = intent.getStringExtra("peso") ?: "0.0 Kg"
        val fecElab = intent.getStringExtra("fec_elab") ?: "Fecha no disponible"
        val fecVenc = intent.getStringExtra("fec_venc") ?: "Fecha no disponible"
        val precioActual = intent.getStringExtra("precio_actual") ?: "0.0"
        val precioTotal = intent.getStringExtra("precio_total") ?: "0.0"
        val barcodeNumber = intent.getStringExtra("barcode_number") ?: "No disponible"

        // Asignar los datos a los TextViews
        findViewById<TextView>(R.id.resolucion).text = resolucion
        findViewById<TextView>(R.id.nombreProd).text = nombreProd
        findViewById<TextView>(R.id.itemNbr).text = itemNbr
        findViewById<TextView>(R.id.peso).text = peso
        findViewById<TextView>(R.id.fecElab).text = fecElab
        findViewById<TextView>(R.id.fecVenc).text = fecVenc
        findViewById<TextView>(R.id.precioActual).text = precioActual
        findViewById<TextView>(R.id.precioTotal).text = precioTotal

        // Generar el código de barras si barcodeNumber no es nulo
        val barcodeImage = findViewById<ImageView>(R.id.barcode_image)
        if (barcodeNumber != "No disponible") {
            generateBarcode(barcodeNumber, barcodeImage)
        }

        // Botón de impresión
        val buttonPrint = findViewById<Button>(R.id.buttonPrint)
        buttonPrint.setOnClickListener {
            // Verifica que los valores no sean nulos antes de llamar a printLabel
            if (barcodeNumber != "No disponible" && peso != "0.0 Kg" && precioActual != "0.0" && nombreProd != "Producto no disponible" && fecElab != "Fecha no disponible" && fecVenc != "Fecha no disponible" && precioTotal != "0.0") {
                // Llamada correcta con todos los parámetros
                printLabel(barcodeNumber, peso, precioActual, nombreProd, fecElab, fecVenc, precioTotal)
            } else {
                Toast.makeText(this, "Datos incompletos para imprimir la etiqueta", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun generateBarcode(data: String, imageView: ImageView) {
        val writer = MultiFormatWriter()
        try {
            val bitMatrix: BitMatrix = writer.encode(data, BarcodeFormat.EAN_13, 600, 300)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bmp = Bitmap.createBitmap(width, height + 50, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            val paint = Paint().apply {
                color = Color.rgb(128, 128, 128)
                isAntiAlias = true
            }

            for (x in 0 until width) {
                val barHeight = if (bitMatrix[x, 0]) height.toFloat() else 0f
                canvas.drawRect(x.toFloat(), (height - barHeight), (x + 1).toFloat(), height.toFloat(), paint)
            }

            val textPaint = Paint().apply {
                color = Color.rgb(150, 150, 150)
                textSize = 40f
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }

            canvas.drawText(data, (width / 2).toFloat(), (height + 40).toFloat(), textPaint)
            imageView.setImageBitmap(bmp)
        } catch (e: WriterException) {
            e.printStackTrace()
        }
    }

    private fun printLabel(barcodeNumber: Number, peso: String, precioActual: String, nombreProd: String, fecElab: String, fecVenc: String, precioTotal: String) {
        val connection = BluetoothConnection("AC:3F:A4:B8:DE:A1") // Cambia esta dirección MAC por la correcta
        try {
            connection.open()

            // Limpiar datos de entrada
            val cleanBarcode = barcodeNumber.trim()
            val cleanPeso = peso.trim()
            val cleanPrecioActual = precioActual.trim()
            val cleanNombreProd = nombreProd.trim()
            val cleanFecElab = fecElab.trim()
            val cleanFecVenc = fecVenc.trim()
            val cleanPrecioTotal = precioTotal.trim()

            // Crear ZPL para imprimir la etiqueta completa
            val zplCommand = """
    ^XA
    ^PW800
    ^LL600
    ^FO50,50^BQN,2,10^FDQA,$cleanBarcode^FS  // Código de barras QR
    ^FO50,160^A0N,30,30^FD$cleanNombreProd^FS   // Nombre del producto
    ^FO50,200^A0N,25,25^FDFecha ELAB: $cleanFecElab^FS
    ^FO50,240^A0N,25,25^FDFecha VENC: $cleanFecVenc^FS
    ^FO50,280^A0N,25,25^FDPeso: $cleanPeso^FS
    ^FO50,320^A0N,25,25^FDPrecio: $cleanPrecioActual^FS
    ^FO50,360^A0N,25,25^FDPrecio Total: $cleanPrecioTotal^FS
    ^XZ
    """.trimIndent()

            connection.write(zplCommand.toByteArray())
            connection.close()
            Toast.makeText(this, "Etiqueta enviada a imprimir", Toast.LENGTH_SHORT).show()
        } catch (e: ConnectionException) {
            e.printStackTrace()
            Toast.makeText(this, "Error al enviar la etiqueta", Toast.LENGTH_SHORT).show()
        } finally {
            if (connection.isConnected) {
                connection.close()
            }
        }
    }
}