package route
//fun calcPartialScore(facilityType: Int?, distance: Int?=0, roadType: Int?, turnType: Int?, saftyScore: SaftyScore)
//를 기능을 분리할거임
//1. 배열에다가 모든 루트의 정보를 담기만 하는 함수 ( 점수매기기 X )
//2. 위험요소 초안을 받아서 정규화 하는 함수
//3. 1,2를 바탕으로 최종 점수를 계산하는 함수

import android.preference.PreferenceGroup
import android.util.Log
import model.Preference
import model.RouteInfor
import model.otherRouteInfor


object SafeRoute {//End of object SafeRoute
    //End Of object SafeRoute
   // var tw = Preference.tableWeight
    var pf = Preference

    var Wr = pf.tableWeight_road!!
    var Wd = pf.tableWeight_danger!!



    //1. 배열에다가 모든 루트의 정보를 담기만 하는 함수 ( 점수매기기 X )
    fun gatherRouteInfor(
        facilityType: Int?,
        distance: Double?,
        roadType: Int?,
        turnType: Int?,
        routeInfor : RouteInfor
    ){
        //그냥 totaldDistance를 여기서 누적계산할까..
        //도로타입정보(RoadType)
        if (roadType != null && distance != null) {
            when (roadType) {
                21 -> {//차도와 인도가 분리, 정해진 횡단구역으로만 횡단 가능 : 배점 ) 90
                    routeInfor.roadTypeB += distance
                    routeInfor.roadScore_draft += (distance * 90) }

                22 -> {//차도 인도 분리 X || 보행자 횡단에 제약 X 보행자도로 : 배점 ) 50
                    routeInfor.roadTypeC += distance
                    routeInfor.roadScore_draft += (distance * 50)  }

                23 -> {//차량 통행 불가 보행자 도로 : 배점) 100
                    routeInfor.roadTypeA += distance
                    routeInfor.roadScore_draft += (distance * 100)  }

                24 -> {//쾌적 X 도로 :  배점 ) 50
                    routeInfor.roadTypeD += distance
                    routeInfor.roadScore_draft += (distance * 0)
                    routeInfor.roadScore_draft += (distance*0) }

                else -> {}
            }
        }

        //구간 시설물 정보 (Danger)
        if (facilityType != null) {
            when (facilityType) {

                //Danger C : 보행자도로 / 차도  분리
                12 ->{ //routeInfor.overPasses = routeInfor.overPasses?.plus(1)        //육교. 순서2번
                    //routeInfor.DangerScore_draft = routeInfor.DangerScore_draft?.plus(pf.algorithmWeight_facilityNoCar!!).toInt()
                  //  routeInfor.hasDanger = true
                   // routeInfor.hasDangerA?:0+10
                    routeInfor.overPasses += 1
                    routeInfor.DangerScore_draft += (pf.algorithmWeight_facilityNoCar!!)
                    routeInfor.DangerCount++}
                14 ->{ //routeInfor.underPasses = routeInfor.underPasses?.plus(1)     //지하보도. 순서3번
                  //  routeInfor.DangerScore_draft = routeInfor.DangerScore_draft?.plus(pf.algorithmWeight_facilityNoCar!!).toInt()
                  //  routeInfor.hasDanger = true
                  //  routeInfor.hasDangerA?:0+100
                    routeInfor.underPasses += 1
                    routeInfor.DangerScore_draft += (pf.algorithmWeight_facilityNoCar!!)
                    routeInfor.DangerCount++}
                17 ->{ //routeInfor.stairs  = routeInfor.stairs?.plus(1)              //계단
                  //  routeInfor.DangerScore_draft = routeInfor.DangerScore_draft?.plus(pf.algorithmWeight_facilityNoCar!!).toInt()
                    routeInfor.overPasses += 1
                    routeInfor.DangerScore_draft += (pf.algorithmWeight_facilityNoCar!!)
                    //     routeInfor.hasDanger = true
                //    routeInfor.hasDangerA?:0+1000
                    routeInfor.DangerCount++}

                //Danger D : 보행자도로 / 차도  분리 X
                1 ->{ //routeInfor.bridge  = routeInfor.bridge?.plus(1)            //교량. 순서1
                  //  routeInfor.DangerScore_draft = routeInfor.DangerScore_draft?.plus(pf.algorithmWeight_facilityCar!!).toInt()
               //     routeInfor.hasDanger = true
                //    routeInfor.hasDangerB?:0+1
                    routeInfor.bridge += 1
                    routeInfor.DangerScore_draft += (pf.algorithmWeight_facilityCar!!)
                    routeInfor.DangerCount++}
                2 ->{ //routeInfor.turnnels  = routeInfor.turnnels?.plus(1)       //터널. 순서2
                 //   routeInfor.DangerScore_draft = routeInfor.DangerScore_draft?.plus(pf.algorithmWeight_facilityCar!!).toInt()
                    //    routeInfor.hasDanger = true
                 //   routeInfor.hasDangerB?:0+10
                    routeInfor.turnnels += 1
                    routeInfor.DangerScore_draft += (pf.algorithmWeight_facilityCar!!)
                    routeInfor.DangerCount++}
                3 ->{ //routeInfor.highroad  = routeInfor.highroad?.plus(1)       ///고가도로. 순서3
                   // routeInfor.DangerScore_draft = routeInfor.DangerScore_draft?.plus(pf.algorithmWeight_facilityCar!!).toInt()
                //    routeInfor.hasDanger = true
                //    routeInfor.hasDangerB?:0+100
                    routeInfor.highroad += 1
                    routeInfor.DangerScore_draft += (pf.algorithmWeight_facilityCar!!)
                    routeInfor.DangerCount++}
                16 ->{ //routeInfor.largeFacilitypassage  = routeInfor.largeFacilitypassage?.plus(1)      //대형 시설물 이동 통로. 순서4
                 //   routeInfor.DangerScore_draft = routeInfor.DangerScore_draft?.plus(pf.algorithmWeight_facilityCar!!).toInt()
                //    routeInfor.hasDanger = true
                //    routeInfor.hasDangerB?:0+1000
                    routeInfor.largeFacilitypassage += 1
                    routeInfor.DangerScore_draft += (pf.algorithmWeight_facilityCar!!)
                    routeInfor.DangerCount++}

                //횡단보도. DangerA -순서1
                15 ->{ //routeInfor.crossWalk = routeInfor.crossWalk?.plus(1)
                  //  routeInfor.DangerScore_draft = routeInfor.DangerScore_draft?.plus(pf.algorithmWeight_crossWalk!!).toInt()
                //    routeInfor.hasDanger = true
                 //   routeInfor.hasCrossWalk.plus(1)
                    routeInfor.crossWalk += 1
                    routeInfor.DangerScore_draft += (pf.algorithmWeight_crossWalk!!)
                    routeInfor.DangerCount++}     //횡단보도
            }
        }

        //회전정보
        if (turnType != null) {
            when (turnType) {
                in 1..7, 11 -> {
                    routeInfor.DangerCount++
                    //DangerScore에 어짜피 0더하니까 안더해도 됨.
                }//안내없음 또는 직진

                //분기점
                in 12..19 -> {
                    routeInfor.DangerCount++
                    routeInfor.turnPoint += 1
                    routeInfor.DangerScore_draft += (pf.algorithmWeight_turnPoint!!)
                   // routeInfor.turnPoint = routeInfor.turnPoint?.plus(1)
                  //  routeInfor.DangerScore_draft = routeInfor.DangerScore_draft?.plus(pf.algorithmWeight_turnPoint!!).toInt()
                }

                //횡단보도
                in 211..217 -> {
                    routeInfor.DangerCount++
                    routeInfor.crossWalk += 1
                    routeInfor.DangerScore_draft += (pf.algorithmWeight_crossWalk!!)
                    //routeInfor.crossWalk = routeInfor.crossWalk?.plus(1)
                    // routeInfor.DangerScore_draft = routeInfor.DangerScore_draft?.plus(pf.algorithmWeight_crossWalk!!).toInt()}
                }
                //Danger A : 보행자도로 / 차도  분리
                218 -> {
                    routeInfor.DangerCount++
                    routeInfor.elevator += 1
                    routeInfor.DangerScore_draft += (pf.algorithmWeight_facilityNoCar!!)
                    //routeInfor.elevator = routeInfor.elevator?.plus(1)          //엘리베이터
                    //routeInfor.DangerScore_draft = routeInfor.DangerScore_draft?.plus(pf.algorithmWeight_facilityNoCar!!).toInt()}                   //엘리베이터
                }
                125 ->{//육교
                    routeInfor.DangerCount++
                    routeInfor.overPasses += 1
                    routeInfor.DangerScore_draft += (pf.algorithmWeight_facilityNoCar!!) }

                126 ->{ //지하보도
                    routeInfor.DangerCount++
                    routeInfor.underPasses += 1
                    routeInfor.DangerScore_draft += (pf.algorithmWeight_facilityNoCar!!)}
                127 ->{//계단 진입
                    routeInfor.DangerCount++
                    routeInfor.stairs += 1
                    routeInfor.DangerScore_draft += (pf.algorithmWeight_facilityNoCar!!)}
              //128 ->{ //경사로진입}
                129 ->{//계단+경사로
                    routeInfor.DangerCount
                    routeInfor.stairs += 1
                    routeInfor.DangerScore_draft += (pf.algorithmWeight_facilityNoCar!!)}                       //계단 + 경사로 진입

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

        //DangerScore Nomalize :: 업데이트 버전
        routeList.forEach{
            if(it!= null) {
                it.DangerScore_final = (it.DangerScore_draft / it.DangerCount)
                Log.d("SafeRoute-nomalizeScore() -", "${it.DangerScore_final} = (${it.DangerScore_draft} / ${it.DangerCount})")
            }
            else {
                Log.d("SafeRoute-nomalizeScore()","RouteInfor is null :: ERROR")
            }
        }


        //2.DangerScore Nomalize
        //각 경로의 위험점수 : 총 4가지를 모아서 1~50 사이로 정규화
        /*Log.d("SafeRoute-nomalizeScore()","DangerScore 평준화 시작")
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
        Log.d("SafeRoute-nomalizeScore() -","DangerScore 평준화 완료")*/
    }//End of nomalizeDangerScore()

    fun makeFinalScore(routeList: ArrayList<RouteInfor?>) {
        routeList.forEach{
            if (it!= null){
                it.routeScore = (Wr*it.roadScore_final+ Wd*it.DangerScore_final)
                Log.d("SafeRoute-makeFinalScore() - ","(RoadScore:) ${it.roadScore_final} *$Wr +  (DanferScore:)${it.DangerScore_final} * $Wd = ${it.routeScore}")
            }
        }
    }//End of makeFinalScore()

    fun makeStringData_forPublish(routeList: ArrayList<RouteInfor?>) {
        routeList.forEach {
            if (it!= null){
                var stringDataBuilder = StringBuilder()
                if (it.turnPoint > 0)
                    stringDataBuilder.append("분기점(${it.turnPoint}회)")
                if (it.crossWalk >0)
                    stringDataBuilder.append(", 횡단보도(${it.crossWalk}회")
                if (it.elevator >0)
                    stringDataBuilder.append(", 엘리베이터(${it.elevator}회")
                if (it.overPasses >0)
                    stringDataBuilder.append(", 육교(${it.overPasses}회")
                if (it.underPasses >0)
                    stringDataBuilder.append(", 지하도로(${it.underPasses}회")
                if (it.stairs >0)
                    stringDataBuilder.append(", 계단(${it.stairs}회")
                if (it.bridge >0)
                    stringDataBuilder.append(", 교량(${it.bridge}회")
                if (it.turnnels >0)
                    stringDataBuilder.append(", 터널(${it.turnnels}회")
                if (it.highroad >0)
                    stringDataBuilder.append(", 고가도로(${it.highroad}회")
                if (it.largeFacilitypassage >0)
                    stringDataBuilder.append(", 대형시설물이동통로(${it.largeFacilitypassage}회")
                stringDataBuilder.append(" 가 포함된 경로입니다.")

                it.stringData = stringDataBuilder.toString()
                Log.d("SafeRoute-makeStringData() : ","${it.stringData}")
            }
        }
    }//End of makeStringData_forPublish()

    fun makeScoreInfor_forPublish(routeList: ArrayList<RouteInfor?>) {
        routeList.forEach{
            if (it!= null){
                var inforBuilder = StringBuilder()
                inforBuilder.append(it.roadScore_final)
                inforBuilder.append(",")
                inforBuilder.append(it.DangerScore_final)
                inforBuilder.append(",")
                inforBuilder.append(it.routeScore)
                it.scoreData = inforBuilder.toString()
                Log.d("SafeRoute-makeScoreInfor_forPublish() : ","${it.scoreData}")
            }
        }
    }//End of makeScoreInfor_forPublish()

    /*
    fun makeEctInfor_forPublish(routeList: ArrayList<RouteInfor?>){
        routeList.forEach{
            if (it!=null){
                var noEct: Boolean = true
                var ectBuilder = StringBuilder()
                if (it.elevator > 0) {
                    ectBuilder.append("엘리베이터 ${it.elevator}개 ")
                    noEct = false
                }
                if (it.overPasses > 0) {
//                    if (ectBuilder.length != 0)
//                        ectBuilder.append(",")
                    ectBuilder.append("육교 ${it.overPasses}개 ")
                    noEct = false
                }
                if (it.underPasses > 0) {
//                    if (ectBuilder.length != 0)
//                        ectBuilder.append(",")
                    ectBuilder.append("지하보도 ${it.underPasses}개 ")
                    noEct = false
                }
                if (it.stairs > 0) {
//                    if (ectBuilder.length != 0)
//                        ectBuilder.append(",")
                    ectBuilder.append("계단 ${it.stairs}개 ")
                    noEct = false
                }
                if (it.bridge > 0) {
//                    if (ectBuilder.length != 0)
//                        ectBuilder.append(",")
                    ectBuilder.append("교량 ${it.bridge}개 ")
                    noEct = false
                }
                if (it.turnnels > 0) {
//                    if (ectBuilder.length != 0)
//                        ectBuilder.append(",")
                    ectBuilder.append("터널 ${it.turnnels}개 ")
                    noEct = false
                }
                if (it.highroad > 0) {
//                    if (ectBuilder.length != 0)
//                        ectBuilder.append(",")
                    ectBuilder.append("고가도로 ${it.highroad}개 ")
                    noEct = false
                }
                if (it.largeFacilitypassage > 0) {
//                    if (ectBuilder.length != 0)
//                        ectBuilder.append(",")
                    ectBuilder.append("대형 시설물 이동 통로 ${it.largeFacilitypassage}개 ")
                    noEct = false
                }
                if (noEct){
                    ectBuilder.append("특이사항 없음")
                }
                else {
                    ectBuilder.append("포함된 경로입니다.")
                }
                it.ectInforStringData = ectBuilder.toString()
                Log.d("SafeRoute-makeEctInfor_forPublish() : ","${it.ectInforStringData}")
            }
        }
    }//End of makeEctInfor_forPublish()

*/

}










