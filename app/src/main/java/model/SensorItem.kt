package model

import java.io.Serializable

data class SensorItem(val lat : Double,
                      val lon : Double,
                      val midPointList : ArrayList<List<Double>>,
                      val midPointNum : Int,
                      val setting : Int  ): Serializable
{ }
