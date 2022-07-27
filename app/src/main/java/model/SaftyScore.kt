package model

import java.io.Serializable

data class SaftyScore(var score:Double?, var turnPoint:Int?, var elevator:Int?,
                      var crossWalkCount:Int?, var crossWalkLength:Double?,
                      var roadTypeLength1:Double?, var roadTypeLength2:Double?, var roadTypeLength3:Double?, var roadTypeLength4:Double?,
                      var bridge:Int?, var turnnels:Int?, var highroad:Int?,
                      var overPasses:Int?, var underPasses:Int?, var stairs:Int?,
                      var largeFacilitypassage:Int?, var totalDistance: Int?
): Serializable {
    init{
        score = 0.0             //최종 점수
        turnPoint = 0           //분기점 개수
        elevator = 0            //엘리베이터 개수
        crossWalkCount = 0      //횡단보도 개수
        bridge= 0               //교량 개수
        turnnels = 0            //터널 개수
        highroad = 0            //고가도로 개수
        overPasses = 0          //육교 개수
        underPasses = 0         //지하보도 개수
        stairs = 0              //계단 개수
        crossWalkLength = 0.0   //횡단보도 거리(전체 경로에서 총몇미터)
        roadTypeLength1 = 0.0   //21번 roadType 총 미터
        roadTypeLength2 = 0.0   //22번 roadType 총 미터
        roadTypeLength3 = 0.0   //23번 roadType 총 미터
        roadTypeLength4 =0.0    //24번 roadType 총 미터
        largeFacilitypassage = 0    //대형시설물이동통로 개수
    }
}
