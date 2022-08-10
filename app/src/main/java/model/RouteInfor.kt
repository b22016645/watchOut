package model

import java.io.Serializable

data class RouteInfor (var searchOption : Int ) : Serializable{

    /////////////////////////////////////SCORE /////////////////////////////////////
    var routeScore : Int = 0        //경로별 최종 점수
    //최종 루트 결정시 기준이 되는 값.
    //RouteScore = roadScore + DangerScore(final)
    //min = 0 , max = 100

    var roadScore : Int = 0         //도로 점수
    // min = 50, max = 100
    // Σ (RoadType 각 배점 * 각 Distance) / ΣDistance

    var DangerScore_draft : Int = 0         //위험 점수 (정규화 전)
    //Σ (배점 * 개수)

    var DangerScore_final : Int = 0         //위험점수 (정규화 O)
    // min = -50, max = 0  으로 정규화
    // 위험시설이 많을 수록 -50에 가까운 값이 된다.


    /////////////////////////////////////Danger/////////////////////////////////////
    //Danger
    var turnPoint : Int = 0
    var crossWalk : Int = 0

    //Danger A
    var elevator : Int = 0              //엘리베이터
    var overPasses : Int= 0             //육교
    var underPasses : Int = 0           //지하보도
    var stairs : Int = 0                //계단

    //Danger B
    var bridge : Int = 0                //교량
    var turnnels : Int = 0              //터널
    var highroad  : Int = 0             //고가도로
    var largeFacilitypassage  : Int = 0 //대형시설물이동통로


    /////////////////////////////////////ROAD_TYPE/////////////////////////////////////
    //단위 : m
    var roadTypeA :Double = 0.0         //보행자 전용 도로(23)
    var roadTypeB :Double = 0.0         //인도 분리(21)
    var roadTypeC :Double = 0.0          //인도 분리 X(22)
    var roadTypeD :Double = 0.0          //쾌적 X(24)

}

