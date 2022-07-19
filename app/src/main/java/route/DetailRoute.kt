package route

import java.util.ArrayList
import kotlin.math.abs
import kotlin.math.pow

object DetailRoute {

    fun midPoint(lat1 : Double, lon1 : Double, lat2 : Double, lon2 : Double, midpointList : ArrayList<List<Double>>) : ArrayList<List<Double>> {
        if(getDistance(lat1, lon1, lat2, lon2) > 15.0) {

            var nlat1 = lat1
            var nlon1 = lon1
            var nlat2 = lat2
            val dLon = Math.toRadians(lon2 - lon1)

            //convert to radians
            nlat1 = Math.toRadians(nlat1)
            nlat2 = Math.toRadians(nlat2)
            nlon1 = Math.toRadians(nlon1)
            val Bx = Math.cos(nlat2) * Math.cos(dLon)
            val By = Math.cos(nlat2) * Math.sin(dLon)

            var lat3 = Math.atan2(
                Math.sin(nlat1) + Math.sin(nlat2),
                Math.sqrt((Math.cos(nlat1) + Bx) * (Math.cos(nlat1) + Bx) + By * By)
            )
            var lon3 = nlon1 + Math.atan2(By, Math.cos(nlat1) + Bx)

            lat3 = Math.toDegrees(lat3)
            lon3 = Math.toDegrees(lon3)

            midPoint(lat1, lon1, lat3, lon3, midpointList)

            midpointList.add(listOf(lat3, lon3))

            midPoint(lat3, lon3, lat2, lon2, midpointList)

        }
        return midpointList
    }

    //두 좌표 사이의 거리를 m단위로 알려줌
    fun getDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2).pow(2.0) + Math.sin(dLon / 2).pow(2.0) * Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
        val c = 2 * Math.asin(Math.sqrt(a))
        return (6372.8 * 1000 * c)
    }



    //현재위치와 미드포인트를 받고, 오차 범위를 받아서
    //현재 위치가 미드포인트의 오차범위 밖에 있으면 경로이탈이라고 판단 -> False 반환.
    fun isRightCource(nowX: Double, nowY: Double, midPointX: Double, midPointY: Double,distanceRange:Double):Boolean{
        //현재위치x와 현재위치 y가 기준위치 x,y와 비교했을때 절댓값 기준으로 오차범위만큼 차이나면 경로이탈로 판단
        var howFarX:Double = abs(nowX - midPointX)
        var howFarY:Double = abs(nowY-midPointY)
        return !((howFarX >distanceRange) || (howFarY) > distanceRange)
    }



}