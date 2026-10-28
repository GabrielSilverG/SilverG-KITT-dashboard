package com.silverg.kitt

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.*
import android.location.*
import android.os.*
import android.view.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

class MainActivity : Activity(), LocationListener, SensorEventListener {
    private lateinit var screen: Dash
    private lateinit var gps: LocationManager
    private lateinit var sensors: SensorManager
    private val handler = Handler(Looper.getMainLooper())
    private var speed = 0f
    private var heading = 0f
    private var gpsActive = false
    private var demo = false
    private var scanner = 0f
    private var direction = 1f
    private var battery = 0
    private val tick = object : Runnable {
        override fun run() {
            scanner += direction * .055f
            if (scanner >= 1f || scanner <= 0f) direction *= -1f
            scanner = scanner.coerceIn(0f, 1f)
            battery = (getSystemService(BATTERY_SERVICE) as BatteryManager)
                .getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).coerceIn(0, 100)
            screen.invalidate()
            handler.postDelayed(this, 60L)
        }
    }
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = 5894 or View.SYSTEM_UI_FLAG_FULLSCREEN
        gps = getSystemService(LOCATION_SERVICE) as LocationManager
        sensors = getSystemService(SENSOR_SERVICE) as SensorManager
        screen = Dash(this)
        setContentView(screen)
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 17)
    }
    private fun openSpotify() {
        try {
            val launch = Intent(Intent.ACTION_VIEW, Uri.parse("spotify:"))
                .setPackage("com.spotify.music")
            startActivity(launch)
        } catch (_: Exception) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://open.spotify.com/")))
        }
    }
    private fun openBluetooth() {
        startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
    }
    private fun startGps() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                gps.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, this)
            } catch (_: SecurityException) {}
            catch (_: IllegalArgumentException) {}
        }
    }
    override fun onResume() {
        super.onResume()
        handler.removeCallbacks(tick)
        tick.run()
        startGps()
        sensors.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)?.let {
            sensors.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }
    override fun onPause() {
        handler.removeCallbacks(tick)
        gps.removeUpdates(this)
        sensors.unregisterListener(this)
        super.onPause()
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, results)
        if (requestCode == 17 && results.any { it == PackageManager.PERMISSION_GRANTED }) startGps()
    }
    override fun onLocationChanged(location: Location) {
        gpsActive = true
        speed = if (location.hasSpeed()) max(0f, location.speed * 3.6f) else 0f
        if (location.hasBearing() && speed > 3f) heading = location.bearing
    }
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR && speed < 3f) {
            val m = FloatArray(9)
            val a = FloatArray(3)
            SensorManager.getRotationMatrixFromVector(m, event.values)
            SensorManager.getOrientation(m, a)
            heading = ((Math.toDegrees(a[0].toDouble()).toFloat() + 360f) % 360f)
        }
    }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    inner class Dash(context: Context) : View(context) {
        private val p = Paint(3)
        private val red = Color.rgb(255, 40, 66)
        private val cyan = Color.rgb(46, 225, 245)
        private val amber = Color.rgb(255, 180, 45)
        private val muted = Color.rgb(115, 135, 162)
        private val panel = Color.rgb(13, 23, 38)
        private fun text(c: Canvas, s: String, x: Float, y: Float, size: Float, color: Int, center: Boolean = false) {
            p.reset(); p.isAntiAlias = true; p.color = color; p.textSize = size
            p.typeface = Typeface.create("monospace", Typeface.BOLD)
            p.textAlign = if (center) Paint.Align.CENTER else Paint.Align.LEFT
            c.drawText(s, x, y, p)
        }
        private fun box(c: Canvas, x: Float, y: Float, r: Float, b: Float, color: Int, outline: Boolean = false) {
            p.reset(); p.isAntiAlias = true; p.color = color
            p.style = if (outline) Paint.Style.STROKE else Paint.Style.FILL
            p.strokeWidth = 2f
            c.drawRoundRect(x, y, r, b, 9f, 9f, p)
        }
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            canvas.save()
            canvas.scale(width / 900f, height / 430f)
            canvas.drawColor(Color.rgb(3, 7, 15))
            box(canvas, 12f, 12f, 888f, 418f, Color.rgb(45, 55, 72), true)
            text(canvas, "SILVERG // KITT", 32f, 48f, 27f, red)
            text(canvas, "RETRO DIGITAL COCKPIT  /  1980s EDITION", 32f, 69f, 12f, muted)
            text(canvas, SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(Date()), 860f, 48f, 16f, cyan, true)
            for (i in 0..16) {
                val strength = (1f - abs(i / 16f - scanner) * 5f).coerceIn(.1f, 1f)
                box(canvas, 245f + i * 24f, 88f, 264f + i * 24f, 114f,
                    Color.rgb((90 + 165 * strength).toInt(), (10 + 30 * strength).toInt(), (20 + 45 * strength).toInt()))
            }
            box(canvas, 25f, 140f, 405f, 344f, panel)
            box(canvas, 420f, 140f, 875f, 344f, panel)
            text(canvas, "VELOCITY / GPS", 44f, 166f, 15f, muted)
            text(canvas, if (demo) "088" else speed.roundToInt().toString().padStart(3, '0'), 210f, 281f, 100f, cyan, true)
            text(canvas, "KM/H", 342f, 283f, 20f, cyan, true)
            text(canvas, "NAVIGATION / COMPASS", 441f, 166f, 15f, muted)
            val cx = 545f; val cy = 252f; val radius = 63f
            p.reset(); p.isAntiAlias = true; p.color = cyan; p.style = Paint.Style.STROKE; p.strokeWidth = 2f
            canvas.drawCircle(cx, cy, radius, p)
            for (a in 0 until 360 step 30) {
                val angle = Math.toRadians((a - 90).toDouble())
                canvas.drawLine(cx + cos(angle).toFloat() * 54f, cy + sin(angle).toFloat() * 54f,
                    cx + cos(angle).toFloat() * radius, cy + sin(angle).toFloat() * radius, p)
            }
            val bearing = if (demo) 315f else heading
            val angle = Math.toRadians((bearing - 90).toDouble())
            p.color = red; p.strokeWidth = 5f
            canvas.drawLine(cx, cy, cx + cos(angle).toFloat() * 48f, cy + sin(angle).toFloat() * 48f, p)
            text(canvas, "N", cx, 177f, 14f, amber, true)
            text(canvas, "${bearing.roundToInt()}°", cx, 337f, 15f, cyan, true)
            text(canvas, "SYSTEM", 654f, 202f, 15f, muted)
            text(canvas, "BATTERY", 654f, 235f, 13f, muted)
            text(canvas, "$battery%", 834f, 235f, 18f, cyan, true)
            box(canvas, 654f, 246f, 843f, 259f, Color.rgb(40, 55, 70))
            box(canvas, 654f, 246f, 654f + 189f * battery / 100f, 259f, if (battery < 20) red else cyan)
            text(canvas, "GPS", 654f, 294f, 13f, muted)
            text(canvas, if (demo) "DEMO" else if (gpsActive) "ACTIVE" else "WAITING", 762f, 294f, 15f, if (gpsActive) cyan else amber)
            box(canvas, 27f, 357f, 227f, 403f, Color.rgb(18, 65, 52))
            text(canvas, "[ SPOTIFY ]", 127f, 386f, 18f, cyan, true)
            box(canvas, 239f, 357f, 445f, 403f, Color.rgb(24, 44, 73))
            text(canvas, "[ BLUETOOTH ]", 342f, 386f, 16f, cyan, true)
            text(canvas, if (demo) "DEMO" else "LIVE GPS", 530f, 386f, 13f, if (demo) amber else muted, true)
            box(canvas, 650f, 357f, 863f, 403f, Color.rgb(55, 22, 39))
            text(canvas, if (demo) "[ LIVE MODE ]" else "[ DEMO MODE ]", 756f, 386f, 16f, red, true)
            canvas.restore()
        }
        override fun onTouchEvent(event: MotionEvent): Boolean {
            if (event.action == MotionEvent.ACTION_UP) {
                val x = event.x * 900f / width
                val y = event.y * 430f / height
                if (y in 345f..415f && x in 27f..227f) {
                    openSpotify()
                } else if (y in 345f..415f && x in 239f..445f) {
                    openBluetooth()
                } else if (x in 650f..875f && y in 345f..415f) {
                    demo = !demo
                    invalidate()
                    performClick()
                }
            }
            return true
        }
        override fun performClick(): Boolean { super.performClick(); return true }
    }
}
