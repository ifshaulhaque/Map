package com.example.map.ui

import com.example.map.Constants
import com.example.map.retrofit.IMapApi
import com.example.map.retrofit.entities.DirectionModel
import com.example.map.retrofit.entities.PlaceListModel
import com.example.map.retrofit.entities.PlaceModel
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.PolyUtil
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MapsPresenter(private val iMapApi: IMapApi, private val iMaps: IMaps) {
    private val listLatLng = arrayListOf<LatLng>()
    private val listPlaces = arrayListOf<PlaceModel>()

    fun getPlaces(location: String, radius: Int) {
        iMapApi.getPlaces(
            location,
            radius,
            Constants.KEY
        ).enqueue(
            object : Callback<PlaceListModel> {
                override fun onResponse(
                    call: Call<PlaceListModel>,
                    response: Response<PlaceListModel>
                ) {
                    listPlaces.clear()
                    response.body()?.let {
                        listPlaces.addAll(it.results)
                        if (it.next_page_token != null) {
                            getPlacesNextPage(
                                it.next_page_token,
                            )
                        } else {
                            iMaps.onGetPlaces(listPlaces)
                        }
                    }
                }

                override fun onFailure(call: Call<PlaceListModel>, t: Throwable) {

                }

            }
        )
    }

    fun getPolyLines(origin: String, destination: String) {
        var distance = ""
        var duration = ""
        listLatLng.clear()
        iMapApi.getDirection(
            origin,
            destination,
            Constants.KEY
        ).enqueue(
            object : Callback<DirectionModel> {
                override fun onResponse(
                    call: Call<DirectionModel>,
                    response: Response<DirectionModel>
                ) {
                    response.body()?.routes?.let { routes ->
                        for (route in routes) {
                            for (leg in route.legs) {
                                distance = leg.distance.text
                                duration = leg.duration.text
                                for (step in leg.steps) {
                                    for (latLng in PolyUtil.decode(step.polyline.points)) {
                                        listLatLng.add(latLng)
                                    }
                                }
                            }
                        }
                        if (routes.isNotEmpty()) {
                            iMaps.onGetPath(
                                listLatLng,
                                distance,
                                duration,
                                LatLngBounds(
                                    LatLng(
                                        routes[0].bounds.southwest.lat,
                                        routes[0].bounds.southwest.lng,
                                    ),
                                    LatLng(
                                        routes[0].bounds.northeast.lat,
                                        routes[0].bounds.northeast.lng,
                                    )
                                )
                            )
                        }
                    }
                }

                override fun onFailure(call: Call<DirectionModel>, t: Throwable) {

                }
            }
        )
    }

    private fun getPlacesNextPage (
        token: String,
    ) {
        iMapApi.getPlacesNextPage(
            token,
            Constants.KEY
        ).enqueue(
            object : Callback<PlaceListModel> {
                override fun onResponse(
                    call: Call<PlaceListModel>,
                    response: Response<PlaceListModel>
                ) {
                    response.body()?.let {
                        listPlaces.addAll(it.results)
                        if (it.next_page_token != null) {
                            getPlacesNextPage(it.next_page_token)
                        } else {
                            iMaps.onGetPlaces(listPlaces)
                        }
                    }
                }

                override fun onFailure(call: Call<PlaceListModel>, t: Throwable) {

                }

            }
        )
    }
}