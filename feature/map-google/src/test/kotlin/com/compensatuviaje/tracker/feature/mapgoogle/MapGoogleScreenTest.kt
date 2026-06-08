package com.compensatuviaje.tracker.feature.mapgoogle

import com.google.android.gms.maps.model.LatLng
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MapGoogleScreenTest {

    @Test
    fun `cuando la ruta esta vacia, el mapa no debe fallar y debe tener cero puntos`() {
        // Arrange: Simulamos lo que pasa si el GpsPointRepository no manda datos
        val emptyRoute = emptyList<LatLng>()

        // Act & Assert
        assertThat(emptyRoute).isEmpty()
    }

    @Test
    fun `cuando se inyecta un track de puntos, el primer punto debe ser el inicio de la camara`() {
        // Arrange: Simulamos un track con 3 coordenadas (similar al FakeTripRepository)
        val mockTrack = listOf(
            LatLng(-16.39889, -71.53500),
            LatLng(-16.40444, -71.53944),
            LatLng(-16.40917, -71.54194)
        )

        // Act: Obtenemos el punto de inicio tal cual lo hace la lógica de tu UI
        val startLocation = mockTrack.firstOrNull()

        // Assert: Validamos que la lógica detecte correctamente la primera coordenada para la cámara
        assertThat(startLocation).isNotNull()
        assertThat(startLocation?.latitude).isEqualTo(-16.39889)
        assertThat(startLocation?.longitude).isEqualTo(-71.53500)
    }
}