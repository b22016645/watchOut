package model

import java.io.Serializable

//DoRetrofitActivity에서 getPOI를 호출할 때 사용할 변수
data class DoRetrofitData(val byteAudioData: ByteArray?,
                          val destination : String,
                          val lat : Double,
                          var lon : Double
):Serializable
{}