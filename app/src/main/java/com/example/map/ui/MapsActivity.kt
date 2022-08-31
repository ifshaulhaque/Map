package com.example.map.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.map.AutoCompleteFragmentType
import com.example.map.Constants
import com.example.map.R
import com.example.map.databinding.ActivityMainBinding
import com.example.map.di.NetworkModule
import com.example.map.retrofit.entities.PlaceModel
import com.example.map.utils.AppUtils
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.maps.android.SphericalUtil
import com.google.maps.android.clustering.ClusterManager
import timber.log.Timber


class MapsActivity : AppCompatActivity(), OnMapReadyCallback, IMaps {

    private lateinit var center: LatLng
    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMainBinding
    private lateinit var appUtils: AppUtils
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var currentLatitude: Double = 28.679079
    private var currentLongitude: Double = 77.069710
    private lateinit var currentMarker: Marker
    private var destinationMarker: Marker? = null
    private lateinit var placesClient: PlacesClient
    private lateinit var originPlace: Place
    private lateinit var destinationPlace: Place
    private var polyline: Polyline? = null
    private lateinit var mapsPresenter: MapsPresenter
    private lateinit var clusterManager: ClusterManager<ClusterItem>

    private val requestLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            onGetLocation()
        } else {
            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                onLocationRequestPermission()
            } else {
                permissionAlert()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Timber.plant(Timber.DebugTree())

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        appUtils = AppUtils()
        onLocationRequestPermission()
        mapsPresenter = MapsPresenter(
            NetworkModule.providesDirectionAPI(
                NetworkModule.providesRetrofit()
            ),
            this
        )

        Places.initialize(applicationContext, Constants.KEY)
        placesClient = Places.createClient(this)

        setAutoCompleteFragment(AutoCompleteFragmentType.Origin)
        setAutoCompleteFragment(AutoCompleteFragmentType.Destination)

        binding.directionFab.setOnClickListener {
            if (originPlace != null && destinationPlace != null) {
                mapsPresenter.getPolyLines(
                    "${originPlace.latLng?.latitude},${originPlace.latLng?.longitude}",
                    "${destinationPlace.latLng?.latitude},${destinationPlace.latLng?.longitude}"
                )
            }
        }

        binding.polylineFab.setOnClickListener {
            if (originPlace != null && destinationPlace != null) {
                drawPolyLine()
            }
            setVisibilityGone()
        }
        binding.placesFab.setOnClickListener {
            center = SphericalUtil.interpolate(
                mMap.projection.visibleRegion.nearLeft,
                mMap.projection.visibleRegion.farRight,
                0.5
            )
            mapsPresenter.getPlaces(
                "${center.latitude},${center.longitude}",
                2000
            )
            setVisibilityGone()
        }

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun setAutoCompleteFragment(type: AutoCompleteFragmentType) {
        val autocompleteSupportFragment: AutocompleteSupportFragment =
            supportFragmentManager.findFragmentById(if (type == AutoCompleteFragmentType.Origin) R.id.start_autocompleteFragment else R.id.destination_autocompleteFragment)
                    as AutocompleteSupportFragment
        autocompleteSupportFragment.setPlaceFields(
            listOf(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.LAT_LNG
            )
        )
        autocompleteSupportFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                place.latLng?.let { latLong ->
                    mMap.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            latLong,
                            10f
                        )
                    )
                    if (type == AutoCompleteFragmentType.Origin) {
                        originPlace = place
                        currentMarker.position = latLong
                    } else {
                        destinationPlace = place
                        if (destinationMarker != null) {
                            destinationMarker?.position = latLong
                        } else {
                            destinationMarker = mMap.addMarker(
                                MarkerOptions().position(latLong).icon(
                                    BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                                )
                            )
                        }
                    }
                }
            }

            override fun onError(status: Status) {
                Timber.e("error : $status")
            }
        })
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        setUpClusterer()
        with(mMap.uiSettings) {
            isZoomControlsEnabled = true
            isCompassEnabled = true
        }
    }

    private fun onLocationRequestPermission() {
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) -> {
                onGetLocation()
            }
            else -> {
                requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun permissionAlert() {
        appUtils.alertShow(
            this,
            getString(R.string.alert),
            getString(R.string.permission_alert_msg),
            "Ok",
            { _, _ ->
                val packageName = "com.example.map"
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            },
            "Cancel",
            { _, _ ->
                appUtils.alertDismiss()
            }
        )
    }

    @SuppressLint("MissingPermission")
    private fun onGetLocation() {
        fusedLocationProviderClient.lastLocation.addOnCompleteListener { task ->
            Timber.e("Task : ${task.result}")
            if (task.result != null) {
                currentLatitude = task.result.latitude
                currentLongitude = task.result.longitude
            }
            mMap.addMarker(MarkerOptions().position(LatLng(currentLatitude, currentLongitude)))
                ?.let { marker ->
                    currentMarker = marker
                }
            mMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(
                        currentLatitude,
                        currentLongitude
                    ), 5F
                )
            )
        }
    }

    private fun setVisibilityGone() {
        binding.distanceTv.text = ""
        binding.durationTv.text = ""
    }

    override fun onGetListLatLng(
        listLatLng: ArrayList<LatLng>,
        distance: String,
        duration: String,
        latLngBounds: LatLngBounds
    ) {
        val polylineOptions = PolylineOptions().addAll(listLatLng)
        polyline?.remove()
        polyline = mMap.addPolyline(polylineOptions)
        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 100))
        binding.distanceTv.text = distance
        binding.durationTv.text = duration
    }

    override fun onGetPlaces(listPlace: List<PlaceModel>) {
        currentMarker.remove()
        for (place in listPlace) {
            clusterManager.addItem(
                ClusterItem(
                    place.geometry.location.lat,
                    place.geometry.location.lng,
                    place.name,
                    place.vicinity,
                )
            )
        }
        clusterManager.cluster()
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(center, 13f))
    }

    private fun drawPolyLine() {
        val polylineOptions = PolylineOptions()
            .add(originPlace.latLng)
            .add(destinationPlace.latLng)
            .color(Color.BLUE)
        polyline?.remove()
        polyline = mMap.addPolyline(polylineOptions)

        originPlace.latLng?.let { originLatLng ->
            destinationPlace.latLng?.let { destinationLatLng ->
                mMap.moveCamera(
                    CameraUpdateFactory.newLatLngBounds(
                        LatLngBounds(
                            LatLng(
                                if (originLatLng.latitude < destinationLatLng.latitude) originLatLng.latitude else destinationLatLng.latitude,
                                if (originLatLng.longitude < destinationLatLng.longitude) originLatLng.longitude else destinationLatLng.longitude
                            ),
                            LatLng(
                                if (originLatLng.latitude > destinationLatLng.latitude) originLatLng.latitude else destinationLatLng.latitude,
                                if (originLatLng.longitude > destinationLatLng.longitude) originLatLng.longitude else destinationLatLng.longitude
                            )
                        ),
                        100
                    )
                )
            }
        }
    }

    private fun setUpClusterer() {
        clusterManager = ClusterManager(this, mMap)
        mMap.setOnCameraIdleListener(clusterManager)
    }

}