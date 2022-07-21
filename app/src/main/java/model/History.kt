package model

data class History ( //히스토리 데이터 클래스 (파이어베이스 저장용)

    //StartingPoint 출발지
    var spName : String? = null,    //출발지이름
    var spLat : Double? = null,     //출발지 위도 (x)
    var spLon : Double? = null,     //출발지 경도 (y)

    //DestinationPoint 도착지
    var dpName : String? = null,     //도착지이름
    var dpLat : Double? = null,      //도착지 위도 (x)
    var dpLon : Double? = null,      //도착지 경도 (y)

    //TimeInfor 시간정보
    var departureTime : Double? = null,  //출발시간
    var arrivedTime : Double ? = null,   //도착시간
    var expectedTime : Double ? = null,  //예상 소요 시간

    //Biometric Infor 생체성보
    var heartRateAverage : Int? = null,  //평균 심박
    var heartRateMax : Int? = null,      //최대 심박
    var kcal : Double? = null,           //소요 칼로리

    //RouteInfor 루트 정보 (exp = existPoint: 경로 이탈 부분)
    var rootNum : Int? = null,           //이용했던 경로 번호
    var expTurnPoint : Int? = null,      //분기점에서 이탈한 횟수
    var expFacility : Int? = null,       //FT 이탈 횟수
    var expLineWay : Int? = null,        // 직선길 이탈 횟수
    var expTotal : Int? = null           //총 이탈 횟수
)
