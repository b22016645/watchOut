package model

import java.io.Serializable

data class DirectionItem(val lat : Double,
                         val lon : Double,
                         val midPointList : ArrayList<List<Double>>,
                         val midPointNum : Int): Serializable
{ }
