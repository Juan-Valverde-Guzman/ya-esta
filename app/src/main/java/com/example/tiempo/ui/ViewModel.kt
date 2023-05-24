package com.example.tiempo.ui

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.tiempo.data.Repositorio
import com.example.tiempo.data.models.tiempo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ViewModel(val context: Context): ViewModel() {

    private val repository = Repositorio(context)

    val liveData = MutableLiveData<tiempo?>()

    fun getTiempo(clave: String, latitud: Double, longitud: Double, unidades: String, lenguaje: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val response = repository.getTiempo(clave, latitud, longitud, unidades, lenguaje)
            if (response.isSuccessful) {
                val miRespuesta = response.body()
                liveData.postValue(miRespuesta)
            }
        }
    }

    class MyViewModelFactory(private val context: Context): ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            return modelClass.getConstructor(Context::class.java).newInstance(context)
        }
    }
}