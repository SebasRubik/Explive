package com.example.explive

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.explive.databinding.ActivityMapasitioBinding
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.Priority
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.tasks.Task
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.util.GeoPoint
import java.io.IOException

class Mapasitio : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private lateinit var mLocationCallback: LocationCallback
    private lateinit var mLocationRequest: LocationRequest

    private var userLocationMarker: Marker? = null

    private var pos = LatLng(4.6284875, -74.0646645)

    private var settingsOK = false

    lateinit var roadManager: RoadManager

    private lateinit var sensorManager: SensorManager
    private lateinit var lightSensor: Sensor
    private lateinit var lightSensorListener: SensorEventListener

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapasitioBinding

    private val getLocationSettings = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        Log.i("LOCATION",
            "Result from settings: ${result.resultCode}")
        if(result.resultCode == RESULT_OK){
            settingsOK = true
            startLocationUpdates()
        }else {

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)!!

        binding = ActivityMapasitioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val btnirSitio = findViewById<Button>(R.id.sitiocomprabtn)

        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                0
            )
            return
        }


        mLocationRequest = createLocationRequest()
        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                for (location in locationResult.locations) {
                    if (location != null) {
                        if (this@Mapasitio::mMap.isInitialized) {
                            try {
                                val newPos = LatLng(location.latitude, location.longitude)
                                Log.d("LOCATION_UPDATE", "Updating position to: $newPos")
                                updateMarker(newPos)
                            } catch (e: Resources.NotFoundException) {
                                Log.e("MAP_STYLE", "Can't find style. Error: ", e)
                            }
                        }
                    }
                }
            }

        }



        btnirSitio.setOnClickListener {
            val intent = Intent(this, LinkConcierto::class.java)
            startActivity(intent)
        }

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        mFusedLocationClient.lastLocation.addOnSuccessListener(this) { location ->
            Log.i("LOCATION",
                "onSuccess location")
            if (location != null) {
                pos = LatLng(location.latitude, location.longitude)
            }
        }

        roadManager = OSRMRoadManager(this, "ANDROID")
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)


        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        startLocationUpdates()

        checkLocationSettings()

        mFusedLocationClient.lastLocation.addOnSuccessListener(this) { location ->
            Log.i("LOCATION",
                "onSuccess location")
            if (location != null) {
                Log.i("LOCATION", "Longitud: " + location.longitude)
                Log.i("LOCATION", "Latitud: " + location.latitude)
            }
        }


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.


        lightSensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                // Only attempt to change map style if mMap has been initialized
                if (this@Mapasitio::mMap.isInitialized) {
                    try {
                        val styleId = if (event.values[0] < 5000) R.raw.dark else R.raw.light
                        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this@Mapasitio, styleId))
                    } catch (e: Resources.NotFoundException) {
                        Log.e("MAP_STYLE", "Can't find style. Error: ", e)
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
        }


        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onResume() {
        super.onResume()
        if (this::mMap.isInitialized) {
            sensorManager.registerListener(lightSensorListener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }


    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
        if (this::mMap.isInitialized) {
            sensorManager.unregisterListener(lightSensorListener)
        }
    }
    private fun stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback)
    }

    private fun updateMarker(newPos: LatLng) {
        Log.d("LOCATION_UPDATE", "Updating position to: $newPos")
        if (this::mMap.isInitialized) {
            pos = newPos // Update global position
            if (userLocationMarker == null) {
                userLocationMarker = mMap.addMarker(MarkerOptions().position(newPos).title("Ubicación Actual"))
            } else {
                userLocationMarker!!.position = newPos
            }
        }
    }






    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */


    private fun createLocationRequest(): LocationRequest =
        // New builder
        LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY,10000).apply {
            setMinUpdateIntervalMillis(5000)
        }.build()

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {

            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null)
        }
    }

    private fun checkLocationSettings() {
        val builder = LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest)
        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())
        task.addOnSuccessListener { locationSettingsResponse ->
            Log.i("LOCATION", "GPS is ON")

            settingsOK = true
            startLocationUpdates()
        }

        task.addOnFailureListener { e ->
            if ((e as ApiException).statusCode == CommonStatusCodes.RESOLUTION_REQUIRED) {
                val resolvable = e as ResolvableApiException
                val isr = IntentSenderRequest.Builder(resolvable.resolution).build()
                getLocationSettings.launch(isr)
            }
        }
    }

    fun drawRouteCurrentlocationToTarget(CurrentLocation: LatLng, Target: LatLng){

        val roadManager : RoadManager = OSRMRoadManager(this,"ANDROID")

        val start = GeoPoint(CurrentLocation.latitude, CurrentLocation.longitude)
        val end = GeoPoint(Target.latitude, Target.longitude)

        val waypoints = ArrayList<GeoPoint>()
        waypoints.add(start)
        waypoints.add(end)

        val road = roadManager.getRoad(waypoints)

        val latLNGRoute = road.mRouteHigh.map {LatLng(it.latitude, it.longitude)}


        val polylineoptions = PolylineOptions().addAll(latLNGRoute).color(Color.RED).width(10f)
        mMap.addPolyline(polylineoptions)
    }

    override fun onMapReady(googleMap: GoogleMap) {



        pos = userLocationMarker?.position ?: pos
        Log.d("LOCATION_RECIEVED", "Position is: $pos")

        mMap = googleMap

        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.uiSettings.isZoomGesturesEnabled = true

        val lugar = intent.getStringExtra("lugar") ?: return
        val ciudad = intent.getStringExtra("ciudad") ?: return
        sensorManager.registerListener(lightSensorListener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)

        if (pos.latitude != 0.0 && pos.longitude != 0.0) {
            updateMarker(pos)
        }

        val mGeocoder = Geocoder(this)
        try {
            val searchString = "$lugar, $ciudad"
            val addresses = mGeocoder.getFromLocationName(searchString, 2)
            if (addresses != null && addresses.size > 0) {
                val address = addresses[0]
                val latLng = LatLng(address.latitude, address.longitude)

                mMap.addMarker(MarkerOptions().position(latLng).title(lugar))
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))

                    drawRouteCurrentlocationToTarget(pos, latLng)


            } else {
                Toast.makeText(this, "Ubicación no encontrada", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}