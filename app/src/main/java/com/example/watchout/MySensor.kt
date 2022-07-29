package com.example.watchout

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService

class MySensor(val context: Context) : SensorEventListener {
    private var sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    //발걸음 변수
    private val stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private var startSteps : Int = 0
    private var resSteps :  Int = 0
    private var endSteps : Int = 0


    //심박수 변수
    private val heartSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_HEART_RATE)
    var heartRate : Int = 0

    fun startSensor(){
        if (stepSensor != null && heartSensor != null) {
            sensorManager?.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_FASTEST)
            sensorManager?.registerListener(this, heartSensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
        else{
            Log.d("센서로그","startSensor : 센서 없음 ")
        }
    }

    fun stopSensor(){
        Log.d("센서로그","종료 Steps : " + resSteps)
        sensorManager?.unregisterListener(this)
        startSteps = 0
        resSteps = 0
        endSteps = 0
    }

    fun pauseManager(){
        sensorManager?.unregisterListener(this)
    }

    fun resumeManager(){
        sensorManager?.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_FASTEST)
        sensorManager?.registerListener(this, heartSensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    //총발걸음수
    fun getresSteps() : Int{
        resSteps = endSteps - startSteps
        Log.d("센서로그","총 발걸음 수  : " + resSteps)
        return resSteps
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.d("센서로그","onAccuracyChanged: Sensor: $sensor; accuracy: $accuracy")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        //StepCounter
        if(event!!.sensor.type == Sensor.TYPE_STEP_COUNTER){
            if (startSteps < 1) {
                // 초기값
                startSteps = event!!.values[0].toInt();
                Log.d("센서로그","startSteps<1  : " + startSteps)
            }
            endSteps = event!!.values[0].toInt()
            Log.d("센서로그","endSteps : " + endSteps)
        }

        //HeartRate
        if(event!!.sensor.type == Sensor.TYPE_HEART_RATE){
            val heartRateFloat = event!!.values[0]
            heartRate = heartRateFloat.toInt()
           // Log.d("센서로그","심박수  : " +heartRate.toString())
        }
    }

}

