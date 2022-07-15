package com.example.watchout

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.ActivityCompat
import com.example.watchout.databinding.ActivityMainBinding
import model.DoRetrofitData
import model.NaviData
import utils.Constant.API.LOG
import java.util.*

class MainActivity : Activity(), LocationListener {

    private lateinit var binding: ActivityMainBinding
    lateinit var x: TextView
    lateinit var y: TextView

    //mqtt관련
    //var server_uri = "tcp://15.165.174.55:1883"
    var server_uri = "tcp://172.20.10.6:1883"
    private lateinit var myMqtt: MyMqtt
    val sub_topic = "android"

    //음성출력관련
    private lateinit var tts: TextToSpeech

    //위치정보를 얻기 위한 변수
    private var locationManager: LocationManager? = null
    private var lastKnownLocation: Location? = null

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
        myMqtt = MyMqtt(this,server_uri)
        myMqtt.connect(arrayOf<String>(sub_topic))

        //진동관련
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
//        val isGpsEnable = locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)
//        Log.d(LOG,"isGps?"+"${isGpsEnable}")
        lastKnownLocation = locationManager!!.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        //lastKnownLocation = locationManager!!.getLastKnownLocation(LocationManager.GPS_PROVIDER)?: locationManager!!.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        if(lastKnownLocation == null){
            Log.d(LOG,"위치 받기 실패!!")

        }
        //현재위치받기
        if (lastKnownLocation != null) {
            lon = lastKnownLocation!!.longitude
            lat = lastKnownLocation!!.altitude

            Log.d(LOG,"현재위치 : ["+"${lat}"+", "+"${lon}"+"]")

            locationManager!!.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000,  //1.0초마다
                0.5f, //0.5미터마다 위치 갱신
                gpsLocationListener
            )
        }
    }

    //현재위치 갱신
    val gpsLocationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            lon = location.longitude
            lat = location.latitude

            x.setText(lon.toString())
            y.setText(lat.toString())

            modified++
            if(modified==3){
//            현재위치가 조정 완료되었다는 tts
//            실제로 조정이 완료되었다고는 할 수 없는데 그냥 알려는 줘야 할 것 같아서
                ttsSpeak("현재위치 조정이 완료되었습니다.")
                val effect = VibrationEffect.createOneShot(1500, 150)
                vibrator.vibrate(effect)
            }

            Log.d(LOG,"MainActivity - 현재위치 : ["+"${lat}"+", "+"${lon}"+"]")

            var nowBuilder = StringBuilder()
            nowBuilder.append(lat.toString()).append(",").append(lon.toString())
            var now = nowBuilder.toString()
            publish("now",now)
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
        if(modified<=4){
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
                ttsSpeak("목적지에 도착했습니다. 어플을 종료합니다")
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

//    //워치 자체에 gps가 꺼져있으면 gps설정으로 이동
//    private fun mOnGPSClick() {
//        if (!locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
//            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
//            intent.addCategory(Intent.CATEGORY_DEFAULT)
//            startActivity(intent)
//        }
//    }


    //음성출력
    private fun ttsSpeak(strTTS:String){
        tts.speak(strTTS, TextToSpeech.QUEUE_ADD,null,null)
    }

    override fun onLocationChanged(p0: Location) { }

}