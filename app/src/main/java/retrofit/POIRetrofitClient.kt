package retrofit

import android.os.Handler
import android.os.Looper
import android.util.Log
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import utils.Constant
import utils.Constant.API.LOG


object POIRetrofitClient {

    private var retrofitClient: Retrofit?=null

    fun getPOIClient(baseUrl: String):Retrofit?{

        var client=OkHttpClient.Builder()

        //인터셉터를 통한 기본 파라미터 설정
        val baseParameterInterceptor:Interceptor=(object :Interceptor{
            override fun intercept(chain: Interceptor.Chain): Response {
                //오리지널 리퀘스트
                val originalRequest=chain.request()

                //기본 쿼리 파라미터 추가하기
                val addedUrl=originalRequest.url.newBuilder().
                addQueryParameter("version",Constant.API.VERSION).
                addQueryParameter("format", Constant.API.FORMAT).
                addQueryParameter("count", Constant.API.COUNT).
                addQueryParameter("callback", Constant.API.CALLBACK).
                addQueryParameter("appkey", Constant.API.APPKEY).
                addQueryParameter("radius",Constant.API.RADIUS).
                build()

                //최종요청
                val finalRequest = originalRequest.newBuilder().
                url(addedUrl).
                method(originalRequest.method,originalRequest.body).
                build()
                val response = chain.proceed(finalRequest)

                //Log.d(LOG,"getPOIClient 최종 요청"+"${finalRequest}")

                if(response.code != 200){
                    Handler(Looper.getMainLooper()).post{
                    }
                }
                if(response.code == 204) {
                    Log.d(LOG,"204에러")
                    val finalRequest = originalRequest.newBuilder().
                    method(originalRequest.method,originalRequest.body).
                    build()

                    val response = chain.proceed(finalRequest)
                }
                return response
            }
        })

        // 위에서 설정한 기본파라매터 인터셉터를 okhttp 클라이언트에 추가한다.
        client.addInterceptor(baseParameterInterceptor)

        if(retrofitClient==null){
            retrofitClient=Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client.build())
                .build()
        }
        return retrofitClient  //끝끝내 리턴하는 것은 client
    }
}