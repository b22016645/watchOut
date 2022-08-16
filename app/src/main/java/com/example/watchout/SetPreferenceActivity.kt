package com.example.watchout

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.ImageView
import androidx.core.app.ActivityCompat
import com.example.watchout.databinding.ActivityMainBinding
import com.google.gson.GsonBuilder
import model.History
import model.Preference
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import org.json.JSONException
import org.json.JSONObject
import retrofit.IRetrofit
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import utils.Constant
import java.io.IOException
import android.speech.tts.TextToSpeech
import java.util.*

class SetPreferenceActivity : Activity() {

    private var audioRecord: AudioRecord? = null
    private var byteAudioData: ByteArray? = null

    // private var instance = RetrofitManager()
    private var retrofitService: IRetrofit? = null

    //물리버튼을 눌렀을 때마다 true/false가 바뀌는 변수
    private var activeBool = true
    private var recordingState : Int = 0
    //0 -> 녹음하지 않고 대기중, stringData == null
    //1 -> 녹음중. 버튼 한번 눌렀을때 0->1로 바뀜, byteData쌓는중
    //2 -> 스트링데이타로 변환중, 버튼 한번 더 눌렀을때 1->2로 바뀜 (녹음이 끝났다는 말). stringData만드는중
    //3 -> stringData가 다 만들어진 상태, 쓸 준비 완료. 쓰고 나면 상태변수를 0으로 바꿈

    /* AudioRecord 변수 */
    private val audioSource = MediaRecorder.AudioSource.MIC
    private val sampleRate = 16000
    private val channelCount = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = sampleRate * 5

    lateinit var image : ImageView

    private var isActive = false
    private lateinit var binding: ActivityMainBinding


    //음성출력관련
    private lateinit var tts: TextToSpeech
    var sttReturnData  : String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("ddddddd","일단 액티비티 넘어옴")
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(R.layout.activity_speech_to_text)

        setTTS()        //TTS세팅 및 초기화

        //화면이 꺼지지 않게
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        image = findViewById(R.id.imageView)

        Log.d(Constant.API.LOG,"SET PREFERENCE 호출됨")

        createAudioRecord()
        createRetrofitService()

        updatePreferenceByExpLog()
        preferenceQuestion()
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

    private fun ttsSpeak(strTTS:String){
        tts.speak(strTTS, TextToSpeech.QUEUE_ADD,null,null)
    }


    private fun createRetrofitService() { //retroservice객체 생성
        Log.d(Constant.API.LOG,"createRetrofitService() 호출됨")
        val gson = GsonBuilder()
            .setLenient()
            .create()
        val retrofit = Retrofit.Builder()
            .baseUrl(Constant.API.BASE_URL_KAKAO_API)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        retrofitService = retrofit.create(IRetrofit::class.java)
        Log.i("Stt", "retrofitService create")
    }

    private fun requestStt() :String?{ //요청 보냄

        try {
            Log.i(Constant.API.LOG,"rest api에 요청 보냄")
            val meida = "video/*".toMediaTypeOrNull()
            val requestBody = byteAudioData!!.toRequestBody(meida)
            val getCall = retrofitService!!.get_post_pcm(
                Constant.API.transferEncoding,
                Constant.API.contentType,
                Constant.API.authorization,
                requestBody
            )
            getCall.enqueue(object : Callback<ResponseBody?> {
                override fun onResponse(
                    call: Call<ResponseBody?>,
                    response: Response<ResponseBody?>
                ) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        var result: String? = null
                        try {
                            result = body!!.string()
                            Log.i("Stt", result)
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                        val startIndex = result!!.indexOf("{\"type\":\"finalResult\"")
                        val endIndex = result.lastIndexOf('}')
                        Log.i("Stt", "startIndex = $startIndex, endIndex = $endIndex")
                        if (startIndex > 0 && endIndex > 0) {
                            try {
                                val result_json_string = result.substring(startIndex, endIndex + 1)
                                val json = JSONObject(result_json_string)
                                val sttResultMsg = json.getString("value")
                                Log.i("Stt", sttResultMsg) //STT결과값입니다요!!!!!!!!!!!!!!
                             /*   val returnIntent = Intent()
                                    .putExtra("sttResultMsg", sttResultMsg)
                                setResult(Activity.RESULT_OK, returnIntent)*/
                                sttReturnData = sttResultMsg

                            //    finish()
                            } catch (e: JSONException) {
                                e.printStackTrace()
                            }
                        } else { // errorCalled
                            Log.i(Constant.API.LOG, "errorCalled")
                        }
                    } else {
                        Log.i(Constant.API.LOG, "Status Code = " + response.code())
                    }
                }

                override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
                    Log.i(Constant.API.LOG, "Fail msg = " + t.message)
                }
            })
        } catch (e: Exception) {
            Log.i(Constant.API.LOG, "requestStt fail...")
        }
        return sttReturnData
    }


    private fun threadLoop() {
        byteAudioData = ByteArray(bufferSize*5)
        Log.d(Constant.API.LOG, "byteAudio 생성")
        audioRecord?.startRecording()
        Log.d(Constant.API.LOG, "audioRecord객체 생성 + 녹음 준비 완료")
        while (isActive) {
            val ret = audioRecord?.read(byteAudioData!!, 0, byteAudioData!!.size)
            Log.d(Constant.API.LOG, "read중 크기: $ret")
            if (!isActive) {
                audioRecord!!.stop()
                Log.d(Constant.API.LOG, "audioRecord.stop()")
                audioRecord!!.release()
                audioRecord = null
                Log.d(Constant.API.LOG, " audioRecord = null")
            }
        }
        Log.d(Constant.API.LOG, " Thread돌아가는중")
    }

    private fun createAudioRecord(){
        Log.d(Constant.API.LOG,"createAudioRecord() 호출됨")
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(Constant.API.LOG, "권한없음")
            return
        }
        audioRecord = AudioRecord.Builder()
            .setAudioSource(audioSource)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(audioFormat)
                    .setSampleRate(sampleRate)
                    .setChannelMask(channelCount)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .build()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {

        if(keyCode== KeyEvent.KEYCODE_BACK){
            Log.d(Constant.API.LOG,"누름")
            if(recordingState==0){
                Log.d(Constant.API.LOG,"레코드스테이트0")
                isActive = true
                image.setImageResource(R.drawable.des)
                Log.d(Constant.API.LOG, "Active")

                //짧게 라도 진동을 줘야하나??
                if (audioRecord == null) {
                    createAudioRecord()
                    audioRecord?.startRecording()
                }
                Thread  {
                    threadLoop()
                }.start()
                recordingState = 1
            }
            else if (recordingState == 1){
                Log.d(Constant.API.LOG,"레코드스테이트1")
                isActive = false
                image.setImageResource(R.drawable.search)       //이거바꿔야함

                sttReturnData = requestStt()        //녹음데이타 -> 스트링 데이타

                recordingState = 2
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }










    ////////////////////////////////////////// /////////////////////////////////////////
    //                              경로 안내 종료 후 선호도 조사 함수 모음                    //
    ///////////////////////////////////////// //////////////////////////////////////////

    private fun updatePreferenceByExpLog() {
        //히스토리 내 이탈 로그에 기반하여 자동으로 가중치 조절

        var stnd = History.midPointSize!! //조정기준
        var num = 12121     //인덱스넘버

        Log.d("SetPreferenceActivity - updatePreferenceByExpLog() ","총크기 :${num}/ 총이탈 :,${History.expTotal}(${History.expTotal/num}) / 직선이탈 :,${History.expStraightRoad}(${History.expStraightRoad/History.expTotal}%)/ 횡단보도이탈 :,${History.expCrossWalk}(${History.expCrossWalk/History.expTotal}%)/ ftCar이탈 :${History.expWithCar}(${History.expWithCar/History.expTotal}%)/ftNoCar이탈  :,${History.expNoCar}(${History.expNoCar/History.expTotal}%)")

        if (History.expTotal / num > 0.2){
            //총 이탈 비율이 20%가 넘는 경우에만 가중치를 조절한다. (이하인 경우 사용자가 올바르게 길을 갔다고 판단)

            // 1. 직선길 이탈 --> DangerScore 테이블웨이트 조정
            if (History.expStraightRoad / History.expTotal > stnd){
                //직진길 이탈률이 40% 넘는 경우 테이블전체가중치를 낮춤 (도로상태가 더 중요하다고 판단)
                Log.d("SetPreferenceActivity - updatePreferenceByExpLog() ","직선길 가중치 조절->tableWieght 낮춤")
                setPreference("tableWeight",-1)
            }
            else if ((History.expTotal - History.expStraightRoad) /History.expTotal > 0.8){
                //직진길 이탈률이 20% 이하인 경우 테이즐 전체 가중치를 높임 (DangerScore가 더 중요하다고 판단)
                Log.d("SetPreferenceActivity - updatePreferenceByExpLog() ","직선길 가중치 조절->tableWieght 높임")
                setPreference("tableWeight",+1)
            }

            //2.분기점
            if (History.expTurnPoint / History.expTotal > stnd){
                Log.d("SetPreferenceActivity - updatePreferenceByExpLog() ","분기점 가중치 조절")
                setPreference("turnPoint", 10)
            }

            //3.횡단보도
            if (History.expCrossWalk / History.expTotal > stnd){
                Log.d("SetPreferenceActivity - updatePreferenceByExpLog() ","횡단보도 가중치 조절")
                setPreference("crossWalk", 10)
            }

            //4.CAR
            if (History.expWithCar / History.expTotal > stnd){
                Log.d("SetPreferenceActivity - updatePreferenceByExpLog() ","facilityCar 가중치 조절")
                setPreference("facilityCar", 10)
            }

            //5.NoCar
            if (History.expNoCar / History.expTotal > stnd){
                Log.d("SetPreferenceActivity - updatePreferenceByExpLog() ","failityNoCar 가중치 조절")
                setPreference("failityNoCar", 10)
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
        Log.d(Constant.API.LOG,"preferenceQuestion() 호출됨")
        sttReturnData = null
        var score = -1
        recordingState==0
        ttsSpeak("경로만족도를 0부터 10사이 숫자로 말씀해주세요")
        Log.d(Constant.API.LOG,"경로만족도를 0부터 10사이 숫자로 말씀해주세요")

        while(recordingState==0||recordingState==1||recordingState==3){
            //대기

            if (recordingState == 2)      {
                 var score = sttReturnData?.toInt()?:-1
                Log.d("SCORE",score.toString())
                if(score > 10 || score <0) {
                    ttsSpeak("잘못된 범위입니다.")
                    sttReturnData = null
                    recordingState  = 3
                }else{
                    //정상 범위안의 값인 경우
                    Preference.score = score         //우선 만족도 프리퍼런스오브젝트에 저장
                    recordingState  = 4
                    break
                }
            }
        }

 /*           while (sttReturnData != null||score >10 || score <0){

                var score = "사용자가 말한 숫자".toInt()
                if(score > 10 || score <0)
                    ttsSpeak("잘못된 범위입니다.")
                    sttReturnData = null
                    recordingState  = 0
            }

        Preference.score = score         //우선 만족도 프리퍼런스오브젝트에 저장
 */

        if (score <6) {
            ttsSpeak("설문을 종료하시려면 종료, 계속하시려면 계속 이라고 말해주세요")
            recordingState  = 0
            sttReturnData=null

            while(recordingState==0||recordingState==1||recordingState==3) {
                //대기

                if(recordingState == 2) {
                    if (sttReturnData == "종료") {
                        //안텐트객체추가
                        finish()
                    }
                    else if (sttReturnData == "계속") {
                        recordingState = 4
                        break
                    }
                    else {
                        ttsSpeak("종료 또는 계속중 하나만 말씀해주세요.")
                        recordingState = 3
                    }
                }
            }
        }


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


        ttsSpeak("향후 경로안내를 위한 선호도 가중치 조절을 시작합니다. 현재 상태 유지를 원하시면 유지 라고 말하세요")
        preferenceQuestion_tableWeight()
        preferenceQuestion_turnPoint()


/*
            if) 시설물이 있는 경우
                시설물질문 : 시설물 있는 경우에만
                →횡단보도/ 위험시설A(엘베,육교,지하보도,계단) / 위험시설B(교량,터널,고가도로,대형시설물이동통로)
            : 이용하신 경로에는 (시설물) 이 있었습니다. 향후 (시설물)이 최소화된 길을 안내받으시려면 “최소화”, 현재 상태 유지를 원하시면 “유지”를 말씀하세요

            :해당 시설물 가중치 조절*
 */
        //TEST
        History.hasDanger = true
        History.hasDangerA = 1234
        History.hasDangerB = 5678


        if(History.hasDanger){
            //시설물이 있는 경우에만 시설물 설문 진행
            if (History.hasDangerA != null){
                preferenceQuestion_DangerA()
            }
            if (History.hasDangerB != null){
                preferenceQuestion_DangerB()
            }
        }

        ttsSpeak("선호도 가중치 조절이 완료되었습니다. 향후 경로 안내시 조정된 값으로 경로를 안내합니다.")

    }//End of preferenceQuestion




    fun preferenceQuestion_DangerA(){
        //순서는 엘리베이터-육교-지하보도-계단으로 각 자리수가 시설물의 개수를 나타냄
        var DangerA = History.hasDangerA!!
        var elevator = DangerA .div(1000)
        DangerA %= 1000
        var overPasses =DangerA.div(100)
        DangerA %= 100
        var underPasses = DangerA.div(10)
        DangerA %= 10
        var stairs = DangerA


        var howMuch : Int = 0


        if(elevator != 0) {
            recordingState=0
            ttsSpeak(" 이용하신 경로에는 엘리베이터가 ${elevator}개 포함되었습니다. 향후 엘리베이터가 최소화된 길을 안내받으시려면 “최소화”, 현재 상태 유지를 원하시면 “유지”를 말씀하세요")
            sttReturnData = null
            while(recordingState==0||recordingState==1||recordingState==3) {
                //대기

                if (recordingState == 2) {
                    var ans = sttReturnData
                    if (ans == "유지") {
                       // elevator = 0
                        recordingState = 4
                    } else if (ans == "최소화") {
                        howMuch -= 5
                       // elevator = 0
                        recordingState = 4
                    } else {
                        ttsSpeak("잘못된 음성입니다. 향후 엘리베이터가 최소화된 길을 안내받으시려면 “최소화”, 현재 상태 유지를 원하시면 “유지”를 말씀하세요")
                        recordingState = 3
                    }
                }
            }
        }


        if(overPasses != 0) {
            recordingState = 0

            ttsSpeak(" 이용하신 경로에는 육교가 ${overPasses}개 포함되었습니다. 향후 육교가 최소화된 길을 안내받으시려면 “최소화”, 현재 상태 유지를 원하시면 “유지”를 말씀하세요")
            sttReturnData = null

            while (recordingState == 0 || recordingState == 1 || recordingState == 3) {
                var ans = sttReturnData
                if (ans == "유지") {
                    //overPasses = 0
                    recordingState = 4
                } else if (ans == "최소화") {
                    howMuch -= 5
                    //overPasses = 0
                    recordingState = 4
                } else {
                    ttsSpeak("잘못된 음성입니다. 육교가 최소화된 길을 안내받으시려면 “최소화”, 현재 상태 유지를 원하시면 “유지”를 말씀하세요")
                    recordingState = 3
                }
            }
        }

        if(underPasses != 0) {
            recordingState = 0

            ttsSpeak(" 이용하신 경로에는 지하보도가 ${overPasses}개 포함되었습니다. 향후 지하보도가 최소화된 길을 안내받으시려면 “최소화”, 현재 상태 유지를 원하시면 “유지”를 말씀하세요")
            sttReturnData = null

            while (recordingState == 0 || recordingState == 1 || recordingState == 3) {
                var ans = sttReturnData
                if (ans == "유지") {
                    //underPasses = 0
                    recordingState = 4
                } else if (ans == "최소화") {
                    howMuch -= 5
                    //underPasses = 0
                    recordingState = 4
                } else {
                    ttsSpeak("잘못된 음성입니다. 지하보도가 최소화된 길을 안내받으시려면 “최소화”, 현재 상태 유지를 원하시면 “유지”를 말씀하세요")
                    recordingState = 3
                }
            }
        }

        if(stairs != 0) {
            recordingState = 0

            ttsSpeak(" 이용하신 경로에는 계단이 ${stairs}개 포함되었습니다. 향후 계단이 최소화된 길을 안내받으시려면 “최소화”, 현재 상태 유지를 원하시면 “유지”를 말씀하세요")
            sttReturnData = null

            while (recordingState == 0 || recordingState == 1 || recordingState == 3) {
                var ans = sttReturnData
                if (ans == "유지") {
                    //stairs = 0
                    recordingState = 4
                } else if (ans == "최소화") {
                    howMuch -= 5
                    //stairs = 0
                    recordingState = 4
                } else {
                    ttsSpeak("잘못된 음성입니다. 계단이 최소화된 길을 안내받으시려면 “최소화”, 현재 상태 유지를 원하시면 “유지”를 말씀하세요")
                    recordingState = 3
                }
            }
        }

    } //End of preferenceQuestion_DangerA()




    fun preferenceQuestion_DangerB(){
        //순서는 교량-터널-고가도로-대형시설물이동통로 로 각 자리수가 시설물의 개수를 나타냄

        var DangerB = History.hasDangerB!!
        var bridge = DangerB .div(1000)
        DangerB %= 1000
        var turnnels =DangerB.div(100)
        DangerB %= 100
        var highroad = DangerB.div(10)
        DangerB %= 10
        var largeFacilitypassage = DangerB


        var howMuch : Int = 0


        if(bridge != 0) {
            recordingState = 0

            ttsSpeak(" 이용하신 경로에는 교량이 ${bridge}개 포함되었습니다. 향후 교량이 최소화된 길을 안내받으시려면 “최소화”, 현재 상태 유지를 원하시면 “유지”를 말씀하세요")
            sttReturnData = null

            while (recordingState == 0 || recordingState == 1 || recordingState == 3) {
                var ans = sttReturnData
                if (ans == "유지") {
                    //bridge = 0
                    recordingState = 4
                } else if (ans == "최소화") {
                    howMuch -= 5
                    //bridge = 0
                    recordingState = 4
                } else {
                    ttsSpeak("잘못된 음성입니다. 교량이 최소화된 길을 안내받으시려면 “최소화”, 현재 상태 유지를 원하시면 “유지”를 말씀하세요")
                    recordingState = 3
                }
            }

        }

        if(turnnels != 0) {
            recordingState = 0

            ttsSpeak(" 이용하신 경로에는 터널이 ${turnnels}개 포함되었습니다. 향후 터널이 최소화된 길을 안내받으시려면 “최소화”, 현재 상태 유지를 원하시면 “유지”를 말씀하세요")
            sttReturnData = null

            while (recordingState == 0 || recordingState == 1 || recordingState == 3) {
                var ans = sttReturnData
                if (ans == "유지") {
                    //turnnels = 0
                    recordingState = 4
                } else if (ans == "최소화") {
                    howMuch -= 5
                    //turnnels = 0
                    recordingState = 4
                } else {
                    ttsSpeak("잘못된 음성입니다. 터널이 최소화된 길을 안내받으시려면 “최소화”, 현재 상태 유지를 원하시면 “유지”를 말씀하세요")
                    recordingState = 3
                }
            }

        }



        if(highroad != 0) {
            recordingState = 0

            ttsSpeak(" 이용하신 경로에는 고가도로가 ${highroad}개 포함되었습니다. 향후 고가도로가 최소화된 길을 안내받으시려면 “최소화”, 현재 상태 유지를 원하시면 “유지”를 말씀하세요")
            sttReturnData = null

            while (recordingState == 0 || recordingState == 1 || recordingState == 3) {
                var ans = sttReturnData
                if (ans == "유지") {
                    //highroad = 0
                    recordingState = 4
                } else if (ans == "최소화") {
                    howMuch -= 5
                    //highroad = 0
                    recordingState = 4
                } else {
                    ttsSpeak("잘못된 음성입니다. 고가도로가 최소화된 길을 안내받으시려면 “최소화”, 현재 상태 유지를 원하시면 “유지”를 말씀하세요")
                    recordingState = 3
                }
            }

        }


        if(largeFacilitypassage != 0) {
            recordingState = 0

            ttsSpeak(" 이용하신 경로에는  대형 시설물 이동통로가 ${largeFacilitypassage}개 포함되었습니다. 향후  대형 시설물 이동통로가 최소화된 길을 안내받으시려면 “최소화”, 현재 상태 유지를 원하시면 “유지”를 말씀하세요")
            sttReturnData = null

            while (recordingState == 0 || recordingState == 1 || recordingState == 3) {
                var ans = sttReturnData
                if (ans == "유지") {
                    //largeFacilitypassage = 0
                    recordingState = 4
                } else if (ans == "최소화") {
                    howMuch -= 5
                    //largeFacilitypassage = 0
                    recordingState = 4
                } else {
                    ttsSpeak("잘못된 음성입니다. 대형 시설물 이동통로가 최소화된 길을 안내받으시려면 “최소화”, 현재 상태 유지를 원하시면 “유지”를 말씀하세요")
                    recordingState = 3
                }
            }

        }
        Log.d("howmuch",howMuch.toString())

        setPreference("facilityCar", howMuch)
    }//End of preferenceQuestion_DangerB()




    fun preferenceQuestion_tableWeight(){

        recordingState==0
        sttReturnData = null


        while(recordingState == 0 || recordingState == 1 || recordingState ==3){

            ttsSpeak("경로 안내에 있어 전반적인 도로상태와 시설물개수 중 더 중요한 것은 무엇입니까? 도로, 시설물, 유지 중 하나를 말씀하세요")

            recordingState==0
            if (recordingState == 2){
                var ans = sttReturnData

                if (ans == "유지"){
                    Log.d("Main-preferenceQuestion_tableWeight()","유지")
                    recordingState = 4
                }
                else if (ans == "도로") {
                    setPreference("tableWeight", -1)
                    recordingState = 4
                }
                else if (ans == "시설물 개수") {
                    setPreference("tableWeight", +1)
                    recordingState = 4
                }
                else {
                    ttsSpeak("잘못된 음성입니다.")
                    recordingState = 3
                    sttReturnData = null
                }
            }



        }
    }
    //end of preferenceQuestion_tableWeight()

    fun preferenceQuestion_turnPoint(){

        recordingState==0
        sttReturnData = null


        while(recordingState == 0 || recordingState == 1 || recordingState ==3) {
            recordingState = 0
            ttsSpeak("직진우선길과 최단거리우선중 어떤 경로를 선호하십니까? 직진우선, 최단거리, 유지 중 하나를 말씀하세요")

            if (recordingState == 2) {
                var ans = sttReturnData

                if (ans == "유지") {
                    Log.d("Main-preferenceQuestion_turnPoint()", "유지")
                    recordingState = 4
                } else if (ans == "직진우선") {
                    //분기점 가중치를 높힌다  ->음수값이 클수록 가중치가 높은거임
                    setPreference("turnPoint", -5)
                    recordingState = 4
                } else if (ans == "최단거리 ") {
                    //분기점 가중치를 낮춘다{
                    setPreference("turnPoint", +5)
                    recordingState = 4
                } else {
                    ttsSpeak("잘못된 음성입니다.")
                    recordingState = 3
                }
            }
        }
    }//End of preferenceQuestion_turnPoint()



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
    //                            선호도 관련 함수 모음 끝                           //
    /////////////////////////////////////////////////////////////////////////////














}