package route

import android.util.Log
import com.example.watchout.MainActivity
import model.Preference
import model.SaftyScore
import utils.Constant
import utils.Constant.API.LOG
import utils.Constant.API.SAFEROUTE


//roadtype+facilityType이랑 turntype중 하나만 옴
object SafeRoute {
    var tw = Preference.tableWeight
    var pf = Preference


    fun calcPartialScore(
        facilityType: Int?,
        distance: Int?=0,
        roadType: Int?,
        turnType: Int?,
        saftyScore: SaftyScore
    ){
        //여기서는 미리 업데이트 해놓은 값의 preference 쓸 수 있음
        if (turnType != null) {
            //회전정보
            when (turnType) {
                in 1..7, 11 -> {        //안내없음 또는 직진
                  //  saftyScore.score = saftyScore.score?.plus(0)
                }

                in 12..19 -> {      //분기점
                    saftyScore.turnPoint = saftyScore.turnPoint?.plus(1)
                    saftyScore.score = saftyScore.score?.plus(tw!! * pf.awturnPoint!!)
                }
                218 -> {            //엘리베이터. 고정가중치(-20)
                    saftyScore.elevator = saftyScore.elevator?.plus(1)
                    saftyScore.score = saftyScore.score?.plus(-20*tw!!)
                }
/*                in 125..129 -> {
                    //육교, 지하보도, 계단진입, 경사로진입, +
                    //facility에서 계산
                }*/
                125->{
                     //육교
                    saftyScore.overPasses = saftyScore.overPasses?.plus(1)
                    saftyScore.score = saftyScore.score?.plus(tw!! * pf.awft_noCar!!)
                 }
                 126 ->{ //지하보도
                     saftyScore.underPasses = saftyScore.underPasses?.plus(1)
                     saftyScore.score = saftyScore.score?.plus(tw!! * pf.awft_noCar!!)
                 }
                 127 ->{ //계단진입
                     saftyScore.stairs = saftyScore.stairs?.plus(1)
                     saftyScore.score = saftyScore.score?.plus(tw!! * pf.awft_noCar!!)
                 }
/*                 128 ->{ //경사로진입
                     Log.d(SAFEROUTE, "0/TT/facility관련" );
                     Log.d(SAFEROUTE, "추가점수"+"${score}" );
                 }*/
                 129 ->{ //계단 + 경사로 진입
                     saftyScore.stairs = saftyScore.stairs?.plus(1)
                     saftyScore.score = saftyScore.score?.plus(tw!! * pf.awft_noCar!!)
                 }
                in 211..217 -> {
                    //횡단보도, facility 에서 계산
            //        Log.d(SAFEROUTE, "0/TT/횡단보도" );
            //        Log.d(SAFEROUTE, "추가점수"+"${score}" );
                    saftyScore.crossWalkLength = saftyScore.crossWalkLength?.plus(distance!!)
                    saftyScore.crossWalkCount = saftyScore.crossWalkCount?.plus(1)
                    saftyScore.score = saftyScore.score?.plus(tw!! * distance!! * pf.awcrossWalk!!)
                }
                else -> {

                }
            }
        }

        if (facilityType != null) {
            //구간 시설물 정보
            when (facilityType) {

                //차와 함께가는 시설물
                1->{    //교량
                    saftyScore.bridge = saftyScore.bridge?.plus(1)
                    saftyScore.score = saftyScore.score?.plus(tw!! * pf.awft_car!!)
                }
                2->{        //터널
                    saftyScore.turnnels = saftyScore.turnnels?.plus(1)
                    saftyScore.score = saftyScore.score?.plus(tw!! * pf.awft_car!!)
                }
                3->{        //고가도로
                    saftyScore.highroad = saftyScore.highroad?.plus(1)
                    saftyScore.score = saftyScore.score?.plus(tw!! * pf.awft_car!!)
                }


                //차와 분리된 시설물
                12->{           //육교
                    saftyScore.overPasses = saftyScore.overPasses?.plus(1)
                    saftyScore.score = saftyScore.score?.plus(tw!! * pf.awft_noCar!!)
                }
                14 ->{          //지하보도
                    saftyScore.underPasses = saftyScore.underPasses?.plus(1)
                    saftyScore.score = saftyScore.score?.plus(tw!! * pf.awft_noCar!!)

                }
                17->{           //계단
                    saftyScore.stairs = saftyScore.stairs?.plus(1)
                    saftyScore.score = saftyScore.score?.plus(tw!! * pf.awft_noCar!!)
                }


                //완전위험
                16 -> {     //대형시설물이동통로: 고정값
                    saftyScore.largeFacilitypassage = saftyScore.largeFacilitypassage?.plus(1)
                    saftyScore.score = saftyScore.score?.plus(tw!! *-80)

                }

                15 ->{      //횡단보도 : 거리비례
                    saftyScore.crossWalkLength = saftyScore.crossWalkLength?.plus(distance!!)
                    saftyScore.crossWalkCount = saftyScore.crossWalkCount?.plus(1)
                    saftyScore.score = saftyScore.score?.plus(tw!! * distance!! * pf.awcrossWalk!!)
                }
            }
        }


        if (roadType != null) {
            //도로타입정보
            when (roadType) {
                21 -> {//차도와 인도가 분리, 정해진 횡단구역으로만 횡단 가능
                    saftyScore.roadTypeLength1 = saftyScore.roadTypeLength1?.plus(distance!!)
                    saftyScore.score = saftyScore.score?.plus(distance!!*.3)
                }

                22 -> {//차도 인도 분리 X || 보행자 횡단에 제약 X 보행자도로
                    saftyScore.roadTypeLength2 = saftyScore.roadTypeLength2?.plus(distance!!)
                    saftyScore.score = saftyScore.score?.plus(distance!!*.1)
                }

                23 -> {//차량 통행 불가 보행자 도로
                    saftyScore.roadTypeLength2 = saftyScore.roadTypeLength2?.plus(distance!!)
                    saftyScore.score = saftyScore.score?.plus(distance!!*.5)
                }

                24 -> {//쾌적 X 도로
                    saftyScore.roadTypeLength2 = saftyScore.roadTypeLength2?.plus(distance!!)
                    saftyScore.score = saftyScore.score?.plus(distance!!*.1)
                }

                else -> {

                }
            }

        }

    }
}








