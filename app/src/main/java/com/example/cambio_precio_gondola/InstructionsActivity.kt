package com.example.cambio_precio_gondola
import android.app.AlertDialog
import android.app.ProgressDialog
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
import java.io.File
import java.io.FileNotFoundException
import java.util.concurrent.TimeUnit
import android.os.Handler
import android.os.Looper
import android.util.Log
//import com.example.cambio_precio_gondola.MainActivity.TokenManager.TokenCallback
import java.io.BufferedWriter
import java.io.FileWriter
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.CountDownLatch


class InstructionsActivity : AppCompatActivity() {
    class QrCodeScannerActivity : CaptureActivity() {
        // Esta clase usa la configuración predeterminada de CaptureActivity
    }


    private lateinit var enterButton: Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setContentView(R.layout.activity_instructions)
        fun iniciarEscaner() {
            val integrator = IntentIntegrator(this)
            integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
            integrator.setPrompt("Escanea el código QR")
            integrator.setCameraId(0) // Cámara trasera
            integrator.setBeepEnabled(true)
            integrator.setBarcodeImageEnabled(true)

            // Personaliza la actividad de captura si es necesario
            integrator.setCaptureActivity(QrCodeScannerActivity::class.java)
            integrator.initiateScan()
        }

        val buscarProducto: Button = findViewById(R.id.buscarProducto)
        buscarProducto.setOnClickListener {
            iniciarEscaner()
        }

        // Verifica si se debe iniciar el escáner automáticamente
        val iniciarEscanerAutomaticamente = intent.getBooleanExtra("INICIAR_ESCANER", false)
        if (iniciarEscanerAutomaticamente) {
            iniciarEscaner()
        }

    // Método para iniciar el escáner

        val numeroIngresado = intent.getIntExtra("numero_ingresado", -1)

        val actualizarDataButton: Button = findViewById(R.id.actualizarDataButton)

        // Configuración del botón para llamar a la función al hacer clic
        actualizarDataButton.setOnClickListener {

            fetchTokensAndData(
                numeroIngresado

            )
        }


        // Obtener el TextView






        // Mostrar el número de local en el TextView
        val localNumberTextView: TextView = findViewById(R.id.localNumberTextView)
        localNumberTextView.text = "Local: $numeroIngresado" // Mostrar "Local: numero"

    }

    //funcion para obtener los minutos pasados
    fun obtenerMinutosDesdeUltimaModificacion(context: Context, archivo: String): Long? {
        val file = File(context.filesDir, archivo) // Ruta específica del archivo
        return if (file.exists()) {
            val ultimaModificacion = file.lastModified() // Tiempo de última modificación en milisegundos
            val tiempoActual = System.currentTimeMillis() // Tiempo actual en milisegundos
            val tiempoDiferencia = tiempoActual - ultimaModificacion
            TimeUnit.MILLISECONDS.toMinutes(tiempoDiferencia) // Convertir milisegundos a minutos
        } else {
            null // Devuelve null si el archivo no existe
        }
    }

    //funciontoken manager
    class TokenManager {

        // Interface para manejar el resultado de las solicitudes
        interface TokenCallback {
            fun onSuccess(token: String)
            fun onError(error: String)
        }






        // Método para obtener el token asincrónicamente
        fun fetchTokenAsync(
            serviceUrl: String,
            username: String,
            password: String,
            callback: TokenCallback
        ) {
            Thread {
                var connection: HttpURLConnection? = null
                try {
                    val url = URL(serviceUrl)
                    connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "POST"
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.doOutput = true
                    connection.connectTimeout = 5000 // Tiempo de espera para conectar (5 segundos)
                    connection.readTimeout = 5000 // Tiempo de espera para leer la respuesta (5 segundos)

                    // Crear el cuerpo de la solicitud
                    val requestBody = JSONObject().apply {
                        put("Username", username)
                        put("Password", password)
                    }.toString()

                    // Enviar el cuerpo de la solicitud
                    connection.outputStream.use { outputStream ->
                        BufferedWriter(OutputStreamWriter(outputStream)).use { writer ->
                            writer.write(requestBody)
                            writer.flush()
                        }
                    }

                    // Leer la respuesta
                    if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                        connection.inputStream.use { inputStream ->
                            val response = BufferedReader(InputStreamReader(inputStream)).use { it.readText() }
                            val jsonResponse = JSONObject(response)
                            val token = jsonResponse.getString("token")

                            // Ejecutar la devolución de llamada en el hilo principal
                            Handler(Looper.getMainLooper()).post {
                                callback.onSuccess(token)
                            }
                        }
                    } else {
                        val error = "HTTP Error: ${connection.responseCode}"
                        Handler(Looper.getMainLooper()).post {
                            callback.onError(error)
                        }
                    }
                } catch (e: Exception) {
                    Handler(Looper.getMainLooper()).post {
                        callback.onError("Error: ${e.message}")
                    }
                } finally {
                    connection?.disconnect()
                }
            }.start()
        }
    }

    //final funcion tokenmanager


    //inicio json
    private fun fetchAndSaveProductData(
        context: Context,
        token: String,
        url: String,
        fileName: String
    ) {
        Thread {
            var connection: HttpURLConnection? = null
            try {
                val serviceUrl = URL(url)
                connection = serviceUrl.openConnection() as HttpURLConnection

                // Configurar el timeout de conexión y lectura (en milisegundos)
                connection.connectTimeout = 5000 // 5 segundos para conectar
                connection.readTimeout = 5000 // 5 segundos para leer la respuesta

                connection.requestMethod = "GET"
                connection.setRequestProperty("Authorization", "Bearer $token")
                connection.setRequestProperty("Content-Type", "application/json")

                // Realizar la solicitud
                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Proceso completado, actualizar la UI
                    Handler(Looper.getMainLooper()).post {

                    }

                    val inputStream = connection.inputStream
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val filePath = File(context.filesDir, fileName)

                    // Usamos un BufferedWriter para escribir el contenido de manera eficiente
                    val fileWriter = BufferedWriter(FileWriter(filePath))

                    // Usamos `use` para asegurar que los flujos se cierren correctamente
                    reader.use { bufferedReader ->
                        fileWriter.use { writer ->
                            var line: String?
                            while (bufferedReader.readLine().also { line = it } != null) {
                                writer.write(line)
                                writer.newLine()
                            }
                        }
                    }

                    // Retornar al hilo principal para mostrar el resultado
                    Handler(Looper.getMainLooper()).post {
                        Log.d("ProductData", "JSON guardado exitosamente en: ${filePath.absolutePath}")
                        Toast.makeText(context, "Datos guardados correctamente en $fileName.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Manejar error en la respuesta HTTP
                    Handler(Looper.getMainLooper()).post {

                        Log.e("ProductData", "Error HTTP: $responseCode")
                        Toast.makeText(context, "Error al obtener los datos: $responseCode", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                // Manejar la excepción (timeout incluido)
                Handler(Looper.getMainLooper()).post {
                    if (e is java.net.SocketTimeoutException) {
                        Log.e("ProductData", "Timeout: La solicitud tardó demasiado tiempo.")
                        Toast.makeText(context, "La solicitud tardó demasiado tiempo (Timeout).", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.e("ProductData", "Error en la solicitud: ${e.message}")
                        Toast.makeText(context, "Error al hacer la solicitud: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } finally {
                connection?.disconnect()
                // No es necesario usar un CountDownLatch aquí, ya que estamos manejando todo de manera asincrónica
            }
        }.start()
    }


    //final json


    //funcion para actualizatr
    fun fetchTokensAndData(
        numeroIngresado: Int,
        //onSuccess: () -> Unit,
        //onError: (String) -> Unit
    ) {
        // Mostrar el popup de progreso
        val progressDialog = ProgressDialog(this@InstructionsActivity)
        progressDialog.setMessage("Descargando datos, por favor espere...")
        progressDialog.setCancelable(false) // No permitir que se cierre el diálogo mientras se descargan los datos
        progressDialog.show()

        // Crear un CountDownLatch para esperar las solicitudes de tokens
        val latch = CountDownLatch(2) // Esperar por 2 solicitudes

        // Llamar al primer servicio para obtener el primer token
        val tokenManager = com.example.cambio_precio_gondola.InstructionsActivity.TokenManager()
        val serviceUrl1 = "http://10.177.172.60:55001/auth/infonut"
        val username1 = "infonut_user"
        val password1 = "123"

        tokenManager.fetchTokenAsync(
            serviceUrl1,
            username1,
            password1,
            object : com.example.cambio_precio_gondola.InstructionsActivity.TokenManager.TokenCallback {
                override fun onSuccess(token: String) {
                    Log.d("TokenManager", "Primer token obtenido: $token")
                    val token1 = token

                    // Llamar al segundo servicio para obtener el segundo token
                    val serviceUrl2 = "http://10.177.172.60:55001/auth/product"
                    val username2 = "product_user"
                    val password2 = "123"

                    tokenManager.fetchTokenAsync(
                        serviceUrl2,
                        username2,
                        password2,
                        object : com.example.cambio_precio_gondola.InstructionsActivity.TokenManager.TokenCallback {
                            override fun onSuccess(token: String) {
                                Log.d("TokenManager", "Segundo token obtenido: $token")
                                val token2 = token

                                // Realizar las solicitudes para guardar los productos y la información nutricional
                                val url1 = "http://10.177.172.60:55001/apigateway/product/$numeroIngresado/93"
                                val fileName1 = "product.txt"
                                fetchAndSaveProductData(
                                    this@InstructionsActivity,
                                    token2,
                                    url1,
                                    fileName1
                                )

                                val url2 = "http://10.177.172.60:55001/apigateway/infonut/$numeroIngresado/93"
                                val fileName2 = "info.txt"
                                fetchAndSaveProductData(
                                    this@InstructionsActivity,
                                    token1,
                                    url2,
                                    fileName2
                                )

                                // Decrementar el contador del Latch después de cada solicitud exitosa
                                latch.countDown()
                            }

                            override fun onError(error: String) {
                                Log.e("TokenManager", "Error al obtener el segundo token: $error")
                                Toast.makeText(this@InstructionsActivity, "Error al obtener segundo token: $error", Toast.LENGTH_SHORT).show()
                                latch.countDown() // Asegurar que el Latch se decrementa aunque haya un error
                            }
                        })
                }

                override fun onError(error: String) {
                    Log.e("TokenManager", "Error al obtener el primer token: $error")

                    // Crear un AlertDialog para mostrar el error
                    Handler(Looper.getMainLooper()).post {
                        val alertDialog = AlertDialog.Builder(this@InstructionsActivity)
                            .setTitle("Error")
                            .setMessage("Error al actualizar información ATENCION: NO ESTA TRABAJANDO CON DATA ACTUALIZADA LUEGO DE 30 MIN REVISAR CONECTIVIDAD: $error")
                            .setCancelable(true) // Permitir que se cierre tocando fuera del diálogo
                            .setPositiveButton("Aceptar") { dialog, _ ->
                                dialog.dismiss() // Cerrar el diálogo al presionar el botón
                            }
                            .create()
                        alertDialog.show()
                    }

                    // Decrementar el Latch para asegurarse de que el flujo continúe
                    latch.countDown()
                    latch.countDown() // Asegurar que el Latch se decrementa aunque haya un error
                }
            })

        // Esperar que ambas solicitudes de token se completen antes de continuar
        Thread {
            try {
                // Usar timeout de 30 segundos
                if (!latch.await(30, TimeUnit.SECONDS)) {
                    // Timeout, cerrar el ProgressDialog
                    Handler(Looper.getMainLooper()).post {
                        progressDialog.dismiss()
                        Toast.makeText(
                            this@InstructionsActivity,
                            "Se superó el tiempo de espera.",
                            Toast.LENGTH_SHORT
                        ).show()
                        // Habilitar el botón o realizar alguna acción
                    }
                } else {
                    // Cuando las solicitudes han finalizado, cerrar el ProgressDialog
                    Handler(Looper.getMainLooper()).post {
                        progressDialog.dismiss() // Ocultar el ProgressDialog
                        // Habilitar el botón después de la obtención de datos
                        //onSuccess() // Llamada a la función de éxito
                    }
                }
            } catch (e: InterruptedException) {
                e.printStackTrace()
                // En caso de error en el Thread, se asegura que el ProgressDialog se cierre
                Handler(Looper.getMainLooper()).post {
                    progressDialog.dismiss()
                    Toast.makeText(
                        this@InstructionsActivity,
                        "Error al esperar las respuestas.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }.start()
    }


    //final de funcion
    // actualizacion cada 10 min
    private val handler = Handler()
    private val runnable = object : Runnable {
        override fun run() {
            // Llamar a la función para obtener los minutos desde la última modificación
            val minutosDesdeModificacion = obtenerMinutosDesdeUltimaModificacion(applicationContext, "product.json")

            // Obtener el TextView donde mostrar el tiempo
            val textTimeInfo: TextView = findViewById(R.id.text_time_info)

            // Actualizar el TextView con el tiempo transcurrido o el mensaje si el archivo no existe
            if (minutosDesdeModificacion != null) {
                textTimeInfo.text = "Tiempo sin actualizar información: $minutosDesdeModificacion min"

                if(minutosDesdeModificacion >=30)

                {
// Llamada  a la función con número ingresado
                    val numeroIngresado = intent.getIntExtra("numero_ingresado", -1)
                    fetchTokensAndData(
                        numeroIngresado



                    )
                }



















            } else {
                textTimeInfo.text = "Archivo no encontrado"
            }
// enviar mensaje de actualizacion solo pruebas

            // Volver a ejecutar el Runnable después de 5 minutos (300,000 milisegundos)
            //handler.postDelayed(this, TimeUnit.MINUTES.toMillis(5))
            handler.postDelayed(this, 20000) // 5 segundos
        }
    }

    // En el método onStart() o donde lo necesites, iniciar el ciclo
    override fun onStart() {
        super.onStart()
        // Iniciar el ciclo que ejecutará la función cada 5 minutos
        handler.post(runnable)
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
                val dataParts = qrData.split(";")
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
                fun leerJsonDesdeAssets(context: Context, archivo: String): String {
                    // Cambiar la ruta para leer desde filesDir en lugar de assets
                    val file = File(context.filesDir, archivo)
                    if (file.exists()) {
                        return file.readText() // Leer el archivo directamente desde filesDir
                    } else {
                        throw FileNotFoundException("Archivo $archivo no encontrado en filesDir")
                    }
                }


                // Función para buscar un item en product.json utilizando plu_nbr
                fun buscarEnProduct(context: Context, pluNbr: Int): Map<String, Any?>? {
                    val jsonString = leerJsonDesdeAssets(context, "product.json")
                    val jsonArray = JSONArray(jsonString)

                    val camposDeseadosProduct = listOf("store_nbr", "plu_nbr", "item1_desc", "brand_name", "sell_price")

                    for (i in 0 until jsonArray.length()) {
                        val item = jsonArray.getJSONObject(i)
                        if (item.getInt("plu_nbr") == pluNbr) { // Usamos plu_nbr como clave de búsqueda
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
                        if (item.getInt("plu_nbr") == pluNbr) { // Usamos plu_nbr como clave de búsqueda
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
                            plu_nbr = itemProduct["plu_nbr"] as? Int ?: -1, // Usamos plu_nbr de product.json
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
                if (dataParts.size >= 8) {
                    // Extraer cada parte del código QR
                    val pluNbr = dataParts[3].toInt()
                    val itemNbr = dataParts[3]// Tomar los dígitos de la posición 1 a 5 (por ejemplo, "10633")
                    val resultado = buscarYCombinarInformacion(this,pluNbr )
                    val nombreProd = resultado?.itemDesc // Aquí esta el nombre mapeado según el código
                    val fecElab = dataParts[1].take(6) // Tomar solo los primeros 6 dígitos para la fecha (yymmdd)
                    val fecElabFormatted = "${fecElab.substring(4, 6)}/${fecElab.substring(2, 4)}/${fecElab.substring(0, 2)}" // Formato dd/mm/yy
                    val peso = (dataParts[4].toDoubleOrNull() ?: 0.0) / 1000 // Convertir los últimos 4 dígitos en peso en kilogramos
                    val fecVenc = dataParts[6] // Fecha de vencimiento
                    val fecVencFormatted = "${fecVenc.substring(4,6)}/${fecVenc.substring(2, 4)}/${fecVenc.substring(0, 2)}" // Formato dd/mm/yy
                    val precioAnt = (dataParts[7].toDoubleOrNull() ?: 0.0).toInt() // Convertir precio anterior a entero
                    val precioActual = resultado?.sellPrice // Precio actual fijo convertido a entero
                    val precioTotal = (precioActual?.times(peso))?.toInt()

                    // val precioTotal = (precioActual * peso).toInt() // Calcular precio total y convertirlo a entero

                    // Formatear los precios como $<precio>/Kg y el total como $<precio>
                    val precioAntFormatted = "$$precioAnt/Kg"
                    val precioActualFormatted = "$$precioActual/Kg"
                    val precioTotalFormatted = "$$precioTotal"

                    // Enviar datos a ProdDetailActivity
                    val intent = Intent(this, ProdDetailActivity::class.java)
                    intent.putExtra("resolucion",resultado?.resolucion)
                    intent.putExtra("nombre_prod", nombreProd)
                    intent.putExtra("item_nbr", itemNbr)
                    intent.putExtra("peso", "$peso Kg") // Agregar 'Kg' al final del peso
                    intent.putExtra("fec_elab", fecElabFormatted)
                    intent.putExtra("fec_venc", fecVencFormatted)
                    intent.putExtra("precio_ant", precioAntFormatted)
                    intent.putExtra("precio_actual", precioActualFormatted)
                    intent.putExtra("precio_total", precioTotalFormatted)
                    intent.putExtra("precio_total_numero", precioTotal)
                    intent.putExtra("brand_name", resultado?.brandName)

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