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
import utils.Constant.API.LOG
import java.io.IOException

class SpeechToTextActivity : Activity() {

    private var audioRecord: AudioRecord? = null
    private var byteAudioData: ByteArray? = null

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
    }


    private fun requestStt(){
        val returnIntent = Intent()
            .putExtra("byteAudioData", byteAudioData)
        setResult(RESULT_OK, returnIntent)
        finish()
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