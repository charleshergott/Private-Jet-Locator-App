package app.blinkshare.android

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog

import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
//import android.content.res.Resources
import android.graphics.Bitmap
import android.location.Location
import android.net.Uri
import android.opengl.Visibility
import android.os.*
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsSession
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import app.blinkshare.android.databinding.ActivityMainBinding
import app.blinkshare.android.model.Product
import app.blinkshare.android.utills.AppUtils
import app.blinkshare.android.utills.isNetworkAvailable
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.android.gms.maps.model.Marker
//import com.google.android.gms.maps.GoogleMap
//import com.google.android.gms.maps.model.BitmapDescriptorFactory
//import com.google.android.gms.maps.model.Marker

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.remoteconfig.ktx.remoteConfig

import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import java.util.*

import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.LinearInterpolator
import android.view.animation.ScaleAnimation
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.reflect.typeOf

class MainActivity : AppCompatActivity(), OnMapReadyCallback,
    GoogleMap.OnMarkerClickListener {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var binding: ActivityMainBinding
    private val blockList = HashMap<String, String>()
    lateinit var mapFragment: SupportMapFragment
    lateinit var googleMap: GoogleMap
    var isRefresh: Boolean = false
    val permission = Manifest.permission.READ_MEDIA_IMAGES // the permission you want to request
    val activityLauncher: BaseActivityResult<Intent, ActivityResult> =
        BaseActivityResult.registerActivityForResult(this)

    //    private var COORDINATE_OFFSET: Double = 0.0000445
    private var COORDINATE_OFFSET: Double = 0.00002
    private val markerCoordinates: ArrayList<LatLng> = ArrayList()

    private var currLocation: Location? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var REQUEST_CHECK_SETTINGS: Int = 101
    private lateinit var locationCallback: LocationCallback
    private val list: ArrayList<Product> = ArrayList()
    private var mCustomTabsSession: CustomTabsSession? = null

    private lateinit var ref: DatabaseReference

    private var topLeft: LatLng? = null
    private var topRight: LatLng? = null
    private var bottomLeft: LatLng? = null
    private var bottomRight: LatLng? = null

    private val commentsAircraftList = mutableListOf<String>()
    private val blinkAnimation_parked = AnimationDrawable()
    private lateinit var marker_blink: Marker
//    private lateinit vara bitmapDrawable:BitmapDrawable




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //try {
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ref = FirebaseDatabase.getInstance().getReference("comments")
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                println("Token: "+token)
//                Log.d(TAG, "FCM Token: $token")
                // Save or use the token as needed
            } else {
//                Log.e(TAG, "Failed to get FCM token: ${task.exception}")
            }
        }
//        println("Token: "+FirebaseMessaging.getInstance().token)


//        blinkAnimation_parked.addFrame(
//            ContextCompat.getDrawable(applicationContext, R.drawable.ic_baseline_local_parking_24)!!,
//            500 // duration in milliseconds for this frame
//        )
//        blinkAnimation_parked.addFrame(
//            ContextCompat.getDrawable(applicationContext, R.drawable.ic_baseline_local_parking_24_transparent)!!,
//            500 // duration in milliseconds for this frame
//        )
//        blinkAnimation_parked.isOneShot = false // set to true if you want the animation to play only once
//        blinkAnimation_parked.start()
//        bitmapDrawable = BitmapDrawable(resources, )

        ref.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                for (childSnapshot in snapshot.children){
                    println("comments:   "+childSnapshot.key)
                    commentsAircraftList.add(childSnapshot.key.toString())
                }
                getBlockedList()
            }

            override fun onCancelled(error: DatabaseError) {
                println("Cheking: "+error.message)
            }

        })


        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            println("PERMISSION gRANTED")
            // Permission is already granted
            // Do your work here
        } else {
            // Permission is not granted
            // Request the permission
            ActivityCompat.requestPermissions(this, arrayOf(permission), 10)
        }

        if(!applicationContext.getIsAdmin()) {
            binding.imgCamera.visibility = View.GONE
            binding.imgInfoTutorial.visibility = View.GONE
//            showAlertDialog(this@MainActivity,"",getString(R.string.guest_mode_info))
        }
        else {
            binding.imgCamera.visibility = View.VISIBLE
            binding.imgInfoTutorial.visibility = View.VISIBLE
        }

        var popup_text ="Welcome"
        val remoteConfig = Firebase.remoteConfig

        // Enable developer mode to allow for frequent refreshes of the cache
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 0
        }
        remoteConfig.setConfigSettingsAsync(configSettings)

        // Set default values for the remote config parameters
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)

        // Fetch and activate the remote config values
        remoteConfig.fetchAndActivate()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
//                    println("Successful!!")
                    val popupText = remoteConfig.getString("popup_text")
                    popup_text = popupText
                    alert_maintanence(popup_text,"")
//                    val popupTextView = findViewById<TextView>(R.id.popup_text_view)
//                    popupTextView.text = popupText
                }
            }
        //showOnboarding Work
//        val isFromLogin = intent.getBooleanExtra("isFromLogin", false)
//        if (this.getOnBoarding() && isFromLogin) {
//            showOnBoarding()
//            println("boarding")
//        }
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//            val hasPermission = Environment.isExternalStorageManager()
//            if (!hasPermission) {
//                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
//                val uri: Uri = Uri.fromParts("package", packageName, null)
//                intent.data = uri
//                startActivity(intent)
//            } else {
//                println("Granted")
//                // Permission granted, do something with external storage
//            }
//        } else {
//            // For devices below Android 11, use the legacy storage permission
//            // Check and request WRITE_EXTERNAL_STORAGE permission
//        }

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        updateToken()
        //addUser()
        mapFragment = (supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment?)!!
        mapFragment.getMapAsync(this@MainActivity)
        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(this@MainActivity)
        initListeners()
        showLocationAccessPopup()
        getAppStatus()





//        } catch (ex: Exception) {
//            Toast.makeText(applicationContext, ex.message.toString(), Toast.LENGTH_LONG).show()
//        }
    }
    private fun alert_maintanence(popup_text:String,title:String){
        val builder = AlertDialog.Builder(this@MainActivity)
        builder.setTitle(title)
        builder.setMessage(popup_text)



        builder.setPositiveButton("OK"){dialogInterface, which ->
//                                Toast.makeText(applicationContext,"clicked yes",Toast.LENGTH_LONG).show()
            dialogInterface.dismiss()
        }
        builder.setNeutralButton("Support Now"){dialogInterface, which->
            openLink("https://buy.stripe.com/9AQeUXgTd1Ly3M45kl")
            dialogInterface.dismiss()
        }

//                            Toast.makeText(this@MainActivity,"Error! Please Input Again",Toast.LENGTH_LONG).show()

        val alertDialog: AlertDialog = builder.create()
        // Set other dialog properties
        alertDialog.setCancelable(false)
        alertDialog.show()
    }
    private fun showAlertDialog(context: Context, title: String, message: String) {
        val alertDialogBuilder = AlertDialog.Builder(context)

        // Set the title and message for the dialog
        alertDialogBuilder.setTitle(title)
        alertDialogBuilder.setMessage(message)

        // Set a positive button and its click listener
        alertDialogBuilder.setPositiveButton("OK") { dialog, which ->
            // Do something when the OK button is clicked
            // For example, you can dismiss the dialog
            dialog.dismiss()
        }


        // Create and show the dialog
        val alertDialog = alertDialogBuilder.create()
        alertDialog.setCancelable(false)

        alertDialog.show()
    }


    private fun updateToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("updateToken Home", "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result
            try {
                val dataToken = hashMapOf(
                    "token" to token,
                )
                db.collection("Tokens").document(auth.currentUser?.uid!!)
                    .set(dataToken)
            } catch (ex: Exception) {

            }
        })
    }

    private fun initListeners() {
        binding.imgUserAccount.setOnClickListener {
            if (AppUtils().isUserLoggedIn(applicationContext)) {
                val sIntent = Intent(this@MainActivity, UserAccountActivity::class.java)
                activityLauncher.launch(
                    sIntent,
                    object : BaseActivityResult.OnActivityResult<ActivityResult> {
                        @SuppressLint("NotifyDataSetChanged")
                        override fun onActivityResult(result: ActivityResult) {
                            if (result.resultCode == Activity.RESULT_OK) {
//                                        val mLatitudeCustom =
//                                            result.data!!.getDoubleExtra("lat", 0.0).toString()
//                                        getSingleProduct(mLatitudeCustom)
                                if (result.data != null && result.data!!.hasExtra("zoom")) {
                                    val zoomLevel = 12.0f //This goes up to 21
                                    googleMap.moveCamera(
                                        CameraUpdateFactory.newLatLngZoom(
                                            LatLng(
                                                result.data!!.getDoubleExtra("latitude", 0.0),
                                                result.data!!.getDoubleExtra("longitude", 0.0)
                                            ), zoomLevel
                                        )
                                    )
                                    var markerOption = MarkerOptions()
                                        .position(
                                            //getLatLng(
                                            LatLng(
                                                result.data!!.getDoubleExtra("latitude", 0.0),
                                                result.data!!.getDoubleExtra("longitude", 0.0)
                                            )
                                            //)
                                        )
                                        .title("")
                                    markerOption.icon(
                                        BitmapDescriptorFactory.fromBitmap(
                                            ContextCompat.getDrawable(
                                                applicationContext,
                                                R.drawable.airplane_zoom
                                            )?.toBitmap()?.copy(Bitmap.Config.ARGB_8888, true)!!
                                        )
                                    )
                                    googleMap.addMarker(markerOption)
                                } else {
                                    if (isNetworkAvailable()) {
                                        getBlockedList()
                                    } else {
                                        hideShowLoading(false)
                                        val dialog = NetworkPopUp()
                                        dialog.show(supportFragmentManager, "NetworkPopUp")
                                    }
                                }
                            }

                        }
                    })
            }
            else
                startActivity(Intent(this@MainActivity, LoginActivity::class.java))
        }
        binding.adminContact.setOnClickListener{
            val contact = "+41767337484" // phone number with country code
            val message = "Hello, I need help with Private Jet Locator"
            val url = "https://api.whatsapp.com/send?phone=$contact&text=${Uri.encode(message)}"
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            startActivity(i)
        }

        binding.imgCamera.setOnClickListener {
            if (AppUtils().isUserLoggedIn(applicationContext)) {
                if (applicationContext.getIsAdmin()) {
                    val sIntent = Intent(applicationContext, AddProductActivity::class.java)
                    activityLauncher.launch(
                        sIntent,
                        object : BaseActivityResult.OnActivityResult<ActivityResult> {
                            @SuppressLint("NotifyDataSetChanged")
                            override fun onActivityResult(result: ActivityResult) {
                                if (result.resultCode == Activity.RESULT_OK) {
                                    if (result.data != null) {
//                                        val mLatitudeCustom =
//                                            result.data!!.getDoubleExtra("lat", 0.0).toString()
//                                        getSingleProduct(mLatitudeCustom)
                                        if (isNetworkAvailable()) {
                                            getBlockedList()
                                        } else {
                                            hideShowLoading(false)
                                            val dialog = NetworkPopUp()
                                            dialog.show(supportFragmentManager, "NetworkPopUp")
                                        }
                                    }
                                }
                            }
                        })
                } else {
                    val sIntent = Intent(this@MainActivity, MainActivity2::class.java)
                    activityLauncher.launch(
                        sIntent,
                        object : BaseActivityResult.OnActivityResult<ActivityResult> {
                            @SuppressLint("NotifyDataSetChanged")
                            override fun onActivityResult(result: ActivityResult) {
                                if (result.resultCode == Activity.RESULT_OK) {
                                    if (result.data != null) {
//                                        val mLatitudeCustom =
//                                            result.data!!.getStringExtra("lat")?:""
//                                        getSingleProduct(mLatitudeCustom)
                                        if (isNetworkAvailable()) {
                                            getBlockedList()
                                        } else {
                                            hideShowLoading(false)
                                            val dialog = NetworkPopUp()
                                            dialog.show(supportFragmentManager, "NetworkPopUp")
                                        }
                                    }
                                }
                            }
                        })
                }
            } else {
                startActivity(Intent(this@MainActivity, LoginActivity::class.java))
            }
        }
        binding.imgRefresh.setOnClickListener {

            //isRefresh = true
            //selectedRadius = 0.5
            if (isNetworkAvailable()) {
                commentsAircraftList.removeAll(commentsAircraftList)
                ref.addValueEventListener(object : ValueEventListener{
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for (childSnapshot in snapshot.children){
                            println("comments:   "+childSnapshot.key)
                            commentsAircraftList.add(childSnapshot.key.toString())
                        }
                        getBlockedList()
                    }

                    override fun onCancelled(error: DatabaseError) {
                        println("checing: "+error.message)
                    }

                })
            } else {
                hideShowLoading(false)
                val dialog = NetworkPopUp()
                dialog.show(supportFragmentManager, "NetworkPopUp")
            }
        }
        binding.imgInfoTutorial.setOnClickListener {
//            val customTabsIntent = CustomTabsIntent.Builder(mCustomTabsSession)
//                .setToolbarColor(resources.getColor(R.color.colorBg))
//                .setShowTitle(true)
//                .build()
//
//            customTabsIntent.launchUrl(this, Uri.parse("https://youtu.be/mo4jNq18Pg0"))
            showOnBoarding()
        }
    }

    fun showLocationAccessPopup() {

        if (ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission is not granted
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                // Show an explanation to the user
                val dialogBuilder = AlertDialog.Builder(applicationContext)
                dialogBuilder.setMessage("App needs location permission to associate location with your product. Do you want to grant location permission")
                    ?.setCancelable(false)
                    ?.setPositiveButton(
                        "Yes, Grant permission",
                        DialogInterface.OnClickListener { dialog, id ->
                            ActivityCompat.requestPermissions(
                                this,
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                                100
                            )
                        })
                    // negative button text and action
                    ?.setNegativeButton("Cancel", DialogInterface.OnClickListener { dialog, id ->
                        dialog.cancel()
                        currLocation = null
                    })
                val alert = dialogBuilder.create()
                alert.setTitle("Location Permission")
                alert.show()

            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    100
                )
            }
        } else {
            checkLocationSettings()
        }
    }

    private fun checkLocationSettings() {
        val locationRequest = LocationRequest.create()?.apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        val builder =
            locationRequest?.let { LocationSettingsRequest.Builder().addLocationRequest(it) }
        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder?.build()!!)

        task.addOnSuccessListener { locationSettingsResponse ->
            getLocation()
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    exception.startResolutionForResult(
                        this,
                        REQUEST_CHECK_SETTINGS
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }
    }

    private fun getLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            googleMap.isMyLocationEnabled = true
        }
        val locationRequest = LocationRequest.create()?.apply {
            interval = 5000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                if (locationResult.locations != null && locationResult.locations.size > 0) {
                    val location: Location = locationResult.locations[0]
                    if (location != null) {
                        fusedLocationClient.removeLocationUpdates(locationCallback)
                        currLocation = location

                        Handler(Looper.getMainLooper()).postDelayed({
                            googleMap.moveCamera(
                                CameraUpdateFactory.newLatLng(
                                    LatLng(
                                        currLocation!!.latitude, currLocation!!.longitude
                                    )
                                )
//                                        CameraUpdateFactory.newLatLngZoom(
//                                        LatLng(
//                                            currLocation!!.latitude, currLocation!!.longitude
//                                        ), 16.0f
//                            )
                            )
                        }, 1000)

                    }
                } else {
                    currLocation = null
                    fusedLocationClient.removeLocationUpdates(locationCallback)
                }
            }
        }
        fusedLocationClient.requestLocationUpdates(
            locationRequest!!,
            locationCallback,
            Looper.getMainLooper()
        )
            .addOnFailureListener {
                binding.rlProgressLoading.visibility = View.GONE
                Toast.makeText(
                    applicationContext,
                    "Failed to get your location. Please check location services/internet and try again.",
                    Toast.LENGTH_SHORT
                ).show()
            }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == requestCode && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Permission is granted
            // Do your work here
        } else {
            // Permission is not granted
            // Handle the denied permission
        }

        when (requestCode) {
            100 -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // permission was granted.
                    checkLocationSettings()
                } else {
                    //viewModel.setCurrentLocation(Location(""))
                    currLocation = null
                    Toast.makeText(
                        applicationContext,
                        "Location permission denied. Please grant permission from Application info settings.",
                        Toast.LENGTH_LONG
                    ).show()
                }
                return
            }
            else -> {
                // Ignore all other requests.
            }
        }
    }

    override fun onMarkerClick(p0: Marker): Boolean {
        if(!p0.title.equals("New Point")) {
            val intent_ = Intent(applicationContext, ViewProductActivity::class.java)
            intent_.putExtra("product_id", intent.getStringExtra("product_id"))
            intent_.putExtra("latitude", p0.position.latitude.toString())
            intent_.putExtra("longitude", p0.position.longitude.toString())
            println("1: "+intent_.getStringExtra("product_id"))

            println(p0.id+" "+p0.title)
//            intent_.putExtra("product_id",p0.title)
            intent_.putExtra("isFromProfile", false)
            activityLauncher.launch(
                intent_,
                object : BaseActivityResult.OnActivityResult<ActivityResult> {
                    @SuppressLint("NotifyDataSetChanged")
                    override fun onActivityResult(result: ActivityResult) {
                        if (result.resultCode == Activity.RESULT_OK) {
//                                        val mLatitudeCustom =
//                                            result.data!!.getDoubleExtra("lat", 0.0).toString()
//                                        getSingleProduct(mLatitudeCustom)
                            if (result.data != null && result.data!!.hasExtra("zoom")) {
                                val zoomLevel = 12.0f //This goes up to 21
                                googleMap.moveCamera(
                                    CameraUpdateFactory.newLatLngZoom(
                                        LatLng(
                                            result.data!!.getDoubleExtra("latitude", 0.0),
                                            result.data!!.getDoubleExtra("longitude", 0.0)
                                        ), zoomLevel
                                    )
                                )
                                var markerOption = MarkerOptions()
                                    .position(
                                        //getLatLng(
                                        LatLng(
                                            result.data!!.getDoubleExtra("latitude", 0.0),
                                            result.data!!.getDoubleExtra("longitude", 0.0)
                                        )
                                        //)
                                    )
                                    .title("")
                                markerOption.icon(
                                    BitmapDescriptorFactory.fromBitmap(
                                        ContextCompat.getDrawable(
                                            applicationContext,
                                            R.drawable.airplane_zoom
                                        )?.toBitmap()?.copy(Bitmap.Config.ARGB_8888, true)!!
                                    )
                                )
                                googleMap.addMarker(markerOption)
                            } else {
                                if (isNetworkAvailable()) {
                                    getBlockedList()
                                } else {
                                    hideShowLoading(false)
                                    val dialog = NetworkPopUp()
                                    dialog.show(supportFragmentManager, "NetworkPopUp")
                                }
                            }
                        }

                    }
                })
        }else{
            val new_point_text = getString(R.string.new_point_text)
            alert_maintanence(new_point_text,"SAF Station")
        }
        return true
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap
        try {
            // Customise the styling of the base map using a JSON object defined
            // in a raw resource file.
            val success = googleMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    this, R.raw.style_json
                )
            )

        } catch (e: Exception) {

        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            googleMap.isMyLocationEnabled = true
        }

        googleMap.uiSettings.isZoomControlsEnabled = true
        this.googleMap.setOnMarkerClickListener(this)


        this.googleMap.setOnCameraIdleListener {
            //val midLatLng: LatLng = this.googleMap.cameraPosition.target//map's center position latitude & longitude
            val visibleRegion: VisibleRegion = this.googleMap.getProjection().getVisibleRegion()
            topLeft = visibleRegion.farLeft
            topRight = visibleRegion.farRight
            bottomLeft = visibleRegion.nearLeft
            bottomRight = visibleRegion.nearRight
            Log.e("farLeft->", topLeft?.latitude.toString() + ", " + topLeft?.longitude.toString())
            Log.e(
                "farRight->",
                topRight?.latitude.toString() + ", " + topRight?.longitude.toString()
            )
            Log.e(
                "nearLeft->",
                bottomLeft?.latitude.toString() + ", " + bottomLeft?.longitude.toString()
            )
            Log.e(
                "nearRight->",
                bottomRight?.latitude.toString() + ", " + bottomRight?.longitude.toString()
            )
        }
    }
//    private fun setNewPoints(lat:Double,lng:Double){
//        var markerOption = MarkerOptions()
//            .position(
//                getLatLng(
//                    LatLng(
//                        lat,
//                        lng
//                    )
//                )
//            ).title("New Point")
//            .icon(
//                BitmapDescriptorFactory.fromBitmap(
//                    ContextCompat.getDrawable(
//                        applicationContext,
//                        R.drawable.new_point
//                    )?.toBitmap()?.copy(Bitmap.Config.ARGB_8888, true)!!
//                )
//            )
//        val marker = googleMap.addMarker(markerOption)
////        marker?.tag = index
////        index += 1
//
//        if (isRefresh && currLocation != null) {
//            isRefresh = false
//            googleMap.moveCamera(
//                CameraUpdateFactory.newLatLng(
//                    LatLng(
//                        lat, lng
//                    )
//                )
//            )
//     }
//    }
//private fun setNewPoints(lat:Double,lng:Double){
//    val markerOptions = MarkerOptions()
//        .position(getLatLng(LatLng(lat, lng)))
//        .title("New Point")
//        .icon(BitmapDescriptorFactory.fromBitmap(ContextCompat.getDrawable(applicationContext, R.drawable.new_point)?.toBitmap()?.copy(Bitmap.Config.ARGB_8888, true)!!))
//
//    val marker = googleMap.addMarker(markerOptions)
//    if (isRefresh && currLocation != null) {
//        isRefresh = false
//        googleMap.moveCamera(CameraUpdateFactory.newLatLng(LatLng(lat, lng)))
//    }
//
//    googleMap.setOnCameraIdleListener {
//        val zoom = googleMap.cameraPosition.zoom
//        val iconSize = (32 * (2.0.pow(zoom - 15.00))).roundToInt() // calculate the new size of the icon based on the zoom level
//        val drawable = ContextCompat.getDrawable(applicationContext, R.drawable.new_point)?.toBitmap()?.copy(Bitmap.Config.ARGB_8888, true)
//        if(iconSize>0) {
//            val scaledBitmap = Bitmap.createScaledBitmap(drawable!!, iconSize, iconSize, false)
//            marker?.setIcon(BitmapDescriptorFactory.fromBitmap(scaledBitmap))
//        }
//    }
//}


    private fun setMapData(product: Product) {

        var index: Int = 0

        if (product.latitude.contains(",")) {
            product.latitude = product.latitude.replace(",", ".")
        }
        if (product.longitude.contains(",")) {
            product.longitude = product.longitude.replace(",", ".")
        }
        //updateLatLang(product, product.latitude.toDouble(), product.longitude.toDouble())
        val markerOption = MarkerOptions()
            .position(
                getLatLng(
                    LatLng(
                        product.latitude.toDouble(),
                        product.longitude.toDouble()
                    )
                )
            )
            .title(product.description)
        var markerOption_blink = MarkerOptions()
            .position(
                getLatLng(
                    LatLng(
                        product.latitude.toDouble(),
                        product.longitude.toDouble()
                    )
                )
            )
            .title(product.description)

        // marker.icon = BitmapDescriptorFactory.fromResource(R.drawable.home)
//        ContextCompat.getDrawable(applicationContext, R.drawable.airplane)?.toBitmap()?.copy(Bitmap.Config.ARGB_8888,true)


        if (product.is_object) {
            if (product.is_flight) {
//                println("Parked: "+product.is_parked)
                if (product.is_parked) {
                    if (product.id in commentsAircraftList){
                        try {
                            markerOption_blink.icon(
                                BitmapDescriptorFactory.fromBitmap(
                                    ContextCompat.getDrawable(
                                        applicationContext,
                                        R.drawable.ic_baseline_local_parking_24
                                    )?.toBitmap()?.copy(Bitmap.Config.ARGB_8888, true)!!
                                )
                            )
                            markerOption.icon(
                                BitmapDescriptorFactory.fromBitmap(
                                    ContextCompat.getDrawable(
                                        applicationContext,
                                        R.drawable.ic_baseline_local_parking_24_transparent
                                    )?.toBitmap()?.copy(Bitmap.Config.ARGB_8888, true)!!
                                )
                            )
                            println("running!!!!   " +product.id)


                        }
                        catch (ex: Exception) {
                            markerOption_blink.icon(
                                BitmapDescriptorFactory.fromResource(
                                    R.drawable.ic_baseline_local_parking_24
                                )
                            )
                        }
                    }
                    else{
//                    println("Not present")
                        try {
                            markerOption.icon(
                                BitmapDescriptorFactory.fromBitmap(
                                    ContextCompat.getDrawable(
                                        applicationContext,
                                        R.drawable.ic_baseline_local_parking_24
                                    )?.toBitmap()?.copy(Bitmap.Config.ARGB_8888, true)!!
                                )
                            )
                            markerOption_blink.icon(
                                BitmapDescriptorFactory.fromBitmap(
                                    ContextCompat.getDrawable(
                                        applicationContext,
                                        R.drawable.ic_baseline_local_parking_24_transparent
                                    )?.toBitmap()?.copy(Bitmap.Config.ARGB_8888, true)!!
                                )
                            )
                        }
                        catch (ex: Exception) {
                            markerOption.icon(
                                BitmapDescriptorFactory.fromResource(
                                    R.drawable.ic_baseline_local_parking_24
                                )
                            )
                        }
                    }

                }
                if (product.is_regular) {
                    if (product.id in commentsAircraftList){
                        try {
                            markerOption_blink.icon(
                                BitmapDescriptorFactory.fromBitmap(
                                    ContextCompat.getDrawable(
                                        applicationContext,
                                        R.drawable.charter
                                    )?.toBitmap()?.copy(Bitmap.Config.ARGB_8888, true)!!
                                )
                            )
                            markerOption.icon(
                                BitmapDescriptorFactory.fromBitmap(
                                    ContextCompat.getDrawable(
                                        applicationContext,
                                        R.drawable.ic_baseline_local_parking_24_transparent
                                    )?.toBitmap()?.copy(Bitmap.Config.ARGB_8888, true)!!
                                )
                            )
                            println("running!!!!   " +product.id)


                        }
                        catch (ex: Exception) {
                            markerOption_blink.icon(
                                BitmapDescriptorFactory.fromResource(
                                    R.drawable.charter
                                )
                            )
                        }
                    }
                    else{
//                    println("Not present")
                        try {
                            markerOption.icon(
                                BitmapDescriptorFactory.fromBitmap(
                                    ContextCompat.getDrawable(
                                        applicationContext,
                                        R.drawable.charter
                                    )?.toBitmap()?.copy(Bitmap.Config.ARGB_8888, true)!!
                                )
                            )
                            markerOption_blink.icon(
                                BitmapDescriptorFactory.fromBitmap(
                                    ContextCompat.getDrawable(
                                        applicationContext,
                                        R.drawable.ic_baseline_local_parking_24_transparent
                                    )?.toBitmap()?.copy(Bitmap.Config.ARGB_8888, true)!!
                                )
                            )
                        }
                        catch (ex: Exception) {
                            markerOption.icon(
                                BitmapDescriptorFactory.fromResource(
                                    R.drawable.charter
                                )
                            )
                        }
                    }

                }
                if(!product.is_regular && !product.is_parked){
                    if (product.id in commentsAircraftList){
                        try {
                            markerOption_blink.icon(
                                BitmapDescriptorFactory.fromBitmap(
                                    ContextCompat.getDrawable(
                                        applicationContext,
                                        R.drawable.airplane
                                    )?.toBitmap()?.copy(Bitmap.Config.ARGB_8888, true)!!
                                )
                            )
                            markerOption.icon(
                                BitmapDescriptorFactory.fromBitmap(
                                    ContextCompat.getDrawable(
                                        applicationContext,
                                        R.drawable.ic_baseline_local_parking_24_transparent
                                    )?.toBitmap()?.copy(Bitmap.Config.ARGB_8888, true)!!
                                )
                            )
                        }
                        catch (ex: Exception) {
                            markerOption.icon(
                                BitmapDescriptorFactory.fromResource(
                                    R.drawable.airplane
                                )
                            )
                        }
                    }
                    else{
                        try {
                            markerOption.icon(
                                BitmapDescriptorFactory.fromBitmap(
                                    ContextCompat.getDrawable(
                                        applicationContext,
                                        R.drawable.airplane
                                    )?.toBitmap()?.copy(Bitmap.Config.ARGB_8888, true)!!
                                )
                            )
                            markerOption_blink.icon(
                                BitmapDescriptorFactory.fromBitmap(
                                    ContextCompat.getDrawable(
                                        applicationContext,
                                        R.drawable.ic_baseline_local_parking_24_transparent
                                    )?.toBitmap()?.copy(Bitmap.Config.ARGB_8888, true)!!
                                )
                            )
                        }
                        catch (ex: Exception) {
                            markerOption.icon(
                                BitmapDescriptorFactory.fromResource(
                                    R.drawable.airplane
                                )
                            )
                        }
                    }

                }
            } else {
                try {
                    markerOption.icon(
                        BitmapDescriptorFactory.fromBitmap(
                            ContextCompat.getDrawable(
                                applicationContext,
                                R.drawable.home
                            )?.toBitmap()?.copy(Bitmap.Config.ARGB_8888, true)!!
                        )
                    )
                }
                catch (ex: Exception) {
                    markerOption.icon(
                        BitmapDescriptorFactory.fromResource(
                            R.drawable.home
                        )
                    )

                }
//                            BitmapDescriptorFactory.fromResource(
//                        R.drawable.home
//                    )
            }
        }
        else {
            try {
                markerOption.icon(
                    BitmapDescriptorFactory.fromBitmap(
                        ContextCompat.getDrawable(
                            applicationContext,
                            R.drawable.dot
                        )?.toBitmap()?.copy(Bitmap.Config.ARGB_8888, true)!!
                    )
                )
            }
            catch (ex: Exception) {
                markerOption.icon(
                    BitmapDescriptorFactory.fromResource(
                        R.drawable.dot
                    )
                )

            }
        }
//        println("marker")
        val marker = googleMap.addMarker(markerOption)
        val marker_blink = googleMap.addMarker(markerOption_blink)
        val handler = Handler()

        handler.postDelayed(object : Runnable {
            private var isVisible = true

            override fun run() {
                if (isVisible) {
//                                        println("on")
                    marker_blink?.isVisible = false
                    isVisible = false
                } else {
//                                        println("off")
                    marker_blink?.isVisible = true
                    isVisible = true
                }
                handler.postDelayed(this, 1000) // Toggle visibility every 1 second
            }
        }, 1000) // Start the animation after 1 second
//        val handler = Handler()
//
//        handler.postDelayed(object : Runnable {
//            private var isVisible = true
//
//            override fun run() {
//                if (isVisible) {
////                                        println("on")
//                    marker?.isVisible = false
//                    isVisible = false
//                } else {
////                                        println("off")
//                    marker?.isVisible = true
//                    isVisible = true
//                }
//                handler.postDelayed(this, 1000) // Toggle visibility every 1 second
//            }
//        }, 1000) // Start the animation after 1 second


//        val animation = AnimationUtils.loadAnimation(applicationContext, R.anim.marker_animation)
//        marker?.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_baseline_local_parking_24))
//        marker.animateMarker
//        val handler = Handler()
//        handler.postDelayed(object : Runnable {
//            override fun run() {
//                blinkAnimation_parked.start()
//                handler.postDelayed(this, 250)
////                println("checking animation")
//            }
//        }, 1000)
//        marker?.tag = index
//        index += 1

        if (isRefresh && currLocation != null) {
            isRefresh = false
            googleMap.moveCamera(
                CameraUpdateFactory.newLatLng(
                    LatLng(
                        currLocation!!.latitude, currLocation!!.longitude
                    )
                )
//                        CameraUpdateFactory.newLatLngZoom(
//                    LatLng(
//                        currLocation!!.latitude, currLocation!!.longitude
//                    ), 16.0f
//                )
            )
        }





    }

    private val TOP_LEFT = 1
    private val BOTTOM_LEFT = 2
    private val TOP_RIGHT = 3
    private val BOTTOM_RIGHT = 4
    private val BOTTOM = 5
    private val TOP = 6
    private val LEFT = 7
    private val RIGHT = 8
    private fun updateLatLang(
        product: Product,
        latitude: Double = 0.0,
        longitude: Double = 0.0,
        type: Int = -1
    ) {
        val data =
            list.filter { item -> item.latitude.toDouble() == latitude && item.longitude.toDouble() == longitude }
        if (data.isNullOrEmpty()) {
            if (type != -1) {
                product.latitude = latitude.toString()
                product.longitude = longitude.toString()
                updateLatLang(product)
            }
            return
        } else {
            val lat = product.latitude.toDouble()
            val lang = product.longitude.toDouble()
            when (type) {
                -1 -> {
                    updateLatLang(product, lat - COORDINATE_OFFSET, lang, LEFT)
                }
                LEFT -> {
                    updateLatLang(product, lat + COORDINATE_OFFSET, lang, RIGHT)
                }
                RIGHT -> {
                    updateLatLang(product, lat, lang + COORDINATE_OFFSET, TOP)
                }
                TOP -> {
                    updateLatLang(product, lat, lang - COORDINATE_OFFSET, BOTTOM)
                }
                BOTTOM -> {
                    updateLatLang(
                        product,
                        lat - COORDINATE_OFFSET,
                        lang + COORDINATE_OFFSET,
                        TOP_LEFT
                    )
                }
                TOP_LEFT -> {
                    updateLatLang(
                        product,
                        lat + COORDINATE_OFFSET,
                        lang + COORDINATE_OFFSET,
                        TOP_RIGHT
                    )
                }
                TOP_RIGHT -> {
                    updateLatLang(
                        product,
                        lat - COORDINATE_OFFSET,
                        lang - COORDINATE_OFFSET,
                        BOTTOM_LEFT
                    )
                }
                BOTTOM_LEFT -> {
                    updateLatLang(
                        product,
                        lat + COORDINATE_OFFSET,
                        lang - COORDINATE_OFFSET,
                        BOTTOM_RIGHT
                    )
                }
                BOTTOM_RIGHT -> {
                    COORDINATE_OFFSET = COORDINATE_OFFSET + 0.00002
                    updateLatLang(product, lat - COORDINATE_OFFSET, lang, LEFT)
                }
            }
        }
    }

    private fun getBlockedList() {

        if (auth.currentUser?.uid.isNullOrEmpty()) {
            getProduct()
            return
        }
        db.collection("ReportedProducts").document(auth.currentUser?.uid.toString())
            .collection("products")
            .get()
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    for (snap in it.result) {
                        blockList.put(snap.id, snap.id)
                    }
                    getProduct()
                } else {
                    getProduct()
                }
            }
            .addOnFailureListener {
                getProduct()
            }
    }

    private fun getProduct() {


        hideShowLoading(true)
        val cal = Calendar.getInstance()
        val time = cal.timeInMillis
        Log.e("time->", time.toString())

        db.collection("Products").whereGreaterThanOrEqualTo("createdDateTime", time).get()
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    googleMap.clear()
                    for (snapShot in it.result) {
                        try {
                            COORDINATE_OFFSET = 0.00002
                            val product = snapShot.toObject(Product::class.java)
                            if (blockList.containsKey(product.id)) {
                                continue
                            }
                            if (product.is_parked){
                                println("Product1 "+product.id)
                            }
                            if(!product.is_parked && !product.is_regular)
                                setMapData(product)
                            if(product.end_date!!.toLong()>time) {
                                println("Product2 "+product.id)
                                setMapData(product)
                            }

                            //list.add(product)
                        } catch (e: Exception) {

                        }

                    }
//                            db.collection("Products").whereGreaterThanOrEqualTo("end_date",time).get()
////                    db.collection("Products").whereIn("is_parked",listOf(true)).get()
//                        .addOnCompleteListener {
//                            if (it.isSuccessful) {
//                                println(it.result.size())
////                    googleMap.clear()
//                                for (snapShot in it.result) {
//                                    println("Snapshot")
//                                    try {
//                                        COORDINATE_OFFSET = 0.00002
//                                        val product = snapShot.toObject(Product::class.java)
//                                        println("End Date: ${product.end_date}")
//                                        if (blockList.containsKey(product.id)) {
//                                            continue
//                                        }
//                                        setMapData(product)
//                                        //list.add(product)
//                                    } catch (e: Exception) {
//
//                                    }
//
//                                }
//                            }
//                            hideShowLoading(false)
//                        }.addOnFailureListener {
//                            binding.rlProgressLoading.visibility = View.GONE
//                        }
                }
//                setNewPoints(45.53728274225272,-122.94756604922745)
//                setNewPoints(47.532778688730964, -122.30386041914569)
//                setNewPoints(39.318120349499495, -120.13726269366511)
//                setNewPoints(38.50793064412749, -122.81151776887997)
//                setNewPoints(37.364024860347826, -121.93025318110985)
//                setNewPoints(33.63016028227066, -116.16060988407031)
//                setNewPoints(33.93856517026942, -118.40456850029214)
//                setNewPoints(34.21235602482045, -119.08437108454777)
//                setNewPoints(39.22432436746936, -106.86916517428068)
//                setNewPoints(39.64420336994383, -106.91537592859093)
//                setNewPoints(37.95404897363255, -107.90591545125692)
//                setNewPoints(30.19666433616699, -97.666513834704)
//                setNewPoints(29.648108993936734, -95.2798471483278)
//                setNewPoints(36.587533341954924, -121.84495547066295)
//                setNewPoints(34.21048277306451, -118.49049238008995)
//                setNewPoints(34.19796610207508, -118.35611614187417)
//                setNewPoints(33.676047226639874, -117.86810890339486)
//                setNewPoints(42.66561204265032, -83.42301505814828)
//                setNewPoints(36.10028508765741, -79.93222124192313)
//                setNewPoints(55.8722225467935, -4.432400024128323)
//                setNewPoints(55.952527151635536, -3.363612358337239)
//                setNewPoints(53.17839469940174, -2.9736369434447254)
//                setNewPoints(51.38283356092806, -2.7143080490045697)
//                setNewPoints(51.27894316459329, -0.7682175451383122)
//                setNewPoints(51.87531077498304, -0.3694833174482506)
//                setNewPoints(51.3323069223751, 0.03340439547774498)
//                setNewPoints(52.313098674881076, 4.775374898450814)
//                setNewPoints(48.96268270208251, 2.4369774210204027)
//                setNewPoints(45.78668585003324, 3.1693562661878696)
//                setNewPoints(48.1120823917108, 16.57605033997289)
                hideShowLoading(false)
            }.addOnFailureListener {
                binding.rlProgressLoading.visibility = View.GONE
            }


    }

//    override fun onResume() {
//        super.onResume()
//        if (isNetworkAvailable()) {
//            commentsAircraftList.removeAll(commentsAircraftList)
//            ref.addValueEventListener(object : ValueEventListener{
//                override fun onDataChange(snapshot: DataSnapshot) {
//                    for (childSnapshot in snapshot.children){
//                        println("comments:   "+childSnapshot.key)
//                        commentsAircraftList.add(childSnapshot.key.toString())
//                    }
//                    getBlockedList()
//                }
//
//                override fun onCancelled(error: DatabaseError) {
//                    println("testing: "+error.message)
//                }
//
//            })
//        }
//
//    }
    private fun getSingleProduct(lat: String) {
        hideShowLoading(true)
        db.collection("Products").whereEqualTo("latitude", lat).get()
            .addOnCompleteListener {
                hideShowLoading(false)
                if (it.isSuccessful) {
                    for (snapShot in it.result) {
                        try {
                            val product = snapShot.toObject(Product::class.java)
                            if (blockList.containsKey(product.id)) {
                                continue
                            }
                            setMapData(product)
                            //list.add(product)
                        } catch (e: Exception) {

                        }

                    }
                }
            }.addOnFailureListener {
                binding.rlProgressLoading.visibility = View.GONE
            }
    }

    private fun getLatLng(latLng: LatLng): LatLng {
        val updatedLatLng: LatLng
        if (markerCoordinates.contains(latLng)) {
            return latLng
        } else {
            markerCoordinates.add(latLng)
            updatedLatLng = latLng
        }
        return updatedLatLng
    }

    private fun showOnBoarding() {
        val dialog = OnBoardingDialog()
        dialog.show(supportFragmentManager, "OnBoardingDialog")
    }

    private fun hideShowLoading(show: Boolean) {
        if (show) {
            binding.rlProgressLoading.visibility = View.VISIBLE
            binding.animationView.visibility = View.VISIBLE
        } else {
            binding.rlProgressLoading.visibility = View.GONE
            binding.animationView.visibility = View.GONE
        }
    }

    private fun getAppStatus() {
        val docRef = db.collection("AppStatus").document("status")
        docRef.get().addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {
                if (documentSnapshot.contains("status")) {
                    if (documentSnapshot.get("status").toString() == "0") {
                        val dialog = AppStatusPopup()
                        dialog.show(supportFragmentManager, "AppStatusPopup")
                        hideShowLoading(false)
                    } else {
                        if (isNetworkAvailable()) {
                            getBlockedList()
                        } else {
                            hideShowLoading(false)
                            val dialog = NetworkPopUp()
                            dialog.show(supportFragmentManager, "NetworkPopUp")
                        }
                        if (intent != null) {
                            if (intent.hasExtra("product_id")) {
                                val intent_ =
                                    Intent(applicationContext, ViewProductActivity::class.java)
                                println("ProductID "+intent_.getStringExtra("product_id"))
                                intent_.putExtra("product_id", intent.getStringExtra("product_id"))
                                intent_.putExtra("isFromProfile", false)
                                startActivity(intent_)
                                activityLauncher.launch(
                                    intent_,
                                    object : BaseActivityResult.OnActivityResult<ActivityResult> {
                                        @SuppressLint("NotifyDataSetChanged")
                                        override fun onActivityResult(result: ActivityResult) {
                                            if (result.resultCode == Activity.RESULT_OK) {
//                                        val mLatitudeCustom =
//                                            result.data!!.getDoubleExtra("lat", 0.0).toString()
//                                        getSingleProduct(mLatitudeCustom)
                                                if (result.data != null && result.data!!.hasExtra("zoom")) {
                                                    val zoomLevel = 12.0f //This goes up to 21
                                                    googleMap.moveCamera(
                                                        CameraUpdateFactory.newLatLngZoom(
                                                            LatLng(
                                                                result.data!!.getDoubleExtra("latitude", 0.0),
                                                                result.data!!.getDoubleExtra("longitude", 0.0)
                                                            ), zoomLevel
                                                        )
                                                    )
                                                    var markerOption = MarkerOptions()
                                                        .position(
                                                            //getLatLng(
                                                            LatLng(
                                                                result.data!!.getDoubleExtra("latitude", 0.0),
                                                                result.data!!.getDoubleExtra("longitude", 0.0)
                                                            )
                                                            //)
                                                        )
                                                        .title("")
                                                    markerOption.icon(
                                                        BitmapDescriptorFactory.fromBitmap(
                                                            ContextCompat.getDrawable(
                                                                applicationContext,
                                                                R.drawable.airplane_zoom
                                                            )?.toBitmap()?.copy(Bitmap.Config.ARGB_8888, true)!!
                                                        )
                                                    )
                                                    googleMap.addMarker(markerOption)
                                                } else {
                                                    if (isNetworkAvailable()) {
                                                        getBlockedList()
                                                    } else {
                                                        hideShowLoading(false)
                                                        val dialog = NetworkPopUp()
                                                        dialog.show(supportFragmentManager, "NetworkPopUp")
                                                    }
                                                }
                                            }

                                        }
                                    })
                            }
                        }
                    }
                }
            }

        }.addOnFailureListener {
        }
    }
//    override fun onResume() {
//        super.onResume()
//        if (isNetworkAvailable()) {
//            getBlockedList()
//        } else {
//            hideShowLoading(false)
//            val dialog = NetworkPopUp()
//            dialog.show(supportFragmentManager, "NetworkPopUp")
//        }
//    }
}