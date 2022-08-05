package com.example.watchout

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.ActivityCompat
import com.example.watchout.R
import com.example.watchout.databinding.ActivityMainBinding
import com.google.gson.GsonBuilder
import model.Favorites
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import org.json.JSONException
import org.json.JSONObject
import retrofit.IRetrofit
import retrofit.RetrofitManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import utils.Constant
import utils.Constant.API
import utils.Constant.API.BASE_URL_KAKAO_API
import utils.Constant.API.LOG
import utils.Constant.API.authorization
import utils.Constant.API.contentType
import utils.Constant.API.transferEncoding
import java.io.IOException


class SpeechToTextActivity : Activity() {

    private var audioRecord: AudioRecord? = null
    private var byteAudioData: ByteArray? = null

   // private var instance = RetrofitManager()
   private var retrofitService: IRetrofit? = null

    //물리버튼을 눌렀을 때마다 true/false가 바뀌는 변수
    private var activeBool = true

    /* AudioRecord 변수 */
    private val audioSource = MediaRecorder.AudioSource.MIC
    private val sampleRate = 16000
    private val channelCount = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = sampleRate * 5

    lateinit var image : ImageView

    private var isActive = false
    private lateinit var binding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(R.layout.activity_speech_to_text)

        //화면이 꺼지지 않게
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        image = findViewById(R.id.imageView)

        Log.d(LOG,"SpeechToTextActivity 호출됨")

        createAudioRecord()
        createRetrofitService()
    }

    private fun createRetrofitService() { //retroservice객체 생성
        val gson = GsonBuilder()
            .setLenient()
            .create()
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL_KAKAO_API)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        retrofitService = retrofit.create(IRetrofit::class.java)
        Log.i("Stt", "retrofitService create")
    }

    private fun requestStt() { //요청 보냄
        try {
            Log.i(LOG,"rest api에 요청 보냄")
            val meida = "video/*".toMediaTypeOrNull()
            val requestBody = byteAudioData!!.toRequestBody(meida)
            val getCall = retrofitService!!.get_post_pcm(
                transferEncoding,
                contentType,
                authorization,
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
                                val returnIntent = Intent()
                                    .putExtra("sttResultMsg", sttResultMsg)
                                setResult(RESULT_OK, returnIntent)
                                finish()
                            } catch (e: JSONException) {
                                e.printStackTrace()
                            }
                        } else { // errorCalled
                            Log.i(LOG, "errorCalled")
                        }
                    } else {
                        Log.i(LOG, "Status Code = " + response.code())
                    }
                }

                override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
                    Log.i(LOG, "Fail msg = " + t.message)
                }
            })
        } catch (e: Exception) {
            Log.i(LOG, "requestStt fail...")
        }
    }


    private fun threadLoop() {
        byteAudioData = ByteArray(bufferSize*5)
        Log.d(LOG, "byteAudio 생성")
        audioRecord?.startRecording()
        Log.d(LOG, "audioRecord객체 생성 + 녹음 준비 완료")
        while (isActive) {
            val ret = audioRecord?.read(byteAudioData!!, 0, byteAudioData!!.size)
            Log.d(LOG, "read중 크기: $ret")
            if (!isActive) {
                audioRecord!!.stop()
                Log.d(LOG, "audioRecord.stop()")
                audioRecord!!.release()
                audioRecord = null
                Log.d(LOG, " audioRecord = null")
            }
        }
        Log.d(LOG, " Thread돌아가는중")
    }

    private fun createAudioRecord(){
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(LOG, "권한없음")
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
            if(activeBool==true){
                isActive = true
                image.setImageResource(R.drawable.des)
                Log.d(LOG, "Active")

                //짧게 라도 진동을 줘야하나??
                if (audioRecord == null) {
                    createAudioRecord()
                    audioRecord?.startRecording()
                }
                Thread  {
                    threadLoop()
                }.start()
                activeBool = false
            }
            else{
                isActive = false
                image.setImageResource(R.drawable.search)

                requestStt()

                activeBool = true
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }


}