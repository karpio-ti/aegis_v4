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

class MainActivity : AppCompatActivity() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private val deviceName = "WF-1000XM5 de Daniel"  // Nombre del dispositivo Bluetooth
    private val requestBluetoothPermissionsCode = 1

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action: String? = intent?.action
            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED == action) {
                updateBluetoothStatus() // Actualiza el estado de Bluetooth
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

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter() // Inicializar BluetoothAdapter
        updateBluetoothStatus() // Actualizar estado de Bluetooth

        val enterButton: Button = findViewById(R.id.enterButton)
        enterButton.setOnClickListener {
            val intent = Intent(this, InstructionsActivity::class.java)
            startActivity(intent) // Navegar a InstructionsActivity
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
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

        if (checkBluetoothPermissions() && isBluetoothHeadsetConnected()) {
            enterButton.isEnabled = true  // Habilitar botón
            enterButton.setBackgroundTintList(
                ContextCompat.getColorStateList(this, android.R.color.holo_orange_light)
            )
            enterButton.setTextColor(ContextCompat.getColor(this, android.R.color.white))
            statusTextView.text = getString(R.string.connected_to_bluetooth, deviceName)
        } else {
            enterButton.isEnabled = false  // Deshabilitar botón
            enterButton.setBackgroundColor(Color.LTGRAY)
            enterButton.setTextColor(Color.GRAY)
            statusTextView.text = getString(R.string.bluetooth_not_connected, deviceName)
            requestBluetoothPermissions() // Solicitar permisos de Bluetooth
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
}