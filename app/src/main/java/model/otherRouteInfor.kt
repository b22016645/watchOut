package model

import java.io.Serializable

data class otherRouteInfor (var roadScore : Double ?,
                            var dangerScore : Double?,
                            var stringData : String ?,
                            var isSelected : Boolean = false
): Serializable