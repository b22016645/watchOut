package retrofit


import android.util.Log
import com.google.gson.JsonElement
import model.POI
import model.Route
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Response
import route.SafeRoute
import utils.Constant
import utils.Constant.API.BASE_URL_KAKAO_API
import utils.Constant.API.LOG
import utils.Constant.API.authorization
import utils.Constant.API.contentType
import utils.Constant.API.transferEncoding
import java.io.IOException
import kotlin.collections.ArrayList

class RetrofitManager {

    //혹시나 이상한 문제가 생기면 intanceㄹㄹ 싱글턴으로 바꾸는 걸 해봐라.

    //명칭검색 api호출
    fun searchPOI(searchKeyword:String?, centerLat:Double, centerLon: Double, completion:(Constant.RESPONSE_STATE,ArrayList<POI>?)->Unit){
        // 레트로핏 인터페이스 가져오기
        val iRetrofitROI : IRetrofit? = POIRetrofitClient.getPOIClient(Constant.API.BASE_URL)?.create(IRetrofit::class.java)

        val keyword = searchKeyword.let {
            it  //keyword
        }?: ""

        val CLat = centerLat
        val CLon = centerLon

        val call = iRetrofitROI?.searchPOI(searchKeyword = keyword, centerLat=CLat,centerLon=CLon).let {
            it  //call
        }?: return


        //본격적인 요청
        call.enqueue(object : retrofit2.Callback<JsonElement>{
            override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                completion(Constant.RESPONSE_STATE.FAIL, null)
            }

            //성공시
            override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {

                when(response.code()){
                    200->{ //응답코드가 200일때만 작동할 수 있게 예를 들어 응답이 없는 경우는 작동하지 않음.
                        response.body()?.let{//body가 있다면
                            Log.d(LOG,"POI api 호출 성공")
                            var parsePOIDataArray = ArrayList<POI>() //model.POI데이터를 받아 넣는 리스트

                            val body = it.asJsonObject

                            val searchPoiInfo=body.get("searchPoiInfo").asJsonObject

                            val pois = searchPoiInfo.get("pois").asJsonObject

                            val poi = pois.getAsJsonArray("poi")

                            poi.forEach { poiItem ->
                                val poiItemObject = poiItem.asJsonObject
                                val name = poiItemObject.get("name").asString
                                val frontLat= poiItemObject.get("frontLat").asString
                                val frontLon= poiItemObject.get("frontLon").asString

                                val POIItem = POI(
                                    name=name,
                                    frontLat=frontLat,
                                    frontLon=frontLon
                                )
                                parsePOIDataArray.add(POIItem)
                            }
                            completion(Constant.RESPONSE_STATE.OKAY,parsePOIDataArray)
                        }
                    }
                    204->{
                        Log.d(LOG,"204에스러러러")
                        completion(Constant.RESPONSE_STATE.NO_CONTENT,null)
                    }
                }
            }
        })
    }


    //경로탐색 api호출
    fun searchRoute(startX:Double, startY:Double, endX:Double, endY:Double, startname:String, endname:String, searchOption:Int, completion: (Constant.RESPONSE_STATE, ArrayList<Route>?,Double) -> Unit){
        val iRetrofitRoute : IRetrofit? = RouteRetrofitClient.getRouteClient(Constant.API.BASE_URL)?.create(IRetrofit::class.java)
        val startX=startX
        val startY=startY
        val endX=endX
        val endY=endY
        val startname=startname
        val endname=endname
        val searchOption=searchOption

        val call=iRetrofitRoute?.searchRoute(startX = startX,startY=startY,endX=endX,endY=endY,startName = startname,endName = endname,searchOption=searchOption).let{
            it
        }?:return

        call.enqueue(object : retrofit2.Callback<JsonElement>{
            override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                completion(Constant.RESPONSE_STATE.FAIL, null,0.0)
            }

            override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                when(response.code()){
                    200->{
                        Log.d(LOG,"ROUTE api 호출 성공")
                        response.body()?.let {
                            var parseRouteDataArray = ArrayList<Route>()

                            val body = it.asJsonObject

                            var score = 0.0
                            var totalDistance : Int? = 0
                            val features=body.getAsJsonArray("features")

                            features.forEach { featuresItem->
                                val featureObject = featuresItem.asJsonObject
                                val geometry=featureObject.get("geometry").asJsonObject
                                val coordinates=geometry.get("coordinates")//JsonElement

                                val properties = featureObject.get("properties").asJsonObject
                                var turnType : Int?
                                var roadType : Int?
                                var distance : Int?

                                turnType = properties.get("turnType")?.asInt
                                roadType = properties.get("roadType")?.asInt
                                distance = properties.get("distance")?.asInt

                                if (totalDistance==0){
                                    totalDistance = properties.get("totalDistance")?.asInt
                                }

                                score += SafeRoute.calcPartialScore(totalDistance,distance,roadType,turnType)

                                var RouteItem = Route(
                                    coordinates = coordinates,
                                    turnType = turnType
                                )
                                parseRouteDataArray.add(RouteItem)
                            }
                            completion(Constant.RESPONSE_STATE.OKAY,parseRouteDataArray,score)
                        }
                    }
                    403->{
                        Log.d(LOG,"Manager - ROUTE API 403에러")
                        completion(Constant.RESPONSE_STATE.ERROR403,null,0.0)
                    }
                }
            }
        })
    }


    //STT
    fun requestStt(byteAudioData: ByteArray?, completion: (Constant.RESPONSE_STATE, String) -> Unit) {

        var byteAudioData: ByteArray? = byteAudioData
        val meida = "video/*".toMediaTypeOrNull()
        val requestBody = byteAudioData!!.toRequestBody(meida)

        val iRetrofitSTT : IRetrofit? = STTRetrofitClient.getSTTClient(BASE_URL_KAKAO_API)?.create(IRetrofit::class.java)

        val call=iRetrofitSTT?.get_post_pcm(transferEncoding,contentType,authorization,requestBody).let{
            it
        }?:return

        call.enqueue(object : retrofit2.Callback<ResponseBody>{
            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.d(LOG, "Fail msg = " + t.message)
                completion(Constant.RESPONSE_STATE.FAIL,"")
            }

            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    val body = response.body()
                    var result: String? = null
                    var sttResultMsg = ""
                    try {
                        result = body!!.string()
                        Log.d(LOG, result)
                    }
                    catch (e: IOException) {
                        e.printStackTrace()
                    }
                    val startIndex = result!!.indexOf("{\"type\":\"finalResult\"")
                    val endIndex = result.lastIndexOf('}')
                    Log.d(LOG, "startIndex = $startIndex, endIndex = $endIndex")
                    if (startIndex > 0 && endIndex > 0) {
                        try {
                            val result_json_string = result.substring(startIndex, endIndex + 1)
                            val json = JSONObject(result_json_string)
                            sttResultMsg = json.getString("value")

                            Log.d(LOG, "sttManager - 결과값 : "+sttResultMsg)
                            completion(Constant.RESPONSE_STATE.OKAY,sttResultMsg)
                        }
                        catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    }
                    else { // errorCalled
                        //다시입력해주세요.
                        Log.d(LOG, "errorCalled")
                        completion(Constant.RESPONSE_STATE.FAIL,sttResultMsg)
                    }
                }
            }
        })
    }
}