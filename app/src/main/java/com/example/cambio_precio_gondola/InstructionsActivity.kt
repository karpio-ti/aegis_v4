package com.example.cambio_precio_gondola

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.integration.android.IntentIntegrator
import com.journeyapps.barcodescanner.CaptureActivity

class InstructionsActivity : AppCompatActivity() {
    class QrCodeScannerActivity : CaptureActivity() {
        // Esta clase usa la configuración predeterminada de CaptureActivity
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_instructions)

        val buscarProducto: Button = findViewById(R.id.buscarProducto)
        buscarProducto.setOnClickListener {
            val integrator = IntentIntegrator(this)
            integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
            integrator.setPrompt("Escanea el código QR")
            integrator.setCameraId(0) // Cámara trasera
            integrator.setBeepEnabled(true)
            integrator.setBarcodeImageEnabled(true)

            // Aquí se utiliza la actividad personalizada para forzar orientación
            integrator.setCaptureActivity(QrCodeScannerActivity::class.java)
            integrator.initiateScan()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents == null) {
                Toast.makeText(this, "Escaneo cancelado", Toast.LENGTH_LONG).show()
            } else {
                // Código escaneado
                val qrData = result.contents

                // Supongamos que los datos del QR están separados por '|'
                val dataParts = qrData.split("|")

                // Dentro de la función que procesa los datos escaneados
                if (dataParts.size >= 6) {
                    // Extraer cada parte del código QR
                    val nombreProd = "V HUACHALOMO"  // Aquí podrías tener un nombre mapeado según el código
                    val fecElab = dataParts[0].take(6)  // Tomar solo los primeros 6 dígitos para la fecha (yymmdd)
                    val fecElabFormatted = "${fecElab.substring(4, 6)}/${fecElab.substring(2, 4)}/${fecElab.substring(0, 2)}"  // Formato dd/mm/yy
                    val itemNbr = "450224"  // Número de ítem fijo
                    val peso = (dataParts[2].toDoubleOrNull() ?: 0.0) / 1000  // Convertir peso a entero
                    val fecVenc = dataParts[1].take(6)   // Fecha de vencimiento
                    val fecVencFormatted = "${fecVenc.substring(4,6)}/${fecVenc.substring(2, 4)}/${fecVenc.substring(0, 2)}"  // Formato dd/mm/yy
                    val precioAnt = (dataParts[4].toDoubleOrNull() ?: 5490.0).toInt()  // Convertir precio anterior a entero
                    val precioActual = 5490.0.toInt()  // Precio actual fijo convertido a entero
                    val precioTotal = (precioActual * peso).toInt()  // Calcular precio total y convertirlo a entero

                    // Formatear los precios como $<precio>/Kg y el total como $<precio>
                    val precioAntFormatted = "$$precioAnt/Kg"
                    val precioActualFormatted = "$$precioActual/Kg"
                    val precioTotalFormatted = "$$precioTotal"

                    // Enviar datos a ProdDetailActivity
                    val intent = Intent(this, ProdDetailActivity::class.java)
                    intent.putExtra("nombre_prod", nombreProd)
                    intent.putExtra("item_nbr", itemNbr)
                    intent.putExtra("peso", "$peso Kg")  // Agregar 'Kg' al final del peso
                    intent.putExtra("fec_elab", fecElabFormatted)
                    intent.putExtra("fec_venc", fecVencFormatted)
                    intent.putExtra("precio_ant", precioAntFormatted)
                    intent.putExtra("precio_actual", precioActualFormatted)
                    intent.putExtra("precio_total", precioTotalFormatted)

                    startActivity(intent)
                } else {
                    Toast.makeText(this, "Datos del QR inválidos", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
}