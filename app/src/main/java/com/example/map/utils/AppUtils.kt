package com.example.map.utils

import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.location.LocationManager
import androidx.appcompat.app.AlertDialog
import com.example.map.R
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.libraries.places.api.model.Place

class AppUtils {
    private lateinit var builder: AlertDialog.Builder

    fun alertShow(
        context: Context,
        title: String,
        alertMsg: String,
        posBtnName: String,
        ok: DialogInterface.OnClickListener,
        negBtnName: String,
        no: DialogInterface.OnClickListener
    ) {
        builder = AlertDialog.Builder(context)
        builder.setTitle(title)
        builder.setIcon(R.drawable.ic_baseline_warning_24)
        builder.setMessage(alertMsg)
        builder.setPositiveButton(posBtnName, ok)
        builder.setNegativeButton(negBtnName, no)
        builder.setCancelable(false)
        builder.create().show()
    }

    fun alertDismiss() {
        builder.create().dismiss()
    }

    fun getBound(originPlace: Place?, destinationPlace: Place?): LatLngBounds? {
        originPlace?.latLng?.let { originLatLng ->
            destinationPlace?.latLng?.let { destinationLatLng ->
                return LatLngBounds(
                    LatLng(
                        if (originLatLng.latitude < destinationLatLng.latitude) originLatLng.latitude else destinationLatLng.latitude,
                        if (originLatLng.longitude < destinationLatLng.longitude) originLatLng.longitude else destinationLatLng.longitude
                    ),
                    LatLng(
                        if (originLatLng.latitude > destinationLatLng.latitude) originLatLng.latitude else destinationLatLng.latitude,
                        if (originLatLng.longitude > destinationLatLng.longitude) originLatLng.longitude else destinationLatLng.longitude
                    )
                )
            }
        }
        return null
    }

    fun isGPSEnable(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    fun getLocationRequest(): LocationRequest {
        val mLocationRequest = LocationRequest()
        mLocationRequest.interval = 10000
        mLocationRequest.fastestInterval = 1000
        mLocationRequest.priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
        LocationSettingsRequest.Builder()
            .addLocationRequest(mLocationRequest)

        return mLocationRequest
    }

    private var innerCircle: Circle? = null
    private var outerCircle: Circle? = null

    fun createLocationCircles(mMap: GoogleMap, currentLatitude: Double, currentLongitude: Double) {
        innerCircle?.remove()
        outerCircle?.remove()
        innerCircle = mMap.addCircle(
            CircleOptions().center(LatLng(currentLatitude, currentLongitude))
                .radius(25.0)
                .fillColor(Color.argb(200, 84, 110, 122))
                .strokeColor(Color.argb(200, 84, 110, 122))
        )
        outerCircle = mMap.addCircle(
            CircleOptions().center(LatLng(currentLatitude, currentLongitude))
                .radius(250.0)
                .fillColor(Color.argb(100, 120, 144, 156))
                .strokeColor(Color.argb(200, 120, 144, 156))
        )
    }
}