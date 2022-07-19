package route

import android.util.Log
import model.SaftyScore
import utils.Constant
import utils.Constant.API.LOG
import utils.Constant.API.SAFEROUTE


//roadtype+facilityType이랑 turntype중 하나만 옴
object SafeRoute {
    //infix fun Int.fdiv(i: Int): Double = this / i.toDouble();
    fun calcPartialScore(
        facilityType: Int?,
        distance: Int?,
        roadType: Int?,
        turnType: Int?,
        saftyScore: SaftyScore
    ){

        var score: Double = 0.0
        var dist = distance?: 0


        // case: Point

        if (turnType != null) {
            //회전정보
            when (turnType) {
                in 1..7, 11 -> {
                    score += 10
                    saftyScore.score = saftyScore.score?.plus(10)
                //    Log.d(SAFEROUTE, "+10/TT/안내없음또는 직진" );
                //   Log.d(SAFEROUTE, "추가점수"+"${score}" );
                    saftyScore.score = saftyScore.score?.plus(score)
                }

                //안내없음, 직진
                in 12..19 -> {
                    score -= 10
                    saftyScore.turnPoint = saftyScore.turnPoint?.plus(1)
                    saftyScore.score = saftyScore.score?.plus(score)
                    Log.d(SAFEROUTE, "-10/TT/분기점" );
                //   Log.d(SAFEROUTE, "추가점수"+"${score}" );
                }
                218 -> {
                    score -= 10
                    saftyScore.elevator = saftyScore.elevator?.plus(1)
                    saftyScore.score = saftyScore.score?.plus(score)
                //    Log.d(SAFEROUTE, "-10/TT/엘리베이터" );
                //    Log.d(SAFEROUTE, "추가점수"+"${score}" );
                }

                //분기점, 엘리베이터
                in 125..129 -> {
                    //육교, 지하보도, 계단진입, 경사로진입, +
                    //facility에서 계산
                //    Log.d(SAFEROUTE, "0/TT/facility관련" );
                //    Log.d(SAFEROUTE, "추가점수"+"${score}" );
                }
                /* 125->{
                     //육교
                     Log.d(SAFEROUTE, "0/TT/facility관련" );
                     Log.d(SAFEROUTE, "추가점수"+"${score}" );
                 }
                 126 ->{ //지하보도
                     Log.d(SAFEROUTE, "0/TT/facility관련" );
                     Log.d(SAFEROUTE, "추가점수"+"${score}" );
                 }
                 127 ->{ //계단진입
                     Log.d(SAFEROUTE, "0/TT/facility관련" );
                     Log.d(SAFEROUTE, "추가점수"+"${score}" );
                 }
                 128 ->{ //경사로진입
                     Log.d(SAFEROUTE, "0/TT/facility관련" );
                     Log.d(SAFEROUTE, "추가점수"+"${score}" );
                 }
                 129 ->{ //계단 + 경사로 진입
                     Log.d(SAFEROUTE, "0/TT/facility관련" );
                     Log.d(SAFEROUTE, "추가점수"+"${score}" );
                 }*/
                in 211..217 -> {
                    //횡단보도, facility 에서 계산
            //        Log.d(SAFEROUTE, "0/TT/횡단보도" );
            //        Log.d(SAFEROUTE, "추가점수"+"${score}" );
                }
                else -> {

                }
            }
        }

        if (facilityType != null) {

            //구간 시설물 정보
            when (facilityType) {

                1->{
                    score -= 30
                //    Log.d(SAFEROUTE, "-30/FT/교량");
                //    Log.d(SAFEROUTE, "추가점수"+"${score}" );
                    saftyScore.bridge = saftyScore.bridge?.plus(1)
                    saftyScore.score = saftyScore.score?.plus(score)
                }
                2->{
                    score -= 30
                //    Log.d(SAFEROUTE, "-30/FT/터널");
                //    Log.d(SAFEROUTE, "추가점수"+"${score}" );
                    saftyScore.turnnels = saftyScore.turnnels?.plus(1)
                    saftyScore.score = saftyScore.score?.plus(score)
                }
                3->{
                    score -= 30
                //    Log.d(SAFEROUTE, "-30/FT/고가도로");
                //    Log.d(SAFEROUTE, "추가점수"+"${score}" );
                    saftyScore.highroad = saftyScore.highroad?.plus(1)
                    saftyScore.score = saftyScore.score?.plus(score)
                }



                12->{
                    score -= 20
                //    Log.d(SAFEROUTE, "-20/FT/육교");
                //    Log.d(SAFEROUTE, "추가점수"+"${score}" );
                    saftyScore.overPasses = saftyScore.overPasses?.plus(1)
                    saftyScore.score = saftyScore.score?.plus(score)
                }
                14 ->{
                    score -= 20
                 //   Log.d(SAFEROUTE, "-20/FT/지하보도");
                //    Log.d(SAFEROUTE, "추가점수"+"${score}" );
                    saftyScore.underPasses = saftyScore.underPasses?.plus(1)
                    saftyScore.score = saftyScore.score?.plus(score)
                }
                17->{
                    score -= 20
                //    Log.d(SAFEROUTE, "-20/FT/계단");
                //    Log.d(SAFEROUTE, "추가점수"+"${score}" );
                    saftyScore.stairs = saftyScore.stairs?.plus(1)
                    saftyScore.score = saftyScore.score?.plus(score)
                }


                16 -> {
                    score -= 100
                //    Log.d(SAFEROUTE, "-100/FT/대형시설물이동통로");
                //    Log.d(SAFEROUTE, "추가점수"+"${score}" );
                    saftyScore.largeFacilitypassage = saftyScore.largeFacilitypassage?.plus(1)
                    saftyScore.score = saftyScore.score?.plus(score)
                }

                15 ->{
                    score -= (dist * 10)
                //    Log.d(SAFEROUTE, "횡단보도 * " + "${dist}");
                //    Log.d(SAFEROUTE, "추가점수"+"${score}" );
                    saftyScore.crossWalkLength = saftyScore.crossWalkLength?.plus(dist)
                    saftyScore.crossWalkCount = saftyScore.crossWalkCount?.plus(1)
                    saftyScore.score = saftyScore.score?.plus(score)
                }
                //distance: 구간거리 (단위:m)
            }
        }


        if (roadType != null) {
            //도로타입정보
            when (roadType) {
                21 -> {
                    score += (dist * 2)
                //    Log.d(SAFEROUTE, "2/RT/21번도로 *  "+ "${dist}")
                //    Log.d(SAFEROUTE, "추가점수"+"${score}" );
                    saftyScore.roadTypeLength1 = saftyScore.roadTypeLength1?.plus(dist)
                    saftyScore.score = saftyScore.score?.plus(score)
                }
                //차도와 인도가 분리, 정해진 횡단구역으로만 횡단 가능
                22 -> {
                    score += (dist * 1)
                //    Log.d(SAFEROUTE, "1/RT/22번도로 *  " + "${dist}")
                //    Log.d(SAFEROUTE, "추가점수"+"${score}" );
                    saftyScore.roadTypeLength2 = saftyScore.roadTypeLength2?.plus(dist)
                    saftyScore.score = saftyScore.score?.plus(score)
                }
                //차도 인도 분리 X || 보행자 횡단에 제약 X 보행자도로
                23 -> {
                    score += (dist * 0.5)
                  //  Log.d(SAFEROUTE, "0.5/RT/23번도로 *  " + "${dist}")
                  //  Log.d(SAFEROUTE, "추가점수"+"${score}" );
                    saftyScore.roadTypeLength3 = saftyScore.roadTypeLength3?.plus(dist)
                    saftyScore.score = saftyScore.score?.plus(score)
                }

                //차량 통행 불가 보행자 도로
                24 -> {
                    score -= (dist * .5)
                //    Log.d(SAFEROUTE, "0.5/RT/24번도로 *  " + "${dist}")
                //    Log.d(SAFEROUTE, "추가점수"+"${score}" );
                    saftyScore.roadTypeLength4 = saftyScore.roadTypeLength4?.plus(dist)
                    saftyScore.score = saftyScore.score?.plus(score)
                }
                //쾌적 X 도로
                else -> {

                }
            }

        }

    }
}








