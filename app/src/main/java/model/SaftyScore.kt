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
        score = 0.0
        turnPoint = 0
        elevator = 0
        crossWalkCount = 0
        crossWalkLength = 0.0
        roadTypeLength1 = 0.0
        roadTypeLength2 = 0.0
        roadTypeLength3 = 0.0
        roadTypeLength4 =0.0
        bridge= 0
        turnnels = 0
        highroad = 0
        overPasses = 0
        underPasses = 0
        stairs = 0
        largeFacilitypassage = 0
    }
}
