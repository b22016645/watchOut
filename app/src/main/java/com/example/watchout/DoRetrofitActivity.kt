package com.example.watchout

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.WindowManager
import com.example.watchout.MyMqtt
import com.example.watchout.R
import com.example.watchout.databinding.ActivityMainBinding
import com.google.gson.Gson
import model.*
import retrofit.RetrofitManager
import route.DetailRoute
import utils.Constant
import utils.Constant.API.LOG
import utils.Constant.API.SCORE_SAFEROUTE
import kotlin.concurrent.timer
import kotlin.math.floor


class DoRetrofitActivity : Activity(){

    //mqtt관련
    private lateinit var myMqtt: MyMqtt
    val sub_topic = "android"

    //안전한 길 개수
//    var rawRouteNum = 0
//    var routeRes : String = ""

    //mqtt보낼 route4개저장
    private var routeBuilder = StringBuilder()

    //안전할 길 점수 받을 배열
    //private var scoreList = arrayListOf<Double>()
    private var scoreList = arrayListOf<SaftyScore?>()
    //private var scoreList = Array<SaftyScore?>(4){null}

    //searchOption 목록
    private var safeList = listOf(0,4,10,30)

    //private var lat: Double = 37.58217852030164
    //private var lon: Double = 127.01152516595631
    private var lat: Double = 0.0
    private var lon: Double = 0.0

    private var sttResultMsg : String = ""

    private var getscorecount = 0

    private var errorcount = 0

    private var instance = RetrofitManager()

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(R.layout.activity_doretrofit)

        //화면이 꺼지지 않게
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        //일단 호출은 된다.
        Log.d(LOG,"DoRetrofit호출됨")

        //mqtt관련
        myMqtt = MyMqtt(this)
        myMqtt.connect(arrayOf<String>(sub_topic))

        val doRrtrofitData = intent.getSerializableExtra("doRrtrofitData") as model.DoRetrofitData

        Log.d(LOG,"DoRetrofit - doRrtrofitData : "+"${doRrtrofitData}")

        routeBuilder.clear()

        var byteAudioData = doRrtrofitData.byteAudioData
        lat = doRrtrofitData.lat
        lon = doRrtrofitData.lon
        sttResultMsg = doRrtrofitData.destination

        Handler().postDelayed(java.lang.Runnable {

            if (byteAudioData != null) { //정상적으로 들어왔을 때
                requestStt(byteAudioData)
            } else if (byteAudioData == null) { //경로이탈로 재검색해서 들어왔을 때
                getPOI(sttResultMsg, lat, lon)
            }
        }, 1000)
    }

    override fun onDestroy() {
        super.onDestroy()
        myMqtt.mqttClient.unregisterResources()
    }

    //mqtt관련
    fun publish(topic:String,data:String){
//        Handler().postDelayed(java.lang.Runnable {
//            myMqtt.publish(topic, data)
//        },1000)
        myMqtt.publish(topic, data)
    }

    private fun requestStt(byteAudioData : ByteArray){
        instance.requestStt(byteAudioData) { responseState, sttResultMsg ->
            when (responseState) {
                Constant.RESPONSE_STATE.OKAY -> {  //만약 STATE가 OKEY라면
                    if (lat == 1.1 && lon == 1.1){ //즐겨찾기 등록시
                        Favorites.dat.replace("nickname", sttResultMsg)
                        Log.d(LOG,"nickname ="+"${sttResultMsg}")
                        val returnintent = Intent()
                        setResult(0,returnintent)
                        finish()
                    }
                    else {
                        //여기에 즐겨찾기 목록에서 찾는 코드 쓰면 돼용
                        //만약 즐겨찾기 목록에 있으면 밑에 들어가는ㄴ 이름->
                        getPOI(sttResultMsg, lat, lon)
                    }
                }
                Constant.RESPONSE_STATE.FAIL -> {
                    Log.d(LOG,"DoRetrofit - 잘못된 음성으로 main으로 돌아감.")
                    val returnintent = Intent()
                    setResult(RESULT_CANCELED,returnintent)
                    finish()
                }
            }
        }
    }


    private fun getPOI(location : String, lat : Double, lon : Double){
        Log.d(LOG,"DoRetrofit - getPOI호출")
        Log.d(LOG, "DoRetrofit - 목적지 : "+"${location}")
        History.dpName = location       //DB저장음(히스토리)
        Favorites.dat.replace("address",location)   //DB즐겨찾기 추가

        //추후에 경로이탈일 때 사용되니깐 그냥 여기에 두면 됨.
        sttResultMsg = location

        publish("des",sttResultMsg)

        //목적지 좌표
        var destinationPoint = arrayListOf<Double>() //[0]=>lat, [1]=>lon

        //경로탐색시 startname과 endname은 중요하지 않기 때문에 그냥 아무거난 만듦.
        val startname = "%EC%B6%9C%EB%B0%9C"
        val endname = "%EB%B3%B8%EC%82%AC"

        instance.searchPOI(searchKeyword = location, centerLat = lat, centerLon = lon,completion = {
                responseState, parsePOIDataArray ->
            when(responseState){
                Constant.RESPONSE_STATE.OKAY->{  //만약 STATE가 OKEY라면

                    if (parsePOIDataArray != null) {
                        //목적지가 ㅇㅇ역일 때 두번째값 = n번출구
                        if (parsePOIDataArray.get(0).name.contains("역") == true ){
                            destinationPoint.add(parsePOIDataArray.get(1).frontLat.toDouble())
                            destinationPoint.add(parsePOIDataArray.get(1).frontLon.toDouble())
                        }//아닐때 걍 목적지
                        else {
                            destinationPoint.add(parsePOIDataArray.get(0).frontLat.toDouble())
                            destinationPoint.add(parsePOIDataArray.get(0).frontLon.toDouble())
                        }
                        Log.d(LOG,"목적지 좌표 : "+"${destinationPoint[0]}"+", "+"${destinationPoint[1]}")
                        History.dpLat = destinationPoint[0]         //DB저장용
                        History.dpLon = destinationPoint[1]         //DB저장용
                        Favorites.dat.replace("lat",destinationPoint[0])  //즐겨찾기 저장용
                        Favorites.dat.replace("lon",destinationPoint[0])  //즐겨찾기 저장용


                        var timercount = 0
                        timer(period = 500,initialDelay = 500){
                            if(timercount!=4){
                                getScore(lon,lat,destinationPoint[1],destinationPoint[0],startname,endname,safeList[timercount],timercount)

                                timercount++
                                getscorecount++
                            }
                            else{
                                cancel()
                            }
                        }
                    }
                }
                Constant.RESPONSE_STATE.NO_CONTENT->{//204에러면 main으로 갔다가 stt로 보냄
                    Log.d(LOG,"204maind에러러러러러러")
                    Log.d(LOG,"DoRetrofit - 잘못된 목적지로 main으로 돌아감.")
                    val returnintent = Intent()
                    setResult(RESULT_CANCELED,returnintent)
                    finish()
                }
            }
        })
    }


    private fun getScore(startx : Double, starty : Double, endx : Double, endy : Double, startname : String, endname : String, searchOption: Int, tcount: Int) {
        Log.d(LOG,"DoRetrofit - getScore호출")

        //api를 통해 얻은 JSON을 파싱해서 가져온 이중배열 좌표
        var rawRoute = arrayListOf<List<Double>>() //[0]=>lon, [1]=>lat

        // 길찾기 호출
        instance.searchRoute(
            startX = startx,
            startY = starty,
            endX = endx,
            endY = endy,
            startname = startname,
            endname = endname,
            searchOption = searchOption,
            completion = { responseState, parseRouteDataArray, saftyScore ->
                when (responseState) {
                    Constant.RESPONSE_STATE.OKAY -> {  //만약 STATE가 OKEY라면

                        if (parseRouteDataArray != null) {
                            var jsonarr = parseRouteDataArray.listIterator()
                            while (jsonarr.hasNext()) {
                                var jsonarrNext = jsonarr.next()

                                var corrdinate = jsonarrNext.coordinates.asJsonArray
                                var next = Gson().fromJson(corrdinate, ArrayList::class.java)
                                    .listIterator()
                                var whatType = 0
                                while (next.hasNext()) {
                                    val next2 = next.next()
                                    if (next2 !is Double) {
                                        rawRoute.add(next2 as List<Double>)
                                        whatType++
                                    }
                                }
                                if (whatType != 0) {
                                    rawRoute.removeAt(rawRoute.size - 1)
                                }
                            }
                        }

                        rawRoute.add(arrayListOf(endx, endy))

                        // x좌표 다 넣기
                        for (i in rawRoute.indices) {
                            routeBuilder.append(rawRoute[i][1].toString())
                            if (i < rawRoute.size - 1) {
                                routeBuilder.append(",")
                            }
                        }

                        //y좌표 다 스트링으로 만듬
                        routeBuilder.append("/")
                        for (i in rawRoute.indices) {
                            routeBuilder.append(rawRoute[i][0].toString())
                            if (i < rawRoute.size - 1) {
                                routeBuilder.append(",")
                            }
                        }

                        if (tcount < 3 ){
                            routeBuilder.append("!")
                        }

/*                        var routeString = routeBuilder.toString()
                        publish("route",routeString)*/

                        if (getscorecount == 4 && errorcount != 0) {  // 4번 돌았는데 403에러가 1개라도 있었다면
                            Log.d(LOG, "DoRetrofit - ROUTE API 403에러 - getScore")
                            scoreList.clear()
                            getscorecount = 0
                            errorcount = 0
                            getPOI(sttResultMsg, lat, lon)
                        } else {
                            //  Log.d(SCORE_SAFEROUTE, "나누기전 스코어" + "${saftyScore?.score}")
                            //  Log.d(SCORE_SAFEROUTE, "나누기전 토탈디스탄스" + "${saftyScore?.totalDistance}")
                            saftyScore?.score = floor((saftyScore?.score!! / saftyScore.totalDistance!!) *1000)
                            Log.d(SCORE_SAFEROUTE, "나누고 스코어" + "${saftyScore?.score}")
                            //  Log.d(SCORE_SAFEROUTE, "나누고 토탈디스탄스" + "${saftyScore?.totalDistance}")

                            scoreList.add(saftyScore)
                            Log.d(SCORE_SAFEROUTE, "scoreList : " + "${saftyScore}")

                            //경로 배열에 경로의 모든 정보 추가함수, 경로 하나 추가시 마다 호출

                            if (scoreList.size ==4)  {
                                var routeString = routeBuilder.toString()
                                publish("route",routeString)

                                Log.d(SCORE_SAFEROUTE, ""+"${saftyScore}" )
                                //경로 배열내 4가지(전부임)경로 모두 프린트(정보), 경로 다 추가 되면 한번 불림

                                var max = scoreList[0]?.score!!

                                var ind = 0
                                for (i in 1..3) {
                                    if (scoreList[i]?.score!! > max) {
                                        max = scoreList[i]?.score!!
                                        ind=i
                                    }
                                }
                                History.routNum = ind       //DB저장용
                                var timercount = 0
                                timer(period = 500, initialDelay = 500) {
                                    if (timercount == 0) {
                                        getRoute(
                                            startx,
                                            starty,
                                            endx,
                                            endy,
                                            startname,
                                            endname,
                                            safeList[ind]
                                        )
                                        timercount++
                                        cancel()
                                    }
                                }
                            }
                        }
                    }
                    Constant.RESPONSE_STATE.ERROR403 -> {
                        errorcount++
                        if(getscorecount==4){ //4번째 값이 403에러라면
                            Log.d(LOG,"DoRetrofit - ROUTE API 403에러 - getScore")
                            getscorecount=0
                            errorcount=0
                            scoreList.clear()
                            getPOI(sttResultMsg,lat,lon)
                        }
                    }
                }
            }
        )
    }


    private fun getRoute(startx : Double, starty : Double, endx : Double, endy : Double, startname : String, endname : String, searchOption: Int) {
        Log.d(LOG,"DoRetrofit - getRoute호출")
        Log.d(LOG,"DoRetrofit - searchOption : "+"${searchOption}")
//        publish("topic","안전한 길 알고리즘으로 선택된 길입니다")

        //Safey data 4개 pub
        var scoreBuilder = StringBuilder()
        for(i in 0..3) {
            scoreBuilder.append(scoreList[i].toString())
            if (i < 3) {
                scoreBuilder.append("!")
            }
        }
        var scoreStr = scoreBuilder.toString()
        // Log.d(LOG,"SaftyScore : "+scoreStr)
        publish("saftyScore",scoreStr)

        scoreList.clear() //안전한 길에서 빠져나와 getRoute를 호출했으면 초기화

        getscorecount = 0
        errorcount = 0

        //세분화된 좌표를 저장할 배열
        var midpointList = arrayListOf<List<Double>>() //[0]=>lat, [1]=>lon

        //api를 통해 얻은 JSON을 파싱해서 가져온 이중배열 좌표
        var rawRouteRes = arrayListOf<List<Double>>() //[0]=>lon, [1]=>lat

        //api를 통해 얻은 JSON을 파싱해서 가져온 분기점 배열 좌표
        var turnTypeList = arrayListOf<Int>()

        var turnPoint = arrayListOf<List<Double>>()

        // 길찾기 호출
        instance.searchRoute(
            startX = startx,
            startY = starty,
            endX = endx,
            endY = endy,
            startname = startname,
            endname = endname,
            searchOption = searchOption,
            completion = { responseState, parseRouteDataArray, saftyScore ->
                when (responseState) {
                    Constant.RESPONSE_STATE.OKAY -> {  //만약 STATE가 OKEY라면
                        if (parseRouteDataArray != null) {
                            var jsonarr = parseRouteDataArray.listIterator()
                            while (jsonarr.hasNext()){
                                var jsonarrNext = jsonarr.next()

                                var corrdinate = jsonarrNext.coordinates.asJsonArray
                                var next = Gson().fromJson(corrdinate,ArrayList::class.java).listIterator()
                                var whatType = 0
                                while (next.hasNext()){
                                    val next2 = next.next()
                                    if(next2 !is Double){
                                        rawRouteRes.add(next2 as List<Double>)
                                        whatType++
                                    }
                                }
                                if ( whatType != 0 ) {
                                    rawRouteRes.removeAt(rawRouteRes.size-1)
                                }
                                var turnType = jsonarrNext.turnType
                                if(turnType!=null){
                                    turnTypeList.add(turnType)
                                }
                                else{
                                    for(x in turnTypeList.size .. rawRouteRes.size-1) {
                                        turnTypeList.add(0)
                                    }
                                }
                            }
                        }

                        rawRouteRes.add(arrayListOf(endx,endy))

                        //UI에 나오는 거리 계산을 위해 분기점 좌표만 얻는 중
                        for( i in turnTypeList.indices){
                            if((turnTypeList[i] >= 212 || (turnTypeList[i] in 12..19))){
                                turnPoint.add(listOf(rawRouteRes[i][1],rawRouteRes[i][0]))
                            }
                        }


//                        //routeRes 보냄
//
//                        var routeResBuilder = StringBuilder()
//
//                        // x좌표 다 넣기
//                        for (i in rawRouteRes.indices) {
//                            routeResBuilder.append(rawRouteRes[i][1].toString())
//                            if (i < rawRouteRes.size-1){
//                                routeResBuilder.append(",")
//                            }
//                        }
//
//                        //y좌표 다 스트링으로 만듬
//                        routeResBuilder.append("/")
//                        for (i in rawRouteRes.indices) {
//                            routeResBuilder.append(rawRouteRes[i][0].toString())
//                            if (i < rawRouteRes.size-1){
//                                routeResBuilder.append(",")
//                            }
//                        }
//
//                        var routeResString = routeResBuilder.toString()
//
//                        publish("route_res",routeResString)
//                        Log.d(LOG,"routeRes : "+"${routeResString}")

                        var size=0

                        //중간좌표를 얻음.
                        for (i in rawRouteRes.indices) {
                            if (i < rawRouteRes.size-1) {
                                midpointList.add(listOf(rawRouteRes[i][1], rawRouteRes[i][0]))
                                size = midpointList.size
                                midpointList = DetailRoute.midPoint(
                                    rawRouteRes[i][1],
                                    rawRouteRes[i][0],
                                    rawRouteRes[i + 1][1],
                                    rawRouteRes[i + 1][0],
                                    midpointList
                                )
                                for (j in size..midpointList.size - 1) {
                                    turnTypeList.add(j, 0)
                                }
                            }

                            else {
                                midpointList.add(  //맨 마지막 값 넣기
                                    listOf(
                                        rawRouteRes[i][1],
                                        rawRouteRes[i][0]
                                    )
                                )
                            }
                        }

                        //midpointList 보냄

                        var midpointBuilder = StringBuilder()

                        // x좌표 다 넣기
                        for (i in midpointList.indices) {
                            midpointBuilder.append(midpointList[i][0].toString())
                            if (i < midpointList.size-1){
                                midpointBuilder.append(",")
                            }
                        }

                        //y좌표 다 스트링으로 만듬
                        midpointBuilder.append("/")
                        for (i in midpointList.indices) {
                            midpointBuilder.append(midpointList[i][1].toString())
                            if (i < midpointList.size-1){
                                midpointBuilder.append(",")
                            }
                        }

                        var midpointString = midpointBuilder.toString()

                        publish("midpoint",midpointString)
                        Log.d(LOG,"midpoint : "+"${midpointString}")


                        var naviData = NaviData(midpointList, turnTypeList, sttResultMsg, turnPoint)
                        //받아서 retrunMain호출함수호출
                        returnMain(naviData)

                    }
                    Constant.RESPONSE_STATE.ERROR403 -> {
                        Log.d(LOG,"DoRetrofit - ROUTE API 403에러 - getRoute")
                        getRoute(startx,starty,endx,endy,startname,endname,searchOption)
                    }
                }
            }
        )
    }


    //returnMain 호출함수다
    private fun returnMain(naviDataItem : NaviData ) {
        Log.d(LOG,"DoRetrofit - retrunMain호출")
        val returnintent = Intent()
        returnintent.putExtra("naviData",naviDataItem)
        setResult(RESULT_OK,returnintent)
        finish()
    }
}