package route

object SafeRoute {
    infix fun Int.fdiv(i: Int): Double = this / i.toDouble();
    fun calcPartialScore(
        totalDistance: Int?,
        distance: Int?,
        roadType: Int?,
        turnType: Int?
    ): Double {
        var weight: Double = -1.0
        // case: Point
        if (turnType != null) {
            when (turnType) {
                in 1..7, 11, 233 -> weight = 0.0
                in 12..19 -> weight = 1.0
                in 125..129, in 211..217, 218 -> weight = 2.0
                else -> {
                }
            }
            return weight
        }
        // case: LineString
        else {
            when (roadType) {
                23 -> weight = 0.0
                21 -> weight = 1.0
                22, 24 -> weight = 2.0
                else -> {
                }
            }
            return weight * distance!!.fdiv(totalDistance!!)
        }
    }
}