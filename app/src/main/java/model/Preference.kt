package model

object Preference {   //도착지 도착 후 선호도 평가 데이터 베이스 (파이어베이스 저장용)
    var score: Int? = null  //만족도

    //AW : Alogrithm Weight : 안전한길 알고리즘 가중치
    var awturnPoint: Double? = null      //분기점 가중치
    var awcrossWalk: Double? = null      //횡단보도 가중치 (주의 : 미터당 마이너스점수)
    var awft_car: Double? = null       //FT_Car 가중치 : 교량, 터널, 고가도로
    var awft_noCar: Double? = null       //FT_noCar 가중치 : 육교, 지하보도, 계단

    var tableWeight: Double? = null      //알고리즘 마이너스 점수 테이블 전체의 가중치
}
