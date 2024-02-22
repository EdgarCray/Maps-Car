package com.edgar.mapscar

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.edgar.mapscar.databinding.ActivityMapsBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {
    private val REFERENCIA: String ="coordenadas"
    private lateinit var database:FirebaseDatabase
    private lateinit var dbRef: DatabaseReference
    private lateinit var marcadorActual: Marker;
    private val GPS_REQUEST_CODE: Int= 1
    private val MY_PERMISSION_REQUEST_GPS: Int = 99
    private lateinit var mMap: GoogleMap
    //Variables para la ubicacion en tiempo real
    lateinit var locationCallBack: LocationCallback
    lateinit var lr: com.google.android.gms.location.LocationRequest

    lateinit var fusedLocationClient: FusedLocationProviderClient
    var gpsIniciado:Boolean =false

    private lateinit var binding: ActivityMapsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)



        database=FirebaseDatabase.getInstance()
        dbRef = database.getReference(REFERENCIA)

        lr = createLocationRequest()
        //obtener ubicacion periodicamente
        locationCallBack = object :LocationCallback(){

        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun createLocationRequest(): com.google.android.gms.location.LocationRequest {
        //declarar variables
        //funcion lambda para preguntar que no venga vacio con el ?
        val locationRequest = com.google.android.gms.location.LocationRequest.create()?.apply {
            interval = 2000
            // tasa mas rapida de tranferencia que actualiza
            fastestInterval = 1000
            //solicitar la ubicacion mas precisa posible
            priority = com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        // signos de admiracion en caso de que sea algun nulo
        return locationRequest!!
    }
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        // Add a marker in Sydney and move the camera
        val sydney = LatLng(-34.0, 151.0)
        marcadorActual = mMap.addMarker(MarkerOptions().position(sydney).title("Posicion actual"))!!
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))
        mMap.uiSettings.isZoomControlsEnabled = true
        solicitarPermisosGPS()
    }
    //funcion para mostrara ultima ubicacion del gps
    @SuppressLint("MissingPermission")
    private fun activarGPS(){
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.lastLocation.addOnSuccessListener {
            if(it != null){
                actualizarMarcador(it.latitude, it.longitude)
            }
        }
        startLocationUpdates()
        gpsIniciado=true
    }
    private fun actualizarMarcador(latitud :Double,Longitud:Double){
        marcadorActual.position = LatLng(latitud, Longitud)
        mMap.moveCamera(CameraUpdateFactory.newLatLng(marcadorActual.position))
        mMap.animateCamera(CameraUpdateFactory.zoomTo(16.0f))

        dbRef.child("latitud").setValue(latitud)
        dbRef.child("longitud").setValue(Longitud)

    }
    private fun solicitarPermisosGPS(){
        if(Build.VERSION.SDK_INT >- Build.VERSION_CODES.M){
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
                if(ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION)){
                    mostrarMensaje("Necesario para usar la aplicación con el Mapa")
                }
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                    MY_PERMISSION_REQUEST_GPS)
            }else{
                if(! gpsEncendido()){
                    mostrarDialogoAxtivarGps()
                }else{
                    activarGPS()
                }
            }
        }else{
            if(! gpsEncendido()){
                mostrarDialogoAxtivarGps()
            }else{
                activarGPS()
            }
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when(requestCode){
            MY_PERMISSION_REQUEST_GPS->{
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    if(! gpsEncendido()){
                        mostrarDialogoAxtivarGps()
                    }else{
                        activarGPS()
                    }
                    mostrarMensaje("Permiso concedido")
                }else{
                    mostrarMensaje("Permisos requerido")
                    ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), MY_PERMISSION_REQUEST_GPS)
                }
            }
            else->{
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }
        }
    }
    private fun mostrarMensaje(mensaje: String) {
        Toast.makeText(applicationContext, mensaje, Toast.LENGTH_LONG).show()
    }
    fun gpsEncendido():Boolean{
        var manager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return manager.isProviderEnabled(LocationManager.GPS_PROVIDER)

    }
    fun mostrarDialogoAxtivarGps(){
        var alertDialog: AlertDialog = this.let {
            val builder = AlertDialog.Builder(it)
            builder.apply {
                setTitle("Activar GPS")
                setMessage("Es necesario que el gps este activado")
                setPositiveButton("Aceptar",
                    DialogInterface.OnClickListener { dialog, id ->
                        startActivityForResult(
                            Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS),
                            GPS_REQUEST_CODE
                        )
                    }
                )
                setNegativeButton("Cancelar",
                    DialogInterface.OnClickListener{dialog, id ->
                        dialog.cancel()
                        mostrarDialogoAxtivarGps()
                    })
            }
            builder.create()
        }
        alertDialog.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == GPS_REQUEST_CODE && gpsEncendido()){
            activarGPS()
        }else{
            mostrarDialogoAxtivarGps()
        }
    }

    //funcion para no generar muchos recursos
    override fun onRestart() {
        super.onRestart()
        if (gpsIniciado){
            startLocationUpdates()

        }
    }

    override fun onPause() {
        super.onPause()
        if(gpsIniciado){
            startLocationUpdates()

        }
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates(){
        fusedLocationClient.requestLocationUpdates(lr,locationCallBack,null)
    }
    fun stopLocationUpdates(){
        fusedLocationClient.removeLocationUpdates(locationCallBack)

        dbRef.removeValue()
    }
}

