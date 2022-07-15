package model

import java.io.Serializable

//getter만 생성하기 위해 val
data class POI (val name : String,
                val frontLat : String,  //시설물 입구 위도
                val frontLon : String):Serializable  //시설물 입구 경도
{}