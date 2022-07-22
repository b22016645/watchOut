package com.example.watchout

import android.Manifest
import android.app.Activity
import android.content.ContentValues
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
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import model.DoRetrofitData
import model.History
import model.NaviData
import model.Preference
import route.SafeRoute
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

    //FireBase관련
    private var auth : FirebaseAuth? = null     //FireBase Auth
    var firestore : FirebaseFirestore? = null
    private val firebaseDatabase: FirebaseDatabase = FirebaseDatabase.getInstance()     //FireBase RealTime
    private val databaseReference: DatabaseReference = firebaseDatabase.reference       //FireBase RealTime
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

    //여기서부터 onCreate
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
       // LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        auth = Firebase.auth
        firestore = FirebaseFirestore.getInstance()
        auth?.signInWithEmailAndPassword("watch@out.com", "watchout1234")?.addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                Log.d("파이어베이스로그인", "로그인 성공" + "${auth}")
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


        //FireBase에서 알고리즘 가중치를 불러와 pf에 저장.
        // 처음 앱 실행하고 한번만 불러오고 도중에 바꿀 필요 없기 때문에 싱글톤 객체 Prefrence사용
        // 파이어베이스에서 데이터를 가져오는데 시간이 걸리기 때문에 바로 Preference가 업데이트 되지는 않지만
        // SafeRoute 실행 전까지는 충분히 데이터를 가져올 수 있기 때문에 미리 앱 시작하자마자 받아오려고 onCreate에 작성
        // 수정사항>> 데이터를 하나하나씩 가져오지 말고 스냅샷으로 찍어서 한번에 가져올 수 있는 방법 찾기
        uid = FirebaseAuth.getInstance().currentUser?.uid
        firestore = FirebaseFirestore.getInstance()
        firestore!!.collection("Preference")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    Log.d(ContentValues.TAG, "${document.id} => ${document.data}")
                    //Log.d("특정 데이터 가져오는 예시","${document.data.get("score")}")
                    Preference.awcrossWalk = "${document.data.get("awcrossWalk")}".toDouble()
                    Preference.awft_car= "${document.data.get("awft_car")}".toDouble()
                    Preference.awft_noCar= "${document.data.get("awft_noCar")}".toDouble()
                    Preference.tableWeight= "${document.data.get("tableWeight")}".toDouble()
                    Preference.awturnPoint= "${document.data.get("awturnPoint")}".toDouble()
                    Preference.score= "${document.data.get("score")}".toInt()
                }
            }
            .addOnFailureListener { exception ->
                Log.w(ContentValues.TAG, "Error getting documents.", exception)
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
                Log.d("로그","TTS 세텅 실패")
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
        myMqtt.connect(arrayOf<String>(sub_topic))

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
                    Log.d("Test", "Latitude: $lat, Longitude: $lon")

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

                    var nowBuilder = StringBuilder()
                    nowBuilder.append(lat.toString()).append(",").append(lon.toString())
                    var now = nowBuilder.toString()
                    publish("now",now)
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

    private fun startSTT(){
        //SpeechToTextActivity 실행
        if(modified<3){
            ttsSpeak("위치 조정 중")
        }
        else {
            ttsSpeak("버튼을 눌러 목적지를 말하세요.")
            publish("topic", "목적지 입력을 시작했습니다")
            val intent = Intent(this, SpeechToTextActivity::class.java)
            startActivityForResult(intent, 0)
        }
    }

    //물리버튼을 눌러 STT를 실행
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if(keyCode== KeyEvent.KEYCODE_BACK){
            startSTT()

            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    //다른 Activity로부터 결과값 받기
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)

        //SpeechToTextActivity에서 받음
        if (requestCode == 0 ) {
            if (resultCode == RESULT_OK) {
                var byteAudioData: ByteArray? = null
                byteAudioData = data?.getByteArrayExtra("byteAudioData")

                if (byteAudioData != null) { //아무것도 입력하지 않아도 null값은 아니다.
                    publish("topic","목적지를 입력하였습니다")
                    var doRrtrofitData = DoRetrofitData(byteAudioData,"",lat,lon)
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

//                //중간지점 알려줌
//                var midString = "37.58217852030164,127.01152516595631"
//                publish("now",midString)

                val intent = Intent(this, NavigationActivity::class.java)
                intent.putExtra("naviData",naviData)
                startActivityForResult(intent, 1)
            }
            else if (resultCode == RESULT_CANCELED) { //음성이 이상할 때 혹은 204에러 때 다시 stt호출
                publish("topic","목적지를 다시 입력해주세요")
                startSTT()
            }
        }


        //NavigationActivity에서 받음
        if ( requestCode == 1 ) {
            if (resultCode == 0 ) {//목적지 도착했을 때
//                publish("topic","목적지에 도착하였습니다")
                Log.d(LOG, "도착")
                History.arrivedTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))      //DB저장용
                ttsSpeak("목적지에 도착했습니다. 목적지를 즐겨찾기에 등록하려면 아래버튼을 두번 이상 눌러주세요.")
                addFavorite()       //즐겨찾기 추가함수
                 /*         //데이터 베잇 에 히스토리 넘기기
                firestore?.collection("History")?.document("${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))}")?.set(History)
                Log.d("파이어베이스에 히스토리 저장","${History}")

                 */

                //finish()
            }
            else if (resultCode == 2) {//navi에서 뒤로가기 버튼을 눌렀을 때
//                publish("topic","reSTT2")
                Log.d(LOG, "뒤로가기 버튼 누름 : Navigation -> Main")
                startSTT()
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



        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            //권한이 없을 경우 최초 권한 요청 또는 사용자에 의한 재요청 확인
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) &&
                ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            ) {
                // 권한 재요청
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ),
                    100
                )
                return
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ),
                    100
                )
                return
            }
        }
    }


    // 위치 권한이 있는지 확인하는 메서드 seul
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
        TODO("Not yet implemented")
        ttsSpeak("즐겨찾기에 등록할 별명을 말해주세요")

    }

}