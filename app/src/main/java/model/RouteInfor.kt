package model
//이건 경로의 모든 정보를 담고있는 데이타클래스,
//웹에 퍼블리쉬할 함수도 만들어야함 (스트링으로 붙이는 함수)

import java.io.Serializable

data class RouteInfor (var searchOption : Int )
    : Serializable{


    var totalDistance :Double = 0.0

    // WEB Publish를 위한 stringData
    //var routeInforStringData : String? = null       //publish할 정보를 담고있는 string
    //var ectInforStringData : String? = null         //publish할 기타정보를 담고있는 string

    var stringData : String? = null
    var scoreData : String? = null

    // DB업데이트를 위한 Flag 모음
   // var hasDanger : Boolean = false                 //DangerA,B중 하나라도 있으면 true, 기본값은 False
    //var hasDangerA: Int? = null                 //DangerA중 하나라도 있으면 notNull, 순서는 엘리베이터-육교-지하보도-계단으로 각 자리수가 시설물의 개수를 나타냄
   // var hasDangerB: Int? = null                 //DangerB중 하나라도 있으면 notNull, 순서는 교량-터널-고가도로-대형시설물이동통로 로 각 자리수가 시설물의 개수를 나타냄
   // var hasCrossWalk: Int = 0



    /////////////////////////////////////SCORE /////////////////////////////////////
    var routeScore : Double = 0.0       //경로별 최종 점수
    //최종 루트 결정시 기준이 되는 값.
    //RouteScore = roadScore + DangerScore(final)


    var roadScore_draft : Double  = 0.0        //도로 점수
    var roadScore_final : Double = 0.0


    var DangerScore_draft = 0         //위험 점수 (정규화 전)
    var DangerScore_final = 0         //위험점수 (정규화 O)

    /////////////////////////////////////Danger/////////////////////////////////////
    //Danger
    var turnPoint : Int = 0             //DangerA
    var crossWalk : Int = 0             //DangerB

    //Danger C
    var elevator : Int = 0              //엘리베이터
    var overPasses : Int= 0             //육교
    var underPasses : Int = 0           //지하보도
    var stairs : Int = 0                //계단

    //Danger D
    var bridge : Int = 0                //교량
    var turnnels : Int = 0              //터널
    var highroad  : Int = 0             //고가도로
    var largeFacilitypassage  : Int = 0 //대형시설물이동통로

    var DangerCount : Int = 0           //위험시설 A~D의 총개수, 위험시설 점수 정규화를 위함


    /////////////////////////////////////ROAD_TYPE/////////////////////////////////////
    //단위 : m
    var roadTypeA :Double = 0.0         //보행자 전용 도로(23)
    var roadTypeB :Double = 0.0         //인도 분리(21)
    var roadTypeC :Double = 0.0          //인도 분리 X(22)
    var roadTypeD :Double = 0.0          //쾌적 X(24)

}

