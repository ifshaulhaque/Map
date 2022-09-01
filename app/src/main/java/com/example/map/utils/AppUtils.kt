package com.example.map.utils

import android.content.Context
import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog
import com.example.map.R
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
}