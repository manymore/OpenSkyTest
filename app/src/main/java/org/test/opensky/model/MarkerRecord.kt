package org.test.opensky.model

import com.google.android.gms.maps.model.Marker
import java.util.*

data class MarkerRecord(
    private val date: Date,
    private val onGround: Boolean,
    private val marker: Marker,
) {
    fun date(): Date {
        return date
    }

    fun onGround(): Boolean {
        return onGround
    }

    fun marker(): Marker {
        return marker
    }
}