package route
//fun calcPartialScore(facilityType: Int?, distance: Int?=0, roadType: Int?, turnType: Int?, saftyScore: SaftyScore)
//를 기능을 분리할거임
//1. 배열에다가 모든 루트의 정보를 담기만 하는 함수 ( 점수매기기 X )
//2. 위험요소 초안을 받아서 정규화 하는 함수
//3. 1,2를 바탕으로 최종 점수를 계산하는 함수

import android.util.Log
import com.example.watchout.MainActivity
import model.Preference
import model.RouteInfor
import model.SaftyScore
import utils.Constant
import utils.Constant.API.LOG
import utils.Constant.API.SAFEROUTE



object SafeRoute {           //End Of object SafeRoute
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
                21 -> {//차도와 인도가 분리, 정해진 횡단구역으로만 횡단 가능
                    routeInfor.roadTypeB = routeInfor.roadTypeB?.plus(distance!!) }

                22 -> {//차도 인도 분리 X || 보행자 횡단에 제약 X 보행자도로
                    routeInfor.roadTypeC = routeInfor.roadTypeC?.plus(distance!!) }

                23 -> {//차량 통행 불가 보행자 도로
                    routeInfor.roadTypeA = routeInfor.roadTypeA.plus(distance!!) }

                24 -> {//쾌적 X 도로
                    routeInfor.roadTypeD = routeInfor.roadTypeD?.plus(distance!!) }

                else -> {}
            }
        }

        //구간 시설물 정보 (Danger)
        if (facilityType != null) {
            when (facilityType) {

                //Danger A : 보행자도로 / 차도  분리
                12 ->{ routeInfor.overPasses = routeInfor.overPasses?.plus(1) }         //육교
                14 ->{ routeInfor.underPasses = routeInfor.underPasses?.plus(1) }       //지하보도
                17 ->{ routeInfor.stairs  = routeInfor.stairs?.plus(1) }                //계단

                //Danger B : 보행자도로 / 차도  분리 X
                1 ->{ routeInfor.bridge  = routeInfor.bridge?.plus(1) }  //교량
                2 ->{ routeInfor.turnnels  = routeInfor.turnnels?.plus(1) }  //터널
                3 ->{ routeInfor.highroad  = routeInfor.highroad?.plus(1) }  //고가도로
                16 ->{ routeInfor.largeFacilitypassage  = routeInfor.largeFacilitypassage?.plus(1) }//대형시설물이동통로

                //횡단보도
                15 ->{ routeInfor.crossWalk = routeInfor.crossWalk?.plus(1)}     //횡단보도
            }
        }

        //회전정보
        if (turnType != null) {
            when (turnType) {
                in 1..7, 11 -> {        }//안내없음 또는 직진

                //분기점
                in 12..19 -> { routeInfor.turnPoint = routeInfor.turnPoint?.plus(1) }           //분기점

                //횡단보도
                in 211..217 ->{ routeInfor.crossWalk = routeInfor.crossWalk?.plus(1)}

                //Danger A : 보행자도로 / 차도  분리
                218 -> { routeInfor.elevator = routeInfor.elevator?.plus(1) }                   //엘리베이터
                125 ->{ routeInfor.overPasses = routeInfor.overPasses?.plus(1) }                //육교
                126 ->{ routeInfor.underPasses = routeInfor.underPasses?.plus(1) }              //지하보도
                127 ->{ routeInfor.stairs  = routeInfor.stairs?.plus(1) }                       //계단 진입
              //128 ->{ //경사로진입}
                129 ->{ routeInfor.stairs  = routeInfor.stairs?.plus(1) }                       //계단 + 경사로 진입

                else -> {}
            }
        }
    }       //End Of gatherRouteInfor()

    //2. 위험요소 초안을 ㅇ아서 정규화 하는 함수
    fun nomalizeDangerScore(routeList: ArrayList<RouteInfor?>) {
        //input : RouteInfor 4개 배열
        //return값 : 없음
        //output: 인자로 들어온 데이터클래스에 0~50값으로 정규화 된 DangerScore 4개를 넣는다

    }//End of nomalizeDangerScore()


}










