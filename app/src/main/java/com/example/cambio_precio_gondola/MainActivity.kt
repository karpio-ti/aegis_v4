package com.example.cambio_precio_gondola
import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import android.app.AlertDialog
import android.app.ProgressDialog
import android.text.InputType
import android.widget.EditText
import android.widget.Toast
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

//mira linea 498

class MainActivity : AppCompatActivity() {
    private lateinit var enterButton: Button
    private var habilitar=2
    //borrar estas funciones despues son solo de prueba para la copia al no tener la red


    fun copyAssetToFilesDir(context: Context, assetFileName: String) {
        try {
            // Abrir el archivo en assets
            val inputStream = context.assets.open(assetFileName)
            val outputFile = File(context.filesDir, assetFileName)

            // Crear el archivo de salida en filesDir
            FileOutputStream(outputFile).use { outputStream ->
                val buffer = ByteArray(1024)
                var length: Int

                // Leer y escribir en bloques de 1024 bytes
                while (inputStream.read(buffer).also { length = it } > 0) {
                    outputStream.write(buffer, 0, length)
                }
            }

            inputStream.close()
            println("Archivo copiado exitosamente a: ${outputFile.absolutePath}")
        } catch (e: IOException) {
            e.printStackTrace()
            println("Error al copiar el archivo: ${e.message}")
        }
    }


    // fun funcion de copia para ser borrada
    // Función que habilita o deshabilita el botón dependiendo del parámetro



    fun actualizarBoton(habilitar: Boolean) {
        // Realiza la actualización en el hilo principal
        runOnUiThread {
            if (habilitar) {
                enterButton.isEnabled = true
                enterButton.backgroundTintList =
                    ContextCompat.getColorStateList(this, android.R.color.holo_orange_light)
                enterButton.setTextColor(ContextCompat.getColor(this, android.R.color.white))
                enterButton.setBackgroundColor(
                    ContextCompat.getColor(
                        this,
                        android.R.color.holo_orange_light
                    )
                )
            } else {
                enterButton.isEnabled = false
                enterButton.setBackgroundColor(Color.LTGRAY)
                enterButton.setTextColor(Color.GRAY)
            }

            // Forzar la actualización de la vista
            enterButton.invalidate() // Redibuja la vista
            enterButton.requestLayout() // Realiza el layout del botón
        }
    }
    //fin borrado

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

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private val deviceName = "WF-1000XM5 de Daniel"
    private val requestBluetoothPermissionsCode = 1
    private val numeroIngresado = 0
    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action: String? = intent?.action
            when (action) {
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    updateBluetoothStatus() // Actualiza el estado cuando hay cambio en el emparejamiento
                }

                BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED -> {
                    val state = intent?.getIntExtra(
                        BluetoothAdapter.EXTRA_CONNECTION_STATE,
                        BluetoothAdapter.ERROR
                    )
                    when (state) {
                        BluetoothAdapter.STATE_CONNECTED -> {
                            Log.d("BluetoothConnection", "Bluetooth connected")
                            updateBluetoothStatus() // Actualiza el estado al conectar
                        }

                        BluetoothAdapter.STATE_DISCONNECTED -> {
                            Log.d("BluetoothConnection", "Bluetooth disconnected")
                            updateBluetoothStatus() // Actualiza el estado al desconectar
                        }
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        enterButton = findViewById(R.id.enterButton) // Ahora es accesible en cualquier parte de la actividad


        // Configuración de padding con los insets del sistema
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        mostrarPopupNumeros(this)
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter() // Inicializar BluetoothAdapter
        updateBluetoothStatus() // Actualizar estado de Bluetooth

        val enterButton: Button = findViewById(R.id.enterButton)
        enterButton.setOnClickListener {
            val numeroIngresado = findViewById<TextView>(R.id.localNumberTextView).text.toString()
                .removePrefix("Local: ").trim()
            val numero = numeroIngresado.toIntOrNull() // Convertir el número a Int
            val intent = Intent(this, InstructionsActivity::class.java)
            intent.putExtra("numero_ingresado", numero) // Enviar el valor a la segunda actividad
            startActivity(intent) // Navegar a InstructionsActivity
        }

        // Botón para ingresar el número de local
        val localButton: Button = findViewById(R.id.localButton)
        localButton.setOnClickListener {
            mostrarPopupNumeros(this)
        }
    }


    override fun onStart() {
        super.onStart()
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED) // Escuchar cambios de conexión de Bluetooth
        }
        registerReceiver(bluetoothReceiver, filter) // Registrar el receptor
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(bluetoothReceiver) // Desregistrar el receptor
    }

    // Actualiza el estado de Bluetooth
    private fun updateBluetoothStatus() {
        // val enterButton: Button = findViewById(R.id.enterButton)
        val statusTextView: TextView = findViewById(R.id.statusTextView)
        Log.d("BluetoothConnection", "Checking Bluetooth connection for device: $deviceName")

        if (checkBluetoothPermissions() ) {
            // && isBluetoothHeadsetConnected()) {
            // Para habilitar el botón
            actualizarBoton(true)

            statusTextView.text = getString(R.string.connected_to_bluetooth, deviceName)
        } else {
            // Deshabilitar botón y cambiar color a gris
            actualizarBoton(false)
            statusTextView.text = getString(R.string.bluetooth_not_connected, deviceName)
            requestBluetoothPermissions() // Solicitar permisos de Bluetooth si es necesario
        }
    }


    // Verifica si un auricular Bluetooth está conectado
    @SuppressLint("MissingPermission", "WrongConstant")
    private fun isBluetoothHeadsetConnected(): Boolean {
        val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        return (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled
                && mBluetoothAdapter.getProfileConnectionState(BluetoothHeadset.HEADSET) == BluetoothHeadset.STATE_CONNECTED)
    }

    // Verifica si el dispositivo Bluetooth tiene los permisos adecuados
    private fun checkBluetoothPermissions(): Boolean {
        val isBluetoothConnectGranted = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
        val isBluetoothScanGranted = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.BLUETOOTH_SCAN
        ) == PackageManager.PERMISSION_GRANTED
        val isLocationGranted = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            isBluetoothConnectGranted && isBluetoothScanGranted && isLocationGranted
        } else {
            isBluetoothConnectGranted && isLocationGranted
        }
    }

    // Solicita los permisos de Bluetooth necesarios
    private fun requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ),
                requestBluetoothPermissionsCode
            )
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.ACCESS_FINE_LOCATION),
                requestBluetoothPermissionsCode
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == requestBluetoothPermissionsCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                updateBluetoothStatus() // Permisos otorgados, actualizar estado
            } else {
                val statusTextView: TextView = findViewById(R.id.statusTextView)
                statusTextView.text =
                    getString(R.string.bluetooth_permission_denied) // Permisos denegados
            }
        }
    }

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
                        habilitar = 1
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
                        habilitar = 0
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








    fun mostrarPopupNumeros(context: Context) {

        // Función interna para mostrar la confirmación
        fun mostrarConfirmacion(numeroIngresado: String) {

            val localNumberTextView: TextView = findViewById(R.id.localNumberTextView)

            AlertDialog.Builder(this)
                .setTitle("Confirmar número de local")
                .setMessage("¿El número de local ingresado es $numeroIngresado?")
                .setPositiveButton("Sí") { _, _ ->
                    localNumberTextView.text = "Local: $numeroIngresado"
                    Toast.makeText(this, "Número confirmado: $numeroIngresado", Toast.LENGTH_SHORT).show()

                    // Mostrar el popup de progreso
                    val progressDialog = ProgressDialog(this)
                    progressDialog.setMessage("Descargando datos, por favor espere...")
                    progressDialog.setCancelable(false) // No permitir que se cierre el diálogo mientras se descargan los datos
                    progressDialog.show()

                    // Crear un CountDownLatch para esperar las solicitudes de tokens
                    val latch = CountDownLatch(2) // Esperar por 2 solicitudes

                    // Llamar al primer servicio para obtener el primer token
                    val tokenManager = TokenManager()
                    val serviceUrl1 = "http://10.177.172.60:55001/auth/infonut"
                    val username1 = "infonut_user"
                    val password1 = "123"

                    tokenManager.fetchTokenAsync(serviceUrl1, username1, password1, object : TokenManager.TokenCallback {
                        override fun onSuccess(token: String) {
                            Log.d("TokenManager", "Primer token obtenido: $token")
                            val token1 = token

                            // Llamar al segundo servicio para obtener el segundo token
                            val serviceUrl2 = "http://10.177.172.60:55001/auth/product"
                            val username2 = "product_user"
                            val password2 = "123"

                            tokenManager.fetchTokenAsync(serviceUrl2, username2, password2, object : TokenManager.TokenCallback {
                                override fun onSuccess(token: String) {
                                    Log.d("TokenManager", "Segundo token obtenido: $token")
                                    val token2 = token

                                    // Realizar las solicitudes para guardar los productos y la información nutricional
                                    val url1 = "http://10.177.172.60:55001/apigateway/product/$numeroIngresado/93"
                                    val fileName1 = "product.txt"
                                    fetchAndSaveProductData(this@MainActivity, token2, url1, fileName1)

                                    val url2 = "http://10.177.172.60:55001/apigateway/infonut/$numeroIngresado/93"
                                    val fileName2 = "info.txt"
                                    fetchAndSaveProductData(this@MainActivity, token1, url2, fileName2)

                                    // Decrementar el contador del Latch después de cada solicitud exitosa
                                    latch.countDown()
                                }

                                override fun onError(error: String) {
                                    Log.e("TokenManager", "Error al obtener el segundo token: $error")
                                    Toast.makeText(this@MainActivity, "Error al obtener segundo token: $error", Toast.LENGTH_SHORT).show()
                                    latch.countDown() // Asegurar que el Latch se decrementa aunque haya un error
                                }
                            })
                        }

                        override fun onError(error: String) {
                            Log.e("TokenManager", "Error al obtener el primer token: $error")
                            Toast.makeText(this@MainActivity, "Error al obtener primer token: $error", Toast.LENGTH_SHORT).show()
                            latch.countDown()
                            latch.countDown() // Asegurar que el Latch se decrementa aunque haya un error
                            habilitar=2

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
                                    Toast.makeText(this@MainActivity, "Se superó el tiempo de espera.", Toast.LENGTH_SHORT).show()
                                    actualizarBoton(false) // Habilitar el botón
                                }
                            } else {
                                // Cuando las solicitudes han finalizado, cerrar el ProgressDialog
                                Handler(Looper.getMainLooper()).post {
                                    progressDialog.dismiss() // Ocultar el ProgressDialog
                                    // Habilitar el botón después de la obtención de datos
                                    if (habilitar==1){
                                        actualizarBoton(false)}
                                    else {actualizarBoton(true)}
                                }
                            }
                        } catch (e: InterruptedException) {
                            e.printStackTrace()
                            // En caso de error en el Thread, se asegura que el ProgressDialog se cierre
                            Handler(Looper.getMainLooper()).post {
                                progressDialog.dismiss()
                                Toast.makeText(this@MainActivity, "Error al esperar las respuestas.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }.start()
                }
                .setNegativeButton("No") { _, _ ->
                    mostrarPopupNumeros(this) // Volver a mostrar el cuadro de entrada
                }
                .create()
                .show()
        }

        // Crear un EditText que solo permita números
        val input = EditText(context).apply {
            inputType = InputType.TYPE_CLASS_NUMBER // Permite solo números
            filters = arrayOf(android.text.InputFilter.LengthFilter(3)) // Limitar a 3 dígitos
        }

        // Crear el AlertDialog
        val dialog = AlertDialog.Builder(context)
            .setTitle("Ingrese número de LOCAL")
            .setView(input)
            .setPositiveButton("Aceptar") { _, _ ->
                // Obtener el valor ingresado
                val numeroIngresado = input.text.toString()

                // Validar si el campo está vacío, comienza con 0, o no es un número válido
                if (numeroIngresado.isNotEmpty() && numeroIngresado.toIntOrNull() != null && !numeroIngresado.startsWith("0")) {
                    mostrarConfirmacion(numeroIngresado) // Mostrar cuadro de confirmación
                    copyAssetToFilesDir(this, "product.json") // Copiar 'product.json' desde assets
                    copyAssetToFilesDir(this, "info.json") //copiar archivo
                } else {
                    // Si el número no es válido o comienza con 0, mostrar mensaje de error
                    val errorMessage = when {
                        numeroIngresado.isEmpty() -> "Por favor, ingrese un número."
                        numeroIngresado.startsWith("0") -> "El número no puede comenzar con 0."
                        else -> "Por favor, ingrese un número válido."
                    }
                    Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                    mostrarPopupNumeros(context) // Volver a mostrar el cuadro de entrada
                }
            }
            .setCancelable(false) // Evita que el usuario cierre el cuadro de diálogo tocando fuera de él
            .create()

        dialog.show()
    }






}