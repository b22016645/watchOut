package com.example.watchout

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import calling.MyMqtt
import com.example.watchout.databinding.ActivityMainBinding
import com.google.gson.Gson
import model.*
import retrofit.RetrofitManager
import route.DetailRoute
import route.SafeRoute
import utils.Constant
import utils.Constant.API.LOG
import kotlin.concurrent.timer


class DoRetrofitActivity : Activity(){

    //mqtt관련
    private lateinit var myMqtt: MyMqtt
    val sub_topic = "android"

    //mqtt보낼 route4개저장
    private var routeBuilder = StringBuilder()

    //안전할 길 점수 받을 배열
   // private var scoreList = arrayListOf<SaftyScore?>()
    private var routeList = arrayListOf<RouteInfor?>()

    //searchOption 목록
    private var searchOptionList = listOf(0,4,10,30)

    //private var lat: Double = 37.58217852030164
    //private var lon: Double = 127.01152516595631
    private var lat: Double = 0.0
    private var lon: Double = 0.0

    private var destination : String = ""

    private var getScoreCount = 0

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
        val num = intent.getIntExtra("name",0)

        Log.d(LOG,"DoRetrofit - doRrtrofitData : "+"${doRrtrofitData}")

        routeBuilder.clear()

        destination = doRrtrofitData.destination!!
        lat = doRrtrofitData.lat
        lon = doRrtrofitData.lon

        if (num == 1){ //즐겨찾기에 있었다면
            startScore(lon,lat,"",destination)
        }
        else { //없었다면
            getPOI(destination, lat, lon)
        }
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

    private fun getPOI(location : String, lat : Double, lon : Double){
        Log.d(LOG,"DoRetrofit - getPOI호출")
        Log.d(LOG, "DoRetrofit - 목적지 : "+"${location}")

        //publish를 여기ㅅ 꼭 해야하는지??
        //publish("des",location)

        //목적지 좌표
        var destinationPoint = arrayListOf<Double>() //[0]=>lat, [1]=>lon

        //경로탐색시 startname과 endname은 중요하지 않기 때문에 그냥 아무거난 만듦.
        val startname = "%EC%B6%9C%EB%B0%9C"
        val endname = "%EB%B3%B8%EC%82%AC"

        instance.searchPOI(searchKeyword = location, centerLat = lat, centerLon = lon,completion = {
                responseState, parsePOIDataArray ->
            when(responseState){
                Constant.RESPONSE_STATE.OKAY->{  //만약 STATE가 OKEY라면
                    val poiArray : POI
                    if (parsePOIDataArray != null) {
                        //목적지가 ㅇㅇ역일 때 두번째값 = n번출구
                        if (parsePOIDataArray.get(0).name.contains("역") == true ){
                            poiArray = parsePOIDataArray.get(1)
                        }//아닐때 걍 목적지
                        else {
                            poiArray = parsePOIDataArray.get(0)
                        }
                        destinationPoint.add(poiArray.frontLat.toDouble())
                        destinationPoint.add(poiArray.frontLon.toDouble())

                        Log.d(LOG,"목적지 좌표 : "+"${destinationPoint[0]}"+", "+"${destinationPoint[1]}")

                        History.dpLat = destinationPoint[0]         //DB저장용
                        History.dpLon = destinationPoint[1]         //DB저장용
                        History.dpName = poiArray.address           //DB저장용 (목적지주소)
                        Favorites.dat.replace("lat",destinationPoint[0])  //즐겨찾기 저장용
                        Favorites.dat.replace("lon",destinationPoint[0])  //즐겨찾기 저장용

                        startScore(destinationPoint[1],destinationPoint[0],startname,endname)
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

    private fun startScore(des1 : Double , des2 : Double, sName: String, eName : String) {
        var timercount = 0
        timer(period = 1000,initialDelay = 500){
            if(timercount!=4){
                getScore(lon,lat,des1,des2,sName,eName,searchOptionList[timercount],timercount)

                timercount++
                getScoreCount++
            }
            else{
                cancel()
            }
        }
    }


    private fun getScore(startx : Double, starty : Double, endx : Double, endy : Double, startname : String, endname : String, searchOption: Int, tcount: Int) {
        Log.d(LOG,"DoRetrofitActivity - getScore()시작")

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
            completion = { responseState, parseRouteDataArray, routeInfor ->
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

                        if (getScoreCount == 4 && errorcount != 0) {  // 4번 돌았는데 403에러가 1개라도 있었다면
                            Log.d(LOG, "DoRetrofit - ROUTE API 403에러 - getScore")
                            routeList.clear()
                            getScoreCount = 0
                            errorcount = 0
                            getPOI(destination, lat, lon)
                        } else {
                            //  Log.d(SCORE_SAFEROUTE, "나누기전 스코어" + "${saftyScore?.score}")
                            //  Log.d(SCORE_SAFEROUTE, "나누기전 토탈디스탄스" + "${saftyScore?.totalDistance}")
                                //saftyScore?.score = floor((saftyScore?.score!! / saftyScore.totalDistance!!) *1000)
                                //Log.d(SCORE_SAFEROUTE, "나누고 스코어" + "${saftyScore?.score}")
                            //  Log.d(SCORE_SAFEROUTE, "나누고 토탈디스탄스" + "${saftyScore?.totalDistance}")

                            //scoreList.add(saftyScore)
                            routeList.add(routeInfor)
                            Log.d("DoRetrofitActivity-getScore() :", "!!routeList : " + "${routeList}")
                            //경로 배열에 경로의 모든 정보 추가함수, 경로 하나 추가시 마다 호출

                            if (routeList.size ==4)  {
                                Log.d("DoRetrofitActivity-getScore() :","routeList Size = 4")
                                var routeString = routeBuilder.toString()
                                publish("route",routeString)

                                //여기까지 오면 루트 4개가 모두 모인 상태.
                                //이제 여기서 점수를 내야함

                                SafeRoute.nomalizeScore(routeList)  //점수 정규화
                                SafeRoute.makeFinalScore(routeList)     //최종 점수 합성
                                SafeRoute.makeRouteInfor_forPublish(routeList)      //pub할 스트링 데이타 만들기
                                SafeRoute.makeEctInfor_forPublish(routeList)      //pub할 기타내용 스트링 데이타 만들기

                                Log.d("DoRetrofitActivity-getScore() :최종 경로 4가지 모음", ""+"${routeInfor}" )
                                //경로 배열내 4가지(전부임)경로 모두 프린트(정보), 경로 4가지 모두 추가되면 한 번

                                var max = routeList[0]?.routeScore!!

                                var ind = 0
                                for (i in 1..3) {
                                    if (routeList[i]?.routeScore!! > max) {
                                        max = routeList[i]?.routeScore!!
                                        ind=i
                                    }
                                }
                                History.routNum = ind       //DB저장용
                                History.hasDanger = routeList[ind]!!.hasDanger
                                History.hasDangerA = routeList[ind]!!.hasDangerA
                                History.hasDangerB = routeList[ind]!!.hasDangerB
                                History.hasCrossWalk=routeList[ind]!!.hasCrossWalk
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
                                            searchOptionList[ind]
                                        )
                                        timercount++
                                        cancel()
                                    }
                                }
                            }//end of if (scoreList.size ==4)
                        }//
                    }
                    Constant.RESPONSE_STATE.ERROR403 -> {
                        errorcount++
                        if(getScoreCount==4){ //4번째 값이 403에러라면
                            Log.d(LOG,"DoRetrofit - ROUTE API 403에러 - getScore")
                            getScoreCount=0
                            errorcount=0
                            routeList.clear()
                            getPOI(destination,lat,lon)
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
            scoreBuilder.append(routeList[i]?.routeInforStringData)
            scoreBuilder.append(",");
            scoreBuilder.append(routeList[i]?.ectInforStringData)
            //여기서 routeInfor에 만들어놓은 퍼블리쉬할 스트링 모두 이어붙이기
            if (i < 3) {
                scoreBuilder.append("!")
            }
        }
        var routeData = scoreBuilder.toString()
        // Log.d(LOG,"SaftyScore : "+scoreStr)
        Log.d("DoRetrofit-getRoute() : 퍼블리쉬할 스트링데이터 (전체루트정보)",routeData)
        //웹 내 표에 띄울 데이타들 퍼블리쉬
        publish("saftyScore",routeData)

        routeList.clear() //안전한 길에서 빠져나와 getRoute를 호출했으면 초기화

        getScoreCount = 0
        errorcount = 0

        //세분화된 좌표를 저장할 배열
        var midpointList = arrayListOf<List<Double>>() //[0]=>lat, [1]=>lon
        History.midPointSize = midpointList.size

        //api를 통해 얻은 JSON을 파싱해서 가져온 이중배열 좌표
        var rawRouteRes = arrayListOf<List<Double>>() //[0]=>lon, [1]=>lat

        //api를 통해 얻은 JSON을 파싱해서 가져온 분기점 배열 좌표
        var turnTypeList = arrayListOf<Int>()

        //facility리스트
        var facilityTypeList = arrayListOf<Int>()

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
            completion = { responseState, parseRouteDataArray, routeInfor ->
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
                                var facilityType = jsonarrNext.facilityType
                                if(turnType!=null){
                                    turnTypeList.add(turnType)
                                    facilityTypeList.add(facilityType!!)
                                }
                                else{
                                    for(x in turnTypeList.size .. rawRouteRes.size-1) {
                                        turnTypeList.add(0)
                                        facilityTypeList.add(facilityType!!)
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

                        //routeRes 보냄

                        var routeResBuilder = StringBuilder()

                        // x좌표 다 넣기
                        for (i in rawRouteRes.indices) {
                            routeResBuilder.append(rawRouteRes[i][1].toString())
                            if (i < rawRouteRes.size-1){
                                routeResBuilder.append(",")
                            }
                        }

                        //y좌표 다 스트링으로 만듬
                        routeResBuilder.append("/")
                        for (i in rawRouteRes.indices) {
                            routeResBuilder.append(rawRouteRes[i][0].toString())
                            if (i < rawRouteRes.size-1){
                                routeResBuilder.append(",")
                            }
                        }

                        var routeResString = routeResBuilder.toString()

                        publish("route_res",routeResString)
                        Log.d(LOG,"routeRes : "+"${routeResString}")

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

//                        //midpointList 보냄
//
//                        var midpointBuilder = StringBuilder()
//
//                        // x좌표 다 넣기
//                        for (i in midpointList.indices) {
//                            midpointBuilder.append(midpointList[i][0].toString())
//                            if (i < midpointList.size-1){
//                                midpointBuilder.append(",")
//                            }
//                        }
//
//                        //y좌표 다 스트링으로 만듬
//                        midpointBuilder.append("/")
//                        for (i in midpointList.indices) {
//                            midpointBuilder.append(midpointList[i][1].toString())
//                            if (i < midpointList.size-1){
//                                midpointBuilder.append(",")
//                            }
//                        }
//
//                        var midpointString = midpointBuilder.toString()
//
//                        publish("midpoint",midpointString)
//                        Log.d(LOG,"midpoint : "+"${midpointString}")


                        val naviData = NaviData(midpointList, turnTypeList, facilityTypeList, destination, turnPoint)
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