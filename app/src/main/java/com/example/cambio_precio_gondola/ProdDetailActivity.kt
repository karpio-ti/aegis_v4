package com.example.cambio_precio_gondola

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ProdDetailActivity : AppCompatActivity() {
    // Declarar la variable del código de barras
    private var barcodeNumber: String = "210633000988" // Puedes recibir este valor dinámicamente

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_prod_detail)



        val button2: Button = findViewById(R.id.button2)
        button2.setOnClickListener {
            val intent = Intent(this, InstructionsActivity::class.java)
            intent.putExtra("INICIAR_ESCANER", true) // Pasar extra para iniciar el escáner automáticamente
            startActivity(intent)
        }







        // Recibir datos del intent
        val intent = intent
        val nombreProd = intent.getStringExtra("nombre_prod") ?: "Producto desconocido"
        val itemNbr = intent.getStringExtra("item_nbr") ?: "N/A"
        val peso = intent.getStringExtra("peso") ?: "0.0"
        val fecElab = intent.getStringExtra("fec_elab") ?: "N/A"
        val fecVenc = intent.getStringExtra("fec_venc") ?: "N/A"
        val precioAnt = intent.getStringExtra("precio_ant") ?: "0.0"
        val precioActual = intent.getStringExtra("precio_actual") ?: "0.0"
        val precioTotal = intent.getStringExtra("precio_total") ?: "0.0"
        var resolucion=intent.getStringExtra("resolucion")?:"N/A"
        val precioTotalNumero = intent.getIntExtra("precio_total_numero", 555)
        val brandName = intent.getStringExtra("brand_name") ?: "N/A"
        // Asignar los datos a los TextViews (asegúrate de tener TextViews correspondientes en tu layout)
        findViewById<TextView>(R.id.text_view_nombre_prod).text = nombreProd
        findViewById<TextView>(R.id.text_view_item_nbr).text = itemNbr
        findViewById<TextView>(R.id.text_view_peso_pieza).text = peso
        findViewById<TextView>(R.id.text_view_fec_elab).text = fecElab
        findViewById<TextView>(R.id.text_view_fec_venc).text = fecVenc
        findViewById<TextView>(R.id.text_view_precio_ant).text = precioAnt
        findViewById<TextView>(R.id.text_view_precio_act).text = precioActual
        findViewById<TextView>(R.id.text_view_precio_total).text = precioTotal


        // Botón para pre-visualizar la etiqueta
        val buttonPreview = findViewById<Button>(R.id.button_preview_label)

        // Asignar funcionalidad al botón
        buttonPreview.setOnClickListener {
            // Crear un intent para enviar los datos a QRResultActivity
            val intent = Intent(this, QRResultActivity::class.java)

            // Pasar los datos de la actividad actual al intent
            intent.putExtra("nombre_prod", nombreProd)
            intent.putExtra("item_nbr", itemNbr)
            intent.putExtra("peso", peso)
            intent.putExtra("fec_elab", fecElab)
            intent.putExtra("fec_venc", fecVenc)
            intent.putExtra("precio_ant", precioAnt)
            intent.putExtra("precio_actual", precioActual)
            intent.putExtra("precio_total", precioTotal)
            intent.putExtra("barcode_number", barcodeNumber) // También puedes pasar el número de código de barras si es necesario
            intent.putExtra("resolucion",resolucion)
            intent.putExtra("precio_total_numero",precioTotalNumero)
            intent.putExtra("brand_name", brandName)




            // Iniciar QRResultActivity
            startActivity(intent)
        }
    }
}