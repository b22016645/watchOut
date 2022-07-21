package model

data class Preference (   //도착지 도착 후 선호도 평가 데이터 베이스 (파이어베이스 저장용)
    var score : Int= 50,  //만족도

    //AW : Alogrithm Weight : 안전한길 알고리즘 가중치
    var AWturnPoint : Double? = -20.0,      //분기점 가중치
    var AWcrossWalk : Double? = -10.0,      //횡단보도 가중치 (주의 : 미터당 마이너스점수)
    var AWFT_car : Double ?  = -60.0,       //FT_Car 가중치 : 교량, 터널, 고가도로
    var AWFT_noCar : Double ? = -80.0,       //FT_noCar 가중치 : 육교, 지하보도, 계단

    var AWTableWeight : Double ? = 0.4      //알고리즘 마이너스 점수 테이블 전체의 가중치
)