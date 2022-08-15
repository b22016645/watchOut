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

    lateinit var image : ImageView
    private lateinit var binding: ActivityMainBinding
    private var activeBool = true
    private lateinit var mystt: MySTT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(R.layout.activity_speech_to_text)

        //화면이 꺼지지 않게
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        image = findViewById(R.id.imageView)

        Log.d(LOG,"SpeechToTextActivity 호출됨")
        mystt = MySTT(this)

    }


    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if(keyCode== KeyEvent.KEYCODE_BACK){
            if(activeBool==true){
                mystt.startAudioRecord()
                activeBool = false
            }
            else{
                image.setImageResource(R.drawable.search)
                val resText = mystt.finishAudioRecordAndGetText()
                val returnIntent = Intent()
                    .putExtra("sttResultMsg", resText)
                Log.i("Stt", "SpeechToTextActivity: " +resText)
                setResult(Activity.RESULT_OK, returnIntent)
                finish()
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }


}