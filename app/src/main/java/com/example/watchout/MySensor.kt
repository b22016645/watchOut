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

    // variable gives the running status
    private var running = false

     private var startSteps : Int = 0
      var resSteps :  Int = 0
     private var flag = false
     private var endSteps : Int = 0

     fun startSensor(){
        running = true
       // startSteps = 0
        val stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (stepSensor == null) {
            Log.d("센서로그","디바이스에 스텝센서가 없습니다")
        } else {
            Log.d("센서로그","시작합니당")
            sensorManager?.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_GAME)

        }
    }

     fun getSteps(){
         resSteps = endSteps - startSteps
         Log.d("센서로그","총 발걸음 수  : " + resSteps)
     }

    fun stopSensor(){
        running = false
        Log.d("센서로그","종료 : " + resSteps)
        sensorManager?.unregisterListener(this)
        startSteps = 0
        resSteps = 0
        endSteps = 0
    }


    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.d("센서로그","onAccuracyChanged: Sensor: $sensor; accuracy: $accuracy")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (running) {
            Log.d("센서로그","running 들어옴 ")
            if (startSteps < 1) {
                // 초기값
                startSteps = event!!.values[0].toInt();
                Log.d("센서로그","startSteps<1  : " + startSteps)
            }

                endSteps = event!!.values[0].toInt()
                Log.d("센서로그","flag true , endSteps : " + endSteps)

            }
            //totalSteps = event!!.values[0]

            //val cSteps = totalSteps.toInt()
        }
    }


