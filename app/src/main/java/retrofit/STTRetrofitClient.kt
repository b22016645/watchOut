package retrofit

import android.util.Log
import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import utils.Constant.API.LOG

//코틀린에서 object는 무조건 싱글톤
object STTRetrofitClient {

    //레트로핏 클라이언트 선언
    //private lateinit var retrofitClient:Retrofit  아래와 완전히 같은 의미
    private var retrofitClient: Retrofit?=null

    fun getSTTClient(baseUrl: String):Retrofit?{
        Log.d(LOG,"getSTTClient 호출됨")
        val gson = GsonBuilder()
            .setLenient()
            .create()

        if(retrofitClient==null){
            retrofitClient=Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
        }
        return retrofitClient  //끝끝내 리턴하는 것은 client
    }

}