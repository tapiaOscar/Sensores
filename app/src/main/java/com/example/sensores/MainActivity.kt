package com.example.sensores

import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var mainLayout: ConstraintLayout
    private lateinit var tvTitulo: TextView
    private lateinit var tvValores: TextView
    private lateinit var tvEstado: TextView

    // Objeto que permite ocupar cualquier sensor
    private lateinit var sensorManager: SensorManager

    // Atributo que permite ocupar un sensor en particular
    private var sensorLuz: Sensor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Vincular vistas
        mainLayout = findViewById(R.id.main)
        tvTitulo = findViewById(R.id.tvTitulo)
        tvEstado = findViewById(R.id.tvEstado)
        tvValores = findViewById(R.id.tvValores)

        // PERMITIR EL USO DEL SENSOR QUE SE VA A OCUPAR
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        // OCUPAR EL SENSOR DE LUZ
        sensorLuz = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

        // VERIFICAR QUE EL SENSOR EXISTA
        if (sensorLuz == null) {
            tvEstado.text = "No hay Sensor de Luz pobre :("
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    // METODO PARA DETECTAR CAMBIOS EN EL SENSOR
    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_LIGHT) {
            val luz = event.values[0]
            tvValores.text = "Luz: $luz lx"

            // EVALUAR LOS UMBRALES DE LA ACTIVIDAD ( < 50 y > 500 )
            if (luz < 50) {
                // Modo Oscuro: Fondo negro, texto blanco
                tvEstado.text = "Modo Oscuro"
                mainLayout.setBackgroundColor(Color.BLACK)
                tvTitulo.setTextColor(Color.WHITE)
                tvValores.setTextColor(Color.WHITE)
                tvEstado.setTextColor(Color.WHITE)
            } else if (luz > 500) {
                // Modo Claro: Fondo blanco, texto negro[cite: 1]
                tvEstado.text = "Modo Claro"
                mainLayout.setBackgroundColor(Color.WHITE)
                tvTitulo.setTextColor(Color.BLACK)
                tvValores.setTextColor(Color.BLACK)
                tvEstado.setTextColor(Color.BLACK)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Sin cambios necesarios aquí
    }

    // Registrar el sensor cuando la app está activa
    override fun onResume() {
        super.onResume()
        sensorLuz?.let {
            sensorManager.registerListener(
                this, it, SensorManager.SENSOR_DELAY_NORMAL
            )
        }
    }

    // Desregistrar el sensor cuando la app pasa a segundo plano (Buenas prácticas)
    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }
}