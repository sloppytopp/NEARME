package com.nearme.android.data

import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.wifi.WifiManager
import androidx.core.content.ContextCompat
import com.nearme.core.model.Geohash
import com.nearme.core.model.LocationBucket

/** Supplies the coarse "place" a scan happened at. */
interface LocationBucketProvider {
    fun currentLocationBucket(): LocationBucket
}

/**
 * Buckets by a precision-7 geohash (~153m x 153m cell) of the last-known GPS/
 * network location fix — coarse enough that the stored bucket id never
 * functions as a precise location record, fine enough to separate distinct
 * places the user actually visited.
 *
 * Deliberately reads only the *last known* fix (no active location requests,
 * no continuous updates) — this is a location bucketing hint for a BLE
 * correlation engine, not a location-tracking feature, and it should not cost
 * the battery or permission weight of one. If no fix is cached yet (fresh
 * install, location off), [currentLocationBucket] returns null and the caller
 * should fall back to another provider.
 */
class GpsLocationBucketProvider(
    context: Context,
    private val precision: Int = Geohash.DEFAULT_PRECISION,
) {
    private val appContext = context.applicationContext
    private val locationManager = appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    fun currentLocationBucketOrNull(): LocationBucket? {
        if (!hasLocationPermission()) return null
        val manager = locationManager ?: return null

        val fix = runCatching {
            manager.allProviders
                .mapNotNull { provider -> manager.getLastKnownLocation(provider) }
                .maxByOrNull { it.time }
        }.getOrNull() ?: return null

        val hash = Geohash.encode(fix.latitude, fix.longitude, precision)
        return LocationBucket(id = "geo:$hash")
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(appContext, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
}

/**
 * v1 fallback: buckets by the currently-associated Wi-Fi BSSID when no GPS fix
 * is available yet — a reasonable proxy for "which building" indoors, where
 * GPS fixes are often stale or missing entirely.
 */
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

/**
 * Prefers a GPS-derived geohash bucket (works anywhere, indoors or out);
 * falls back to Wi-Fi BSSID when no location fix is cached yet (common right
 * after install, or with location services off); falls back to "unknown"
 * only when neither signal is available.
 */
class CompositeLocationBucketProvider(context: Context) : LocationBucketProvider {
    private val gpsProvider = GpsLocationBucketProvider(context)
    private val wifiProvider = WifiBssidLocationBucketProvider(context)

    override fun currentLocationBucket(): LocationBucket =
        gpsProvider.currentLocationBucketOrNull() ?: wifiProvider.currentLocationBucket()
}
