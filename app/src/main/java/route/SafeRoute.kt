package route
//fun calcPartialScore(facilityType: Int?, distance: Int?=0, roadType: Int?, turnType: Int?, saftyScore: SaftyScore)
//를 기능을 분리할거임
//1. 배열에다가 모든 루트의 정보를 담기만 하는 함수 ( 점수매기기 X )
//2. 위험요소 초안을 받아서 정규화 하는 함수
//3. 1,2를 바탕으로 최종 점수를 계산하는 함수

import android.util.Log
import model.Preference
import model.RouteInfor


object SafeRoute {//End of object SafeRoute
    //End Of object SafeRoute
    var tw = Preference.tableWeight
    var pf = Preference


    //1. 배열에다가 모든 루트의 정보를 담기만 하는 함수 ( 점수매기기 X )
    fun gatherRouteInfor(
        facilityType: Int?,
        distance: Int?=0,
        roadType: Int?,
        turnType: Int?,
        routeInfor : RouteInfor
    ){
        //도로타입정보(RoadType)
        if (roadType != null) {
            when (roadType) {
                21 -> {//차도와 인도가 분리, 정해진 횡단구역으로만 횡단 가능 : 배점 ) 90
                    routeInfor.roadTypeB = routeInfor.roadTypeB?.plus(distance!!)
                    routeInfor.roadScore_draft = routeInfor.roadScore_draft?.plus(distance!!*90) }

                22 -> {//차도 인도 분리 X || 보행자 횡단에 제약 X 보행자도로 : 배점 ) 70
                    routeInfor.roadTypeC = routeInfor.roadTypeC?.plus(distance!!)
                    routeInfor.roadScore_draft = routeInfor.roadScore_draft?.plus(distance!!*70) }

                23 -> {//차량 통행 불가 보행자 도로 : 배점) 100
                    routeInfor.roadTypeA = routeInfor.roadTypeA.plus(distance!!)
                    routeInfor.roadScore_draft = routeInfor.roadScore_draft?.plus(distance!!*100) }

                24 -> {//쾌적 X 도로 :  배점 ) 50
                    routeInfor.roadTypeD = routeInfor.roadTypeD?.plus(distance!!)
                    routeInfor.roadScore_draft = routeInfor.roadScore_draft?.plus(distance!!*50) }

                else -> {}
            }
        }

        //구간 시설물 정보 (Danger)
        if (facilityType != null) {
            when (facilityType) {

                //Danger A : 보행자도로 / 차도  분리
                12 ->{ routeInfor.overPasses = routeInfor.overPasses?.plus(1)        //육교
                    routeInfor.DangerScore_draft = routeInfor.DangerScore_draft?.plus(pf.algorithmWeight_facilityNoCar!!).toInt()}
                14 ->{ routeInfor.underPasses = routeInfor.underPasses?.plus(1)     //지하보도
                    routeInfor.DangerScore_draft = routeInfor.DangerScore_draft?.plus(pf.algorithmWeight_facilityNoCar!!).toInt()}
                17 ->{ routeInfor.stairs  = routeInfor.stairs?.plus(1)              //계단
                    routeInfor.DangerScore_draft = routeInfor.DangerScore_draft?.plus(pf.algorithmWeight_facilityNoCar!!).toInt()}

                //Danger B : 보행자도로 / 차도  분리 X
                1 ->{ routeInfor.bridge  = routeInfor.bridge?.plus(1)            //교량
                    routeInfor.DangerScore_draft = routeInfor.DangerScore_draft?.plus(pf.algorithmWeight_facilityCar!!).toInt()}
                2 ->{ routeInfor.turnnels  = routeInfor.turnnels?.plus(1)       //터널
                    routeInfor.DangerScore_draft = routeInfor.DangerScore_draft?.plus(pf.algorithmWeight_facilityCar!!).toInt()}
                3 ->{ routeInfor.highroad  = routeInfor.highroad?.plus(1)       ///고가도로
                    routeInfor.DangerScore_draft = routeInfor.DangerScore_draft?.plus(pf.algorithmWeight_facilityCar!!).toInt()}
                16 ->{ routeInfor.largeFacilitypassage  = routeInfor.largeFacilitypassage?.plus(1)      //대형 시설물 이동 통로
                    routeInfor.DangerScore_draft = routeInfor.DangerScore_draft?.plus(pf.algorithmWeight_facilityCar!!).toInt()}

                //횡단보도
                15 ->{ routeInfor.crossWalk = routeInfor.crossWalk?.plus(1)
                    routeInfor.DangerScore_draft = routeInfor.DangerScore_draft?.plus(pf.algorithmWeight_crossWalk!!).toInt()}     //횡단보도
            }
        }

        //회전정보
        if (turnType != null) {
            when (turnType) {
                in 1..7, 11 -> {        }//안내없음 또는 직진

                //분기점
                in 12..19 -> { routeInfor.turnPoint = routeInfor.turnPoint?.plus(1)
                    routeInfor.DangerScore_draft = routeInfor.DangerScore_draft?.plus(pf.algorithmWeight_turnPoint!!).toInt()
                }

                //횡단보도
                in 211..217 ->{ routeInfor.crossWalk = routeInfor.crossWalk?.plus(1)
                    routeInfor.DangerScore_draft = routeInfor.DangerScore_draft?.plus(pf.algorithmWeight_crossWalk!!).toInt()}

                //Danger A : 보행자도로 / 차도  분리
                218 -> { routeInfor.elevator = routeInfor.elevator?.plus(1)          //엘리베이터
                    routeInfor.DangerScore_draft = routeInfor.DangerScore_draft?.plus(pf.algorithmWeight_facilityNoCar!!).toInt()}                   //엘리베이터
                125 ->{ routeInfor.overPasses = routeInfor.overPasses?.plus(1)      //육교
                    routeInfor.DangerScore_draft = routeInfor.DangerScore_draft?.plus(pf.algorithmWeight_facilityNoCar!!).toInt()}                //육교
                126 ->{ routeInfor.underPasses = routeInfor.underPasses?.plus(1)    //지하보도
                    routeInfor.DangerScore_draft = routeInfor.DangerScore_draft?.plus(pf.algorithmWeight_facilityNoCar!!).toInt()}              //지하보도
                127 ->{ routeInfor.stairs  = routeInfor.stairs?.plus(1)             //계단
                    routeInfor.DangerScore_draft = routeInfor.DangerScore_draft?.plus(pf.algorithmWeight_facilityNoCar!!).toInt()}                       //계단 진입
              //128 ->{ //경사로진입}
                129 ->{ routeInfor.stairs  = routeInfor.stairs?.plus(1)             //계단+경사로
                    routeInfor.DangerScore_draft = routeInfor.DangerScore_draft?.plus(pf.algorithmWeight_facilityNoCar!!).toInt()}                       //계단 + 경사로 진입

                else -> {}
            }
        }
    }       //End Of gatherRouteInfor()

    //2. 위험요소 초안을 ㅇ아서 정규화 하는 함수
    fun nomalizeScore(routeList: ArrayList<RouteInfor?>) {
        //input : RouteInfor 4개 배열
        //return값 : 없음
        //output: 인자로 들어온 데이터클래스에 0~50값으로 정규화 된 DangerScore 4개를 넣는다

        //1.RoadScore Nomalize
        //경로별 도로점수를 각 총거리로 나눠준다
        Log.d("SafeRoute-nomalizeScore()","RoadScore 평준화 시작")
        Log.d("SafeRoute-nomalizeScore()","roadScoreFinal = roadScoreDraft / totalDistance")
        routeList.forEach{
            if (it != null) {
                it.roadScore_final = it.roadScore_draft / it.totalDistance
                Log.d("SafeRoute-nomalizeScore()","${it.roadScore_final} = ${it.roadScore_draft} / ${it.totalDistance}")
            }
            else {
                Log.d("SafeRoute-nomalizeScore()","RouteInfor is null :: ERROR")
            }
        }
        Log.d("SafeRoute-nomalizeScore()","RoadScore 평준화 완료")

        //2.DangerScore Nomalize
        //각 경로의 위험점수 : 총 4가지를 모아서 1~50 사이로 정규화
        Log.d("SafeRoute-nomalizeScore()","DangerScore 평준화 시작")
        if ( (routeList[0]?.searchOption==routeList[1]?.searchOption) ||
            (routeList[0]?.searchOption==routeList[2]?.searchOption) ||
            (routeList[0]?.searchOption==routeList[3]?.searchOption) ){
            routeList.forEach { it?.DangerScore_final = -25
            Log.d("SafeRoute-namalizeScore() - ","한가지 루트만 존재함 : 평준화결과 :  ${it?.DangerScore_final}")}
        }else{
            Log.d("SafeRoute-nomalizeScore() -","서로 다른 루트 입니다. 평준화 시작")
            //1.최대/최소 구하기
            var min = routeList[0]!!.DangerScore_draft
            var max = routeList[0]!!.DangerScore_draft
            var minIndex = 0
            var maxIndex = 0
            for (i in 1..3){
                if (routeList[i]?.DangerScore_draft!! > max!! ){
                    max = routeList[i]!!.DangerScore_draft
                    maxIndex = i
                }
                if (routeList[i]?.DangerScore_draft!! < min!! ){
                    min = routeList[i]!!.DangerScore_draft
                    minIndex = i
                }
            }
            Log.d("SafeRoute-nomalizeScore() -","min(Index) : ${min}(${minIndex}) / max(Index) : ${max}(${maxIndex}) ")
            Log.d("SafeRoute-nomalizeScore() -","X' = (X-min) / (max-min) * (-50)")

            //2. 평준화 : X-min/max-min * (-50)
            routeList.forEach{
                it?.DangerScore_final = (it?.DangerScore_draft!!- min)/(max-min) * -50
                Log.d("SafeRoute-nomalizeScore() -","${it?.DangerScore_final} = (${it?.DangerScore_draft}- $min)/($max-$min) * -50")
            }
        }
        Log.d("SafeRoute-nomalizeScore() -","DangerScore 평준화 완료")
    }//End of nomalizeDangerScore()

    fun makeFinalScore(routeList: ArrayList<RouteInfor?>) {
        routeList.forEach{
            if (it!= null){
                it.routeScore = it.roadScore_final+it.DangerScore_final
                Log.d("SafeRoute-makeFinalScore() - ","(RoadScore:)${it.roadScore_final} + (DanferScore:)${it.DangerScore_final} = ${it.routeScore}")
            }
        }
    }//End of makeFinalScore()

    fun makeRouteInfor_forPublish(routeList: ArrayList<RouteInfor?>) {
        var inforBuilder = StringBuilder()
        routeList.forEach{
            if (it!= null){
                inforBuilder.append(it.routeScore)
                inforBuilder.append(",")
                inforBuilder.append(it.roadScore_final)
                inforBuilder.append(",")
                inforBuilder.append(it.DangerScore_final)
                inforBuilder.append(",")
                inforBuilder.append(it.crossWalk)
                inforBuilder.append(",")
                inforBuilder.append(it.turnPoint)
                //inforBuilder.append(",")
                //inforBuilder.append(it.totalDistance)
                it.routeInforStringData = inforBuilder.toString()
                Log.d("SafeRoute-makeRouteInfor_forPublish() : ","${it.routeInforStringData}")
            }
        }

    }//End of makeRouteInfor_forPublish()


}










