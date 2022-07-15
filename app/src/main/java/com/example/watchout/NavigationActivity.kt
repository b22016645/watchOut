package com.example.watchout

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.ActivityCompat
import model.DoRetrofitData
import model.SensorItem
import route.DetailRoute
import java.lang.IndexOutOfBoundsException
import kotlin.math.abs
import utils.Constant.API.LOG
import kotlin.math.round
import java.util.*


class NavigationActivity : Activity(), LocationListener {
//네비게이션바껴됴
    lateinit var text: TextView

    //mqtt관련
    //var server_uri = "tcp://15.165.174.55:1883"
    var server_uri = "tcp://172.20.10.6:1883"
    private lateinit var myMqtt: MyMqtt
    val sub_topic = "android"

    //위치정보를 얻기 위한 변수
    private var locationManager: LocationManager? = null
    private var lastKnownLocation: Location? = null

    //세분화된 좌표를 저장할 배열
    private var midpointList = arrayListOf<List<Double>>()

    //분기점 좌표 저장할 배열
    private var turnTypeList = arrayListOf<Int>()

    //목적지 이름 경로이탈 때 사용
    private var destination = ""

    //경로 이탈에 사용한 변수
    private var outNum : Int = 0

    //경로 안내에 사용한 변수
    private var midPointNum: Int = 0

    //초기 위치 선정을 위한 변수
    private var setting : Int = 0

    //웹에 15초마다 위치 보내기위해
    private var mqttInt : Int = 0

    //UI에서 거리 계산을 위해
    private var turnPoint = arrayListOf<List<Double>>()
    private var turnPointCount = 0

    //진동관련
    lateinit var vibrator: Vibrator

    //현재위치
//    private var lat: Double = 37.58217852030164
//    private var lon: Double = 127.01152516595631
    private var lat: Double = 0.0
    private var lon: Double = 0.0

    private var sppoint = 0  //특정 장소에서의 알람을 반복하지 않기 위해 vibe만 이용하면 onSensor가 있는 애들은 해결이 되는데 나머지가 안되서 만듦


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //화면이 꺼지지 않게
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContentView(R.layout.activity_navigation)
        Log.d(LOG,"Navigation호출됨")


        text = findViewById<TextView>(R.id.text)

        //mqtt관련
        myMqtt = MyMqtt(this,server_uri)
        myMqtt.connect(arrayOf<String>(sub_topic))


        //Intent값 받기
        val naviData = intent.getSerializableExtra("naviData") as model.NaviData

        midpointList = naviData.midPointList
        turnTypeList = naviData.turnTypeList
        destination = naviData.destination
        turnPoint = naviData.turnPoint

        Log.d(LOG,"NavigationActivity - turnTypeList : " +"${turnTypeList}")


        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator

        //위치관련 (GPS)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }


        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        lastKnownLocation = locationManager!!.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        if(lastKnownLocation == null){
            Log.d(LOG,"위치 받기 실패!!")
        }

        if (lastKnownLocation != null) {

            lon = lastKnownLocation!!.longitude
            lat = lastKnownLocation!!.altitude

            locationManager!!.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000,  //1초마다  지금은 천천히 움직이지만 빠르게 움직여 1초 사이에 p1->p3로 가는 경우가 발생할 수 있음. 그래서 시간을 줄일 필요가 있음.
                0.5f, //0.5미터마다 위치 갱신
                gpsLocationListener
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        myMqtt.mqttClient.unregisterResources()
    }

    //mqtt관련
    fun publish(topic:String,data:String){
        Handler().postDelayed(java.lang.Runnable {
            myMqtt.publish(topic, data)
        },1000)
//        myMqtt.publish(topic, data)
    }


    //길안내
    val gpsLocationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {

            lon = location.longitude
            lat = location.latitude
//

            if(midpointList.size!=0) { //NavigationActivity가 겹치는 문제를 해결
                Log.d(LOG,"==========================================================================================================")
                Log.d(LOG,"midpointNum : ["+"${midPointNum}"+"] " + "현재위치 : ["+"${lat}"+", "+"${lon}"+"] "  + "다음위치 : ["+"${midpointList[midPointNum][0]}"+", "+"${midpointList[midPointNum][1]}"+"]" )

                var nowBuilder = StringBuilder()
                nowBuilder.append(lat.toString()).append(",").append(lon.toString())
                var now = nowBuilder.toString()
                publish("now",now)

                //Log.d(LOG,"turnTypeList : ["+"${turnTypeList[midPointNum]}"+"]")

                //UI에서 보이는 거리 -> 현재 위치에서부터 가장 가까운 분기점까지
                text.setText("${(round(DetailRoute.getDistance( lat, lon, turnPoint[turnPointCount][0], turnPoint[turnPointCount][1]))*10)/10}"+"m")

                //도착
                if (midPointNum == midpointList.size-1) {
                    vibe(1500,100)
                    publish("topic","목적지에 도착하였습니다")
                    Log.d(LOG,"안내완료")
                    midpointList.clear()
                    turnPoint.clear()
                    turnTypeList.clear()

                    val returnIntent = Intent()
                    setResult(0,returnIntent)
                    finish()
                }

                //안내시작 일단 0->1을 안내
                //나침반불러와서
                if (midPointNum == 0 && setting == 0 && sppoint == 0 ) {
                    vibe(1000,150)
                    sppoint ++
                    Log.d(LOG, "NavigationActivity 첫 방향 조정")
                    doSensor(lat,lon)
                }
                else {

//                //안내시작 일단 1->2를 안내
//                else if (sppoint == 0 && setting == 1 && (DetailRoute.getDistance( lat, lon, midpointList[0][0], midpointList[0][1]) <= 6)) {
//                    vibe(1000,150)
//                    sppoint ++
//                    Log.d(LOG, "NavigationActivity 두번째 방향 조정")
//                    doSensor(lat,lon)
//                }

                    //분기점일때
                    if ((turnTypeList[midPointNum] >= 212 || (turnTypeList[midPointNum] in 12..19)) && sppoint != 0) {
                        vibe(1000, 150)
                        sppoint = 0
                        turnPointCount++
                        publish("topic", "분기점을 마주했습니다")
                        Log.d(LOG, "NavigationActivity 분기점일 때")
                        doSensor(lat, lon)
                    }

                    //위험요소 (횡단보도, 육교 등)
                    else if (((turnTypeList[midPointNum] in 125..129) || turnTypeList[midPointNum] == 211) && sppoint != 0) {
                        publish("topic", "위험요소 앞입니다")
//                    watchOut()
                        sppoint = 0
                        Log.d(LOG, "NavigationActivity 위험요소")
                    }


                    //경로이탈인지 아닌지 판단
                    if (setting != 0 && (DetailRoute.getDistance(
                            lat,
                            lon,
                            midpointList[midPointNum][0],
                            midpointList[midPointNum][1]
                        ) > 8.0)
                    ) {  //p1에서 멀어졌는데
                        if (DetailRoute.getDistance(
                                lat,
                                lon,
                                midpointList[midPointNum + 1][0],
                                midpointList[midPointNum + 1][1]
                            ) > 8.0
                        ) {  //p2에서도 멀어졌다.
                            outNum++
                            Log.d(LOG, "NavigationActivity " + "${outNum}" + "번 나갔다.")
                            //15초동안 이탈이면 경로이탈
                            if (outNum > 15) {
                                youOut()
                                outNum = 0
                                Log.d(LOG, "NavigationActivity 경로이탈일 때")
                                midpointList.clear()
                                turnPoint.clear()
                                turnTypeList.clear()
                                val returnIntent = Intent()
                                returnIntent.putExtra(
                                    "destination",
                                    destination
                                )  //목적지를 main에 넘기고 그 값을 다시 DoRetrofit으로 넘긴다.
                                setResult(3, returnIntent)
                                finish()
                            }
                        } else { //p1에선 멀지만 p2와 가깝다면  그리고 직진일 때 알림을 주는 건 여기서 주면 되는거 아닌가?? => 맞음 여기서 주면 됨
                            outNum = 0
                            Log.d(LOG, "NavigationActivity p1->p2")
//                        goStraiht()
                            if (midPointNum < midpointList.size - 1) {
                                midPointNum++
                                sppoint++
                            }
                        }
                    }
                }

            }
        }

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }
    private fun doSensor(lat : Double, lon : Double) {
        //SensorActivity 실행
        publish("vibe","start")
        var sensorItem = SensorItem(lat,lon,midpointList,midPointNum,setting)
        val intent = Intent(this, SensorActivity::class.java)
        intent.putExtra("sensorItem",sensorItem)
        startActivityForResult(intent, 4)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if(keyCode== KeyEvent.KEYCODE_BACK){
            publish("topic","목적지를 재 입력합니다")
            publish("route","restart")
            midpointList.clear()
            turnTypeList.clear()
            turnPoint.clear()
            val returnIntent = Intent()
            setResult(2,returnIntent)
            finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)

        //SensorActivity에서 받음
        if (requestCode == 4){
            if (resultCode == RESULT_OK) {
                Log.d(LOG, "방향 조정 완료")
                publish("vibe", "end")
                setting++
                publish("topic", "이동중입니다...")
            }
            else if (resultCode == 2){
                publish("vibe", "stop")
                publish("topic","목적지를 재 입력합니다")
                publish("route","restart")
                midpointList.clear()
                turnTypeList.clear()
                turnPoint.clear()
                Handler().postDelayed(java.lang.Runnable {
                    val returnIntent = Intent()
                    setResult(2,returnIntent)
                    finish()
                },1000)
            }
        }
    }


    fun goStraiht() { //비프음

        vibe(1000,100)
    }


    //위험요소앞일때
    fun watchOut() {
        vibe(2000, 100)
    }

    //경로이탈시
    fun youOut() {
        vibe(2000, 100)
    }

    fun vibe( ms : Long , at : Int ){
        val effect = VibrationEffect.createOneShot(ms, at)
        vibrator.vibrate(effect)
    }

    override fun onLocationChanged(p0: Location) {}
//    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {}

}