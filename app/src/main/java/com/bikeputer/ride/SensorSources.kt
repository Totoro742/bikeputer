package com.bikeputer.ride

import com.bikeputer.domain.HeartRateSample
import com.bikeputer.domain.LocationSample
import com.bikeputer.domain.PowerSample
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow

interface PowerSource { val samples: Flow<PowerSample>; val connected: StateFlow<Boolean> }
interface HeartRateSource { val samples: Flow<HeartRateSample>; val connected: StateFlow<Boolean> }
interface LocationProvider { val samples: Flow<LocationSample>; val available: StateFlow<Boolean> }

class FakePowerSource(
    data: List<PowerSample>,
    private val intervalMs: Long = 0,
) : PowerSource {
    private val data = data
    override val samples: Flow<PowerSample> = flow {
        for (s in data) {
            if (intervalMs > 0) delay(intervalMs)
            emit(s)
        }
    }
    override val connected: StateFlow<Boolean> = MutableStateFlow(true)
}

class FakeHeartRateSource(
    data: List<HeartRateSample>,
    private val intervalMs: Long = 0,
) : HeartRateSource {
    private val data = data
    override val samples: Flow<HeartRateSample> = flow {
        for (s in data) {
            if (intervalMs > 0) delay(intervalMs)
            emit(s)
        }
    }
    override val connected: StateFlow<Boolean> = MutableStateFlow(true)
}

class FakeLocationProvider(
    data: List<LocationSample>,
    private val intervalMs: Long = 0,
) : LocationProvider {
    private val data = data
    override val samples: Flow<LocationSample> = flow {
        for (s in data) {
            if (intervalMs > 0) delay(intervalMs)
            emit(s)
        }
    }
    override val available: StateFlow<Boolean> = MutableStateFlow(true)
}
