package com.compensatuviaje.tracker.feature.apiclient

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

internal interface RetrofitApiService {

    @POST("auth/login")
    suspend fun login(
        @Body body: LoginRequest,
    ): Response<LoginResponse>

    @POST("trips/start")
    suspend fun startTrip(
        @Body body: StartTripRequest,
    ): Response<StartTripResponse>

    @POST("trips/{tripId}/sync")
    suspend fun syncBatch(
        @Path("tripId") tripId: String,
        @Body body: SyncBatchRequest,
    ): Response<SyncBatchResponse>

    @POST("trips/{tripId}/end")
    suspend fun endTrip(
        @Path("tripId") tripId: String,
        @Body body: EndTripRequest,
    ): Response<EndTripResponse>
}
