package retrofit

import com.google.gson.JsonElement
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*
import utils.Constant

interface IRetrofit {

    // 카카오 음성인식 REST API
    @POST("v1/recognize")
    fun get_post_pcm(
        @Header("Transfer-Encoding") transferEncoding : String,
        @Header("Content-Type") contentType : String,
        @Header("Authorization") authorization : String,
        @Body  audio : RequestBody
    ) : Call<ResponseBody>

    @GET(Constant.API.SEARCH_POI)
    fun searchPOI(@Query("searchKeyword")searchKeyword:String,
                  @Query("centerLat")centerLat:Double,
                  @Query("centerLon")centerLon:Double
    ) : Call<JsonElement>


    @FormUrlEncoded
    @POST(Constant.API.SEARCH_ROUTE)
    fun searchRoute(@Field("startX")startX:Double,
                    @Field("startY")startY:Double,
                    @Field("endX")endX:Double,
                    @Field("endY")endY:Double,
                    @Field("startName")startName:String,
                    @Field("endName")endName:String,
                    @Field("searchOption")searchOption:Int
    ):Call<JsonElement>
}