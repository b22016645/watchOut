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

        //FireBase환경세팅
        Log.d("firebase","파이어베이스 환경세팅")
        auth = Firebase.auth
        firestore = FirebaseFirestore.getInstance()
        firebaseLogin()                         //파이어베이스 로그인
        uid = FirebaseAuth.getInstance().currentUser?.uid
        Log.d("firebase","$uid")
        algorithmWeightFromDB()                 //파이어베이스에서 알고리즘가중치값 읽어와서 세팅


        setTTS()        //TTS세팅 및 초기화


        //화면이 꺼지지 않게
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(R.layout.activity_main)


        //레이아웃 세팅
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
                startSTT(10)
                overridePendingTransition(0, 0)
            }
            else {
                clickNum = 1
            }
            Handler().postDelayed(java.lang.Runnable {
                if (clickNum == 1){
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
                ttsSpeak("버튼을 눌러 목적지를 말하세요.")
                publish("topic", "목적지 입력을 시작했습니다")
                val intent = Intent(this, SpeechToTextActivity::class.java)
                startActivityForResult(intent, 0)
            }
        }
        else{ //즐겨찾기등록시
            ttsSpeak("즐겨찾기에 등록할 별명을 말해주세요")
            Log.d(LOG,"즐겨찾기 등록 시작")
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
                var sttResultMsg = data?.getStringExtra("sttResultMsg")
                publish("des",sttResultMsg!!)

                if(requestCode == 10) { //즐겨찾기 등록시
                    publish("topic","즐겨찾기 등록 시작")
                    Favorites.dat.replace("nickname",sttResultMsg) //DB즐겨찾기 추가

                    addFavorite()
                }
                else {
                    publish("topic","목적지를 입력하였습니다")

                    //우선 db즐찾에서 검색
                    searchFromDB(sttResultMsg!!)
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
            else if (resultCode == RESULT_CANCELED) { //음성이 이상할 때 혹은 204에러 때 다시 stt호출
                publish("topic","목적지를 다시 입력해주세요")
                startSTT(0)
            }
        }


        //NavigationActivity에서 받음
        if ( requestCode == 1 ) {
            if (resultCode == RESULT_OK ) {//목적지 도착했을 때
                Log.d(LOG, "도착")
                dofavor = true
                History.arrivedTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))      //DB저장용

                addHistory()
                Log.d("파이어베이스 히스토리 데이터 저장","${History}")
                preferenceQuestion()  //선호도 조사
                updatePreferenceToDB()  //선호도 조사 결과 DB에 업데이트

                ttsSpeak("목적지에 도착했습니다. 목적지를 즐겨찾기에 등록하려면 아래버튼을 두번 이상 눌러주세요.")

                //finish()
            }
            else if (resultCode == 2) {//navi에서 뒤로가기 버튼을 눌렀을 때
                Log.d(LOG, "뒤로가기 버튼 누름 : Navigation -> Main")
                modified = 4
                startSTT(0)
            }

            else if (resultCode == 3) {//경로이탈일 때
                ttsSpeak("경로를 이탈하였습니다")
                publish("route","out")
                var destination = data.getStringExtra("destination")
                var doRrtrofitData = DoRetrofitData(destination,lat,lon)
                val intent = Intent(this, DoRetrofitActivity::class.java)
                intent.putExtra("doRrtrofitData",doRrtrofitData)
                //DoRetrofit 실행
                startActivityForResult(intent, 100)
            }
        }
    }
    private fun preferenceQuestion() {      //경로 끝나고 선호도 조사하는 함수
/*        1. 경로 만족도를 1~10사이 숫자로 말씀해주세요
            1-1. 6점 이상인 경우
                - 설문을 종료하시겠습니까?
                    * 예 -> 종료
                    *아니오 -> 설문시작
*/
/*
           score = -1
           while (score >10 || score <0){
               "경로만족도를 0~10사이 숫자로 말씀해주세요"
               var score = 사용자가 말한 숫자
               if(score > 10 || score <0)
                    "잘못된 범위입니다."
           }

           Preference.score = score         //우선 만족도 프리퍼런스오브젝트에 저장

           while (score <6) {
                "설문을 종료하시려면 종료, 계속하시려면 "계속"이라고 말해주세요"
                if(ans == "종료")
                    preferenceQuestion()함수를 종료하는 코드
                else if (ans =="계속")
                    while블록에서 나와서 함수를 계속하는 코드 (break)
                else
                    "잘못된 음성입니다."
           }

 */

 /*
            1-2. 6점 미만인 경우 ->설문시작
            “향후 경로안내를 위한 선호도 조사 질문입니다. 현재 상태 유지를 원하시면 유지 라고 말하세요”

            A. 도로타입 배점질문
            : 경로 안내에 있어 전반적인 도로상태와 시설물개수 중 더 중요한 것은 무엇입니까? 도로, 시설물, 유지 중 하나를 말씀하세요
            :테이블 웨이트 조정

            B. 분기점질문
            :직진우선길과 최단거리우선중 어떤 경로를 선호하십니까? 직진우선, 최단거리, 유지 중 하나를 말씀하세요
            :turntype가중치 조절
*/

/*
        “향후 경로안내를 위한 선호도 가중치 조절을 시작합니다. 현재 상태 유지를 원하시면 유지 라고 말하세요”
        preferenceQuestion_tableWeight()

        fun preferenceQuestion_tableWeight(){
            while(true){
                 "경로 안내에 있어 전반적인 도로상태와 시설물개수 중 더 중요한 것은 무엇입니까? 도로, 시설물, 유지 중 하나를 말씀하세요"
                ans = "사용자가 말한거"
                if (ans == "유지")
                    Log.d("Main-preferenceQuestion_tableWeight()","유지")
                    break
                else if (ans == "도로")
                    setPreference(tableWeight, -1)
                    break
                else if (ans == "시설물 개수")
                    setPreference(tableWeight, +1)
                    break
                else
                    "잘못된 음성입니다."
            }
         } //end of preferenceQuestion_tableWeight()



         preferenceQuestion_turnPoint()

         fun preferenceQuestion_turnPoint(){
             while(true){
                "직진우선길과 최단거리우선중 어떤 경로를 선호하십니까? 직진우선, 최단거리, 유지 중 하나를 말씀하세요"

                if (ans == "유지")
                    Log.d("Main-preferenceQuestion_turnPoint()","유지")
                    break
                else if (ans == "직진우선")
                    //분기점 가중치를 높힌다  ->음수값이 클수록
                    setPreference(turnPoint, -5)
                    break
                else if (ans == "최단거리 ")
                    //분기점 가중치를 낮춘다
                    setPreference(turnPoint, +5)
                    break
                else
                   "잘못된 음성입니다."
            }
         }//End of preferenceQuestion_turnPoint()


*/

/*
            if) 시설물이 있는 경우
                시설물질문 : 시설물 있는 경우에만
                →횡단보도/ 위험시설A(엘베,육교,지하보도,계단) / 위험시설B(교량,터널,고가도로,대형시설물이동통로)
            : 이용하신 경로에는 (시설물) 이 있었습니다. 향후 (시설물)이 최소화된 길을 안내받으시려면 “최소화”, 현재 상태 유지를 원하시면 “유지”를 말씀하세요

            :해당 시설물 가중치 조절*/



    }//End of preferenceQuestion

    fun setPreference(category :String, value:Int){

        //tableWeight가중치 조절. 클수록 Danger 중요, 작을수록 Road 중요
        //       tableWeight:     -> 기본값 = 0.5 | min = 0.1 | max = 1)
        //      roadscore의 범위는 고정 :  50 ~ 100 | 50 ~ 100 | 50 ~ 100
        //  해당 tw일때 DangerScore 범위 :  0 ~ -25 |  0 ~ -5  | 0 ~ -50
        //  해당 tw일때 routeScore 범위 :  25 ~ 100 | 45 ~ 100 | 0 ~ 100
        if (category =="tableWeight"){
            if (value > 0){
                if (Preference.tableWeight >=1)
                    Log.d("Main-setPreference()","tableWeight가 이미 최대치(1)임")
                else
                    Preference.tableWeight += 0.1
                    Log.d("Main-setPreference()","증가된 tableWeight : ${Preference.tableWeight}")
            }
            else if (value < 0){
                if (Preference.tableWeight <=0.1)
                    Log.d("Main-setPrefernece()","tableWeigt가 이미 최소치(0.1)임")
                else
                    Preference.tableWeight -= 0.1
                Log.d("Main-setPreference()","감소된 tableWeight : ${Preference.tableWeight}")
            }
        }

        //분기점 가중치 조정
        else if (category =="turnPoint")
            Preference.algorithmWeight_turnPoint?.plus(value)

        else if (category == " crossWalk")
            Preference.algorithmWeight_crossWalk?.plus(value)

        else if (category =="facilityCar")
            Preference.algorithmWeight_facilityCar?.plus(value)

        else if (category =="facilityNoCar")
            Preference.algorithmWeight_facilityNoCar?.plus(value)

    }//End of setPreference()



    /////////////////////////////////////////////////////////////////////////////
    //                            FIREBASE(DB) 관련 함수                         //
    /////////////////////////////////////////////////////////////////////////////
    fun firebaseLogin(){
        //고정로그인(테스트용)
        auth?.signInWithEmailAndPassword("watch@out.com", "watchout1234")?.addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                Log.d("파이어베이스로그인", "로그인 성공" + "${auth}")
                Log.d("시간",LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
            } else {
                Log.d("파이어베이스로그인", "로그인 실패" + "${auth}")
            }
        }
    }//End of firebaseLogin()

    fun algorithmWeightFromDB() {
        //FireBase에서 알고리즘 가중치를 불러와 데이터스냅샷 형태로 저장후 잘라서 싱글톤객체 Preference에 저장.
        var snapshotData: Map<String, Any>
        val dbData = firestore!!.collection("PersonalData").document("${uid}")
        dbData.get()
            .addOnSuccessListener { doc ->
                if (doc != null) {

                    snapshotData = doc.data as Map<String, Any>
                    Log.d("MainActivity-algorithmWeightFromDB()","알고리즘 가중치 DB에서 불러와서 셋팅합니다.")
                    Preference.algorithmWeight_crossWalk = "${snapshotData.get("algorithmWeight_crossWalk")}".toDouble()
                    Preference.algorithmWeight_facilityCar = "${snapshotData.get("algorithmWeight_crossWalk")}".toDouble()
                    Preference.algorithmWeight_facilityNoCar = "${snapshotData.get("algorithmWeight_facilityNoCar")}".toDouble()
                    Preference.tableWeight = "${snapshotData.get("tableWeight")}".toDouble()
                    Preference.algorithmWeight_turnPoint = "${snapshotData.get("algorithmWeight_turnPoint")}".toDouble()
                    Preference.score = "${snapshotData.get("score")}".toInt()

                } else {
                    Log.d("에러 : 알고리즘 가중치값 DB에서 불러오기", "No such document")
                }
            }
            .addOnFailureListener { exception ->
                Log.d("에러: 알고리즘 가중치값 DB에서 불러오기", "get failed with ", exception)
            }
    }   //End of algorithmWeightFromDB

    fun searchFromDB(destinationName : String) {
        //즐겨찾기에서 먼저 검색하는 코드입니다
        var fav: Map<String, Any>
        //destinationName = "영심이네"     //음성파일을 string형으로 변환한 데이터 ( 목적지)
        var favFromDB =
            firestore!!.collection("PersonalData").document("${uid}").collection("Favorites")
                .document("${destinationName}")
        favFromDB.get()
            .addOnSuccessListener { dat ->
                if (dat.data != null) {
                    Log.d("firebase dat","${dat}")
                    Log.d("firebase data","${dat.data}")
                    fav = dat.data as Map<String, Any>
                    favFromDB.update("frequency", FieldValue.increment(1))
                        .addOnSuccessListener {
                            Log.d("DB_Favorites_Frequency", "+1업데이트완료")
                        }
                    Log.d("즐겨찾기를 맵형태로 불러온다", "${fav}")
                    Log.d("즐겨찾기에서 특정 데이터를 불러오는 코드", "${fav.get("address")}")

                    var dpLat = fav.get("lat") as Double
                    var dpLon = fav.get("lon") as Double
                    Log.d("즐찾","${dpLat}")
                    Log.d("즐찾","${dpLon}")

                    var doRrtrofitData = DoRetrofitData(destinationName,dpLat,dpLon)

                    val intent = Intent(this, DoRetrofitActivity::class.java)
                    intent.putExtra("doRrtrofitData",doRrtrofitData).putExtra("num",1)

                    //DoRetrofit 실행
                    startActivityForResult(intent, 100)


                } else { //등록 안되어있을때
                    Log.d("firebase data","${dat.data}")
                    Log.d("DB_Favorites_ERROR", "즐찾에 등록이 안된 목적지임")

                    var doRrtrofitData = DoRetrofitData(destinationName,lat,lon)

                    val intent = Intent(this, DoRetrofitActivity::class.java)
                    intent.putExtra("doRrtrofitData",doRrtrofitData).putExtra("num",0)

                    //DoRetrofit 실행
                    startActivityForResult(intent, 100)
                }
            }
            .addOnFailureListener { exception ->
                Log.d("DB_Favorites_ERROR", "즐겨찾기에 DB 도큐먼트 레퍼런스 불러오지 못하였음")

                var doRrtrofitData = DoRetrofitData(destinationName,lat,lon)

                val intent = Intent(this, DoRetrofitActivity::class.java)
                intent.putExtra("doRrtrofitData",doRrtrofitData).putExtra("num",0)

                //DoRetrofit 실행
                startActivityForResult(intent, 100)
            }
    }   //End of searchFromDB()

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
    }   //End Of addFavorite()

    private fun addHistory() {
        firestore!!.collection("PersonalData").document("${uid}").collection("History").document(
            "${
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            }"
        ).set(History)
            .addOnSuccessListener {
                Log.d("History", "${History}")
                Log.d(LOG, "히스토리 등록 완료")
                //        ttsSpeak("히스토리 등록 완료")
                //       ^^얘 자꾸 오류나서 임시로 주석처리해놓았어요^^
            }.addOnFailureListener { exception ->
                Log.d("히스토리 저장 실패", exception.toString())
            }
    }//end Of addHistory()

    private fun updatePreferenceToDB() {        //업데이트된 알고리즘가중치 (Preference) DB에 업데이트
        Log.d("MainActivity-updatePreferenceToDB() ","업데이트된 알고리즘가중치 (Preference) DB에 업데이트")
        Preference.score = 9999
        firestore!!.collection("PersonalData").document("${uid}").set(Preference)
            .addOnSuccessListener{
                Log.d("MainActivity-updatePreferenceToDB() ","DB에 알고리즘 가중치 업데이트 완료.")
                //Log.d("DB에 업데이트 완료: 알고리즘 가중치 결과입니다.",Preference.toString()) ->이렇게 로그로 찍어서 업데이트 내역 보여주는거 추가할것

                //        ttsSpeak("선호도 업데이트 완료")
                //       ^^얘 자꾸 오류나서 임시로 주석처리해놓았어요^^
            }.addOnFailureListener{exception ->
                Log.d("MainActivity-updatePreferenceToDB(): 알고리즘가중치 업데이트실패", exception.toString())
            }
    }//End of updatePreferenceToDB




    /*-----------------------------------------------------------------------------------
    -------------------------------------------------------------------------------------
    -------------------------------------------------------------------------------------
    -------------------------------------------------------------------------------------
    -------------------------------------------------------------------------------------
    -------------------------------------------------------------------------------------*/
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

    fun setTTS(){
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
    }
}