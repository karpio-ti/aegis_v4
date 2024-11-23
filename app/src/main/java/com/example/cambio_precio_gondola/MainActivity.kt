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



import android.app.AlertDialog
import android.text.InputType
import android.widget.EditText
import android.widget.Toast

class MainActivity : AppCompatActivity() {





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
                    val state = intent?.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, BluetoothAdapter.ERROR)
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
            val numeroIngresado = findViewById<TextView>(R.id.localNumberTextView).text.toString().removePrefix("Local: ").trim()
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
        val enterButton: Button = findViewById(R.id.enterButton)
        val statusTextView: TextView = findViewById(R.id.statusTextView)
        Log.d("BluetoothConnection", "Checking Bluetooth connection for device: $deviceName")

        if (checkBluetoothPermissions()  ){
           //  && isBluetoothHeadsetConnected()) {
            enterButton.isEnabled = true
            enterButton.backgroundTintList = ContextCompat.getColorStateList(this, android.R.color.holo_orange_light)
            enterButton.setTextColor(ContextCompat.getColor(this, android.R.color.white))
            enterButton.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_orange_light))
            statusTextView.text = getString(R.string.connected_to_bluetooth, deviceName)
        } else {
            // Deshabilitar botón y cambiar color a gris
            enterButton.isEnabled = false
            enterButton.setBackgroundColor(Color.LTGRAY)
            enterButton.setTextColor(Color.GRAY)
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
        val isBluetoothConnectGranted = ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        val isBluetoothScanGranted = ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        val isLocationGranted = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

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
                statusTextView.text = getString(R.string.bluetooth_permission_denied) // Permisos denegados
            }
        }
    }


    fun mostrarPopupNumeros(context: Context) {
        fun mostrarConfirmacion(numeroIngresado: String) {
            val localNumberTextView: TextView = findViewById(R.id.localNumberTextView)

            AlertDialog.Builder(this)
                .setTitle("Confirmar número de local")
                .setMessage("¿El número de local ingresado es $numeroIngresado?")
                .setPositiveButton("Sí") { _, _ ->
                    localNumberTextView.text = "Local: $numeroIngresado"
                    Toast.makeText(this, "Número confirmado: $numeroIngresado", Toast.LENGTH_SHORT).show()
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
            .setTitle("Ingrese  número de LOCAL")
            .setMessage("Solo se Aceptan numeros")
            .setView(input)
            .setPositiveButton("Aceptar") { _, _ ->
                // Obtener el valor ingresado
                val numeroIngresado = input.text.toString()
                if (numeroIngresado.isNotEmpty() && numeroIngresado.toIntOrNull() != null) {
                    mostrarConfirmacion(numeroIngresado) // Mostrar cuadro de confirmación
                    //Toast.makeText(context, "Número ingresado: $numeroIngresado", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Por favor, ingrese un número válido", Toast.LENGTH_SHORT).show()
                    mostrarPopupNumeros(context) // Volver a mostrar el cuadro de entrada
                }
            }
          //  .setNegativeButton("Cancelar")
           // { dialog, _ ->
        //dialog.dismiss()
            //}
           .create()

       dialog.show()
    }
    }