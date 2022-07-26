package com.example.watchout

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.watchout.databinding.ActivityMainBinding
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
//import com.google.firebase.database.DatabaseReference
//import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import model.*
import utils.Constant.API.LOG
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class MainActivity : Activity() {

    private lateinit var binding: ActivityMainBinding
    lateinit var x: TextView
    lateinit var y: TextView

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest:LocationRequest
    private val REQUEST_PERMISSION_LOCATION = 10
    val ACTIVITY_RECOGNITION_REQUEST_CODE = 100

    //FireBase관련
    private var auth : FirebaseAuth? = null     //FireBase Auth
    var firestore : FirebaseFirestore? = null
    // private val firebaseDatabase: FirebaseDatabase = FirebaseDatabase.getInstance()     //FireBase RealTime
    // private val databaseReference: DatabaseReference = firebaseDatabase.reference       //FireBase RealTime
    private var uid : String? = null

    //mqtt관련
    private lateinit var myMqtt: MyMqtt
    val sub_topic = "android"

    //음성출력관련
    private lateinit var tts: TextToSpeech

    // 현재위치
    private var lat: Double = 0.0
    private var lon: Double = 0.0
//    private var lat: Double = 37.58217852030164
//    private var lon: Double = 127.01152516595631

    //현재위치조정완료
    private var modified = 0

    //진동관련
    lateinit var vibrator: Vibrator

    //클릭 이벤트 시간 저장할 변수
    private var clickTime: Long = 0

    //클릭 중복 방지 변수
    private var clickNum = 0

    //도착시 즐겨찾기 등록 활성화
    private var dofavor = false

    //여기서부터 onCreate
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
       // LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        auth = Firebase.auth
        firestore = FirebaseFirestore.getInstance()
        auth?.signInWithEmailAndPassword("watch@out.com", "watchout1234")?.addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                Log.d("파이어베이스로그인", "로그인 성공" + "${auth}")
                Log.d("시간",LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
            } else {
                Log.d("파이어베이스로그인", "로그인 실패" + "${auth}")
            }
        }



   /*
        //Firbase 저장 예시입니다
        var preference = Preference()
        //history.arrivedTime = 11111111.1322
        firestore?.collection("Preference")?.document("AlgorithmWeight")?.set(preference)
        Log.d("파이어베이스 데이터 저장","${preference}")
*/


        //FireBase에서 알고리즘 가중치를 불러와 데이터스냅샷 형태로 저장후 잘라서 싱글톤객체 Preference에 저장.
        uid = FirebaseAuth.getInstance().currentUser?.uid
        firestore = FirebaseFirestore.getInstance()

        addFavorite()

        var snapshotData :Map<String,Any>
        val dbData = firestore!!.collection("PersonalData").document("${uid}")
        dbData.get()
            .addOnSuccessListener { doc ->
            if (doc != null) {
                snapshotData = doc.data as Map<String, Any>
                Preference.awcrossWalk = "${snapshotData.get("crossWalk")}".toDouble()
                Preference.awft_car= "${snapshotData.get("ft_car")}".toDouble()
                Preference.awft_noCar= "${snapshotData.get("ft_noCar")}".toDouble()
                Preference.tableWeight= "${snapshotData.get("tableWeight")}".toDouble()
                Preference.awturnPoint= "${snapshotData.get("turnPoint")}".toDouble()
                Preference.score= "${snapshotData.get("score")}".toInt()
            } else {
                Log.d("알고리즘 가중치값 DB에서 불러오기", "No such document") }
            }
            .addOnFailureListener { exception ->
                Log.d("알고리즘 가중치값 DB에서 불러오기", "get failed with ", exception)
            }

        //즐겨찾기에서 먼저 검색하는 코드입니다
        var fav : Map<String,Any>
        var destinationName = "영심이네"     //음성파일을 string형으로 변환한 데이터 ( 목적지)
        var favFromDB = firestore!!.collection("PersonalData").document("${uid}").collection("Favorites").document("${destinationName}")
        favFromDB.get()
            .addOnSuccessListener { dat ->
                if(dat!= null){
                    fav = dat.data as Map<String,Any>
                    favFromDB.update("frequency",FieldValue.increment(1)).addOnSuccessListener {
                        Log.d("DB_Favorites_Frequency","+1업데이트완료")
                    }
                    Log.d("즐겨찾기를 맵형태로 불러온다","${fav}")
                    Log.d("즐겨찾기에서 특정 데이터를 불러오는 코드","${fav.get("address")}")

                }
                else{
                    Log.d("DB_Favorites_ERROR","즐겨찾기 등록은 되어있으나 데이터가 없음")
                }
            }
            .addOnFailureListener { exception ->
                Log.d("DB_Favorites_ERROR", "즐겨찾기에 등록되어 있지 않은 주소, 일반검색으로 넘어갑니다.")
            }





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
                Log.d("로그","TTS 세팅 실패")
            }
        }


        //화면이 꺼지지 않게
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(R.layout.activity_main)

        x = findViewById<TextView>(R.id.x)
        y = findViewById<TextView>(R.id.y)

        x.setText(lon.toString())
        y.setText(lat.toString())

        //mqtt관련
        myMqtt = MyMqtt(this)
//        myMqtt.connect(arrayOf<String>(sub_topic))

        //진동관련
        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator

        //통합 위치 정보 제공자 클라이언트의 인스턴스
        locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 5000

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        startLocationUpdates()

    }

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

                    x.setText(lon.toString())
                    y.setText(lat.toString())

                    modified++
                    if(modified==3){
                        //현재위치가 조정 완료되었다는 tts
                        ttsSpeak("현재위치 조정이 완료되었습니다.")
                        val effect = VibrationEffect.createOneShot(1500, 150)
                        vibrator.vibrate(effect)
                    }

                    Log.d(LOG,"MainActivity - 현재위치 : ["+"${lat}"+", "+"${lon}"+"]")
                    History.spLat = lat     //DB 저장용
                    History.spLon = lon     // DB저장용


                }
            }
        }
    }

    //mqtt관련
    fun publish(topic:String,data:String){
        Handler().postDelayed(java.lang.Runnable {
            myMqtt.publish(topic, data)
        },1000)
    }

    //물리버튼을 눌러 STT를 실행
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if(keyCode== KeyEvent.KEYCODE_BACK){
            // 두번 클릭시 즐겨찾기 등록
            if (SystemClock.elapsedRealtime() - clickTime < 500 ) {
                clickNum = 2
                ttsSpeak("즐겨찾기에 등록할 별명을 말해주세요")
                Log.d(LOG,"즐겨찾기 등록 시작")
                startSTT(10)
                overridePendingTransition(0, 0)
            }
            else {
                clickNum = 1
            }
            Handler().postDelayed(java.lang.Runnable {
                if (clickNum == 1){
                    ttsSpeak("버튼을 눌러 목적지를 말하세요.")
                    publish("topic", "목적지 입력을 시작했습니다")
                    startSTT(0)
                }
            },500)

            clickTime = SystemClock.elapsedRealtime()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun startSTT(intentNum:Int){
        //SpeechToTextActivity 실행
        if (intentNum == 0) { //목적지입력시
            if (modified < 3) {
                ttsSpeak("위치 조정 중")
            } else {
                val intent = Intent(this, SpeechToTextActivity::class.java)
                startActivityForResult(intent, 0)
            }
        }
        else{ //즐겨찾기등록시
            val intent = Intent(this, SpeechToTextActivity::class.java)
            startActivityForResult(intent, 10)
        }
    }

    //다른 Activity로부터 결과값 받기
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)

        //SpeechToTextActivity에서 받음
        if (requestCode == 0 || requestCode == 10) {
            if (resultCode == RESULT_OK) {
                var byteAudioData: ByteArray? = null
                byteAudioData = data?.getByteArrayExtra("byteAudioData")

                if (byteAudioData != null) { //아무것도 입력하지 않아도 null값은 아니다.
                    var doRrtrofitData: DoRetrofitData? = null
                    if(requestCode == 10) { //즐겨찾기 등록시
                        publish("topic","즐겨찾기 등록 시작")
                        doRrtrofitData = DoRetrofitData(byteAudioData,"",1.1,1.1)
                    }
                    else {
                        publish("topic","목적지를 입력하였습니다")
                        doRrtrofitData = DoRetrofitData(byteAudioData,"",lat,lon)
                    }
                    val intent = Intent(this, DoRetrofitActivity::class.java)
                    intent.putExtra("doRrtrofitData",doRrtrofitData)
                    //DoRetrofit 실행
                    startActivityForResult(intent, 100)
                }
            }
        }




        //DoRetrofit에서 받음
        if ( requestCode == 100 ) {
            if (resultCode == RESULT_OK) { //getRoute까지 정상적으로 호출되었을 때
                val naviData = data?.getSerializableExtra("naviData") as NaviData
                var des = naviData.destination
                ttsSpeak("${des}"+", 으로 안내합니다.")
                publish("topic","길 안내를 시작합니다")
                History.departureTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))      //DB저장용

                val intent = Intent(this, NavigationActivity::class.java)
                intent.putExtra("naviData",naviData)
                startActivityForResult(intent, 1)
            }
            else if (resultCode == 0){
                addFavorite()
            }
            else if (resultCode == RESULT_CANCELED) { //음성이 이상할 때 혹은 204에러 때 다시 stt호출
                publish("topic","목적지를 다시 입력해주세요")
                startSTT(0)
            }
        }


        //NavigationActivity에서 받음
        if ( requestCode == 1 ) {
            if (resultCode == RESULT_OK ) {//목적지 도착했을 때
//                publish("topic","목적지에 도착하였습니다")
                Log.d(LOG, "도착")
                dofavor = true
                History.arrivedTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))      //DB저장용
                ttsSpeak("목적지에 도착했습니다. 목적지를 즐겨찾기에 등록하려면 아래버튼을 두번 이상 눌러주세요.")

                //finish()
            }
            else if (resultCode == 2) {//navi에서 뒤로가기 버튼을 눌렀을 때
//                publish("topic","reSTT2")
                Log.d(LOG, "뒤로가기 버튼 누름 : Navigation -> Main")
                startSTT(0)
            }

            else if (resultCode == 3) {//경로이탈일 때
                ttsSpeak("경로를 이탈하였습니다")
                publish("route","out")
                var destination = data.getStringExtra("destination")
                var doRrtrofitData = DoRetrofitData(null,destination!!,lat,lon)
                val intent = Intent(this, DoRetrofitActivity::class.java)
                intent.putExtra("doRrtrofitData",doRrtrofitData)
                //DoRetrofit 실행
                startActivityForResult(intent, 100)
            }
        }
    }




    //사용자 권한, 바꿀거 없음.
    override fun onStart() {
        super.onStart()
        //권한승인여부 확인후 메시지 띄워줌(둘 중 하나라도)
        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
            || ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                0
            )
        }

        if(ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) != PackageManager.PERMISSION_GRANTED){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
                    100
                )
            }
        }

        if(ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BODY_SENSORS
            ) != PackageManager.PERMISSION_GRANTED){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.BODY_SENSORS),
                    100
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            ACTIVITY_RECOGNITION_REQUEST_CODE -> {
                if ((grantResults.isNotEmpty() &&
                            grantResults[0] == PackageManager.PERMISSION_GRANTED)
                ) {
                }
            }
        }
    }


    // 위치 권한
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

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    override fun onResume() {
        super.onResume()
        startLocationUpdates()
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
    private fun addFavorite() {     //즐겨찾기 추가함수
        firestore!!.collection("PersonalData").document("${uid}").collection("Favorites").document("${Favorites.dat.get("nickname")}").set(Favorites.dat)
        .addOnSuccessListener {
                    Log.d("즐겨찾기 저장값입니다", Favorites.dat.toString())
                    Log.d(LOG, "즐겨찾기 등록 완료")
            //        ttsSpeak("즐겨찾기 등록 완료")
            //       ^^얘 자꾸 오류나서 임시로 주석처리해놓았어요^^
                }
            .addOnFailureListener { exception ->
                Log.d("즐겨찾기 저장 실패", exception.toString())
            }
    }
    }