package model
// DB관련 싱글톤객체 모음

object History { //히스토리 데이터 클래스 (파이어베이스 저장용)



        //얘가 도착지
        var arrivedName: String? = null    //출발지이름           //NavigationActivity - endOfRoute()에서
        var arrivedLat: Double? = null    //출발지 위도 (x)       //Main locationcallback에서 현재위치조정이 완료되었습니다 TTS완료 후 받음
        var arrivedLon: Double? = null     //출발지 경도 (y)      // 경로 이탈시, 목적지 재검색시 다시 받아야할듯?

        //얘가 출발지
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


        //RouteInfor 최종 선택된 루트 정보
        var midPointSize : Int? = null          //DoRetrofit = midPointSize
        var routNum: Int? = null         //이용했던 경로 번호용. DoRetrofit에서 인덱스 결정 후 받음
        var totalDistance : Double =0.0         //경로 길이
        var routeScore : Double = 0.0           //최종 경로 점수
        var stringData : String = ""            //엘리베이터(2회),분기점(31회),지하보도(1회)가 포함된 경로입니다.

        //후보경로 정보.
       // var totalRouteList = arrayListOf<otherRouteInfor>()             //도로점수, 위험점수,

       // lateinit var preference : Preference


        //string데이타 웹에 쉽게 띄울 용으로. 앞에 세개는 경로 결정되는 즉시 저장
        var totalRouteInfor : String = ""                       //후보 경로 정보 (웹 표용) : 경로4개별 도로점수, 위험점수, 총점수, stringData
        var finalRouteInfor_now : String = " "                  //최종 경로 정보 (실시간용) : 출발지, 목적지, 최종경로점수, 경로길이, 예정소요시간, 스트링데이타(엘베2회, 분기점2회 포함된 경로입니다)
        var routePreference : String = ""                       //해당 경로 이용시 설정했던 가중치 정보 : 도로상태 (1.2), 위험시설 (0.8), 횡단보도(80), 차도 비분리 시설 (30), 분기점 (50), 차도 분리 시설 (80)
        //얘는 목적지 달성 후 저장 (navigation- endofRoute)
        var finalRouteInfor_simul : String = ""                 //최종 경로 정보 (시뮬용) : 경로점수, 경로길이, 이탈횟수, 최대심박수, 평균심박수


        //경이탈정보 (exp = existPoint: 경로 이탈 부분)
        var expTurnPoint: Int = 0      //분기점에서 이탈한 횟수  Navigation - locationCallBack
        var expCrossWalk: Int = 0       //횡단보도에서 이탈한 횟수
        var expStraightRoad: Int = 0    //직진길에서 이탈한 횟수
        var expNoCar: Int = 0           //위험시설A(차x)에서 이탈한 횟수
        var expWithCar: Int = 0         //위험시설B(차o)에서 이탈한 횟수
        var expTotal: Int = 0           //총 이탈 횟수

        // 선호도 가중치 DB업데이트를 위한 Flag 모음. DoRetrofir 274
        var hasDanger : Boolean = false                 //DangerA,B중 하나라도 있으면 true, 기본값은 False
        var hasDangerA: Int? = null                 //DangerA중 하나라도 있으면 notNull, 순서는 엘리베이터-육교-지하보도-계단으로 각 자리수가 시설물의 개수를 나타냄
        var hasDangerB: Int? = null                 //DangerB중 하나라도 있으면 notNull, 순서는 교량-터널-고가도로-대형시설물이동통로 로 각 자리수가 시설물의 개수를 나타냄
        var hasCrossWalk: Int = 0




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
              //  hasDanger = false
              //  hasDangerA = null
              //  hasDangerB = null
                midPointSize = null
              //  hasCrossWalk = 0

             //   totalRouteList.clear()

        }

}

object Preference {   //도착지 도착 후 선호도 평가 데이터 베이스 (파이어베이스 저장용)
        var score: Int? = null  //만족도

        //AW : Alogrithm Weight : 안전한길 알고리즘 가중치
        var algorithmWeight_turnPoint: Int? = null   //분기점 가중치
        var algorithmWeight_crossWalk: Int? = null      //횡단보도 가중치 (주의 : 미터당 마이너스점수)
        var algorithmWeight_facilityCar: Int? = null       //FT_Car 가중치 : 교량, 터널, 고가도로
        var algorithmWeight_facilityNoCar: Int? = null      //FT_noCar 가중치 : 육교, 지하보도, 계단

        //var tableWeight: Double = 0.5      //알고리즘 마이너스 점수 테이블 전체의 가중치
        var tableWeight_road : Double  = 1.0
        var tableWeight_danger : Double  = 1.0
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