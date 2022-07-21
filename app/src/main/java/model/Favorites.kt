package model

import android.provider.ContactsContract

data class Favorites ( //즐겨찾기 데이터 클래스 (파이어베이스 저장용)
    var where : String? = null,      //즐겨찾기 주소
    var lat : Double? = null,       //즐겨찾기 위도
    var lon : Double? = null,       //즐겨찾기 경로
    var name : String ?  = null,     //즐겨찾기에 저장할 이름
    var frequency : Int? = 0        //이용 횟수

)