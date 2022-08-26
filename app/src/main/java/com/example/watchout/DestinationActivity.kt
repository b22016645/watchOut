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
    private lateinit var callback : sttAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(R.layout.activity_speech_to_text)

        //화면이 꺼지지 않게
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        image = findViewById(R.id.imageView)

        callback = object : sttAdapter {
            override fun sttOnResponseCallback(text: String) {
                val returnIntent = Intent()
                    .putExtra("sttResultMsg", text)
                Log.i("Stt", "SpeechToTextActivity: " + text)
                setResult(Activity.RESULT_OK, returnIntent)
                finish()
            }

        }
        Log.d(LOG, "DestinationActivity 호출됨")
        mystt = SpeechToText(this)

    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if(keyCode== KeyEvent.KEYCODE_BACK){
            if(activeBool==true){
                mystt.startAudioRecord()
                image.setImageResource(R.drawable.speakdestination)
                activeBool = false
            }
            else{
                image.setImageResource(R.drawable.searchingdestination)
                mystt.finishAudioRecordAndGetText(callback)
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }


}