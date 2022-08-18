package com.example.watchout

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import android.widget.Button
import calling.SpeechToText
import com.example.watchout.databinding.ActivitySetreferenceBinding
import utils.Constant.API.LOG
import java.lang.Thread.sleep
import java.util.*
import kotlin.concurrent.thread

class SetPreferenceActivity : Activity() {
    private lateinit var btn:Button
    private var recordingState : Int = 0
    //0 -> 녹음하지 않고 대기중, stringData == null
    //1 -> 녹음중. 버튼 한번 눌렀을때 0->1로 바뀜, byteData쌓는중
    //2 -> 스트링데이타로 변환중, 버튼 한번 더 눌렀을때 1->2로 바뀜 (녹음이 끝났다는 말). stringData만드는중
    //3 -> stringData가 다 만들어진 상태, 쓸 준비 완료. 쓰고 나면 상태변수를 0으로 바꿈

    private lateinit var binding: ActivitySetreferenceBinding


    //음성출력관련
    private lateinit var tts: TextToSpeech
    private lateinit var callback : sttAdapter //stt결과가 오면 실행할 콜백함수
    private lateinit var mySTT: SpeechToText
    var sttReturnData  : String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("ddddddd","일단 액티비티 넘어옴")
        super.onCreate(savedInstanceState)
        binding = ActivitySetreferenceBinding.inflate(layoutInflater)
        setContentView(R.layout.activity_setreference)
       // btn = findViewById<Button>(R.id.button)
        btn = binding.button
        setTTS()        //TTS세팅 및 초기화

        //화면이 꺼지지 않게
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        Log.d(Constant.API.LOG,"SET PREFERENCE 호출됨")

        mySTT = SpeechToText(this)
        setSTTCallbacks()

        updatePreferenceByExpLog()

        preferenceQuestion()




        btn.setOnClickListener {
            val returnintent = Intent()
            setResult(RESULT_OK,returnintent)
            finish()
            //이거안됌~~~~~~~~~~~~~~~메인으로 안돌아감~~~~~~~~~~

        }
    }


    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        Log.d("SetPreferenceActivity-onKeyDown","온키다운 호출")
        if(keyCode== KeyEvent.KEYCODE_BACK){
            Log.d(Constant.API.LOG,"누름")
            if(recordingState==0){
                Log.d(Constant.API.LOG,"레코드스테이트0")
                mySTT.startAudioRecord()
                Log.d(Constant.API.LOG, "Active")

                recordingState = 1
            }
            else if (recordingState == 1){
                Log.d(Constant.API.LOG,"레코드스테이트1")
                mySTT.finishAudioRecordAndGetText(callback) //음성종료 => text로 변환 => 변환된 text 콜백함수로 건내줌
                //recordingState = 2
                Log.d(Constant.API.LOG,recordingState.toString())

                //여기서 스테이트 바꾸면 안되고 콜백 불리면 거기서 바꿔야함. (동기로)
            }
           return true
        }
        return super.onKeyDown(keyCode, event)
    }



    private fun setSTTCallbacks(){
        callback = object:sttAdapter{//STT결과가 오면 실행되는 콜백 함수여기다 정의

            override fun sttOnResponseCallback(text: String) { //text:STT결과
                Log.d("SetPreferenceActivity STT실행결과: ",text)
                sttReturnData = text
                Log.d("SetPreferenceActivity-sttOnResponseCallback",sttReturnData?:"null")
                recordingState = 2
                Log.d("SetPreferenceActivity-sttOnResponseCallback : recordingState",recordingState.toString())

                //여기서 다 실행하거나
            }

            //여기서 함수 더 만들거나
        }
    }






    ////////////////////////////////////////// /////////////////////////////////////////
    //                              경로 안내 종료 후 선호도 조사 함수 모음                    //
    ///////////////////////////////////////// //////////////////////////////////////////

    private fun updatePreferenceByExpLog() {
        //히스토리 내 이탈 로그에 기반하여 자동으로 가중치 조절

        //테스트를 위해 고정으로
        History.midPointSize = 100
        History.expTotal = 50
        History.expStraightRoad = 7
        History.expTurnPoint = 40
        History.expCrossWalk = 1
        History.expNoCar = 1
        History.expWithCar = 1

        var stnd = 0.4 //조정기준
        var num = History.midPointSize!!      //인덱스넘버

        Log.d("SetPreferenceActivity - updatePreferenceByExpLog() ","총크기 :${num} / 총이탈 :${History.expTotal}(${History.expTotal.toDouble()/num}) / 직선이탈 :${History.expStraightRoad}(${History.expStraightRoad.toDouble()/History.expTotal}%) / 횡단보도이탈 :${History.expCrossWalk}(${History.expCrossWalk.toDouble()/History.expTotal}%) / ftCar이탈:${History.expWithCar}(${History.expWithCar.toDouble()/History.expTotal}%) / ftNoCar이탈:${History.expNoCar}(${History.expNoCar.toDouble()/History.expTotal}%)")

        if (History.expTotal.toDouble() / num > 0.2){
            //총 이탈 비율이 20%가 넘는 경우에만 가중치를 조절한다. (이하인 경우 사용자가 올바르게 길을 갔다고 판단)

            // 1. 직선길 이탈 --> DangerScore 테이블웨이트 조정
            if (History.expStraightRoad.toDouble() / History.expTotal > stnd){
                //직진길 이탈률이 40% 넘는 경우 테이블전체가중치를 낮춤 (도로상태가 더 중요하다고 판단)
                Log.d("SetPreferenceActivity - updatePreferenceByExpLog() ","RoadType 가중치 조절->tableWieght 낮춤")
                setPreference("tableWeight",-1)
            }
            else if ((History.expTotal - History.expStraightRoad).toDouble() /History.expTotal > 0.8){
                //직진길 이탈률이 20% 이하인 경우 테이즐 전체 가중치를 높임 (DangerScore가 더 중요하다고 판단)
                Log.d("SetPreferenceActivity - updatePreferenceByExpLog() ","DangerScore 가중치 조절->tableWieght 높임")
                setPreference("tableWeight",+1)
            }

            //2.분기점
            if (History.expTurnPoint.toDouble() / History.expTotal > stnd){
                Log.d("SetPreferenceActivity - updatePreferenceByExpLog() ","분기점 가중치 조절")
                setPreference("turnPoint", 10)
            }

            //3.횡단보도
            if (History.expCrossWalk.toDouble() / History.expTotal > stnd){
                Log.d("SetPreferenceActivity - updatePreferenceByExpLog() ","횡단보도 가중치 조절")
                setPreference("crossWalk", 10)
            }

            //4.CAR
            if (History.expWithCar.toDouble() / History.expTotal > stnd){
                Log.d("SetPreferenceActivity - updatePreferenceByExpLog() ","facilityCar 가중치 조절")
                setPreference("facilityCar", 10)
            }

            //5.NoCar
            if (History.expNoCar.toDouble() / History.expTotal > stnd){
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
        //recordingState=0
        ttsSpeak("경로만족도를 0부터 10사이 숫자로 말씀해주세요")
        Log.d(Constant.API.LOG,"경로만족도를 0부터 10사이 숫자로 말씀해주세요")
        Log.d("recordingState : ",recordingState.toString())

   var thread = Thread{

       sleep(2000)
       return@Thread
            while(recordingState==0||recordingState==1||recordingState==3){

                //대기
                Log.d("경로만족도)recordingState : ",recordingState.toString())

                    if (recordingState == 2)      {
                        Log.d("recordingState-2 : ",recordingState.toString())
                        var score = sttReturnData?.toInt()?:-1
                        Log.d("SCORE",score.toString())
                        if(score > 10 || score <0) {
                            ttsSpeak("잘못된 범위입니다.경로만족도를 0부터 10사이 숫자로 말씀해주세요")
                            Log.d("잘못된 범위 경로 만족도 : " ,score.toString())
                            sttReturnData = null
                            recordingState = 0
                        }else{
                            //정상 범위안의 값인 경우
                            Preference.score = score         //우선 만족도 프리퍼런스오브젝트에 저장
                            recordingState  = 0
                            Log.d("경로 만족도 : " ,score.toString())
                            ttsSpeak("경로만족도 입력이 완료되었습니다.")
                            break
                        }
                    }
             }
    } .join()


        Log.d("스레드종료","ㄹㄴㄹㅇ")


 /*           while (sttReturnData != null||score >10 || score <0){

                var score = "사용자가 말한 숫자".toInt()
                if(score > 10 || score <0)
                    ttsSpeak("잘못된 범위입니다.")
                    sttReturnData = null
                    recordingState  = 0
            }

        Preference.score = score         //우선 만족도 프리퍼런스오브젝트에 저장
 */
        Thread {
            Log.d("스레드진입","ㄹㄴㄹㅇ")
        if (score >6) {
            ttsSpeak("설문을 종료하시려면 종료, 계속하시려면 계속 이라고 말해주세요")
            Log.d("LOG","설문을 종료하시려면 종료, 계속하시려면 계속 이라고 말해주세요")
            recordingState  = 0
            sttReturnData=null


                while (recordingState == 0 || recordingState == 1 || recordingState == 3) {
                    //대기
                    if (recordingState == 2) {
                        if (sttReturnData == "종료") {
                            Log.d("LOG","종료")
                            //안텐트객체추가
                            finish()
                        } else if (sttReturnData == "계속") {
                            Log.d("LOG","계속")
                            recordingState = 4
                            break
                        } else {
                            ttsSpeak("종료 또는 계속중 하나만 말씀해주세요.")
                            Log.d("LOG","종료 또는 계속중 하나만 말씀해주세요.")
                            recordingState = 3
                        }
                    }
                }

        }
    }.join()


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



        ttsSpeak("향후 경로안내를 위한지 선호도 가중치 조절을 시작합니다. 현재 상태 유지를 원하시면 유지 라고 말하세요")

        Log.d("LOG:-"," 선호도 가중치 조절을 시작합니다. 현재 상태 유지를 원하시면 유지 말하삼")
        preferenceQuestion_tableWeight()
        preferenceQuestion_turnPoint()

        if(History.hasCrossWalk > 0){

            ttsSpeak(" 이용하신 경로에는 횡단보도가 ${History.hasCrossWalk}개 포함되었습니다. 향후 횡단보도가 최소화된 길을 안내받으시려면 “최소화”, 현재 상태 유지를 원하시면 “유지”를 말씀하세요")
            Log.d("LOG:-","이용하신 경로에는 횡단보도가 ${History.hasCrossWalk}개 포함되었습니다. 향후 횡단보도가 최소화된 길을 안내받으시려면 “최소화”, 현재 상태 유지를 원하시면 “유지”를 말씀하세요")
            sttReturnData = null


            Thread {
                while (recordingState == 0 || recordingState == 1 || recordingState == 3) {
                    //대기

                    if (recordingState == 2) {
                        var ans = sttReturnData
                        if (ans == "유지") {
                            recordingState = 4
                        } else if (ans == "최소화") {
                            setPreference("crossWalk", -10)
                            recordingState = 4
                        } else {
                            ttsSpeak("잘못된 음성입니다. 향후 횡단보도가 최소화된 길을 안내받으시려면 “최소화”, 현재 상태 유지를 원하시면 “유지”를 말씀하세요")
                            recordingState = 3
                        }
                    }
                }
            }.join()

        }


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


            Thread {
                while (recordingState == 0 || recordingState == 1 || recordingState == 3) {
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
            }.join()

        }


        if(overPasses != 0) {
            recordingState = 0

            ttsSpeak(" 이용하신 경로에는 육교가 ${overPasses}개 포함되었습니다. 향후 육교가 최소화된 길을 안내받으시려면 “최소화”, 현재 상태 유지를 원하시면 “유지”를 말씀하세요")
            sttReturnData = null

            Thread {
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
            }.join()

        }

        if(underPasses != 0) {
            recordingState = 0

            ttsSpeak(" 이용하신 경로에는 지하보도가 ${overPasses}개 포함되었습니다. 향후 지하보도가 최소화된 길을 안내받으시려면 “최소화”, 현재 상태 유지를 원하시면 “유지”를 말씀하세요")
            sttReturnData = null

            Thread {
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
            }.join()

        }

        if(stairs != 0) {
            recordingState = 0

            ttsSpeak(" 이용하신 경로에는 계단이 ${stairs}개 포함되었습니다. 향후 계단이 최소화된 길을 안내받으시려면 “최소화”, 현재 상태 유지를 원하시면 “유지”를 말씀하세요")
            sttReturnData = null

            Thread {
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
            }.join()

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

            Thread {
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
            }.join()


        }

        if(turnnels != 0) {
            recordingState = 0

            ttsSpeak(" 이용하신 경로에는 터널이 ${turnnels}개 포함되었습니다. 향후 터널이 최소화된 길을 안내받으시려면 “최소화”, 현재 상태 유지를 원하시면 “유지”를 말씀하세요")
            sttReturnData = null


            Thread {
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
            }.join()


        }



        if(highroad != 0) {
            recordingState = 0

            ttsSpeak(" 이용하신 경로에는 고가도로가 ${highroad}개 포함되었습니다. 향후 고가도로가 최소화된 길을 안내받으시려면 “최소화”, 현재 상태 유지를 원하시면 “유지”를 말씀하세요")
            sttReturnData = null


            Thread {
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
            }.join()


        }


        if(largeFacilitypassage != 0) {
            recordingState = 0

            ttsSpeak(" 이용하신 경로에는  대형 시설물 이동통로가 ${largeFacilitypassage}개 포함되었습니다. 향후  대형 시설물 이동통로가 최소화된 길을 안내받으시려면 “최소화”, 현재 상태 유지를 원하시면 “유지”를 말씀하세요")
            sttReturnData = null

            Thread {
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
            }.join()

        }
        Log.d("howmuch",howMuch.toString())

        setPreference("facilityCar", howMuch)
    }//End of preferenceQuestion_DangerB()




    fun preferenceQuestion_tableWeight(){

        recordingState==0
        sttReturnData = null
        ttsSpeak("경로 안내에 있어 전반적인 도로상태와 시설물개수 중 더 중요한 것은 무엇입니까? 도로, 시설물, 유지 중 하나를 말씀하세요")

        Thread {
            while (recordingState == 0 || recordingState == 1 || recordingState == 3) {

                recordingState == 0
                if (recordingState == 2) {
                    var ans = sttReturnData

                    if (ans == "유지") {
                        Log.d("Main-preferenceQuestion_tableWeight()", "유지")
                        recordingState = 4
                    } else if (ans == "도로") {
                        setPreference("tableWeight", -1)
                        recordingState = 4
                    } else if (ans == "시설물 개수") {
                        setPreference("tableWeight", +1)
                        recordingState = 4
                    } else {
                        ttsSpeak("잘못된 음성입니다.도로, 시설물, 유지 중 하나를 말씀하세요")
                        recordingState = 3
                        sttReturnData = null
                    }
                }
            }
        }.join()

    }
    //end of preferenceQuestion_tableWeight()

    fun preferenceQuestion_turnPoint(){
        Log.d("LOG","분기점 질문 함수 진입")

        recordingState==0
        sttReturnData = null
        ttsSpeak("직진우선길과 최단거리우선중 어떤 경로를 선호하십니까? 직진우선, 최단거리, 유지 중 하나를 말씀하세요")
        Log.d("LOG","직진우선길과 최단거리우선중 어떤 경로를 선호하십니까? 직진우선, 최단거리, 유지 중 하나를 말씀하세요")
        Thread {
            while (recordingState == 0 || recordingState == 1 || recordingState == 3) {
                recordingState = 0


                if (recordingState == 2) {
                    var ans = sttReturnData

                    if (ans == "유지") {
                        Log.d("Main-preferenceQuestion_turnPoint()", "유지")
                        recordingState = 4
                    } else if (ans == "직진우선") {
                        //분기점 가중치를 높힌다  ->음수값이 클수록 가중치가 높은거임
                        Log.d("Main-preferenceQuestion_turnPoint()", "직진우선")
                        setPreference("turnPoint", -5)
                        recordingState = 4
                    } else if (ans == "최단거리") {
                        //분기점 가중치를 낮춘다
                        Log.d("Main-preferenceQuestion_turnPoint()", "최단거리")
                        setPreference("turnPoint", +5)
                        recordingState = 4
                    } else {
                        Log.d("Main-preferenceQuestion_turnPoint()", "잘못된 음성입니다. 직진우선, 최단거리, 유지 중 하나를 말씀하세요")
                        ttsSpeak("잘못된 음성입니다. 직진우선, 최단거리, 유지 중 하나를 말씀하세요")
                        recordingState = 3
                    }
                }
            }
        }.join()
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












}