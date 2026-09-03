package com.nearme.android.scan

import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.util.Log
import com.nearme.core.model.BleAdvertisement

/**
 * Bridges the Android BLE scan callback to the platform-agnostic core: maps
 * each raw [ScanResult] to a [BleAdvertisement] and hands it to [onAdvertisement],
 * which the owning repository (IdentityStore) feeds into [com.nearme.core.ScanPipeline].
 */
class NearMeScanCallback(
    private val mapper: BleAdvertisementMapper,
    private val onAdvertisement: (BleAdvertisement) -> Unit,
) : ScanCallback() {

    override fun onScanResult(callbackType: Int, result: ScanResult) {
        onAdvertisement(mapper.map(result))
    }

    override fun onBatchScanResults(results: MutableList<ScanResult>) {
        results.forEach { onAdvertisement(mapper.map(it)) }
    }

    override fun onScanFailed(errorCode: Int) {
        Log.e("NearMeScanCallback", "BLE scan failed with error code $errorCode")
    }
}
