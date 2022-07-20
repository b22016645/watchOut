package com.example.watchout

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.os.*
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import model.DirectionItem
import route.DetailRoute
import utils.Constant.API.LOG
import java.util.*
import kotlin.math.round

class NavigationActivity : Activity(), LocationListener {

    lateinit var text: TextView

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest:LocationRequest
    private val REQUEST_PERMISSION_LOCATION = 10

    //mqtt관련
    private lateinit var myMqtt: MyMqtt
    val sub_topic = "android"

    //음성출력관련
    private lateinit var tts: TextToSpeech

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

    private var sppoint = 0  //특정 장소에서의 알람을 반복하지 않기 위해



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TTS를 생성하고 OnInitListener로 초기화 한다.
        tts= TextToSpeech(this){
            if(it==TextToSpeech.SUCCESS){
                val result = tts?.setLanguage(Locale.KOREAN)
                if(result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED){
                    Log.d("로그","지원하지 않은 언어")
                    return@TextToSpeech
                }
                Log.d("로그","TTS 세팅 성공")
            }else{
                Log.d("로그","TTS 세텅 실패")
            }
        }

        //화면이 꺼지지 않게
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContentView(R.layout.activity_navigation)
        Log.d(LOG,"Navigation호출됨")

        text = findViewById<TextView>(R.id.text)

        //mqtt관련
        myMqtt = MyMqtt(this)
        myMqtt.connect(arrayOf<String>(sub_topic))

        //Intent값 받기
        val naviData = intent.getSerializableExtra("naviData") as model.NaviData

        midpointList = naviData.midPointList
        turnTypeList = naviData.turnTypeList
        destination = naviData.destination
        turnPoint = naviData.turnPoint

        Log.d(LOG,"NavigationActivity - turnTypeList : " +"${turnTypeList}")

        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator

        //통합 위치 정보 제공자 클라이언트의 인스턴스
        locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 5000

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        startLocationUpdates()

    }

//    override fun onSaveInstanceState(outState: Bundle?) {
//        outState?.putBoolean(REQUESTING_LOCATION_UPDATES_KEY, requestingLocationUpdates)
//        super.onSaveInstanceState(outState)
//    }

    private fun startLocationUpdates() {
        if (checkPermissionForLocation(this)) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {

            }
            fusedLocationClient.requestLocationUpdates(locationRequest,
                locationCallback,
                Looper.getMainLooper());
        }
    }

    val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            if (locationResult == null) {
                return
            }

            for (location in locationResult.locations) {
                if (location != null) {
                    lon = location.longitude
                    lat = location.latitude

                    if(midpointList.size!=0) { //NavigationActivity가 겹치는 문제를 해결
                        Log.d(LOG,"==========================================================================================================")
                        Log.d(LOG,"midpointNum : ["+"${midPointNum}"+"/"+"${midpointList.size-1}"+"] " + "현재위치 : ["+"${lat}"+", "+"${lon}"+"] "  + "다음위치 : ["+"${midpointList[midPointNum][0]}"+", "+"${midpointList[midPointNum][1]}"+"]" )

                        var nowBuilder = StringBuilder()
                        nowBuilder.append(lat.toString()).append(",").append(lon.toString())
                        var now = nowBuilder.toString()
                        publish("now",now)

                        //UI에서 보이는 거리 -> 현재 위치에서부터 가장 가까운 분기점까지
                        var distance = (round(DetailRoute.getDistance( lat, lon, turnPoint[turnPointCount][0], turnPoint[turnPointCount][1]))*10)/10
                        text.setText("${distance}"+"m")

                        var turnNum = turnTypeList[midPointNum]

                        //안내시작 일단 0->1을 안내
                        //나침반불러와서
                        if (midPointNum == 0 && sppoint == 0 ) {
                            vibe(1000,150)
                            sppoint ++
                            Log.d(LOG, "NavigationActivity 첫 방향 조정")
                            doSensor(lat,lon)
                        }
                        //도착
                        else if (midPointNum == midpointList.size-1 && sppoint == 0) {
                            vibe(1500,100)
                            publish("topic","목적지에 도착하였습니다")
                            Log.d(LOG,"안내완료")
                            clear()
                            val returnIntent = Intent()
                            setResult(0,returnIntent)
                            finish()
                        }
                        else {
                            //분기점일때
                            if ((turnNum in 12..19) && sppoint == 0) {
                                vibe(1000, 150)
                                sppoint ++
                                turnPointCount++
                                publish("topic", "분기점을 마주했습니다")
                                Log.d(LOG, "NavigationActivity 분기점일 때")
                                doSensor(lat, lon)
                            }

                            //위험요소 (횡단보도, 육교 등)
                            else if ((turnNum in 125..129 || turnNum in 211..217) && sppoint == 0) {
                                publish("topic", "위험요소 앞입니다")
                                ttsSpeak("버튼을 눌러 목적지를 말하세요.")
                                sppoint ++
                                Log.d(LOG, "NavigationActivity 위험요소")
                                val turnName = when (turnNum) {
                                    125 -> "육교"
                                    211 -> "직진 횡단보도"
                                    212 -> "좌측 횡단보도"
                                    213 -> "우측 횡단보도"
                                    214 -> "8시 횡단보도"
                                    215 -> "10시 횡단보도"
                                    216 -> "2시 횡단보도"
                                    217 -> "4시 횡단보도"
                                    else -> ""
                                }
                                ttsSpeak("${distance}"+"m 뒤에 "+"${turnName}"+"입니다")
                            }

                            //예외 (엘베, 직진암시)
                            else if ((turnNum == 218 || turnNum == 233) && sppoint == 0 ){
                                Log.d(LOG,"NavigationActivity 예외 길")
                            }

                            //분기점 다음 좌표에서 직진임을 알려줌
                            else if (sppoint == 1){
                                ttsSpeak("다음 안내까지 "+"${distance}"+"m 직진입니다")
                            }

                            var distanceRange = 8.0      //오차범위

                            //경로이탈인지 아닌지 판단 (false 반환시 이탈)
                            if (!DetailRoute.isRightCource(lat,
                                    lon,
                                    midpointList[midPointNum][0],
                                    midpointList[midPointNum][1],distanceRange)){
                                //p1에서 멀어졌는데
                                if (!DetailRoute.isRightCource(lat,
                                        lon,
                                        midpointList[midPointNum+1][0],
                                        midpointList[midPointNum+1][1],distanceRange)){
                                    //p2에서도 멀어졌다.
                                    outNum++
                                    Log.d(LOG, "NavigationActivity " + "${outNum}" + "번 나갔다.")
                                    //15초동안 이탈이면 경로이탈
                                    if (outNum > 15) {
                                        youOut()
                                        outNum = 0
                                        Log.d(LOG, "NavigationActivity 경로이탈일 때")
                                        clear()
                                        val returnIntent = Intent()
                                        returnIntent.putExtra("destination", destination)
                                        setResult(3, returnIntent)
                                        finish()
                                    }
                                } else {
                                    outNum = 0
                                    Log.d(LOG, "NavigationActivity p1->p2")
                                    midPointNum++
                                    sppoint = 0

                                    //다음 좌표가 분기점일때
                                    if (turnTypeList[midPointNum+1] in 12..19) {
                                        Log.d(LOG, "다음좌표 분기점입니다")
                                        when (turnTypeList[midPointNum+1]) {
                                            12, 16, 17 -> ttsSpeak("${distance}"+"m 뒤에 좌회전입니다")
                                            13, 18, 19 -> ttsSpeak("${distance}"+"m 뒤에 우회전입니다")
                                            else -> Log.d(LOG,"uuuuuuuuuuu턴")
                                        }
                                    }
                                }
                            }
                            else{
                                sppoint++
                            }
                        }
                    }
                }
            }
        }
    }

//    override fun onDestroy() {
//        super.onDestroy()
//        myMqtt.mqttClient.unregisterResources()
//    }

    //mqtt관련
    fun publish(topic:String,data:String){
        Handler().postDelayed(java.lang.Runnable {
            myMqtt.publish(topic, data)
        },1000)
    }

    private fun doSensor(lat : Double, lon : Double) {
        //SensorActivity 실행
        publish("vibe","start")
        var sensorItem = DirectionItem(lat,lon,midpointList,midPointNum)
        val intent = Intent(this, DirectionActivity::class.java)
        intent.putExtra("sensorItem",sensorItem)
        startActivityForResult(intent, 4)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if(keyCode== KeyEvent.KEYCODE_BACK){
            publish("topic","목적지를 재 입력합니다")
            publish("route","restart")
            clear()
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
                publish("topic", "이동중입니다...")
            }
            else if (resultCode == 2){
                publish("vibe", "stop")
                publish("topic","목적지를 재 입력합니다")
                publish("route","restart")
                clear()
                Handler().postDelayed(java.lang.Runnable {
                    val returnIntent = Intent()
                    setResult(2,returnIntent)
                    finish()
                },1000)
            }
        }
    }

    //평소 비프음
    fun goStraiht() {
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

    //clear함수
    fun clear(){
        midpointList.clear()
        turnPoint.clear()
        turnTypeList.clear()
    }

    override fun onLocationChanged(p0: Location) {}

    // 위치 권한이 있는지 확인하는 메서드
    private fun checkPermissionForLocation(context: Context): Boolean {
        // Android 6.0 Marshmallow 이상에서는 위치 권한에 추가 런타임 권한이 필요
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                true
            } else {
                // 권한이 없으므로 권한 요청 알림 보내기
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_PERMISSION_LOCATION)
                false
            }
        } else {
            true
        }
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    override fun onResume() {
        super.onResume()
        //if (requestingLocationUpdates)
            startLocationUpdates()
    }

    //음성출력
    private fun ttsSpeak(strTTS:String){
        tts.speak(strTTS, TextToSpeech.QUEUE_ADD,null,null)
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

}