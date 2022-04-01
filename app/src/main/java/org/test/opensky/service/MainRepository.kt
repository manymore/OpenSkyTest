package org.test.opensky.service

class MainRepository constructor(private val retrofitService: RetrofitService) {

    suspend fun getFlightData() = retrofitService.getFlightData()
}