package org.test.opensky.model

import java.util.*

data class FlightDataRaw(val time: Int? = null, val states: List<Array<Any>>? = null)

data class FlightDataRecordDecoded(
    private val date: Date,
    private val icao24: String?,
    private val callSign: String?,
    private val country: String?,
    private val longitude: Double?,
    private val latitude: Double?,
    private val altitude: Double?,
    private val onGround: Boolean?,
    private val trueTrack: Float?,
) {
    fun key(): String {
        return "${icao24 ?: ""}${callSign ?: ""}${country ?: ""}"
    }

    fun hasKey(otherKey: String?): Boolean {
        return otherKey == key()
    }

    fun date(): Date {
        return date
    }

    fun icao24(): String? {
        return icao24
    }

    fun callSign(): String? {
        return callSign
    }

    fun country(): String? {
        return country
    }

    fun longitude(): Double? {
        return longitude
    }

    fun latitude(): Double? {
        return latitude
    }

    fun altitude(): Double? {
        return altitude
    }

    fun onGround(): Boolean {
        return onGround ?: false
    }

    fun bearig(): Float {
        return trueTrack ?: 0.0f
    }
}
