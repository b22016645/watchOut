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
import com.example.vibrateexam.VibratorPattern
import utils.Constant
import utils.Constant.API.LOG
import java.lang.Math.abs

class DirectionActivity : Activity(), SensorEventListener {

    //onsencorChange가 너무 빠르고 많이 호출되기 때문에 진동을 적당히 주기가 어려움. 그래서 사용함.
    private var sensorCount = 1

    private var ccccount = 0

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

    //현재위치
    private var lat: Double = 0.0
    private var lon: Double = 0.0

    private var startDirection = 0f

    private lateinit var viberatorPattern: VibratorPattern

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //화면이 꺼지지 않게
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContentView(R.layout.activity_sensor)
        Log.d(LOG,"Sensor호출됨")

        viberatorPattern = VibratorPattern(this)

        //Intent값 받기
        val directionItem = intent.getSerializableExtra("sensorItem") as model.DirectionItem

        lat = directionItem.lat
        lon = directionItem.lon
        midpointList = directionItem.midPointList
        midPointNum = directionItem.midPointNum

        //나침반관련 변수 초기화
        mSensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        if (midPointNum == 0){
            Log.d(LOG,"direction - midpoint 0일때")
            onSensor()
        }
        else {
            Log.d(LOG,"direction - midpoint 분기점일때")
            startDirection = getAngle(
                midpointList[midPointNum-1][0],
                midpointList[midPointNum-1][1],
                midpointList[midPointNum][0],
                midpointList[midPointNum][1]
            )
            direction()
        }
    }
    private fun direction() {

        if (startDirection != 0f) {

            var endDirection = getAngle(
                midpointList[midPointNum][0],
                midpointList[midPointNum][1],
                midpointList[midPointNum + 1][0],
                midpointList[midPointNum + 1][1]
            )
            var trueDir = endDirection - startDirection

            if (abs(trueDir)>180f) {
                if (trueDir<0f) {
                    trueDir += 360f
                }
                else {
                    trueDir -= 360f
                }
            }

            Log.d(LOG,"trueDir="+"${trueDir}")

            var trueName = when(trueDir) {
                in -30f .. 30f -> "직진"
                in 30f .. 60f -> "2시 방향 우회전"
                in 60f .. 120f -> "우회전"
                in 120f .. 150f -> "4시 방향 우회전"
                in -60f .. -30f -> "10시 방향 좌회전"
                in -120f .. -60f -> "좌회전"
                in -150f .. -120f -> "8시 방향 좌회전"
                else -> "유턴"
            }
            var trueNum = when(trueDir){
                in 30f..150f -> 1
                in -30f..-150f -> 2
                else -> 0
            }
            midpointList.clear()
            val returnIntent = Intent()
            returnIntent.putExtra("trueName",trueName)
            returnIntent.putExtra("trueNum",trueNum)
            setResult(RESULT_OK,returnIntent)
            finish()
        }

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
            var azimuthinDegress = ((Math.toDegrees(SensorManager.getOrientation(mR, mOrientation)[0].toDouble()) + 360).toInt() % 360).toFloat()

            if (midpointList.size != 0) {  //NavigationActivity가 겹치는 문제를 해결
                //시계를 들었으면 현재 바라보는 방향 인식
                if ((x in -1.8..1.8) && (y in -1.8..1.8) && (z in 9.0..9.8)) {
                    sensorCount++
                    if (sensorCount % 100 == 0) {  //이러면 대략 1초에 한 번씩 판단함.
                        ccccount++
                        Log.d(LOG, "ccccount=" + "${ccccount}" + ", azimuthin=" + "${azimuthinDegress}")
//                       viberatorPattern.simplePattern()
                        if (ccccount == 5) {
                            startDirection = azimuthinDegress
                            offSensor()
                        }
                    }
                }

                try {
                    mCurrentDegree = -azimuthinDegress
                } catch (e: IndexOutOfBoundsException) {
                    mCurrentDegree = -azimuthinDegress
                }
            }
        }
    }

    //점1과 점2의 각도를 구함
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

    //나침반센서 켬
    private fun onSensor() {
        Log.d(Constant.API.LOG,"NavigationActivity 나침반센서 ON")
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME)
        mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_GAME)
    }

    //나침반센서 끔
    private fun offSensor() {
        Log.d(Constant.API.LOG,"NavigationActivity 나침반센서 꺼짐 OFF")
        direction()
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

//    fun vibe( ms : Long , at : Int ){
//        val effect = VibrationEffect.createOneShot(ms, at)
//        vibrator.vibrate(effect)
//    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {}

}