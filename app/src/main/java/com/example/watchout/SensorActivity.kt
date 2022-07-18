package com.example.watchout

import android.app.Activity
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.TextView
import utils.Constant
import utils.Constant.API.LOG
import java.lang.IndexOutOfBoundsException
import kotlin.math.abs

class SensorActivity : Activity(), SensorEventListener {

//    //초기 위치 선정을 위한 변수
//    private var setting : Int = 0

    //onsencorChange가 너무 빠르고 많이 호출되기 때문에 진동을 적당히 주기가 어려움. 그래서 사용함.
    private var sensorCount = 1

    //세분화된 좌표를 저장할 배열
    private var midpointList = arrayListOf<List<Double>>()

    //경로 안내에 사용한 변수
    private var midPointNum: Int = 0

    //나침반관련
    private lateinit var mSensorManager: SensorManager
    private lateinit var mAccelerometer: Sensor
    private lateinit var mMagnetometer: Sensor
    private val mLastAccelerometer = FloatArray(3)
    private val mLastMagnetometer = FloatArray(3)
    private var mLastAccelerometerSet = false
    private var mLastMagnetometerSet = false
    private val mR = FloatArray(9)
    private val mOrientation = FloatArray(3)
    private var mCurrentDegree = 0f

    //가속도 센서, 시계를 들었는지 확인할 때 쓰는 변수
    private var accelerationData = FloatArray(3)
    private var magneticData = FloatArray(3)
    private var rotationMatrix = FloatArray(9)
    private var x = 0f
    private var y = 0f
    private var z = 0f

    //진동관련
    lateinit var vibrator: Vibrator

    //현재위치
    private var lat: Double = 0.0
    private var lon: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //화면이 꺼지지 않게
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContentView(R.layout.activity_sensor)
        Log.d(Constant.API.LOG,"Sensor호출됨")

        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator

        //Intent값 받기
        val sensorItem = intent.getSerializableExtra("sensorItem") as model.SensorItem

        lat = sensorItem.lat
        lon = sensorItem.lon
        midpointList = sensorItem.midPointList
        midPointNum = sensorItem.midPointNum
        //setting = sensorItem.setting

        //나침반관련 변수 초기화
        mSensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        onSensor()
    }

    private fun closeSensor() {
        offSensor()
        midpointList.clear()
        val returnIntent = Intent()
        setResult(RESULT_OK,returnIntent)
        finish()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if(keyCode== KeyEvent.KEYCODE_BACK){
            offSensor()
            midpointList.clear()
            val returnIntent = Intent()
            setResult(2,returnIntent)
            finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null) {
            if (event.sensor == mAccelerometer) {
                getAccelerationData(event)
                System.arraycopy(event.values, 0, mLastAccelerometer, 0, event.values.size)
                mLastAccelerometerSet = true
            }
            else if (event.sensor == mMagnetometer) {
                getMagneticFieldData(event)
                System.arraycopy(event.values, 0, mLastMagnetometer, 0, event.values.size)
                mLastMagnetometerSet = true
            }
        }
        if (mLastAccelerometerSet && mLastMagnetometerSet) {
            SensorManager.getRotationMatrix(mR, null, mLastAccelerometer, mLastMagnetometer)
            //현재 나침반 방향
            var azimuthinDegress = ((Math.toDegrees( SensorManager.getOrientation( mR, mOrientation )[0].toDouble()) + 360).toInt() % 360).toFloat()

//            Log.d(LOG,"azimuthinDegress : "+"$azimuthinDegress")
                if (midpointList.size != 0) {  //NavigationActivity가 겹치는 문제를 해결
                    //시계를 들었으면, 나침반 방향이 다음 좌표에 맞을때까지 진동
                    if ((x in -1.8..1.8) && (y in -1.8..1.8) && (z in 9.0..9.8)) {
                        var size = 0f
                        var angle = 0f
                       // if (setting == 0) { //현재위치에서 p1을 바라보기
                        size = getAngle(
                            lat,
                            lon,
                            midpointList[midPointNum + 1][0],
                            midpointList[midPointNum + 1][1]
                        ) - azimuthinDegress
                        angle = getAngle(
                            lat,
                            lon,
                            midpointList[midPointNum + 1][0],
                            midpointList[midPointNum + 1][1]
                        )
                       // }
//                        else {//p1에서 p2바라보기
//                            size = getAngle(
//                                midpointList[midPointNum][0],
//                                midpointList[midPointNum][1],
//                                midpointList[midPointNum + 1][0],
//                                midpointList[midPointNum + 1][1]
//                            ) - azimuthinDegress
//                            angle = getAngle(
//                                midpointList[midPointNum][0],
//                                midpointList[midPointNum][1],
//                                midpointList[midPointNum + 1][0],
//                                midpointList[midPointNum + 1][1]
//                            )
//                        }
//                        Log.d(LOG,"size : "+"${size}")
                        //맞으면 센서 끔
                        if (abs(size) < 2f) {
                            vibe(1500,200)
                            Log.d(LOG,"방향 맞음!")
                            azimuthinDegress = 1000f
                            sensorCount = 0
                            closeSensor()
                        }
                        //아니면 맞을 때 까지 진동
                        else {
                            sensorCount++
                            if (sensorCount % 100 == 0) {  //이러면 대략 1초에 한 번씩 판단함.

                                if ((azimuthinDegress + 179.9f) > 360f) {
                                    var over = (azimuthinDegress + 179.9f) % 360f
                                    if ((angle in azimuthinDegress..360f) || (angle in 0f..over)) {
                                        //오른쪽
                                        turnRoad(2, size)
                                    } else {
                                        //왼쪽
                                        turnRoad(1, size)
                                    }
                                } else {
                                    var over = (azimuthinDegress + 179.9f) % 360f
                                    if (angle in azimuthinDegress..over) {
                                        //오른쪽
                                        turnRoad(2, size)
                                    } else {
                                        //왼쪽
                                        turnRoad(1, size)
                                    }
                                }
                            }

                    }
                }
            }
            try{
                mCurrentDegree = -azimuthinDegress
            } catch (e: IndexOutOfBoundsException) {
                mCurrentDegree = -azimuthinDegress
            }
        }
    }

    //나침반관련

    //나침반센서 켬
    private fun onSensor() {
        Log.d(Constant.API.LOG,"NavigationActivity 나침반센서 ON")
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME)
        mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_GAME)
    }

    //나침반센서 끔
    private fun offSensor() {
        Log.d(Constant.API.LOG,"NavigationActivity 나침반센서 꺼짐 OFF")
        mSensorManager.unregisterListener(this, mAccelerometer)
        mSensorManager.unregisterListener(this, mMagnetometer)
    }

    //가속도
    private fun getMagneticFieldData(event: SensorEvent) {
        magneticData = event.values.clone()
    }

    //가속도
    private fun getAccelerationData(event: SensorEvent) {
        accelerationData = event.values.clone()
        SensorManager.getRotationMatrix(rotationMatrix, null, accelerationData, magneticData)

        x = accelerationData[0]
        y = accelerationData[1]
        z = accelerationData[2]
    }

    private fun getAngle(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        var y1 = lat1 * Math.PI / 180
        var y2 = lat2 * Math.PI / 180
        var x1 = lon1 * Math.PI / 180
        var x2 = lon2 * Math.PI / 180

        var x = Math.sin(x2 - x1) * Math.cos(y2)
        var y = Math.cos(y1) * Math.sin(y2) - Math.sin(y1) * Math.cos(y2) * Math.cos(x2 - x1)
        var rad = Math.atan2(x, y)
        var bearing: Float = ((rad * 180 / Math.PI + 360) % 360).toFloat()
        return bearing
    }

    //분기점시
    fun turnRoad(num : Int, size : Float) {

        val com = size.toInt()
        var at = 0

        //분기점안내 (when)
        when (com) {
            in 120..150 -> at = 230
            in 90..120 -> at = 200
            in 60..90 -> at = 170
            in 30..60 -> at = 130
            in 1..30 -> at = 100
            in 0..1 -> at = 0
            else -> at = 255
        }
        //왼쪽
        if (num == 1) {
            vibe(500,at)
        }
        //오른쪽
        else if (num == 2) {
            val timing = longArrayOf(0,500,0,500)
            val amplitudes = intArrayOf(0, at, 0, at)
            val effect = VibrationEffect.createWaveform(timing,amplitudes,-1)
            vibrator.vibrate(effect)
        }
    }

    fun vibe( ms : Long , at : Int ){
        val effect = VibrationEffect.createOneShot(ms, at)
        vibrator.vibrate(effect)
    }


    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {}

}