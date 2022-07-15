package utils

object Constant {
    object API {
        const val BASE_URL : String = "https://apis.openapi.sk.com/tmap/"

        const val APPKEY : String = "l7xx79d570bd9bf74163a98c47d00783f59f"
        const val VERSION : String ="1"
        const val FORMAT : String="json"
        const val CALLBACK : String="result"
        const val COUNT : String="2"
        const val RADIUS : String="20"


        const val SEARCH_POI : String = "pois"
        const val SEARCH_ROUTE : String = "routes/pedestrian"

        const val LOG : String = "로그"


        /* 카카오 음성 REST API 변수 */
        const val BASE_URL_KAKAO_API = "https://kakaoi-newtone-openapi.kakao.com/"
        const val REST_API_KEY = "e0297fd4d33c3ebf9c206117821bec13"
        const val transferEncoding = "chunked"
        const val contentType = "application/octet-stream"
        const val authorization = "KakaoAK $REST_API_KEY"

    }
    enum class RESPONSE_STATE {
        OKAY,
        FAIL,
        NO_CONTENT,
        ERROR403
    }

}