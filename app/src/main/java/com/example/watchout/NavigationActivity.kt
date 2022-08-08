package com.example.watchout

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
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
import model.History
import route.DetailRoute
import utils.Constant.API.LOG
import java.io.IOException
import java.util.*
import kotlin.math.round

class NavigationActivity : Activity(), LocationListener {
    private var mContext: Context? = null
    lateinit var text: TextView
    //GPS관련
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest:LocationRequest
    private val REQUEST_PERMISSION_LOCATION = 10

    //mqtt관련
    private lateinit var myMqtt: MyMqtt
    val sub_topic = "android"

    //stepCounter
    private lateinit var mySensor: MySensor
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

        //stepcounter 시작
        mySensor = MySensor(this)
        mySensor.startSensor()

        //Intent값 받기
        val naviData = intent.getSerializableExtra("naviData") as model.NaviData

        midpointList = naviData.midPointList
        turnTypeList = naviData.turnTypeList
        destination = naviData.destination
        turnPoint = naviData.turnPoint
        mContext = this

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

                        var myHeartRate = mySensor.heartRate
                        //현재심박수같이보냄냄
                        var nowBuilder = StringBuilder()
                        nowBuilder.append(lat.toString()).append(",").append(lon.toString()).append(",").append(myHeartRate.toString())
                        var now = nowBuilder.toString()
                        Log.d("센서로그","now : " + now)
                        publish("now",now)

                        //UI에서 보이는 거리 -> 현재 위치에서부터 가장 가까운 분기점까지
                        var distance = (round(DetailRoute.getDistance( lat, lon, turnPoint[turnPointCount][0], turnPoint[turnPointCount][1]))*10)/10
                        text.setText("${distance}"+"m")

                        var turnNum = turnTypeList[midPointNum]

                        //안내시작 일단 0->1을 안내
                        //나침반불러와서
                        if (midPointNum == 0 && sppoint == 0 ) {
                            vibe(1000,100)
                            sppoint ++
                            Log.d(LOG, "NavigationActivity 첫 방향 조정")
                            doSensor(lat,lon)
                        }
                        //도착
                        else if (midPointNum == midpointList.size-1 && sppoint == 0) {
                            endOfRoute()
                        }
                        else {
                            //분기점일때
                            if ((turnNum in 12..19) && sppoint == 0) {
                                turnRoad()
                                sppoint ++
                                turnPointCount++
                                publish("topic", "분기점을 마주했습니다")
                                Log.d(LOG, "NavigationActivity 분기점일 때")
                                doSensor(lat, lon)
/*                                if (outNum ==15) {
                                    History.expTurnPoint.plus(1)
                                }*/
                            }

                            //위험요소 (횡단보도, 육교 등)
                            else if ((turnNum in 125..129 || turnNum in 211..217) && sppoint == 0) {
                                watchOut()
                                publish("topic", "위험요소 앞입니다")
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

                            var distanceRange = 8.0      //오차범위

                            //경로이탈 판단
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
                                        History.expTotal.plus(1)        //경로이탈 +1
                                        if (turnNum == 11)          //직선 경로이탈
                                            History.expLineWay.plus(1)
                                        else if(turnNum in 12..19)                //분기점 경로이탈
                                            History.expTurnPoint.plus(1)
                                        else if ((turnNum in 125..129 || turnNum in 211..217))      //Facility 경로이탈
                                            History.expFacility.plus(1)

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
                                    //분기점 다음 좌표에서 직진임을 알려줌
                                    else if (turnTypeList[midPointNum-1] in 12..19) {
                                        ttsSpeak("다음 안내까지 "+"${distance}"+"m 직진입니다")
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

    private fun endOfRoute() {
        vibe(1500, 100)
        publish("topic", "목적지에 도착하였습니다")
        Log.d(LOG, "안내완료")

        //히스토리 저장용 (발걸음 수)
        val historySteps = mySensor.getresSteps()
        History.stepNum= historySteps         //발걸음수 확정되면 주석 풀어서 히스토리에 넘겨주세요

        //히스토리 저장용 (출발 위경도 -> 주소)
        (History.spLat)?:midpointList[0][0]
        (History.spLon)?:midpointList[0][1]
        History.spName = getAddress(History.spLat!!, History.spLon!!)



        //이제 여기에 히스토리,즐겨찾기 담아 데베로 넘긴다
        /*
     //Firbase 저장 예시입니다
     var preference = Preference()
     //history.arrivedTime = 11111111.1322
     firestore?.collection("Preference")?.document("AlgorithmWeight")?.set(preference)
     Log.d("파이어베이스 데이터 저장","${preference}")
*/




        clear()
        mySensor.stopSensor()
        val returnIntent = Intent()
        setResult(RESULT_OK, returnIntent)
        finish()
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
                var trueName = data.getStringExtra("trueName")
                ttsSpeak("${trueName}"+" 입니다.")
                Log.d(LOG, "${trueName}")
                //publish("vibe", "end")
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

    //위험요소앞일때
    fun watchOut() {
        vibe(2000, 100)
    }

    //경로이탈시
    fun youOut() {
        val timing = longArrayOf(0, 500, 0, 500, 0, 500, 0, 500)
        val amplitudes = intArrayOf(0, 100, 0, 100, 0, 100, 0, 100)
        val effect = VibrationEffect.createWaveform(timing,amplitudes,-1)
        vibrator.vibrate(effect)
    }

    //분기점시
    fun turnRoad() {
        vibe(2000,100)
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


    //음성출력
    private fun ttsSpeak(strTTS:String){
        tts.speak(strTTS, TextToSpeech.QUEUE_ADD,null,null)
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    /*위도 경도 -> 주소*/
    fun getAddress(lat: Double, lon: Double): String? {
        var nowAddr = "없는 주소 입니다." //구글맵에 정보없는 위경도
        val geocoder = Geocoder(mContext, Locale.KOREA)
        val address: List<Address>?
        try {
            address = geocoder.getFromLocation(lat, lon, 1)
            if (address != null && address.size > 0) {
                nowAddr = address[0].getAddressLine(0).toString()
            }
        } catch (e: IOException) {
            Log.d("로그","주소를 가져올 수 없습니다") //인터넷 에러 , 잘못된 위경도
            e.printStackTrace()
        }
        return nowAddr
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
        mySensor.pauseManager()//센서종료
    }

    override fun onResume() {
        super.onResume()
        //if (requestingLocationUpdates)
        startLocationUpdates()
        mySensor.resumeManager() //sensor다시부착
    }


}