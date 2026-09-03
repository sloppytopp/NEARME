package com.nearme.android.data

import android.content.Context
import android.net.wifi.WifiManager
import com.nearme.core.model.LocationBucket

/**
 * Supplies the coarse "place" a scan happened at. v1 deliberately avoids GPS/
 * fine location entirely — it buckets by the currently-associated Wi-Fi BSSID,
 * which is a reasonable proxy for "which building/venue the user is in" without
 * ever recording an actual coordinate.
 *
 * Known limitation: readers on the same Wi-Fi network as the user (e.g. two
 * rooms of one office) collapse into one bucket, and readings taken off Wi-Fi
 * entirely fall back to a single "unknown" bucket — meaning the correlation
 * engine can't detect movement between two non-Wi-Fi outdoor locations in this
 * version. A GPS/geohash-based provider is a natural v2 upgrade behind the
 * same interface.
 */
interface LocationBucketProvider {
    fun currentLocationBucket(): LocationBucket
}

class WifiBssidLocationBucketProvider(context: Context) : LocationBucketProvider {
    private val wifiManager = context.applicationContext
        .getSystemService(Context.WIFI_SERVICE) as? WifiManager

    override fun currentLocationBucket(): LocationBucket {
        val bssid = runCatching { wifiManager?.connectionInfo?.bssid }
            .getOrNull()
            ?.takeIf { it.isNotBlank() && it != "02:00:00:00:00:00" } // Android's "unavailable" sentinel

        return if (bssid != null) {
            LocationBucket(id = "wifi:$bssid")
        } else {
            LocationBucket(id = "unknown", label = "Off known Wi-Fi")
        }
    }
}
