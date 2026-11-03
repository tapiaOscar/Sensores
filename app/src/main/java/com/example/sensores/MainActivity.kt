package com.example.sensores

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity(), SensorEventListener {

    companion object {
        private const val TAG = "SENSOR_LUZ"
        private const val TIMEOUT_SIN_DATOS_MS = 3000L
    }

    private lateinit var mainLayout: ConstraintLayout
    private lateinit var tvTitulo: TextView
    private lateinit var tvValores: TextView
    private lateinit var tvEstado: TextView

    private lateinit var sensorManager: SensorManager
    private var sensorLuz: Sensor? = null

    // Para avisar si el sensor existe pero nunca manda datos
    private val handler = Handler(Looper.getMainLooper())
    private var recibioDatos = false
    private val avisoSinDatos = Runnable {
        if (!recibioDatos) {
            Log.w(TAG, "Pasaron ${TIMEOUT_SIN_DATOS_MS / 1000}s sin ningún evento del sensor de luz")
            tvValores.text = "Luz: sin datos"
            tvEstado.text = "El sensor no envía valores"
        }
    }

    // Solicitud del permiso de cámara (algunos equipos, como los Xiaomi/Redmi,
    // usan un sensor de luz virtual basado en la cámara frontal)
    private val pedirCamara = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) {
            Log.d(TAG, "Permiso de cámara concedido")
            registrarSensor()
        } else {
            Log.w(TAG, "Permiso de cámara denegado")
            tvValores.text = "Luz: sin permiso"
            tvEstado.text = "Se necesita permiso de cámara para el sensor de luz"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Vincular vistas
        mainLayout = findViewById(R.id.main)
        tvTitulo = findViewById(R.id.tvTitulo)
        tvEstado = findViewById(R.id.tvEstado)
        tvValores = findViewById(R.id.tvValores)

        // Textos iniciales temporales
        tvValores.text = "Luz: Esperando sensor..."
        tvEstado.text = "Iniciando..."

        // PERMITIR EL USO DEL SENSOR QUE SE VA A OCUPAR
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        // OCUPAR EL SENSOR DE LUZ
        sensorLuz = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

        // VERIFICAR QUE EL SENSOR EXISTA FÍSICAMENTE
        if (sensorLuz == null) {
            tvEstado.text = "No hay Sensor de Luz :("
            tvValores.text = "Luz: No disponible"
        } else {
            tvEstado.text = "Sensor detectado OK"
        }

        // ---------- DIAGNÓSTICO (filtrar Logcat por SENSOR_LUZ) ----------
        sensorLuz?.let {
            Log.d(TAG, "Sensor de luz -> nombre: ${it.name}, fabricante: ${it.vendor}, " +
                    "modo de reporte: ${it.reportingMode}, rango máx: ${it.maximumRange}")
        } ?: Log.d(TAG, "getDefaultSensor(TYPE_LIGHT) devolvió null")
        // -----------------------------------------------------------------

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    // METODO PARA DETECTAR CAMBIOS EN EL SENSOR
    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_LIGHT) {
            recibioDatos = true
            handler.removeCallbacks(avisoSinDatos)

            val luz = event.values[0]
            Log.d(TAG, "Evento recibido: $luz lx")
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
                // Modo Claro: Fondo blanco, texto negro
                tvEstado.text = "Modo Claro"
                mainLayout.setBackgroundColor(Color.WHITE)
                tvTitulo.setTextColor(Color.BLACK)
                tvValores.setTextColor(Color.BLACK)
                tvEstado.setTextColor(Color.BLACK)
            } else {
                // Rango intermedio (entre 50 y 500)
                tvEstado.text = "Luz Ambiental Normal"
                mainLayout.setBackgroundColor(Color.LTGRAY)
                tvTitulo.setTextColor(Color.BLACK)
                tvValores.setTextColor(Color.BLACK)
                tvEstado.setTextColor(Color.BLACK)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.d(TAG, "Precisión cambió a $accuracy")
    }

    // Registra el listener del sensor y arranca el aviso de "sin datos"
    private fun registrarSensor() {
        val sensor = sensorLuz ?: return
        val ok = sensorManager.registerListener(
            this, sensor, SensorManager.SENSOR_DELAY_UI
        )
        Log.d(TAG, "registerListener devolvió: $ok")

        if (!ok) {
            tvValores.text = "Luz: no se pudo activar"
            tvEstado.text = "El sistema rechazó activar el sensor"
        } else {
            // Si en 3 segundos no llega ningún evento, se avisa en pantalla
            handler.removeCallbacks(avisoSinDatos)
            handler.postDelayed(avisoSinDatos, TIMEOUT_SIN_DATOS_MS)
        }
    }

    // Registrar el sensor cuando la app está activa (solo con permiso de cámara)
    override fun onResume() {
        super.onResume()
        recibioDatos = false
        if (sensorLuz == null) return

        val tienePermiso = ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (tienePermiso) {
            registrarSensor()
        } else {
            pedirCamara.launch(Manifest.permission.CAMERA)
        }
    }

    // Desregistrar el sensor cuando la app pasa a segundo plano
    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        handler.removeCallbacks(avisoSinDatos)
    }
}