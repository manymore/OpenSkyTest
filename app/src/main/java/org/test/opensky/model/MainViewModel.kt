package org.test.opensky.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.*
import org.test.opensky.service.MainRepository
import java.util.*

class MainViewModel constructor(private val mainRepository: MainRepository) : ViewModel() {

    val errorMessage = MutableLiveData<String>()

    val liveFlightData = MutableLiveData<ArrayList<FlightDataRecordDecoded>>()
    val flightData: ArrayList<FlightDataRecordDecoded> = arrayListOf()

    lateinit var openSkyDescoded: FlightDataRecordDecoded

    var job: Job? = null
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        onError("Exception handled: ${throwable.localizedMessage}")
    }
    val loading = MutableLiveData<Boolean>()
    val recordCalendar: Calendar = Calendar.getInstance()

    fun getOpenskyData() {
        job = CoroutineScope(Dispatchers.IO + exceptionHandler).launch {
            while (isActive) {
                loading.postValue(true)
                val response = mainRepository.getFlightData()
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val timeSeconds = response.body()?.time as Int

                        recordCalendar.setTimeInMillis(timeSeconds.toLong() * 1000)

                        val flightData: FlightDataRaw = response.body() as FlightDataRaw

                        this@MainViewModel.flightData.clear()
                        flightData?.states?.forEach { record ->
                            this@MainViewModel.flightData.add(processFlightDataRecord(record))
                        }
                        liveFlightData.postValue(this@MainViewModel.flightData)

                        loading.value = false
                    } else {
                        onError("Error : ${response.message()} ")
                    }
                }
                delay(10_000)
            }
        }
    }

    private fun onError(message: String) {
        errorMessage.postValue(message)
        loading.postValue(false)
    }

    override fun onCleared() {
        super.onCleared()
        job?.cancel()
    }

    fun processFlightDataRecord(record: Array<Any>): FlightDataRecordDecoded {
        var sensorICAO: String? = null
        var callSign: String? = null
        var country: String? = null
        var longitude: Double? = null
        var latitude: Double? = null
        var altitude: Double? = null
        var onGround: Boolean? = null
        var trueTrack: Float? = null

        if (record.size > 0) record[0]?.let { sensorICAO = it.toString() }
        if (record.size > 1) record[1]?.let { callSign = it.toString() }
        if (record.size > 2) record[2]?.let { country = it.toString() }
        if (record.size > 5) record[5]?.let {
            longitude = it.toString().toDoubleOrNull()
        }
        if (record.size > 6) record[6]?.let {
            latitude = it.toString().toDoubleOrNull()
        }
        if (record.size > 7) record[7]?.let {
            altitude = it.toString().toDoubleOrNull()
        }
        if (record.size > 8) record[8]?.let {
            onGround = it.toString().toBooleanStrictOrNull()
        }
        if (record.size > 10) record[10]?.let {
            trueTrack = it.toString().toFloatOrNull()
        }

        openSkyDescoded = FlightDataRecordDecoded(
            recordCalendar.time,
            sensorICAO,
            callSign,
            country,
            longitude,
            latitude,
            altitude,
            onGround,
            trueTrack
        )

        return openSkyDescoded
    }
}
