package model

object Favorites  { //즐겨찾기 데이터 클래스 (파이어베이스 저장용)
    var dat = mutableMapOf<String,Any?>(
        "address" to null,                  //DoRetrofit - getPoi()
        "lat" to null,                      //DoRetrofit - getPoi()
        "lon" to null,                      //DoRetrofit - getPoi()
        "nickname" to null,                 //id값       DoRetrofit - requestStt
        "frequency" to null                 //검색횟수     호출즉시+1
    )
}