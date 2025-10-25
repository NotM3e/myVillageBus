package com.myvillagebus.data.model

data class BusStop(
    val stopName: String,
    val arrivalTime: String,
    val delayMinutes: Int = 0
)