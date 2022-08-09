package retrofit


import android.util.Log
import com.google.gson.JsonElement
import model.History
import model.POI
import model.Route
import model.SaftyScore
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

                                val newAddressList = poiItemObject.get("newAddressList").asJsonObject
                                val newAddress = newAddressList.getAsJsonArray("newAddress")

                                newAddress.forEach { address -> //목적지 주소 저장
                                    val addressObject = address.asJsonObject
                                    val address = addressObject.get("fullAddressRoad").asString

                                    val POIItem = POI(
                                        name=name,
                                        frontLat=frontLat,
                                        frontLon=frontLon,
                                        address=address
                                    )
                                    parsePOIDataArray.add(POIItem)
                                }
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
    fun searchRoute(startX:Double, startY:Double, endX:Double, endY:Double, startname:String, endname:String, searchOption:Int, completion: (Constant.RESPONSE_STATE, ArrayList<Route>?,SaftyScore?) -> Unit){
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
                completion(Constant.RESPONSE_STATE.FAIL, null,null)
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
                            var saftyScore = SaftyScore(.0,0,0,0,0,
                                0,0,0,0,
                                0,.0,.0,.0,.0,.0,0,0)

                            features.forEach { featuresItem->
                                val featureObject = featuresItem.asJsonObject
                                val geometry=featureObject.get("geometry").asJsonObject
                                val coordinates=geometry.get("coordinates")//JsonElement

                                val properties = featureObject.get("properties").asJsonObject

                                var turnType : Int? = properties.get("turnType")?.asInt
                                var roadType : Int? = properties.get("roadType")?.asInt
                                var distance : Int? = properties.get("distance")?.asInt
                                var facilityType : Int? = properties.get("facility")?.asInt


                                if (totalDistance==0){
                                    totalDistance = properties.get("totalDistance")?.asInt      //경로 총 길이: 단위(m)
                                    var totalTime : Int? = properties.get("totalTime")?.asInt //경로 총 시간 : 단위(초)
                                    History.expectedTime = totalTime
                                }
                                saftyScore.totalDistance = totalDistance


                                //얘는 점수 하나 추가될 때마다 계속불리는 함수
                                SafeRoute.calcPartialScore(facilityType,distance,roadType,turnType, saftyScore)
                                //Log.d(Constant.API.SCORE_SAFEROUTE, "FFFFFFFFFFFFFFF" + "${saftyScore}")


                                var RouteItem = Route(
                                    coordinates = coordinates,
                                    turnType = turnType
                                )
                                parseRouteDataArray.add(RouteItem)
                            }
                            completion(Constant.RESPONSE_STATE.OKAY,parseRouteDataArray,saftyScore)
                        }
                    }
                    403->{
                        Log.d(LOG,"Manager - ROUTE API 403에러")
                        completion(Constant.RESPONSE_STATE.ERROR403,null,null)
                    }
                }
            }
        })
    }
}