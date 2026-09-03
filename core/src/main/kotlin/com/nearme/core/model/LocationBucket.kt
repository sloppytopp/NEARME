package com.nearme.core.model

/**
 * A coarse place identifier, never raw GPS. Callers are expected to derive this
 * from something like a geohash truncated to city-block precision, a Wi-Fi BSSID
 * of the connected AP, or a user-named place — never exact lat/lng, since the
 * scan history never needs to leave the device and finer precision only adds risk.
 */
data class LocationBucket(
    val id: String,
    val label: String? = null,
)
