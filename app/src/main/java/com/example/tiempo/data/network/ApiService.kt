package com.example.tiempo.data.network

import com.example.tiempo.data.models.tiempo
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {

    @GET("weather")
    suspend fun dameElTiempo(
        @Query("appid") clave: String,
        @Query("lat") latitud: Double,
        @Query("lon") longitud: Double,
        @Query("units") unidades: String,
        @Query("lang") lenguaje: String
    ): Response<tiempo>
}