package model;
// DB관련 싱글톤객체 모음

object History { //히스토리 데이터 클래스 (파이어베이스 저장용)

        //StartingPoint 출발지
        var spName: String? = null    //출발지이름           //NavigationActivity - endOfRoute()에서
        var spLat: Double? = null    //출발지 위도 (x)       //Main locationcallback에서 현재위치조정이 완료되었습니다 TTS완료 후 받음
        var spLon: Double? = null     //출발지 경도 (y)      // 경로 이탈시, 목적지 재검색시 다시 받아야할듯?

        //DestinationPoint 도착지
        var dpName: String? = null     //도착지이름용         DoRetrofit  - getPOI() 목적지 로그 찍고 받ㄸ
        var dpLat: Double? = null      //도착지 위도 (x)     DoRetrofit - getPoi() 중간쯤 목적지 좌표 찍고 받음
        var dpLon: Double? = null      //도착지 경도 (y)     DoRetrofit - getPoi() 중간쯤 목적지 좌표 찍고 받음

        //TimeInfor 시간정보  "yyyy-MM-dd HH:mm:ss"
        var departureTime: String? = null  //출발시간용      Main아래 ~로 길안내를 시작합니다 TTS이후 받음
        var arrivedTime: String? = null   //도착시간        Main아래 도착로그 찍고 TTS찍기 직전 받음
        var expectedTime: Int? = null  //예상 소요 시간트      RetrofitManager에서 경로 불러올 때마다 업데이트, 최종으로 알고리즘 선택된 경로의 값이 들어감

        //Biometric Infor 생체성보
        var heartRateAverage: Int? = null  //평균 심박
        var heartRateMax: Int? = null      //최대 심박
        var stepNum: Int? = null           //발걸음 수
        var kcal: Double? = null           //소요 칼로리

        //RouteInfor 루트 정보 (exp = existPoint: 경로 이탈 부분)
        var routNum: Int? = null           //이용했던 경로 번호용. DoRetrofit에서 인덱스 결정 후 받음
        var expTurnPoint: Int? = null      //분기점에서 이탈한 횟수
        var expFacility: Int? = null       //FT 이탈 횟수
        var expLineWay: Int? = null        // 직선길 이탈 횟수
        var expTotal: Int? = null           //총 이탈 횟수
        }

object Preference {   //도착지 도착 후 선호도 평가 데이터 베이스 (파이어베이스 저장용)
        var score: Int? = null  //만족도

        //AW : Alogrithm Weight : 안전한길 알고리즘 가중치
        var awturnPoint: Double? = null      //분기점 가중치
        var awcrossWalk: Double? = null      //횡단보도 가중치 (주의 : 미터당 마이너스점수)
        var awft_car: Double? = null       //FT_Car 가중치 : 교량, 터널, 고가도로
        var awft_noCar: Double? = null       //FT_noCar 가중치 : 육교, 지하보도, 계단

        var tableWeight: Double? = null      //알고리즘 마이너스 점수 테이블 전체의 가중치
}

object Favorites  { //즐겨찾기 데이터 클래스 (파이어베이스 저장용)
        var dat = mutableMapOf<String,Any?>(
                "address" to null,                  //DoRetrofit - getPoi()
                "lat" to null,                      //DoRetrofit - getPoi()
                "lon" to null,                      //DoRetrofit - getPoi()
                "nickname" to null,                 //id값       DoRetrofit - requestStt
                "frequency" to null                 //검색횟수     호출즉시+1
        )
}