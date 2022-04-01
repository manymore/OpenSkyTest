package org.test.opensky.model

import com.google.android.gms.maps.model.Marker
import java.util.*

data class MarkerRecord(
    val date: Date,
    val onGround: Boolean,
    val marker: Marker,
)