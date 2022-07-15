package model

import java.io.Serializable

data class NaviData(val midPointList : ArrayList<List<Double>>,
                    val turnTypeList : ArrayList<Int>,
                    val destination : String,
                    val turnPoint : ArrayList<List<Double>>
):Serializable
{}