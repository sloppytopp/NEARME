package com.nearme.android.data

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanSettings
import android.content.Context
import com.nearme.android.data.room.NearMeDatabase
import com.nearme.android.data.room.toDomain
import com.nearme.android.data.room.toEntity
import com.nearme.android.scan.AdvertisingIntervalTracker
import com.nearme.android.scan.BleAdvertisementMapper
import com.nearme.android.scan.NearMeScanCallback
import com.nearme.core.ScanOutcome
import com.nearme.core.ScanPipeline
import com.nearme.core.identity.IdentityResolver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Single owner of scanning + persistence + the in-memory [ScanPipeline].
 *
 * On [initialize], prior sightings are loaded from Room and fed back into the
 * pipeline's [IdentityResolver] so a device seen yesterday is still recognized
 * today even though the process restarted and lost all in-memory state.
 */
class IdentityStore private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val db = NearMeDatabase.get(appContext)
    private val resolver = IdentityResolver()
    private val pipeline = ScanPipeline(resolver)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _outcomes = MutableStateFlow<List<ScanOutcome>>(emptyList())
    val outcomes: StateFlow<List<ScanOutcome>> = _outcomes.asStateFlow()

    private var bleScanner: BluetoothLeScanner? = null
    private var scanCallback: NearMeScanCallback? = null

    fun initialize() {
        scope.launch {
            val restored = db.identityDao().getAllIdentitiesWithSightings().map { it.toDomain() }
            resolver.restore(restored)
            _outcomes.value = pipeline.allOutcomes()
        }
    }

    /** Requires BLUETOOTH_SCAN (and BLUETOOTH_CONNECT) to already be granted by the caller. */
    @Suppress("MissingPermission")
    fun startScanning() {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return
        val scanner = adapter.bluetoothLeScanner ?: return
        bleScanner = scanner

        val mapper = BleAdvertisementMapper(
            locationBucketProvider = CompositeLocationBucketProvider(appContext),
            intervalTracker = AdvertisingIntervalTracker(),
        )
        val callback = NearMeScanCallback(mapper) { advertisement ->
            val outcome = pipeline.ingest(advertisement)
            _outcomes.value = pipeline.allOutcomes()
            persist(outcome)
        }
        scanCallback = callback

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
            .build()
        scanner.startScan(null, settings, callback)
    }

    @Suppress("MissingPermission")
    fun stopScanning() {
        scanCallback?.let { bleScanner?.stopScan(it) }
        scanCallback = null
    }

    private fun persist(outcome: ScanOutcome) {
        scope.launch {
            db.identityDao().upsertIdentity(outcome.identity.toEntity())
            val newestSighting = outcome.identity.sightings.last()
            db.identityDao().insertSighting(newestSighting.toEntity(outcome.identity.id))
        }
    }

    companion object {
        // The app runs single-process, so a plain singleton is enough for the
        // foreground service and the UI to share one live ScanPipeline/StateFlow
        // instead of drifting out of sync with independent in-memory state.
        @Volatile private var instance: IdentityStore? = null

        fun get(context: Context): IdentityStore =
            instance ?: synchronized(this) {
                instance ?: IdentityStore(context.applicationContext).also {
                    it.initialize()
                    instance = it
                }
            }
    }
}
