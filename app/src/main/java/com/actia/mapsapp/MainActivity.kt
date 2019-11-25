package com.actia.mapsapp

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.compat.GeoDataClient
import com.google.android.libraries.places.compat.PlaceDetectionClient
import com.google.android.libraries.places.compat.Places
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.alert
import org.jetbrains.anko.noButton
import org.jetbrains.anko.yesButton

/**
 * An activity that displays a map showing the place at the device's current location.
 */
class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private var mMap: GoogleMap? = null
    private var mCameraPosition: CameraPosition? = null

    // The entry points to the Places API.
    private var mGeoDataClient: GeoDataClient? = null
    private var mPlaceDetectionClient: PlaceDetectionClient? = null

    // The entry point to the Fused Location Provider.
    private var mFusedLocationProviderClient: FusedLocationProviderClient? = null

    // A default location (Sydney, Australia) and default zoom to use when location permission is
    // not granted.
    private val mDefaultLocation = LatLng(-33.8523341, 151.2106085)
    private var mLocationPermissionGranted: Boolean = false

    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.
    private var mLastKnownLocation: Location? = null
    private var mLikelyPlaceNames: Array<String?>? = null
    private var mLikelyPlaceAddresses: Array<String?>? = null
    private var mLikelyPlaceAttributions: Array<String?>? = null
    private var mLikelyPlaceLatLngs: Array<LatLng?>? = null

    private var mConfigLatitude : Double = 0.0
    private var mConfigLongitude : Double = 0.0
    private var mRayon : Int =0
    private var currentPosMarker : Marker? =null
    private var currentMarker : Marker? =null
    private var currentCircle : Circle? =null
    var inCircle = false

    var mLocManager : LocationManager? = null
    var mLocListener : android.location.LocationListener? = null

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!isLocationEnabled(this)) {
            showLocationIsDisabledAlert()
        } else {
            // Retrieve location and camera position from saved instance state.
            if (savedInstanceState != null) {
                mLastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION)
                mCameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION)
            }

            // Retrieve the content view that renders the map.
            setContentView(R.layout.activity_main)

            // Construct a GeoDataClient.
            mGeoDataClient = Places.getGeoDataClient(this)

            // Construct a PlaceDetectionClient.
            mPlaceDetectionClient = Places.getPlaceDetectionClient(this)

            // Construct a FusedLocationProviderClient.
            mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

            val intent = intent
            mRayon = intent.getIntExtra("rayon", 0)
            inCircle = intent.getBooleanExtra("state", false)
            mConfigLatitude = intent.getDoubleExtra("lat", 0.0)
            mConfigLongitude = intent.getDoubleExtra("lng", 0.0)

            // Build the map.
            val mapFragment = supportFragmentManager
                    .findFragmentById(R.id.mapFragment) as SupportMapFragment?
            mapFragment!!.getMapAsync(this)

            btnConfig.setOnClickListener {
                val myIntent = Intent(this, ConfigActivity::class.java)
                myIntent.putExtra("lat", mConfigLatitude)
                myIntent.putExtra("lng", mConfigLongitude)
                myIntent.putExtra("state", inCircle)
                this.startActivity(myIntent)
            }

            mLocManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            mLocListener = Speed()
            try {
                // Request location updates
                mLocManager?.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0.0f, mLocListener!!)
            } catch (ex: SecurityException) {
                Log.d("myTag", "Security Exception, no location available")
            }
        }
    }



    /**
     * Saves the state of the map when the activity is paused.
     */
    override fun onSaveInstanceState(outState: Bundle) {
        if (mMap != null) {
            outState.putParcelable(KEY_CAMERA_POSITION, mMap!!.cameraPosition)
            outState.putParcelable(KEY_LOCATION, mLastKnownLocation)
            super.onSaveInstanceState(outState)
        }
    }

    private fun checkInRange(newPos : LatLng){
        if(mConfigLatitude != 0.0 && mConfigLongitude != 0.0 && mRayon != 0){

            val carPosition = LatLng(newPos.latitude,newPos.longitude)
            val zonePosition = LatLng(mConfigLatitude,mConfigLongitude)
            if(getDistance(carPosition,zonePosition)<mRayon){
                if(!inCircle){
                    inCircle  = true
                    Toast.makeText(this,"You have entered the zone !!",Toast.LENGTH_SHORT).show()
                }
            }else{
                if(inCircle){
                    inCircle  = false
                    Toast.makeText(this,"You have exited the zone !!",Toast.LENGTH_SHORT).show()
                }
            }

        }
    }

    private fun getDistance(latLngA :LatLng, latLngB :LatLng ) : Float {
        val locationA  = Location("point A")
        locationA.latitude = latLngA.latitude
        locationA.longitude = latLngA.longitude
        val locationB  = Location("point B")
        locationB.latitude = latLngB.latitude
        locationB.longitude = latLngB.longitude
        val distance = locationA.distanceTo(locationB)
        return distance
    }

    /**
     * Manipulates the map when it's available.
     * This callback is triggered when the map is ready to be used.
     */
    override fun onMapReady(map: GoogleMap) {
        mMap = map


        if(mConfigLatitude != 0.0 && mConfigLongitude != 0.0){
            currentMarker?.remove()
            currentCircle?.remove()
            currentMarker = mMap!!.addMarker(
                MarkerOptions()
                    .title(getString(R.string.zone))
                    .position(LatLng(mConfigLatitude,mConfigLongitude))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            )
            if(mRayon != 0){
                val circleOptions :CircleOptions =  CircleOptions()
                    .center(LatLng(mConfigLatitude,mConfigLongitude))
                    .radius(mRayon.toDouble())
                    .fillColor(Color.argb(128, 255, 0, 0))
                currentCircle = mMap!!.addCircle(circleOptions)
            }
        }
        // Copy lat/Lng to the next activity
        mMap!!.setOnMapClickListener {
            mConfigLatitude = it.latitude
            mConfigLongitude = it.longitude
            currentMarker?.remove()
            currentCircle?.remove()
            currentMarker = mMap!!.addMarker(
                MarkerOptions()
                    .title(getString(R.string.zone))
                    .position(it)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            )
            if(mRayon != 0){
                 val circleOptions :CircleOptions =  CircleOptions()
                     .center(it)
                     .radius(mRayon.toDouble())
                     .fillColor(Color.argb(128, 255, 0, 0))
                // Get back the mutable Circle
                currentCircle = mMap!!.addCircle(circleOptions)

                checkInRange(LatLng(mLastKnownLocation!!.latitude,mLastKnownLocation!!.longitude))
            }
        }

        // Use a custom info window adapter to handle multiple lines of text in the
        // info window contents.
        mMap!!.setInfoWindowAdapter(object : GoogleMap.InfoWindowAdapter {

            // Return null here, so that getInfoContents() is called next.
            override fun getInfoWindow(arg0: Marker): View? {
                return null
            }

            override fun getInfoContents(marker: Marker): View {
                // Inflate the layouts for the info window, title and snippet.
                val infoWindow = layoutInflater.inflate(
                    R.layout.custom_info_contents,
                    findViewById<FrameLayout>(R.id.mapFragment), false
                )

                val title = infoWindow.findViewById(R.id.title) as TextView
                title.text = marker.title

                val snippet = infoWindow.findViewById(R.id.snippet) as TextView
                snippet.text = marker.snippet

                return infoWindow
            }
        })

        // Prompt the user for permission.
        getLocationPermission()

        // Turn on the My Location layer and the related control on the map.
        updateLocationUI()

        // Get the current location of the device and set the position of the map.
        getDeviceLocation()
    }

    /**
     * Gets the current location of the device, and positions the map's camera.
     */
    private fun getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            if (mLocationPermissionGranted) {
                val locationResult = mFusedLocationProviderClient!!.lastLocation
                locationResult.addOnCompleteListener(this
                ) { task ->
                    if (task.isSuccessful) {
                        // Set the map's camera position to the current location of the device.
                        mLastKnownLocation = task.result
                        mMap!!.moveCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(
                                    mLastKnownLocation!!.latitude,
                                    mLastKnownLocation!!.longitude
                                ), DEFAULT_ZOOM.toFloat()
                            )
                        )
                        //Set marker with car icon
                        val height = 50
                        val width = 50
                        val b : Bitmap  = BitmapFactory.decodeResource(resources, R.drawable.car)
                        val smallMarker = Bitmap.createScaledBitmap(b, width, height, false)
                        val smallMarkerIcon = BitmapDescriptorFactory.fromBitmap(smallMarker)
                        currentPosMarker?.remove()
                        currentPosMarker = mMap!!.addMarker(
                            MarkerOptions()
                                .title(getString(R.string.zone))
                                .position(LatLng(
                                    mLastKnownLocation!!.latitude,
                                    mLastKnownLocation!!.longitude
                                ))
                                .icon(smallMarkerIcon)
                        )

                        checkInRange(LatLng(
                            mLastKnownLocation!!.latitude,
                            mLastKnownLocation!!.longitude
                        ))
                    } else {
                        Log.d(TAG, "Current location is null. Using defaults.")
                        Log.e(TAG, "Exception: %s", task.exception)
                        mMap!!.moveCamera(
                            CameraUpdateFactory
                                .newLatLngZoom(mDefaultLocation, DEFAULT_ZOOM.toFloat())
                        )
                        mMap!!.uiSettings.isMyLocationButtonEnabled = false

                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message!!)
        }

    }


    /**
     * Prompts the user for permission to use the device location.
     */
    private fun getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(
                this.applicationContext,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mLocationPermissionGranted = true
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
            )
        }
    }

    /**
     * Handles the result of the request for location permissions.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        mLocationPermissionGranted = false
        when (requestCode) {
            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true
                    updateLocationUI()
                }
            }
        }

    }

    /**
     * Prompts the user to select the current place from a list of likely places, and shows the
     * current place on the map - provided the user has granted location permission.
     */
    private fun showCurrentPlace() {
        if (mMap == null) {
            return
        }

        if (mLocationPermissionGranted) {
            // Get the likely places - that is, the businesses and other points of interest that
            // are the best match for the device's current location.
            val placeResult = mPlaceDetectionClient!!.getCurrentPlace(null)
            placeResult.addOnCompleteListener { task ->
                if (task.isSuccessful && task.result != null) {
                    val likelyPlaces = task.result

                    // Set the count, handling cases where less than 5 entries are returned.
                    val count: Int
                    if (likelyPlaces!!.count < M_MAX_ENTRIES) {
                        count = likelyPlaces.count
                    } else {
                        count = M_MAX_ENTRIES
                    }

                    var i = 0
                    mLikelyPlaceNames = arrayOfNulls(count)
                    mLikelyPlaceAddresses = arrayOfNulls(count)
                    mLikelyPlaceAttributions = arrayOfNulls(count)
                    mLikelyPlaceLatLngs = arrayOfNulls<LatLng>(count)

                    for (placeLikelihood in likelyPlaces) {
                        // Build a list of likely places to show the user.
                        mLikelyPlaceNames!![i] = placeLikelihood.place.name as String
                        mLikelyPlaceAddresses!![i] = placeLikelihood.place
                            .address as String
                        mLikelyPlaceAttributions!![i] = placeLikelihood.place
                            .attributions as String
                        mLikelyPlaceLatLngs!![i] = placeLikelihood.place.latLng

                        i++
                        if (i > count - 1) {
                            break
                        }
                    }

                    // Release the place likelihood buffer, to avoid memory leaks.
                    likelyPlaces.release()

                    // Show a dialog offering the user the list of likely places, and add a
                    // marker at the selected place.
                    openPlacesDialog()

                } else {
                    Log.e(TAG, "Exception: %s", task.exception)
                }
            }
        } else {
            // The user has not granted permission.
            Log.i(TAG, "The user did not grant location permission.")

            // Add a default marker, because the user hasn't selected a place.
          /*  mMap!!.addMarker(
                MarkerOptions()
                    .title(getString(R.string.default_info_title))
                    .position(mDefaultLocation)
                    .snippet(getString(R.string.default_info_snippet))
            )*/

            // Prompt the user for permission.
            getLocationPermission()
        }
    }

    /**
     * Displays a form allowing the user to select a place from a list of likely places.
     */
    private fun openPlacesDialog() {
        // Ask the user to choose the place where they are now.
        val listener = DialogInterface.OnClickListener { dialog, which ->
            // The "which" argument contains the position of the selected item.
            val markerLatLng = mLikelyPlaceLatLngs!![which]
            var markerSnippet = mLikelyPlaceAddresses!![which]
            if (mLikelyPlaceAttributions!![which] != null) {
                markerSnippet = markerSnippet + "\n" + mLikelyPlaceAttributions!![which]
            }

            // Add a marker for the selected place, with an info window
            // showing information about that place.
           /* mMap!!.addMarker(
                MarkerOptions()
                    .title(mLikelyPlaceNames!![which])
                    .position(markerLatLng!!)
                    .snippet(markerSnippet)
            )*/

            // Position the map's camera at the location of the marker.
            mMap!!.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    markerLatLng,
                    DEFAULT_ZOOM.toFloat()
                )
            )
        }

        // Display the dialog.
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.pick_place)
            .setItems(mLikelyPlaceNames, listener)
            .show()
    }

    /**
     * Updates the map's UI settings based on whether the user has granted location permission.
     */
    private fun updateLocationUI() {
        if (mMap == null) {
            return
        }
        try {
            if (mLocationPermissionGranted) {
                mMap!!.isMyLocationEnabled = true
                mMap!!.uiSettings.isMyLocationButtonEnabled = true
            } else {
                mMap!!.isMyLocationEnabled = false
                mMap!!.uiSettings.isMyLocationButtonEnabled = false
                mLastKnownLocation = null
                getLocationPermission()
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message!!)
        }

    }

    inner class Speed : android.location.LocationListener {
        override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {

        }

        override fun onProviderEnabled(p0: String?) {
        }

        override fun onProviderDisabled(p0: String?) {
        }

        override fun onLocationChanged(p0: Location?) {

            //Set marker with car icon
            val height = 50
            val width = 50
            val b : Bitmap  = BitmapFactory.decodeResource(resources, R.drawable.car)
            val smallMarker = Bitmap.createScaledBitmap(b, width, height, false)
            val smallMarkerIcon = BitmapDescriptorFactory.fromBitmap(smallMarker)
            currentPosMarker?.remove()
            currentPosMarker = mMap!!.addMarker(
                MarkerOptions()
                    .title(getString(R.string.zone))
                    .position(LatLng(
                        p0!!.latitude,
                        p0.longitude
                    ))
                    .icon(smallMarkerIcon)
            )

            checkInRange(LatLng(
                p0.latitude,
                p0.longitude
            ))

            if(mRayon != 0){
                val theSpeed :Float = p0.speed
                Log.d("testtag","speed $theSpeed")
                //convert Meter per Sec to Kilometer per Hour (value*3.6)
                if(theSpeed*3.6 > 30){
                    if(!inCircle){
                        inCircle=true
                        Toast.makeText(this@MainActivity,"You have entered the zone2 !!",Toast.LENGTH_SHORT).show()
                    }
                }else{
                    if(inCircle){
                        inCircle=false
                        Toast.makeText(this@MainActivity,"You have exited the zone2 !!",Toast.LENGTH_SHORT).show()
                    }

                }
            }
        }
    }

    companion object {

        private val TAG = MainActivity::class.java.simpleName
        private const val DEFAULT_ZOOM = 15
        private const val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1

        // Keys for storing activity state.
        private const val KEY_CAMERA_POSITION = "camera_position"
        private const val KEY_LOCATION = "location"

        // Used for selecting the current place.
        private const val M_MAX_ENTRIES = 5
    }

    private fun isLocationEnabled(mContext: Context): Boolean {
        val lm = mContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(
                LocationManager.NETWORK_PROVIDER)
    }
    private fun showLocationIsDisabledAlert() {
        alert("We can't show your position because you generally disabled the location service for your device.") {

            neutralPressed("Settings") {
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
              //  finish()


            }
        }.show()
    }


    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
       // getLocationPermission()

        // Turn on the My Location layer and the related control on the map.
        updateLocationUI()

        // Get the current location of the device and set the position of the map.
        getDeviceLocation()
    }

    override fun onRestart()
{
    super.onRestart()
    startActivity(Intent(this@MainActivity, MainActivity::class.java))
    // do some stuff here
}

     public fun refreshActivity() {
     val i =  Intent(this,  MainActivity::class.java)
    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
    startActivity(i);
    finish();

}

}

