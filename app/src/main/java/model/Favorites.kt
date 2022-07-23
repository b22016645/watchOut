package model

import android.provider.ContactsContract

object Favorites { //즐겨찾기 데이터 클래스 (파이어베이스 저장용)
    var address: String? = null      //즐겨찾기 주소
    var lat: Double? = null       //즐겨찾기 위도용        DoRetrofit - getPoi()
    var lon: Double? = null       //즐겨찾기 경로
    var nickname: String? = null     //즐겨찾기에 저장할 이름
    var frequency: Int? = 0        //이용 횟수
    //여기서 기준값을 낫널로 고정시켜야하는데 뭐가 기준값이 될 건지 논의가 필요함

}