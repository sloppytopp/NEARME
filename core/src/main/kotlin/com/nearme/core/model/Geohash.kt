package com.nearme.core.model

/**
 * Standard geohash encode/decode (base32, interleaved lat/lon bits). Pure math,
 * no platform dependency — the Android layer feeds it a last-known-location fix
 * and gets back a coarse place bucket, never a raw coordinate.
 *
 * Precision choice matters: this is the actual privacy boundary between
 * "distinct places you visited" and "your exact position." Cell sizes by
 * character count (roughly): 5 -> ~4.9km x 4.9km, 6 -> ~1.2km x 0.6km,
 * 7 -> ~153m x 153m, 8 -> ~38m x 19m. NEARME defaults to 7 — fine enough to
 * separate a coffee shop from the grocery store next to a shared parking lot,
 * coarse enough that the stored bucket id never functions as a precise
 * location record.
 */
object Geohash {
    private const val BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz"
    const val DEFAULT_PRECISION = 7

    data class Bounds(val latMin: Double, val latMax: Double, val lonMin: Double, val lonMax: Double) {
        fun contains(latitude: Double, longitude: Double): Boolean =
            latitude in latMin..latMax && longitude in lonMin..lonMax
    }

    fun encode(latitude: Double, longitude: Double, precision: Int = DEFAULT_PRECISION): String {
        require(precision > 0) { "precision must be positive" }
        var latMin = -90.0
        var latMax = 90.0
        var lonMin = -180.0
        var lonMax = 180.0

        val hash = StringBuilder()
        var isEven = true
        var bit = 0
        var charBits = 0

        while (hash.length < precision) {
            if (isEven) {
                val mid = (lonMin + lonMax) / 2
                if (longitude >= mid) {
                    charBits = (charBits shl 1) or 1
                    lonMin = mid
                } else {
                    charBits = charBits shl 1
                    lonMax = mid
                }
            } else {
                val mid = (latMin + latMax) / 2
                if (latitude >= mid) {
                    charBits = (charBits shl 1) or 1
                    latMin = mid
                } else {
                    charBits = charBits shl 1
                    latMax = mid
                }
            }
            isEven = !isEven

            bit++
            if (bit == 5) {
                hash.append(BASE32[charBits])
                bit = 0
                charBits = 0
            }
        }
        return hash.toString()
    }

    /** Returns the bounding box a given geohash string represents. */
    fun decodeBounds(hash: String): Bounds {
        require(hash.isNotEmpty()) { "hash must not be empty" }
        var latMin = -90.0
        var latMax = 90.0
        var lonMin = -180.0
        var lonMax = 180.0
        var isEven = true

        for (c in hash) {
            val charIndex = BASE32.indexOf(c)
            require(charIndex >= 0) { "invalid geohash character: $c" }
            for (bitIndex in 4 downTo 0) {
                val bit = (charIndex shr bitIndex) and 1
                if (isEven) {
                    val mid = (lonMin + lonMax) / 2
                    if (bit == 1) lonMin = mid else lonMax = mid
                } else {
                    val mid = (latMin + latMax) / 2
                    if (bit == 1) latMin = mid else latMax = mid
                }
                isEven = !isEven
            }
        }
        return Bounds(latMin, latMax, lonMin, lonMax)
    }
}
