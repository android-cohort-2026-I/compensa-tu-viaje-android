package com.compensatuviaje.tracker.feature.locationservice

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LocationTrackerImplTest {

    @Test
    fun start_guarda_trip_id_correctamente() {

        val tracker = LocationTrackerImpl()

        tracker.start("TRIP_001")

        assertThat(
            tracker.currentTripId()
        ).isEqualTo("TRIP_001")
    }

    @Test
    fun stop_limpia_trip_id() {

        val tracker = LocationTrackerImpl()

        tracker.start("TRIP_001")

        tracker.stop()

        assertThat(
            tracker.currentTripId()
        ).isNull()
    }
}