package com.example.watchout
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.ImageView
import calling.SpeechToText
import com.example.watchout.databinding.ActivityMainBinding
import utils.Constant.API.LOG


class DestinationActivity : Activity() {

    lateinit var image : ImageView
    private lateinit var binding: ActivityMainBinding
    private var activeBool = true
    private lateinit var mystt: SpeechToText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(R.layout.activity_speech_to_text)

        //화면이 꺼지지 않게
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        image = findViewById(R.id.imageView)

        Log.d(LOG,"DestinationActivity 호출됨")
        mystt = SpeechToText(this)

    }


    //MySTT에서 돌아올 때를 위한 instace와 함수
    init{
        instance = this
    }
    companion object{
        private var instance:DestinationActivity? = null
        fun getInstance(): DestinationActivity? {
            return instance
        }
    }

    fun returnToSpeechToTextActivity(resStr : String){
        val returnIntent = Intent()
            .putExtra("sttResultMsg", resStr)
        Log.i("Stt", "SpeechToTextActivity: " +resStr)
        setResult(Activity.RESULT_OK, returnIntent)
        finish()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if(keyCode== KeyEvent.KEYCODE_BACK){
            if(activeBool==true){
                mystt.startAudioRecord()
                image.setImageResource(R.drawable.des)
                activeBool = false
            }
            else{
                image.setImageResource(R.drawable.search)
                mystt.finishAudioRecordAndGetText("des")
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }


}