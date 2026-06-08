package com.compensatuviaje.tracker.feature.vehicle

import com.compensatuviaje.tracker.testing.SampleData
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class VehicleScreenTest {
    @Test
    fun `content state exposes vehicle data`() {
        val state = VehicleUiState.Content(
            session = SampleData.session,
            truck = SampleData.truck,
            hasActiveTrip = false,
            isConfirming = false,
        )

        assertThat(state.truck.licensePlate).isEqualTo("XYZ-987")
        assertThat(state.truck.category).isEqualTo("heavy_duty")
    }
}
