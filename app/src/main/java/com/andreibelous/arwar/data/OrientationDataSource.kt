package com.andreibelous.arwar.data

import android.content.Context
import android.hardware.*
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.andreibelous.arwar.cast
import com.andreibelous.arwar.data.model.Location
import com.andreibelous.arwar.subscribe
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class OrientationDataSource(
    private val activity: AppCompatActivity,
    private val getLocation: () -> Location?
) : SensorEventListener {

    init {
        activity.lifecycle.subscribe(
            onStart = { registerOrUnregisterCompassListener(true) },
            onStop = { registerOrUnregisterCompassListener(false) }
        )
    }

    private val gravs = FloatArray(3)
    private val geoMags = FloatArray(3)
    private val rotationM = FloatArray(9)
    var currentScreenOrientation = 0
    private var previousCorrectionValue = 360f

    private var avgValSin = 0f
    private var avgValCos = 0f
    private var lastValSin = 0f
    private var lastValCos = 0f

    private var inUpdateValue = false

    var heading: Float? = null
        private set

    private val sensorMgr =
        activity.getSystemService(Context.SENSOR_SERVICE).cast<SensorManager>()
    private var sensorRegistered = false

    @Synchronized
    private fun registerOrUnregisterCompassListener(register: Boolean) {
        if (sensorRegistered && !register) {
            Log.d(TAG, "Disable sensor")
            sensorMgr.unregisterListener(this)
            sensorRegistered = false
            heading = null
        } else if (!sensorRegistered && register) {
            Log.d(TAG, "Enable sensor")
            if (!hasOrientaionSensor(sensorMgr)) {
                var s = sensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
                if (s == null || !sensorMgr.registerListener(
                        this,
                        s,
                        SensorManager.SENSOR_DELAY_UI
                    )
                ) {
                    Log.e(TAG, "Sensor accelerometer could not be enabled")
                }
                s = sensorMgr.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
                if (s == null || !sensorMgr.registerListener(
                        this,
                        s,
                        SensorManager.SENSOR_DELAY_UI
                    )
                ) {
                    Log.e(TAG, "Sensor magnetic field could not be enabled")
                }
            } else {
                var s = sensorMgr.getDefaultSensor(Sensor.TYPE_ORIENTATION)
                if (s == null || !sensorMgr.registerListener(
                        this,
                        s,
                        SensorManager.SENSOR_DELAY_UI
                    )
                ) {
                    Log.e(TAG, "Sensor orientation could not be enabled.")
                    s = sensorMgr.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
                    if (s == null || !sensorMgr.registerListener(
                            this,
                            s,
                            SensorManager.SENSOR_DELAY_UI
                        )
                    ) {
                        Log.e(TAG, "Sensor rotation could not be enabled.")
                    }
                }
            }
            sensorRegistered = true
        }
    }

    private fun hasOrientaionSensor(sensorMgr: SensorManager): Boolean =
        (sensorMgr.getDefaultSensor(Sensor.TYPE_ORIENTATION) != null
                || sensorMgr.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null)

    override fun onSensorChanged(event: SensorEvent) {
        if (inUpdateValue) {
            return
        }
        synchronized(this) {
            if (!sensorRegistered) {
                return
            }
            inUpdateValue = true
            try {
                var value = 0f
                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> System.arraycopy(
                        event.values,
                        0,
                        gravs,
                        0,
                        3
                    )
                    Sensor.TYPE_MAGNETIC_FIELD -> System.arraycopy(
                        event.values,
                        0,
                        geoMags,
                        0,
                        3
                    )
                    Sensor.TYPE_ORIENTATION, Sensor.TYPE_ROTATION_VECTOR -> value = event.values[0]
                    else -> return
                }
                if (event.sensor.type == Sensor.TYPE_ACCELEROMETER || event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                    val success =
                        SensorManager.getRotationMatrix(rotationM, null, gravs, geoMags)
                    if (!success) {
                        return
                    }
                    val orientation =
                        SensorManager.getOrientation(rotationM, FloatArray(3))
                    value = Math.toDegrees(orientation[0].toDouble()).toFloat()
                } else if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    SensorManager.getRotationMatrixFromVector(rotationM, event.values)
                    val orientation = SensorManager.getOrientation(rotationM, FloatArray(3))
                    value = Math.toDegrees(orientation[0].toDouble()).toFloat()
                }
                value = calcScreenOrientationCorrection(value)
                value = calcGeoMagneticCorrection(value)
                val valRad = (value / 180f * Math.PI).toFloat()
                lastValSin = sin(valRad.toDouble()).toFloat()
                lastValCos = cos(valRad.toDouble()).toFloat()
                // lastHeadingCalcTime = System.currentTimeMillis();
                avgValSin = lastValSin
                avgValCos = lastValCos
                heading = getAngle(avgValSin, avgValCos)
            } finally {
                inUpdateValue = false
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    private fun getAngle(sinA: Float, cosA: Float): Float =
        unifyRotationTo360(
            (atan2(sinA.toDouble(), cosA.toDouble()) * 180 / Math.PI).toFloat()
        )

    private fun unifyRotationTo360(rotate: Float): Float {
        var rotate = rotate
        while (rotate < -180) {
            rotate += 360f
        }
        while (rotate > +180) {
            rotate -= 360f
        }
        return rotate
    }

    private fun calcScreenOrientationCorrection(value: Float): Float {
        var internalVal = value
        when (currentScreenOrientation) {
            1 -> internalVal += 90f
            2 -> internalVal += 180f
            3 -> internalVal -= 90f
        }
        return internalVal
    }

    private fun calcGeoMagneticCorrection(value: Float): Float {
        var internalVal = value
        val l = getLocation()
        if (previousCorrectionValue == 360f && l != null) {
            val gf = GeomagneticField(
                l.lat.toFloat(), l.lng.toFloat(), 0.0f,
                System.currentTimeMillis()
            )
            previousCorrectionValue = gf.declination
        }
        if (previousCorrectionValue != 360f) {
            internalVal += previousCorrectionValue
        }
        return internalVal
    }

    private companion object {

        private const val TAG = "OrientationDataSource"
    }
}