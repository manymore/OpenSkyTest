package org.test.opensky

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import org.test.opensky.adapter.FlightDataAdapter
import org.test.opensky.databinding.ActivityMainBinding
import org.test.opensky.model.FlightDataRecordDecoded
import org.test.opensky.model.MainViewModel
import org.test.opensky.model.MainViewModelFactory
import org.test.opensky.model.MarkerRecord
import org.test.opensky.service.MainRepository
import org.test.opensky.service.RetrofitService
import java.util.*
import kotlin.collections.HashMap

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    lateinit var viewModel: MainViewModel
    private val adapter = FlightDataAdapter(this)
    lateinit var binding: ActivityMainBinding

    private lateinit var mMap: GoogleMap

    val mapLeftTop = LatLng(51.06, 12.09)
    val mapRightBottom = LatLng(48.55, 18.87)

    val markersMap: HashMap<String, MarkerRecord> = hashMapOf()

    var showMap = false
    var mapLoadedDone = false
    var forceProcessData = false
    val selectedFlightKey = MutableLiveData<String>()
    val selectedFlightKeyMarker: Marker? = null
    var selectedFlightKeyOld: String? = null

    var latestOpenSkyDate: Date? = null

    var markerDefaultDrawable: Drawable? = null
    var markerSelectedDrawable: Drawable? = null
    var markerOnGroundDrawable: Drawable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val retrofitService = RetrofitService.getInstance()
        val mainRepository = MainRepository(retrofitService)
        binding.recyclerview.adapter = adapter

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        viewModel = ViewModelProvider(
            this,
            MainViewModelFactory(mainRepository)
        ).get(MainViewModel::class.java)

        viewModel.liveFlightData.observe(this, {
            adapter.setFlightData(it)
        })

        viewModel.errorMessage.observe(this, {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
        })

        viewModel.loading.observe(this, Observer {
            if (it) {
                binding.progressDialog.visibility = View.VISIBLE
            } else {
                binding.progressDialog.visibility = View.GONE
            }
        })

        viewModel.getOpenskyData()
    }

    override fun onResume() {
        super.onResume()
        toggleMapView(showMap)
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        savedInstanceState.putBoolean("showMap", showMap)
        savedInstanceState.putString("selectedFlightKey", selectedFlightKey.value)
        savedInstanceState.putString("selectedFlightKeyOld", selectedFlightKeyOld)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        showMap = savedInstanceState.getBoolean("showMap")
        selectedFlightKey.value = savedInstanceState.getString("selectedFlightKey")
        selectedFlightKeyOld = savedInstanceState.getString("selectedFlightKeyOld")
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.appmenu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id: Int = item.getItemId()
        if (id == R.id.showMap) {
            toggleMapView(true)
        }
        if (id == R.id.showList) {
            toggleMapView(false)
        }
        return super.onOptionsItemSelected(item)
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        markerDefaultDrawable = getDrawable(R.drawable.ic_airplane)
        markerSelectedDrawable = getDrawable(R.drawable.ic_airplane_selected)
        markerOnGroundDrawable = getDrawable(R.drawable.ic_airplane_on_ground)
        if (markerDefaultDrawable == null) {
            throw IllegalArgumentException("Cannot get airplane Default drawable")
        }
        if (markerSelectedDrawable == null) {
            throw IllegalArgumentException("Cannot get airplane Seleced drawable")
        }
        if (markerOnGroundDrawable == null) {
            throw IllegalArgumentException("Cannot get airplane OnGround drawable")
        }

        val builder = LatLngBounds.Builder()
        builder.include(mapLeftTop)
        builder.include(mapRightBottom)

        mMap.setOnMapLoadedCallback {
            // observe "selected flight" changes
            selectedFlightKey.observe(this) {
                selectedFlightKeyMarker?.remove()
                // remove "old" selected flight marker
                selectedFlightKeyOld?.let {
                    markersMap.get(it)?.marker()?.remove()
                    markersMap.remove(it)
                }
                // redraw map after "selected flight" change
                viewModel.liveFlightData.value?.let { old ->
                    mapProcessFlightData(
                        markerDefaultDrawable!!,
                        markerSelectedDrawable!!,
                        markerOnGroundDrawable!!,
                        old
                    )
                    // remember new "selected flight"
                    selectedFlightKeyOld = it
                }
            }

            // observe live flight data
            viewModel.liveFlightData.observe(this) {
                mapProcessFlightData(
                    markerDefaultDrawable!!,
                    markerSelectedDrawable!!,
                    markerOnGroundDrawable!!,
                    it,
                )
            }
            // set up map
            mMap.animateCamera(
                CameraUpdateFactory.newLatLngBounds(
                    builder.build(),
                    32
                )
            )
            mapLoadedDone = true
        }
    }

    // toggle listview / map
    fun toggleMapView(map: Boolean, key: String? = selectedFlightKey.value) {
        selectedFlightKey.value = key
        if (map) {
            binding.recyclerview.visibility = View.GONE
            binding.map.visibility = View.VISIBLE
            showMap = true
            if (mapLoadedDone) {
                forceProcessData = true
                viewModel.liveFlightData.value?.let { old ->
                    mapProcessFlightData(
                        markerDefaultDrawable!!,
                        markerSelectedDrawable!!,
                        markerOnGroundDrawable!!,
                        old
                    )
                }

            }
        } else {
            binding.recyclerview.visibility = View.VISIBLE
            binding.map.visibility = View.GONE
            showMap = false
            selectedFlightKey.value = null
        }
    }

    // process flight data
    private fun mapProcessFlightData(
        drawableDefault: Drawable,
        drawableSelected: Drawable,
        drawableOnGround: Drawable,
        flightData: List<FlightDataRecordDecoded>
    ) {
        val current = flightData[0].date()
        latestOpenSkyDate?.let {
            // Sometimes an earlier request returns data later than a later request returns its data
            if (!forceProcessData && !current.after(latestOpenSkyDate)) {
                return
            }
            forceProcessData = false
        }
        latestOpenSkyDate = current

        // sort by 'hasKey()' to selected flight marker be drawn as last ... I hope
        flightData.sortedBy { it.hasKey(selectedFlightKey.value) }.forEach { flight ->
            val flightKey = flight.key()
            // select appropriate marker drawable
            if (flight.latitude() != null && flight.longitude() != null) {
                val drawable = if (flight.onGround()) {
                    drawableOnGround
                } else if (selectedFlightKey == null) {
                    drawableDefault
                } else if (flight.key() == selectedFlightKey.value) {
                    drawableSelected
                } else {
                    drawableDefault
                }

                val existingRecord = markersMap.get(flightKey)
                val existingMarker = existingRecord?.marker()
                if (existingMarker != null) {
                    existingMarker.position = LatLng(flight.latitude()!!, flight.longitude()!!)
                    existingMarker.rotation = flight.bearig()
                    existingMarker.setIcon(getBitmapDescriptor(drawable))
                    markersMap.put(
                        flightKey,
                        MarkerRecord(current, flight.onGround(), existingMarker)
                    )
                } else {
                    val newMarker = mMap.addMarker(
                        MarkerOptions().position(LatLng(flight.latitude()!!, flight.longitude()!!))
                            .title("ICAO:${flight.icao24()} callSign:${flight.callSign()} country:${flight.country()}")
                            .icon(getBitmapDescriptor(drawable))
                            .rotation(flight.bearig())
                    )
                    newMarker?.let {
                        markersMap.put(
                            flightKey,
                            MarkerRecord(current, flight.onGround(), newMarker)
                        )
                    }
                }
            }
        }
        // remove "old" flight data (flight records are no longer included in the latest OpenSky list)
        markersMap.filter { it.value.date().before(current) }.forEach {
            it.value.marker().remove()
            markersMap.remove(it.key)
        }
    }

    private fun getBitmapDescriptor(vectorDrawable: Drawable): BitmapDescriptor? {
        val h = 48
        val w = 48
        vectorDrawable.setBounds(0, 0, w, h)
        val bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bm)
        vectorDrawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bm)
    }
}
