package model
// DB관련 싱글톤객체 모음

object History { //히스토리 데이터 클래스 (파이어베이스 저장용)

        //StartingPoint 출발지
        var arrivedName: String? = null    //출발지이름           //NavigationActivity - endOfRoute()에서
        var arrivedLat: Double? = null    //출발지 위도 (x)       //Main locationcallback에서 현재위치조정이 완료되었습니다 TTS완료 후 받음
        var arrivedLon: Double? = null     //출발지 경도 (y)      // 경로 이탈시, 목적지 재검색시 다시 받아야할듯?

        //DestinationPoint 도착지
        var dpName: String? = null     //도착지이름용         DoRetrofit  - getPOI() 중간쯤 목적지 좌표 찍고 받음
        var dpLat: Double? = null      //도착지 위도 (x)     DoRetrofit - getPoi() 중간쯤 목적지 좌표 찍고 받음
        var dpLon: Double? = null      //도착지 경도 (y)     DoRetrofit - getPoi() 중간쯤 목적지 좌표 찍고 받음

        //TimeInfor 시간정보  "yyyy-MM-dd HH:mm:ss"
        var departureTime: String? = null  //출발시간용      Main아래 ~로 길안내를 시작합니다 TTS이후 받음
        var arrivedTime: String? = null   //도착시간        Main아래 도착로그 찍고 TTS찍기 직전 받음
        var expectedTime: Int? = null  //예상 소요 시간트      RetrofitManager에서 경로 불러올 때마다 업데이트, 최종으로 알고리즘 선택된 경로의 값이 들어감

        //Biometric Infor 생체성보
        var heartRateAverage: Int? = null  //평균 심박
        var heartRateMax: Int? = null      //최대 심박
        var stepNum: Int? = null           //발걸음 수      Navigation - endOfRoute()


        //RouteInfor 루트 정보 (exp = existPoint: 경로 이탈 부분)
        var routNum: Int? = null         //이용했던 경로 번호용. DoRetrofit에서 인덱스 결정 후 받음
        var expTurnPoint: Int = 0      //분기점에서 이탈한 횟수  Navigation - locationCallBack
        var expCrossWalk: Int = 0       //횡단보도에서 이탈한 횟수
        var expStraightRoad: Int = 0    //직진길에서 이탈한 횟수
        var expNoCar: Int = 0           //위험시설A(차x)에서 이탈한 횟수
        var expWithCar: Int = 0         //위험시설B(차o)에서 이탈한 횟수
        var expTotal: Int = 0           //총 이탈 횟수

        // 선호도 가중치 DB업데이트를 위한 Flag 모음
        var hasDanger : Boolean = false                 //DangerA,B중 하나라도 있으면 true, 기본값은 False
        var hasDangerA: Int? = null                 //DangerA중 하나라도 있으면 notNull, 순서는 엘리베이터-육교-지하보도-계단으로 각 자리수가 시설물의 개수를 나타냄
        var hasDangerB: Int? = null                 //DangerB중 하나라도 있으면 notNull, 순서는 교량-터널-고가도로-대형시설물이동통로 로 각 자리수가 시설물의 개수를 나타냄

        fun init() {
                arrivedName = null
                arrivedLat = null
                arrivedLon = null
                dpName = null
                dpLat = null
                dpLon = null
                departureTime = null
                arrivedTime = null
                expectedTime = null
                heartRateAverage = null
                heartRateMax = null
                stepNum = null
                routNum = null
                expTurnPoint = 0
                expCrossWalk = 0
                expStraightRoad = 0
                expNoCar = 0
                expWithCar = 0
                expTotal = 0
        }

}

object Preference {   //도착지 도착 후 선호도 평가 데이터 베이스 (파이어베이스 저장용)
        var score: Int? = null  //만족도

        //AW : Alogrithm Weight : 안전한길 알고리즘 가중치
        var algorithmWeight_turnPoint: Double? = null   //분기점 가중치
        var algorithmWeight_crossWalk: Double? = null      //횡단보도 가중치 (주의 : 미터당 마이너스점수)
        var algorithmWeight_facilityCar: Double? = null       //FT_Car 가중치 : 교량, 터널, 고가도로
        var algorithmWeight_facilityNoCar: Double? = null      //FT_noCar 가중치 : 육교, 지하보도, 계단

        var tableWeight: Double = 0.5      //알고리즘 마이너스 점수 테이블 전체의 가중치
}

object Favorites  { //즐겨찾기 데이터 클래스 (파이어베이스 저장용)
        var dat = mutableMapOf<String,Any?>(
                "address" to "주소",                  //DoRetrofit - getPoi()
                "lat" to "위도",                      //DoRetrofit - getPoi()
                "lon" to "경도",                      //DoRetrofit - getPoi()
                "nickname" to "즐겨찾기저장이름",                 //id값       Main - onActivityResult()
                "frequency" to 0                 //검색횟수     호출즉시+1
        )
}