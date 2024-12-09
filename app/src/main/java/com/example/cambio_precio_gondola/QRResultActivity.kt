package com.example.cambio_precio_gondola

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.zebra.sdk.comm.BluetoothConnection
import com.zebra.sdk.comm.ConnectionException
import com.zebra.sdk.printer.ZebraPrinterFactory
import com.zebra.sdk.printer.PrinterLanguage
import com.zebra.sdk.printer.ZebraPrinter
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.cambio_precio_gondola.MainActivity.BluetoothManager.macAddress
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


// Importar la clase ZPLConverter
import com.example.cambio_precio_gondola.ZPLConverter

class QRResultActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_result)
        // Obtener el TextView
        val fecImpresion: TextView = findViewById(R.id.fecImpresion)
        // Obtener la fecha y hora actual
        val currentDateTime = getCurrentDateTime()
        val macAddress = MainActivity.BluetoothManager.macAddress
        // Obtener datos del intent
        fecImpresion.text = "Fecha de impresión: $currentDateTime"
        val resolucion = intent.getStringExtra("resolucion")?.replace("|", " ") ?: "No disponible"
        val nombreProd = intent.getStringExtra("nombre_prod")
        val itemNbr = intent.getStringExtra("item_nbr")
        val peso = intent.getStringExtra("peso")
        val fecElab = intent.getStringExtra("fec_elab")
        val fecVenc = intent.getStringExtra("fec_venc")
        val precioActual = intent.getStringExtra("precio_actual")
        val precioTotal = intent.getStringExtra("precio_total")
        val brandName = intent.getStringExtra("brand_name")
        val precioTotalNumero = intent.getIntExtra("precio_total_numero", 555)
        val barcodeNumber = intent.getStringExtra("barcode_number")
        // Asegurarse de que el precioTotal sea un número válido y formatearlo a 5 dígitos
        val precioTotalStr = precioTotalNumero.toString().padStart(5, '0')
        val ItemTotalStr = itemNbr.toString().padStart(5, '0')
        // Asignar los datos a los TextViews
        findViewById<TextView>(R.id.resolucion).text = resolucion
        findViewById<TextView>(R.id.nombreProd).text = nombreProd

        findViewById<TextView>(R.id.peso).text = peso
        findViewById<TextView>(R.id.fecElab).text = fecElab
        findViewById<TextView>(R.id.fecVenc).text = fecVenc
        findViewById<TextView>(R.id.precioActual).text = precioActual
        findViewById<TextView>(R.id.precioTotal).text = precioTotal
        findViewById<TextView>(R.id.brandName).text = brandName
        // val textView: TextView = findViewById(R.id.textViewBarcode)


        //val textView: TextView = findViewById(R.id.barcode_)
        // findViewById/(R.id.barcode_image)
        // Cargar la fuente desde assets
        val typeface = Typeface.createFromAsset(assets, "fonts/LibreBarcodeEAN13Text-Regular.ttf")

        // Asignar la fuente al TextView
        //textView.typeface = typeface

        // Asignar el número del código de barras al TextView
       // val barcodeData = "210633000984" // Un ejemplo de número para EAN13 (sin el dígito de control)
// Asegurarse de que el precioTotal tenga 5 dígitos, rellenando con ceros a la izquierda si es necesario


// Generar el barcodeData con el formato requerido

        //val barcodeData = "2$itemNbr" + "0" + precioTotalStr
        //val barcodeData = "2$itemNbr" + "0" + precioTotalFormatted
        // Generar el código de barras EAN13 con el número proporcionado
        val barcodeData = "2$ItemTotalStr" + "0" + precioTotalStr
                // val barcode = generateEAN13(barcodeData)


        // Generar código de barras como texto
        val barcodeText = generateEAN13(barcodeData)

        // Convertir el texto del código de barras a imagen, rotarlo y escalarlo
        val barcodeBitmap = textToBitmap(barcodeText, typeface, textSize = 1000f)?.let { originalBitmap ->
            val rotatedBitmap = rotateBitmap(originalBitmap, 90f) // Rotar 90 grados
            scaleBitmap(rotatedBitmap, rotatedBitmap.width, 5000) // Escalar a 800 px de altura

        }


// Mostrar la imagen rotada y escalada en el ImageView
        val barcodeImageView: ImageView = findViewById(R.id.barcode_image)
        if (barcodeBitmap != null) {
            barcodeImageView.setImageBitmap(barcodeBitmap)
        } else {
            Toast.makeText(this, "Error al generar el código de barras", Toast.LENGTH_SHORT).show()
        }





        // Configurar botón para capturar el layout y enviar a imprimir
        val captureButton = findViewById<Button>(R.id.buttonPrint)
        captureButton.setOnClickListener {
            val layoutEtiquetaFinal = findViewById<ConstraintLayout>(R.id.layoutEtiquetaFinal)
            val bitmap = captureLayoutAsBitmap(layoutEtiquetaFinal)
            val zplConverter = ZPLConverter()
            val zplCode = zplConverter.convertFromImage(bitmap, addHeaderFooter = true)

            // Enviar el código ZPL a la impresora
            sendZPLToPrinter(zplCode)
            // Lógica para convertir a ZPL y enviar a la impresora
            Toast.makeText(this, "Captura realizada", Toast.LENGTH_SHORT).show()
        }




    }


    private fun getCurrentDateTime(): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return dateFormat.format(Date())
    }


    // Función para capturar un layout como Bitmap
    fun captureLayoutAsBitmap(view: View): Bitmap {
        // Definir el tamaño exacto de la etiqueta en píxeles
        val widthPx = 433
        val heightPx = 400

        // Crear un Bitmap con las dimensiones deseadas
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Dibujar fondo blanco
        canvas.drawColor(Color.WHITE)
        val marginTop = 100f

        // Escalar el layout al tamaño del Bitmapx|x
        val scaleX = widthPx / view.width.toFloat()
        val scaleY = heightPx / view.height.toFloat()
        canvas.scale(scaleX, scaleY)
        canvas.translate(0f, marginTop)
        // Dibujar el contenido del layout
        view.draw(canvas)

        return bitmap
    }

    // Función para convertir texto a Bitmap usando una fuente específica
    private fun textToBitmap(text: String, typeface: Typeface, textSize: Float): Bitmap? {
        return try {
            val paint = Paint().apply {
                isAntiAlias = true
                this.typeface = typeface
                this.textSize = textSize
                color = Color.BLACK
                textAlign = Paint.Align.LEFT
            }

            val baseline = (-paint.ascent()).toInt() // Altura de la fuente
            val width = (paint.measureText(text) + 0.5f).toInt() // Ancho del texto
            val height = (baseline + paint.descent()).toInt()
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawText(text, 0f, baseline.toFloat(), paint)
            bitmap
        } catch (e: Exception) {
            Log.e("QRResultActivity", "Error al convertir texto a Bitmap: ${e.message}")
            null
        }
    }

    // Función para rotar un Bitmap
    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    // Función para escalar un Bitmap
    private fun scaleBitmap(bitmap: Bitmap, newWidth: Int, newHeight: Int): Bitmap {

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    // Tu función existente para generar el código EAN13
    private fun generateEAN13(chaine: String): String {
        var ean13 = ""
        if (chaine.length == 12 && chaine.all { it.isDigit() }) {
            var checksum = 0
            for (i in 11 downTo 0 step 2) checksum += chaine[i].digitToInt()
            checksum *= 3
            for (i in 10 downTo 0 step 2) checksum += chaine[i].digitToInt()
            val controlDigit = (10 - (checksum % 10)) % 10
            val completeChain = chaine + controlDigit

            var codeBarre = completeChain[0].toString()
            codeBarre += (65 + completeChain[1].digitToInt()).toChar()
            val firstDigit = completeChain[0].digitToInt()
            for (i in 2..6) {
                val tableA: Boolean = when (i) {
                    2 -> firstDigit in 0..3
                    3 -> firstDigit in setOf(0, 4, 7, 8)
                    4 -> firstDigit in setOf(0, 1, 4, 5, 9)
                    5 -> firstDigit in setOf(0, 2, 5, 6, 7)
                    6 -> firstDigit in setOf(0, 3, 6, 8, 9)
                    else -> false
                }
                codeBarre += if (tableA) {
                    (65 + completeChain[i].digitToInt()).toChar()
                } else {
                    (75 + completeChain[i].digitToInt()).toChar()
                }
            }
            codeBarre += "*"
            for (i in 7..12) codeBarre += (97 + completeChain[i].digitToInt()).toChar()
            codeBarre += "+"
            ean13 = codeBarre
        }
        return ean13
    }
    private fun sendZPLToPrinter(zplCode: String) {
        Thread {
            try {
                // Conexión Bluetooth
                val connection = BluetoothConnection(macAddress) // Dirección MAC de la impresora
                connection.open()

                // Obtener la impresora Zebra
                val printer = ZebraPrinterFactory.getInstance(PrinterLanguage.ZPL, connection)

                // Enviar el código ZPL a la impresora
                printer.sendCommand(zplCode)

                // Cerrar la conexión
                connection.close()

                Log.d("QRResultActivity", "Código ZPL enviado a imprimir.")
            } catch (e: ConnectionException) {
                e.printStackTrace()
                Log.e("QRResultActivity", "Error al conectar o imprimir: ${e.message}")
            }
        }.start()
    }











































}