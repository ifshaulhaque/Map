package com.example.map.retrofit

import com.example.map.retrofit.entities.DirectionModel
import com.example.map.retrofit.entities.PlaceListModel
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface IMapApi {
    @GET("directions/json")
    fun getDirection(
        @Query("origin") originLatLong: String,
        @Query("destination") destinationLatLong: String,
        @Query("key") key: String
    ) : Call<DirectionModel>

    @GET("place/nearbysearch/json")
    fun getPlaces(
        @Query("location") location: String,
        @Query("radius") radius: Int,
        @Query("key") key: String
    ) : Call<PlaceListModel>

    @GET("place/nearbysearch/json")
    fun getPlacesNextPage(
        @Query("pagetoken") token: String,
        @Query("key") key: String
    ) : Call<PlaceListModel>
}