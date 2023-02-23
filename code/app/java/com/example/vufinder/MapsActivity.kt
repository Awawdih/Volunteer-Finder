package com.example.vufinder


import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.*
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import java.util.*


internal class MapsActivity : AppCompatActivity(), OnMapReadyCallback ,LocationListener,GoogleMap.OnCameraMoveListener,GoogleMap.OnCameraIdleListener,GoogleMap.OnCameraMoveStartedListener{
    private lateinit var mMap: GoogleMap
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private val permissionId = 2
    private lateinit var mapFragment : SupportMapFragment
    private lateinit var resultLocation:MutableMap<String,String>
    private fun FullScreencall() {
        if (Build.VERSION.SDK_INT > 11 && Build.VERSION.SDK_INT < 19) { // lower api
            val v: View = this.getWindow().getDecorView()
            v.setSystemUiVisibility(View.GONE)
        } else if (Build.VERSION.SDK_INT >= 19) {
            //for new api versions.
            val decorView: View = this.getWindow().getDecorView()
            val uiOptions: Int =
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            decorView.setSystemUiVisibility(uiOptions)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        FullScreencall()
        resultLocation = HashMap<String,String>()
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        //getLocation()
        val buttonChooseLocation = findViewById<Button>(R.id.buttonChooseLocation)
        buttonChooseLocation.setOnClickListener{
            val intent = Intent()//(this, CreateActivityFragment::class.java)
            /*intent.putExtra("longitude",mMap.cameraPosition.target.longitude.toString())
            intent.putExtra("latitude",mMap.cameraPosition.target.latitude.toString())
            intent.putExtra("cityName",mMap.cameraPosition.target.)

             */
            for (entry in resultLocation.entries.iterator()) {
                intent.putExtra(entry.key,entry.value)
            }
            setResult(RESULT_OK, intent)
            finish()
            //startActivity(intent)
        }
    }
    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }
    private fun checkPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        return false
    }
    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            permissionId
        )
    }
    @SuppressLint("MissingSuperCall")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == permissionId) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                getLocation()
            }
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap



        mMap.isMyLocationEnabled = true
        mMap.setOnCameraMoveListener(this)
        mMap.setOnCameraIdleListener(this)
        mMap.setOnCameraMoveStartedListener(this)

        getLocation()

    }

    @SuppressLint("MissingPermission", "SetTextI18n")
    private fun getLocation() {
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                mFusedLocationClient.lastLocation.addOnCompleteListener(this) { task ->
                    val location: Location? = task.result
                    if (location != null) {
                        val geocoder = Geocoder(this, Locale.getDefault())
                        val list: List<Address> =
                            geocoder.getFromLocation(location.latitude, location.longitude, 1) as List<Address>
                        val current = LatLng(list[0].latitude, list[0].longitude)
                        val latLongBoundBuilder = LatLngBounds.builder()
                        mMap.addMarker(MarkerOptions()
                            .position(current)
                            .title("Marker in current location"))
                        latLongBoundBuilder.include(current)


                        val width = resources.displayMetrics.widthPixels
                        val height = resources.displayMetrics.heightPixels
                        val padding = (width * 0.15).toInt()
                        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLongBoundBuilder.build(), width, height, padding))
                        //mMap.moveCamera(CameraUpdateFactory.newLatLng(current))
                    }
                }
            } else {
                Toast.makeText(this, "Please turn on location", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        } else {
            requestPermissions()
        }
    }
    override fun onStatusChanged(p0: String?, status: Int, extras: Bundle?) {

    }
    override fun onProviderEnabled(provider: String) {

    }
    override fun onProviderDisabled(provider: String) {

    }

    override fun onLocationChanged(location: Location) {
        val geocoder = Geocoder(this,Locale.getDefault())
        val addresses = geocoder.getFromLocation(location.latitude,location.longitude,1)
        setAddress(addresses?.get(0))
    }

    private fun setAddress(address: Address?) {
        if(address!=null)
        {
            /*
            if(address.getAddressLine(0)!=null)
            {
                resultLocation["latitude"] = address.getAddressLine(0)
            }
            if(address.getAddressLine(1)!=null)
            {
                resultLocation["longitude"] = address.getAddressLine(1)
            }
             */
            resultLocation["longitude"] = address.longitude.toString()
            resultLocation["latitude"] = address.latitude.toString()
            var cityName = address.adminArea
            if (cityName == null){
                cityName = address.locality
                if (cityName == null){
                    cityName = address.subAdminArea
                }
            }
            val countryName = address.countryName.toString()
            resultLocation["countryName"] = countryName
            if(cityName != null)
                resultLocation["cityName"] = cityName
            else
                resultLocation["cityName"] = "cityName"
        }
    }

    override fun onCameraMove() {

    }

    override fun onCameraIdle() {
        val geocoder = Geocoder(this,Locale.getDefault())
        val addresses = geocoder.getFromLocation(mMap.cameraPosition.target.latitude,mMap.cameraPosition.target.longitude,1)
        if(addresses!=null && addresses.size > 0)
            setAddress(addresses?.get(0))
    }

    override fun onCameraMoveStarted(p0: Int) {

    }
}
