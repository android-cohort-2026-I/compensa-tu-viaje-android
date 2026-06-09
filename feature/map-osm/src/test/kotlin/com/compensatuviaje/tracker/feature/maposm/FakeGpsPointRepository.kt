package com.compensatuviaje.tracker.feature.maposm

import com.compensatuviaje.tracker.domain.GpsPointRepository
import com.compensatuviaje.tracker.model.GpsPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeGpsPointRepository : GpsPointRepository {

    private val _points = MutableStateFlow<List<GpsPoint>>(emptyList())

    fun setPoints(points: List<GpsPoint>) {
        _points.value = points
    }

    override fun pointsForTrip(tripId: String): Flow<List<GpsPoint>> = _points

    override suspend fun insert(point: GpsPoint) = Unit
    override suspend fun unsynced(tripId: String): List<GpsPoint> = emptyList()
    override suspend fun markSynced(ids: List<Long>) = Unit
}