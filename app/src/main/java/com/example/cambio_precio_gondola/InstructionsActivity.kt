package com.example.cambio_precio_gondola

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import com.google.zxing.integration.android.IntentIntegrator
import android.widget.Toast
import com.journeyapps.barcodescanner.CaptureActivity

class QrCodeScannerActivity : CaptureActivity() {
    // Esta clase usa la configuración predeterminada de CaptureActivity
}

class InstructionsActivity : AppCompatActivity() {
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
                Toast.makeText(this, "Código escaneado: ${result.contents}", Toast.LENGTH_LONG).show()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
}