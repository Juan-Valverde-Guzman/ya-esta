package com.example.tiempo.data

import android.content.Context
import com.example.tiempo.data.network.RetrofitHelper

class Repositorio(val context: Context) {

    private val retrofit = RetrofitHelper.getRetrofit()

    suspend fun getTiempo(
        clave: String,
        latitud: Double,
        longitud: Double,
        unidades: String,
        lenguaje: String
    ) = retrofit.dameElTiempo(clave, latitud, longitud, unidades, lenguaje)
}