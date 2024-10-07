package com.example.cambio_precio_gondola

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.Button // Importamos el botón

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Configuración de padding con los insets del sistema (barra de estado, etc.)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Obtener referencia al botón "Entrar" y configurar el evento onClick
        val enterButton: Button = findViewById(R.id.enterButton)
        enterButton.setOnClickListener {
            // Crear un intent para abrir InstructionsActivity
            val intent = Intent(this, InstructionsActivity::class.java)
            startActivity(intent)
        }
    }
}