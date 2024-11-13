package com.example.cambio_precio_gondola
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.integration.android.IntentIntegrator
import com.journeyapps.barcodescanner.CaptureActivity
import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import android.widget.TextView

class InstructionsActivity : AppCompatActivity() {
    class QrCodeScannerActivity : CaptureActivity() {
        // Esta clase usa la configuración predeterminada de CaptureActivity
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_instructions)
        val numeroIngresado = intent.getStringExtra("numero_ingresado")
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
                val dataParts = qrData.split("-")
                // La clase de datos para almacenar la información combinada
                data class DatosItem(
                    val storeNbr: Int,
                    val plu_nbr: Int,
                    val itemDesc: String?,
                    val brandName: String?,
                    val sellPrice: Int?,
                    val procedencia: String?,
                    val resolucion: String?,
                    val diasPerecibilidad: Int?
                )

                // Función para leer el JSON desde los assets
                fun leerJsonDesdeAssets(context: Context, archivo: String): String {
                    val inputStream = context.assets.open(archivo)
                    val bufferedReader = BufferedReader(InputStreamReader(inputStream))
                    return bufferedReader.use { it.readText() }
                }

                // Función para buscar un item en product.json utilizando plu_nbr
                fun buscarEnProduct(context: Context, pluNbr: Int): Map<String, Any?>? {
                    val jsonString = leerJsonDesdeAssets(context, "product.json")
                    val jsonArray = JSONArray(jsonString)

                    val camposDeseadosProduct = listOf("store_nbr", "plu_nbr", "item1_desc", "brand_name", "sell_price")

                    for (i in 0 until jsonArray.length()) {
                        val item = jsonArray.getJSONObject(i)
                        if (item.getInt("plu_nbr") == pluNbr) {  // Usamos plu_nbr como clave de búsqueda
                            val resultado = mutableMapOf<String, Any?>()
                            for (campo in camposDeseadosProduct) {
                                if (item.has(campo)) {
                                    resultado[campo] = item.get(campo)
                                }
                            }
                            return resultado
                        }
                    }
                    return null
                }

                // Función para buscar un item en info.json utilizando plu_nbr
                fun buscarEnInfo(context: Context, pluNbr: Int): Map<String, Any?>? {
                    val jsonString = leerJsonDesdeAssets(context, "info.json")
                    val jsonArray = JSONArray(jsonString)

                    val camposDeseadosInfo = listOf("procedencia", "resolucion", "diasPerecibilidad")

                    for (i in 0 until jsonArray.length()) {
                        val item = jsonArray.getJSONObject(i)
                        if (item.getInt("plu_nbr") == pluNbr) {  // Usamos plu_nbr como clave de búsqueda
                            val resultado = mutableMapOf<String, Any?>()
                            for (campo in camposDeseadosInfo) {
                                if (item.has(campo)) {
                                    resultado[campo] = item.get(campo)
                                }
                            }
                            return resultado
                        }
                    }
                    return null
                }

                // Función para combinar la información de ambos archivos JSON
                fun buscarYCombinarInformacion(context: Context, pluNbr: Int): DatosItem? {
                    val itemProduct = buscarEnProduct(context, pluNbr)
                    val itemInfo = buscarEnInfo(context, pluNbr)

                    // Si encontramos los datos en ambos archivos, los combinamos en un objeto DatosItem
                    return if (itemProduct != null && itemInfo != null) {
                        DatosItem(
                            storeNbr = itemProduct["store_nbr"] as? Int ?: -1,
                            plu_nbr = itemProduct["plu_nbr"] as? Int ?: -1,  // Usamos plu_nbr de product.json
                            itemDesc = itemProduct["item1_desc"] as? String,
                            brandName = itemProduct["brand_name"] as? String,
                            sellPrice = itemProduct["sell_price"] as? Int,
                            procedencia = itemInfo["procedencia"] as? String,
                            resolucion = itemInfo["resolucion"] as? String,
                            diasPerecibilidad = itemInfo["diasPerecibilidad"] as? Int
                        )
                    } else {
                        null
                    }
                }
                if (dataParts.size >= 6) {
                    // Extraer cada parte del código QR
                    val pluNbr = dataParts[1].substring(1, 6).toInt()
                    val itemNbr = dataParts[1].substring(1, 6)  // Tomar los dígitos de la posición 1 a 5 (por ejemplo, "10633")
                    val resultado = buscarYCombinarInformacion(this,pluNbr )
                    val nombreProd =  resultado?.itemDesc // Aquí podrías tener un nombre mapeado según el código
                    val fecElab = dataParts[0].take(6)  // Tomar solo los primeros 6 dígitos para la fecha (yymmdd)
                    val fecElabFormatted = "${fecElab.substring(4, 6)}/${fecElab.substring(2, 4)}/${fecElab.substring(0, 2)}"  // Formato dd/mm/yy
                    val peso = (dataParts[5].toDoubleOrNull() ?: 0.0) / 1000  // Convertir peso a entero
                    val fecVenc = dataParts[3].take(6)   // Fecha de vencimiento
                    val fecVencFormatted = "${fecVenc.substring(4,6)}/${fecVenc.substring(2, 4)}/${fecVenc.substring(0, 2)}"  // Formato dd/mm/yy
                    val precioAnt = (dataParts[4].toDoubleOrNull() ?: 5490.0).toInt()  // Convertir precio anterior a entero
                    val precioActual = resultado?.sellPrice // Precio actual fijo convertido a entero
                    val precioTotal = (precioActual?.times(peso))?.toInt()

                  //  val precioTotal = (precioActual * peso).toInt()  // Calcular precio total y convertirlo a entero

                    // Formatear los precios como $<precio>/Kg y el total como $<precio>
                    val precioAntFormatted = "$$precioAnt/Kg"
                    val precioActualFormatted = "$$precioActual/Kg"
                    val precioTotalFormatted = "$$precioTotal"

                    // Enviar datos a ProdDetailActivity
                    val intent = Intent(this, ProdDetailActivity::class.java)
                    intent.putExtra("resolucion",resultado?.resolucion)
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