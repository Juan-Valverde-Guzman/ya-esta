package com.example.tiempo.ui

import android.os.Bundle
import com.example.tiempo.databinding.ActivityMainBinding
import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.tiempo.R
import com.example.tiempo.data.models.tiempo
import com.example.tiempo.databinding.FragmentTiempoBinding
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Tiempo : Fragment() {

    private var _binding: FragmentTiempoBinding? = null

    private val binding get() = _binding!!

    private lateinit var locationClient: FusedLocationProviderClient

    private lateinit var locationRequest: LocationRequest

    private val viewModel by activityViewModels<ViewModel> {
        ViewModel.MyViewModelFactory(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentTiempoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        locationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        pedirPermisosUbicacion()

        viewModel.getTiempo("3ce1a70f037beb728094dac99524ab84", 44.34, 10.99, "metric", "es")

        /*viewModel.getTiempo((locationClient.lastLocation.addOnSuccessListener {
            it.latitude
        }), (locationClient.lastLocation.addOnSuccessListener {
            it.longitude
        }), "metric", "es")*/

        viewModel.liveData.observe(viewLifecycleOwner) {
            binding.ciudad.text = it?.name
            binding.grados.text = it?.main?.temp.toString()
            binding.parriba.text = it?.main?.temp_max.toString()
            binding.pabajo.text = it?.main?.temp_min.toString()
            binding.nubes.text = it?.clouds?.all.toString() + "%"
            if (it?.rain == null) {
                binding.agua.text = "0" + "mm"
            } else {
                binding.agua.text = it?.rain.`1h`.toString() + "mm"
            }
            if (it?.snow == null) {
                binding.nieve.text = "0" + "mm"
            } else {
                binding.nieve.text = it?.snow.`1h`.toString() + "mm"
            }
            binding.tiempoen.text = it?.weather?.toString()
            binding.tiempoes.text = it?.weather?.toString()
            binding.sensaciontermica.text = it?.main?.feels_like.toString() + "º"
            binding.presion.text = it?.main?.pressure.toString() + " hPa"
            binding.humedad.text = it?.main?.humidity.toString() + "%"
            binding.niveldelmar.text = it?.main?.sea_level.toString() + " hPa"
            binding.nivelterra.text = it?.main?.grnd_level.toString() + " hPa"
            binding.viento.text = it?.wind?.speed.toString() + " m/s - " + it?.wind?.deg.toString() + "º"
            //pasar de metros a km
            binding.visibilidad.text = (it?.visibility?.div(1000) ?: Int).toString() + " km"
            //pasar de milisegundos a horas:minutos
            val sdf = SimpleDateFormat("HH:mm")

            binding.amanecer.text = sdf.format(it?.sys?.sunrise)
            //igual
            binding.atardecer.text = sdf.format(it?.sys?.sunset)
            //url="https://openweathermap.org/img/wn/it.weather.icon.png
        }
    }


    /**
     * Función para obtener la última ubicación conocida
     */
    private fun mostrarUbicacion() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        locationClient.lastLocation.addOnSuccessListener {
            imprimirUbicacion(it)
        }

    }

    private fun configurarUbicacion() {

        locationRequest = LocationRequest.Builder(5000L).apply {
            setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            setMinUpdateDistanceMeters(10F)
        }.build()
        //aqui cambia tomar por comprobar y hace noseque
        comprobarConfiguracion(locationRequest)
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(p0: LocationResult) {
            super.onLocationResult(p0)
            p0.lastLocation?.let {imprimirUbicacion(it)}
        }
    }

    /**
     * Función para comprobar si los ajustes del dispositivo coinciden con los parámetros de ubicación
     */
    private fun comprobarConfiguracion(locationRequest: LocationRequest) {
        val locationSettingsBuilder =
            LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        val task = settingsClient.checkLocationSettings(locationSettingsBuilder.build())
        task.addOnSuccessListener {
            tomarUbicacionEnDirecto()
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    val intentSenderRequest =
                        IntentSenderRequest.Builder(exception.resolution).build()
                    resultadoConfiguracion.launch(intentSenderRequest)
                } catch (throwable: Throwable) {
                    // Ignore the error.
                }
            }
        }
    }

    private val resultadoConfiguracion =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
            if (it.resultCode == AppCompatActivity.RESULT_OK) {
                tomarUbicacionEnDirecto()
            } else {
                // No se puede tomar la ubicación con la configuración actual
                Toast.makeText(
                    requireContext(),
                    "No se puede obtener la ubicación con los ajustes actuales",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    /**
     * Función para obtener la ubicación de forma periódica
     */
    private fun tomarUbicacionEnDirecto() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        locationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }


    /**
     * Función que recibe una instancia de Location e imprime ciertos atributos de la instancia
     */
    private fun imprimirUbicacion(location: Location) {

        val latitud = location.latitude
        val longitud = location.longitude
        val altitud = location.altitude
        val precision = location.accuracy
        val proveedor = location.provider
        val bearing = location.bearing
        val timeStamp = location.time

        val sdf = SimpleDateFormat("yyyy/MM/dd - HH:mm:ss", Locale.getDefault())
        val time = sdf.format(Date(timeStamp))

        val message = """
            Latitud: $latitud, Longitud: $longitud
            Altitud: $altitud
            Accuracy: $precision
            Proveedor: $proveedor
            Orientación: $bearing
            Fecha/Hora : $time
            """
        //viewModel.getTiempo(location.latitude, location.longitude, "metric", "es")
    }

    /****************FUNCIONES PARA PEDIR PERMISO AL USUARIO *******************************/

    /**
     * función para calcular si una lista de permisos está aceptada
     */
    private fun isPermissionsGranted(list: Array<String>): Int {
        // PERMISSION_GRANTED : Constant Value: 0
        // PERMISSION_DENIED : Constant Value: -1
        var counter = 0
        for (permission in list) {
            counter += ContextCompat.checkSelfPermission(requireContext(), permission)
        }
        return counter
    }

    /**
     * Función para pedir permisos
     *  - Si ya fueron aceptados, muestra ubicación o prepara ubicación en directo según el botón pulsado
     *  - Si el usuario los rechazó una vez, se muestra un cuadro de diálogo explicativo
     *  - Si no se han aceptado los permisos, se pide autorización al usuario
     */
    @SuppressLint("SuspiciousIndentation")
    private fun pedirPermisosUbicacion() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {

                mostrarUbicacion()

        } else if (ActivityCompat.shouldShowRequestPermissionRationale(
                requireActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        ) {
            showAlertDialog()
        } else {
            somePermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    /**
     * Contrato con el resultado del usuario al solicitar permisos
     *  - si no hay ningún valor falso es que todos fueron aceptados, por lo que se llama a la función correspondiente
     *  - Si se encuentra algún valor falso, no se han aceptado todos los permisos
     */
    private val somePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (!it.containsValue(false)) {

                mostrarUbicacion()

        } else {
            // al menos un permiso no se ha aceptado
            Toast.makeText(requireContext(), "No se han aceptado los permisos", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Cuadro de diálogo para explicar de forma más extendida los permisos al usuario
     */
    private fun showAlertDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setMessage("Los permisos de ubicación permitirán ver tu ubicación en el mapa. Los creadores no almacenan esta información en ningún lugar")
        builder.setNegativeButton("Rechazar", null)
        builder.setPositiveButton("Aceptar") { _, _ ->
            somePermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
        builder.create().show()
    }

    override fun onPause() {
        super.onPause()
        locationClient.removeLocationUpdates(locationCallback)
    }

    override fun onResume() {
        super.onResume()
        if (this::locationRequest.isInitialized) {
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            locationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }
}