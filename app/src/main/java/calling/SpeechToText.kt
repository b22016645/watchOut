package calling

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.app.ActivityCompat
import com.example.watchout.sttAdapter
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
import utils.Constant
import java.io.IOException
import kotlin.concurrent.thread

class SpeechToText(val context: Context)  {
    private var audioRecord: AudioRecord? = null
    private var byteAudioData: ByteArray? = null

    // private var instance = RetrofitManager()
    private var retrofitService: IRetrofit? = null

    /* AudioRecord 변수 */
    private val audioSource = MediaRecorder.AudioSource.MIC
    private val sampleRate = 16000
    private val channelCount = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = sampleRate * 5
    private var isActive = false

    fun startAudioRecord(){
        createAudioRecord()
        if (audioRecord == null) {
            createAudioRecord()
            audioRecord?.startRecording()
        }
        isActive = true
        var th1 = thread()  {
            Log.d("Stt", "스레드 실행 !!")
            threadLoop()
        }
    }

    fun finishAudioRecordAndGetText(callback:sttAdapter){
        //scallback = callback
        isActive = false
        createRetrofitService()
        requestStt(callback)
    }

    private fun createAudioRecord(){
        if (ActivityCompat.checkSelfPermission(
                context,
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

    private fun threadLoop() {
        Log.d("Stt", "threadLoop들어옴")
        byteAudioData = ByteArray(bufferSize*5)
        Log.d(Constant.API.LOG, "byteAudio 생성")
        audioRecord?.startRecording()
        Log.d(Constant.API.LOG, "audioRecord객체 생성 + 녹음 준비 완료")
        while (isActive) {
            if (!isActive) {
                Log.d("Stt", "thread;isActive: " + isActive)
                audioRecord!!.stop()
                Log.d(Constant.API.LOG, "audioRecord.stop()")
                audioRecord!!.release()
                audioRecord = null
                Log.d(Constant.API.LOG, " audioRecord = null")
                break
            }
            val ret = audioRecord?.read(byteAudioData!!, 0, byteAudioData!!.size)
            Log.d(Constant.API.LOG, "read중 크기: $ret")
        }
        Log.d("Stt", " threadLoop나간다")
    }

    private fun createRetrofitService() { //retroservice객체 생성
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

    private fun requestStt(callback:sttAdapter){ //요청 보냄
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
                                //Log.i("Stt", sttResultMsg) //STT결과값입니다요!!!!!!!!!!!!!!
                                callback.sttOnResponseCallback(sttResultMsg)

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
    }
}

