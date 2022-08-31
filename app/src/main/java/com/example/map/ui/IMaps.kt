package com.example.map.ui

import com.example.map.retrofit.entities.PlaceModel
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds

interface IMaps {
    fun onGetListLatLng(
        listLatLng: ArrayList<LatLng>,
        distance: String,
        duration: String,
        latLngBounds: LatLngBounds
    )
    fun onGetPlaces(listPlace: List<PlaceModel>)
}