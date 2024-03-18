package app.blinkshare.android

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.os.Parcelable
import android.util.Log
import android.view.View
import android.widget.DatePicker
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.exifinterface.media.ExifInterface
import app.blinkshare.android.databinding.ActivityAddProductBinding
import app.blinkshare.android.model.Product
import app.blinkshare.android.model.Tag
import app.blinkshare.android.notification.*
import app.blinkshare.android.utills.isNetworkAvailable
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.TransformationUtils
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class AddProductActivity : AppCompatActivity(), View.OnClickListener,DatePickerDialog.OnDateSetListener {
    private lateinit var  url:String
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var REQUEST_CHECK_SETTINGS: Int = 101
    private lateinit var locationCallback: LocationCallback
    lateinit var binding: ActivityAddProductBinding
    private var selCategory: String = ""
    private lateinit var photoFilePath: String
    var product: Product? = null
    lateinit var mode: String
    private var file_product_image: File? = null
    private var currLocation: Location? = null
    private var currAddress: String = ""
    private var currCustomAddress: String = ""
    private var uri: Uri? = null
    private val list: ArrayList<Product> = ArrayList()
    private var isHomeObject = false
    private var isParkedObject = false
    private var isRegularObject = false
    var customEditAdmin: Boolean = false
    private val OFFSET = 0.0000267
    private var COORDINATE_OFFSET: Double = 0.0000267
    private var apiService: APIService? = null
    private lateinit var storageRef: StorageReference
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var avatar: String = ""
    private var mLatitude: String = ""
    private var mLongitude: String = ""
    private var mLatitudeCustom: String = ""
    private var mLongitudeCustom: String = ""
    private var mId: String = ""
    private var endDate: String = ""
    val activityLauncher: BaseActivityResult<Intent, ActivityResult> =
        BaseActivityResult.registerActivityForResult(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddProductBinding.inflate(layoutInflater)
        setContentView(binding.root)
        apiService = Client.getClient("https://fcm.googleapis.com")?.create(APIService::class.java)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storageRef = FirebaseStorage.getInstance().reference

        binding.radioSchedule.setOnClickListener{
            binding.getAirCraftType.visibility = View.VISIBLE
            binding.etPrice.visibility = View.VISIBLE
            binding.etDestination.visibility = View.VISIBLE
            binding.etDepartureDate.visibility = View.VISIBLE
            binding.etTimeOfDeparture.visibility = View.VISIBLE
            binding.etNumberOfSeats.visibility = View.VISIBLE
            binding.tvLocation.visibility = View.VISIBLE
            binding.etComments.visibility =View.GONE
//            binding.radioGroupScheduleParked.visibility = View.VISIBLE
            binding.etCallUsOnn.visibility = View.VISIBLE
            binding.etEndDate.visibility = View.GONE
            binding.etComments.hint = "Comments"
            isParkedObject = false
            isRegularObject = false
        }
        binding.radioParked.setOnClickListener({
            binding.getAirCraftType.visibility = View.VISIBLE
            binding.etPrice.visibility = View.GONE
            binding.etDestination.visibility = View.GONE
            binding.etDepartureDate.visibility = View.GONE
            binding.etTimeOfDeparture.visibility = View.GONE
            binding.etNumberOfSeats.visibility = View.VISIBLE
            binding.tvLocation.visibility = View.VISIBLE
            binding.etEndDate.visibility = View.VISIBLE
            binding.etComments.visibility =View.VISIBLE

//            binding.radioGroupScheduleParked.visibility = View.VISIBLE
            binding.etCallUsOnn.visibility = View.VISIBLE
            binding.etComments.hint = "Tentative Destination"
            isParkedObject = true
            isRegularObject = false
        })
        binding.radioRegular.setOnClickListener({

            binding.getAirCraftType.visibility = View.VISIBLE
            binding.etPrice.visibility = View.GONE
            binding.etDestination.visibility = View.GONE
            binding.etDepartureDate.visibility = View.GONE
            binding.etTimeOfDeparture.visibility = View.GONE
            binding.etNumberOfSeats.visibility = View.VISIBLE
            binding.tvLocation.visibility = View.VISIBLE
            binding.etEndDate.visibility = View.VISIBLE
            binding.etComments.visibility =View.VISIBLE

//            binding.radioGroupScheduleParked.visibility = View.VISIBLE
            binding.etCallUsOnn.visibility = View.VISIBLE
            binding.etComments.hint = "Comments"
            isParkedObject = false
            isRegularObject= true
        })

        binding.etEndDate.setOnClickListener{
            showDatePicker()
        }

        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(this@AddProductActivity)

        if (intent.hasExtra("fromAirCraft")) {
            val aircraft = intent.getBooleanExtra("fromAirCraft", false)
            if (aircraft) {
//                println("1")
                val photoFileUri = intent.getParcelableExtra<Parcelable?>("image")
                Glide.with(applicationContext).load(photoFilePath)
                    .into(binding.imgPhoto)
            }
        }

        if (intent.hasExtra("product")) {
            product = intent.getParcelableExtra("product")
            avatar = intent.getStringExtra("url").toString()
//            println("PhotoURL has url: "+avatar)
        }

        if (product != null) {
            binding.btnBlinkShare.text = "Save"
            mode = "edit"
            isParkedObject = false
            isRegularObject = false
            val imagesRef = storageRef.child(product?.image ?: "")
            imagesRef.downloadUrl.addOnCompleteListener { task ->
                if (task.isSuccessful) {
//                    println("2")
                    val downloadUri = task.result
                    Glide.with(applicationContext).load(downloadUri).into(binding.imgPhoto)
                    photoFilePath = downloadUri.toString()
                }
            }
//            binding.imgEdit.visibility = View.VISIBLE
            binding.etComments.setText(product!!.comments)
            binding.getAirCraftType.setText(product!!.description)
            mLatitudeCustom = product?.latitude!!
            mLongitudeCustom = product?.longitude!!
            //binding.etTags.setText(getTags(product!!.tags))
            binding.etPrice.setText(product!!.price)
//            binding.imgEdit.setOnClickListener {
//                val sIntent = Intent(applicationContext, MainActivity2::class.java)
//                activityLauncher.launch(
//                    sIntent,
//                    object : BaseActivityResult.OnActivityResult<ActivityResult> {
//                        @SuppressLint("NotifyDataSetChanged")
//                        override fun onActivityResult(result: ActivityResult) {
//                            if (result.resultCode == Activity.RESULT_OK) {
////                                        val mLatitudeCustom =
////                                            result.data!!.getDoubleExtra("lat", 0.0).toString()
////                                        getSingleProduct(mLatitudeCustom)
//                                if (result.data != null && result.data!!.hasExtra("photo")) {
//                                    println("3")
//                                    photoFilePath = result.data!!.getStringExtra("photo") ?: ""
//                                    Glide.with(applicationContext).load(photoFilePath)
//                                        .into(binding.imgPhoto)
//                                }
//                            }
//
//                        }
//                    })
//            }
            //Glide.with(this@AddProductActivity).load(product!!.image).into(binding.imgPhoto)
        } else {
            binding.btnBlinkShare.text = getString(R.string.btn_title_upload_picture)
            mode = "add"
            //if(!applicationContext.getIsAdmin()) {
            if (intent.hasExtra("photo")) {
                photoFilePath = intent.getStringExtra("photo") ?: ""
//                println("Upload Button CLicked")
                binding.imgPhoto.setImageBitmap(
                    rotateImageIfRequired(
                        BitmapFactory.decodeFile(
                            File(photoFilePath).absolutePath
                        ), File(photoFilePath)
                    )
                )
            }
            //}
        }

        showLocationAccessPopup()
        initViews()
        onRadioButtonClicked()
        binding.imgCloseBtn.setOnClickListener(this)
        binding.btnBlinkShare.setOnClickListener(this)
//        binding.imgCustomizeProductAdmin.setOnClickListener(this)


    }

    private fun initViews() {
        if (applicationContext.getIsAdmin()) {
            binding.radioGroup.visibility = View.GONE
//            binding.imgEdit.visibility = View.VISIBLE
//            binding.tvUploadApiCraft.visibility = View.VISIBLE
//            binding.tvUploadApiCraft.setOnClickListener {
//                val sIntent = Intent(applicationContext, AircraftActivity::class.java)
//                activityLauncher.launch(
//                    sIntent,
//                    object : BaseActivityResult.OnActivityResult<ActivityResult> {
//                        @SuppressLint("NotifyDataSetChanged")
//                        override fun onActivityResult(result: ActivityResult) {
//                            if (result.resultCode == Activity.RESULT_OK) {
//                                if (result.data != null && result.data!!.hasExtra("photo")) {
//
//                                }
//                            }
//
//                        }
//                    })
//            }
//            binding.imgEdit.setOnClickListener {
//                val sIntent = Intent(applicationContext, MainActivity2::class.java)
//                activityLauncher.launch(
//                    sIntent,
//                    object : BaseActivityResult.OnActivityResult<ActivityResult> {
//                        @SuppressLint("NotifyDataSetChanged")
//                        override fun onActivityResult(result: ActivityResult) {
//                            if (result.resultCode == Activity.RESULT_OK) {
////                                        val mLatitudeCustom =
////                                            result.data!!.getDoubleExtra("lat", 0.0).toString()
////                                        getSingleProduct(mLatitudeCustom)
//                                if (result.data != null && result.data!!.hasExtra("photo")) {
//                                    println("5")
//                                    photoFilePath = result.data!!.getStringExtra("photo") ?: ""
//                                    Glide.with(applicationContext).load(photoFilePath)
//                                        .into(binding.imgPhoto)
//                                }
//                            }
//
//                        }
//                    })
//            }
            binding.tvLocation.setOnClickListener {
                val sIntent = Intent(applicationContext, LocationActivity::class.java)
                activityLauncher.launch(
                    sIntent,
                    object : BaseActivityResult.OnActivityResult<ActivityResult> {
                        @SuppressLint("NotifyDataSetChanged")
                        override fun onActivityResult(result: ActivityResult) {
                            if (result.resultCode == Activity.RESULT_OK) {
                                if (result.data != null) {
                                    mLatitudeCustom =
                                        result.data!!.getDoubleExtra("lat", 0.0).toString()
                                    mLongitudeCustom =
                                        result.data!!.getDoubleExtra("lang", 0.0).toString()
                                    getAddressFromCurrentLocation(
                                        mLatitudeCustom.toDouble(),
                                        mLongitudeCustom.toDouble()
                                    )
                                    binding.etLocation.setText(currCustomAddress)
                                    binding.tvLocation.text = "$mLatitudeCustom, $mLongitudeCustom"
                                }
                            }
                        }
                    })
            }
            binding.etDepartureDate.setOnClickListener {
                val c = Calendar.getInstance()
                val year = c.get(Calendar.YEAR)
                val month = c.get(Calendar.MONTH)
                val day = c.get(Calendar.DAY_OF_MONTH)


                val dpd = DatePickerDialog(
                    this,
                    DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->

                        // Display Selected date in textbox
                        binding.etDepartureDate.text =
                            "${year}-${String.format("%02d", monthOfYear + 1)}-${
                                String.format(
                                    "%02d",
                                    dayOfMonth
                                )
                            }"

                    },
                    year,
                    month,
                    day
                )

                dpd.show()
            }
            binding.etTimeOfDeparture.setOnClickListener {
                val cal = Calendar.getInstance()
                TimePickerDialog(
                    this,
                    TimePickerDialog.OnTimeSetListener { timePicker, hour, minute ->
                        cal.set(Calendar.HOUR_OF_DAY, hour)
                        cal.set(Calendar.MINUTE, minute)

                        binding.etTimeOfDeparture.text =
                            SimpleDateFormat("hh:mm aa").format(cal.time)
                    },
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE),
                    true
                ).show()

            }
            //binding.imgPhoto.visibility = View.GONE
            //binding.cvProfilePic.visibility = View.GONE
            binding.etPrice.visibility = View.VISIBLE
            binding.etDestination.visibility = View.VISIBLE
            binding.etDestination.visibility = View.VISIBLE
            binding.etDepartureDate.visibility = View.VISIBLE
            binding.etTimeOfDeparture.visibility = View.VISIBLE
            binding.etNumberOfSeats.visibility = View.VISIBLE
//            binding.etLocation.visibility = View.VISIBLE
            binding.tvLocation.visibility = View.VISIBLE
            binding.etCallUsOnn.visibility = View.VISIBLE
            binding.radioGroupScheduleParked.visibility = View.VISIBLE
            if (product != null) {
                mLatitudeCustom = product?.latitude ?: ""
                mLongitudeCustom = product?.longitude ?: ""
                binding.etPrice.setText(product?.price ?: "")
                binding.etDestination.setText(product?.destination ?: "")
                binding.etDepartureDate.setText(product?.departure_date ?: "")
                binding.etTimeOfDeparture.setText(product?.time_of_departure ?: "")
                binding.etNumberOfSeats.setText(product?.number_of_seats ?: "")
                binding.getAirCraftType.setText(product?.description?:"")
                binding.etComments.setText(product?.comments ?: "")
                binding.etCallUsOnn.setText(product?.call_us_on ?: "")
                binding.tvLocation.text = "${product?.latitude ?: ""}, ${product?.longitude ?: ""}"
            }

            binding.getAirCraftType.setOnClickListener {
                val sIntent = Intent(this, AirCraftsTypeActivity::class.java)
                activityLauncher.launch(
                    sIntent,
                    object : BaseActivityResult.OnActivityResult<ActivityResult> {
                        @SuppressLint("NotifyDataSetChanged")
                        override fun onActivityResult(result: ActivityResult) {
                            if (result.resultCode == Activity.RESULT_OK) {
//
                                if (result.data != null && result.data!!.hasExtra("image")) {
//                                    println("6")
                                    val typeName = result.data!!.getStringExtra("typeName")?:null
                                    val photoFileUri =
                                        result.data!!.getParcelableExtra<Uri>("image") ?: null
                                    binding.getAirCraftType.text = typeName
                                    Glide.with(applicationContext).load(photoFileUri)
                                        .into(binding.imgPhoto)
                                    uri = photoFileUri
                                    photoFilePath = photoFileUri.toString()
                                }
                            }

                        }
                    })

            }
        }
        else{
            binding.radioGroup.visibility = View.VISIBLE
            binding.getAirCraftType.visibility = View.GONE
//            binding.tvUploadApiCraft.visibility = View.GONE
            binding.radioFlight.text = "Picture"
        }
    }


    /**
     * Handle click events
     */
    override fun onClick(view: View?) {
        if (view == binding.imgCloseBtn) {
            finish()
        } else if (view == binding.btnBlinkShare) {
//            if(applicationContext.getIsAdmin()){
//                if (isNetworkAvailable()) {
//                    if (!binding.etNumberOfSeats.text.toString()
//                            .isEmpty() || !binding.etPrice.text.toString()
//                            .isEmpty() || !binding.etDestination.text.toString()
//                            .isEmpty() || !binding.etDepartureDate.text.toString()
//                            .isEmpty() || !binding.etTimeOfDeparture.text.toString().isEmpty()
//                    ) {
//                        if (!validate()) {
//                            return
//                        } else {
//                            hideShowLoading(true)
//                            addProductData()
//                            return
//                        }
//                    }
//                    else {
//                        if(mLatitudeCustom.isNullOrEmpty() || mLongitudeCustom.isNullOrEmpty()){
//                            Toast.makeText(applicationContext,"Please Select Location", Toast.LENGTH_LONG).show()
//                        }
//                        else{
//                            hideShowLoading(true)
//                            addProductData()
//                            return
//                        }
//                    }
//
//                }
//                else {
//                    val dialog = NetworkPopUp()
//                    dialog.show(supportFragmentManager, "NetworkPopUp")
//                }
//            }
//            else {
            if (mode == "edit") {
//                println("1")
                hideShowLoading(true)
                addProductData()
            } else if (currLocation != null || (!mLatitudeCustom.isNullOrEmpty() && !mLongitudeCustom.isNullOrEmpty())) {
//                println("2")
//                println("Condition: "+photoFilePath)
                if (::photoFilePath.isInitialized) {
//                    println("4")
                    callAddProductAPI()
                } else {
                    println("5")
                    hideShowLoading(true)
                    addProductData()
                }
            } else {
//                println("6")
                showLocationAccessPopup()
            }
//            }

        }
    }

    /*
  * Call  Add API
  * */
    

    
    private fun callAddProductAPI() {
        if (isNetworkAvailable()) {
            hideShowLoading(true)
            var latitude: String = "0.0"
            var longitude: String = "0.0"
            if (customEditAdmin) {
//                println("7")
            } else {
//                println("8")
                if (applicationContext.getIsAdmin()) {
//                    println("9")
                    if (isNetworkAvailable()) {
//                        println("10")
                        if (!binding.etNumberOfSeats.text.toString()
                                .isEmpty() || !binding.etPrice.text.toString()
                                .isEmpty() || !binding.etDestination.text.toString()
                                .isEmpty() || !binding.etDepartureDate.text.toString()
                                .isEmpty() || !binding.etTimeOfDeparture.text.toString()
                                .isEmpty() || (!binding.etEndDate.text.toString().equals("End Date") || !isParkedObject || !isRegularObject)
                        ) {
//                            println("11")
                            if (!validate()) {
//                                println("12")
                                return
                            } else if (mLatitudeCustom.isNullOrEmpty() || mLongitudeCustom.isNullOrEmpty()) {
//                                println("13")
                                Toast.makeText(
                                    applicationContext,
                                    "Please Select Location",
                                    Toast.LENGTH_LONG
                                ).show()
                                return
                            } else {
//                                println("14")
                                hideShowLoading(true)
                                val id = db.collection("Products").document().id
                                val imagesRef =
                                    storageRef.child("Products/${auth.currentUser?.uid}/$id.jpg")
                                var file = if (uri != null) {
                                    uri ?: Uri.fromFile(File(photoFilePath))
                                } else {
                                    Uri.fromFile(File(photoFilePath))
                                }
//                                println(file.path)
                                Glide.with(this)
                                    .asBitmap()
                                    .load(file)
                                    .into(object : CustomTarget<Bitmap>() {
                                        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                                            // Convert the Bitmap to a byte array
                                            val baos = ByteArrayOutputStream()
                                            resource.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                                            val data = baos.toByteArray()

                                            // Upload the byte array to Firebase Storage
                                            val uploadTask = imagesRef.putBytes(data)

                                            // Add a success listener to handle the successful upload of the image
                                            uploadTask.addOnSuccessListener { taskSnapshot ->
                                                // Image uploaded successfully
                                                imagesRef.downloadUrl.addOnCompleteListener { task ->
                                                    if (task.isSuccessful) {
                                                        val downloadUri = task.result
                                                        avatar = downloadUri.path.toString()
                                                        mId = id
                                                        addProductData()
                                                    }

                                                }
                                            }.addOnFailureListener { exception ->
                                                // Handle any errors that occur during the upload
                                                hideShowLoading(false)
                                            }
                                        }

                                        override fun onLoadCleared(placeholder: Drawable?) {
                                            // Implement this method as required by the CustomTarget class
                                        }
                                    })

//                                val uploadTask = imagesRef.putFile(file)
//
//// Register observers to listen for when the download is done or if it fails
//                                uploadTask.addOnFailureListener {
//                                    // Handle unsuccessful uploads
//                                    hideShowLoading(false)
//                                }.addOnSuccessListener { taskSnapshot ->
//                                    // taskSnapshot.metadata contains file metadata such as size, content-type, etc.
//                                    // ...
//                                    imagesRef.downloadUrl.addOnCompleteListener { task ->
//                                        if (task.isSuccessful) {
//                                            val downloadUri = task.result
//                                            avatar = downloadUri.path.toString()
//                                            mId = id
//                                            addProductData()
//                                        }
//
//                                    }
//                                }
                            }
                        } else {
                            if (mLatitudeCustom.isNullOrEmpty() || mLongitudeCustom.isNullOrEmpty()) {
                                Toast.makeText(
                                    applicationContext,
                                    "Please Select Location",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                hideShowLoading(true)
                                val id = db.collection("Products").document().id
                                val imagesRef =
                                    storageRef.child("Products/${auth.currentUser?.uid}/$id.jpg")
                                var file = Uri.fromFile(File(photoFilePath))
                                val uploadTask = imagesRef.putFile(file)

// Register observers to listen for when the download is done or if it fails
                                uploadTask.addOnFailureListener {
                                    // Handle unsuccessful uploads
                                }.addOnSuccessListener { taskSnapshot ->
                                    // taskSnapshot.metadata contains file metadata such as size, content-type, etc.
                                    // ...
                                    imagesRef.downloadUrl.addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            val downloadUri = task.result
                                            avatar = downloadUri.path.toString()
                                            mId = id
                                            addProductData()
                                        }

                                    }
                                }
                                return
                            }
                        }

                    } else {
                        val dialog = NetworkPopUp()
                        dialog.show(supportFragmentManager, "NetworkPopUp")
                    }
                } else if (currLocation != null) {
                    latitude = currLocation!!.latitude.toString()
                    longitude = currLocation!!.longitude.toString()
                    val id = db.collection("Products").document().id
                    val imagesRef = storageRef.child("Products/${auth.currentUser?.uid}/$id.jpg")
                    var file = Uri.fromFile(File(photoFilePath))
                    val uploadTask = imagesRef.putFile(file)

// Register observers to listen for when the download is done or if it fails
                    uploadTask.addOnFailureListener {
                        // Handle unsuccessful uploads
                    }.addOnSuccessListener { taskSnapshot ->
                        // taskSnapshot.metadata contains file metadata such as size, content-type, etc.
                        // ...
                        imagesRef.downloadUrl.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val downloadUri = task.result
                                avatar = downloadUri.path.toString()
                                mLatitude = latitude
                                mLongitude = longitude
                                mId = id
//                                mLatitude = "31.5478617"
//                                mLongitude = "74.2801746"
                                COORDINATE_OFFSET = 0.000085
                                updateLatLangUpload(
                                    mLatitude.toDouble(),
                                    mLongitude.toDouble(),
                                    mLatitude.toDouble(),
                                    mLongitude.toDouble(), -1, OFFSET
                                )
                                addProductData()
                            }

                        }
                    }
                }
            }


        } else {
            val dialog = NetworkPopUp()
            dialog.show(supportFragmentManager, "NetworkPopUp")
        }
    }

    private fun addProductData() {
        var isFlight = false
        var isHome = false
        if (applicationContext.getIsAdmin()) {
//            println("3")
            if (!binding.etNumberOfSeats.text.toString()
                    .isEmpty() || !binding.etPrice.text.toString()
                    .isEmpty() || !binding.etDestination.text.toString()
                    .isEmpty() || !binding.etDepartureDate.text.toString()
                    .isEmpty() || !binding.etTimeOfDeparture.text.toString()
                    .isEmpty() || (!binding.etEndDate.text.toString().equals("End Date") || !isParkedObject || !isRegularObject)
            ) {
                if (validate()) {
                    isFlight = true
                } else {
                    return
                }
            } else {
                isHome = true
            }
        }
        val cal = Calendar.getInstance()
        cal.timeZone = TimeZone.getTimeZone("UTC")
        cal.add(Calendar.DAY_OF_MONTH, 2)
        val product =
            if (applicationContext.getIsAdmin()) {
                mId = if (mode == "edit") {
                    product?.id!!
                } else {
                    if (mId.isNullOrEmpty()) {
                        db.collection("Products").document().id
                    } else {
                        mId
                    }
                }
                if (isParkedObject){
                    hashMapOf(
                        "id" to mId,
                        "image" to photoFilePath,
                        "authToken" to auth.currentUser?.uid,
                        "photoUrl" to avatar,
                        "latitude" to mLatitudeCustom,
                        "longitude" to mLongitudeCustom,
                        "address" to binding.etLocation.text.toString(),
                        "status" to "available",
                        "title" to "",
                        "description" to binding.getAirCraftType.text.toString(),
                        "comments" to binding.etComments.text.toString(),
//                        "createdDateTime" to cal.timeInMillis,
                        "createdDateTime" to endDate.toLong(),
                        "price" to binding.etPrice.text.toString(),
                        "flight_time" to binding.etFlightTime.text.toString(),
                        "destination" to binding.etDestination.text.toString(),
                        "number_of_seats" to binding.etNumberOfSeats.text.toString(),
                        "time_of_departure" to binding.etTimeOfDeparture.text.toString(),
                        "departure_date" to binding.etDepartureDate.text.toString(),
                        "call_us_on" to binding.etCallUsOnn.text.toString(),
                        "is_object" to true,
                        "is_flight" to true,
                        "is_home" to false,
                        "is_parked" to isParkedObject,
                        "is_regular" to isRegularObject,
                        "end_date" to endDate
                    )
                }
                else if(isRegularObject){
                    hashMapOf(
                        "id" to mId,
                        "image" to photoFilePath,
                        "authToken" to auth.currentUser?.uid,
                        "photoUrl" to avatar,
                        "latitude" to mLatitudeCustom,
                        "longitude" to mLongitudeCustom,
                        "address" to binding.etLocation.text.toString(),
                        "status" to "available",
                        "title" to "",
                        "description" to binding.getAirCraftType.text.toString(),
                        "comments" to binding.etComments.text.toString(),
//                        "createdDateTime" to cal.timeInMillis,
                        "createdDateTime" to endDate.toLong(),
                        "price" to binding.etPrice.text.toString(),
                        "flight_time" to binding.etFlightTime.text.toString(),
                        "destination" to binding.etDestination.text.toString(),
                        "number_of_seats" to binding.etNumberOfSeats.text.toString(),
                        "time_of_departure" to binding.etTimeOfDeparture.text.toString(),
                        "departure_date" to binding.etDepartureDate.text.toString(),
                        "call_us_on" to binding.etCallUsOnn.text.toString(),
                        "is_object" to true,
                        "is_flight" to true,
                        "is_home" to false,
                        "is_parked" to isParkedObject,
                        "is_regular" to isRegularObject,
                        "end_date" to endDate
                    )
                }
                else{
                    hashMapOf(
                        "id" to mId,
                        "image" to photoFilePath,
                        "authToken" to auth.currentUser?.uid,
                        "photoUrl" to avatar,
                        "latitude" to mLatitudeCustom,
                        "longitude" to mLongitudeCustom,
                        "address" to binding.etLocation.text.toString(),
                        "status" to "available",
                        "title" to "",
                        "description" to binding.getAirCraftType.text.toString(),
                        "createdDateTime" to cal.timeInMillis,
                        "price" to binding.etPrice.text.toString(),
                        "flight_time" to binding.etFlightTime.text.toString(),
                        "destination" to binding.etDestination.text.toString(),
                        "comments" to binding.etComments.text.toString(),
                        "number_of_seats" to binding.etNumberOfSeats.text.toString(),
                        "time_of_departure" to binding.etTimeOfDeparture.text.toString(),
                        "departure_date" to binding.etDepartureDate.text.toString(),
                        "call_us_on" to binding.etCallUsOnn.text.toString(),
                        "is_object" to true,
                        "is_flight" to true,
                        "is_home" to false,
                        "is_parked" to isParkedObject,
                        "is_regular" to isRegularObject,
                        "end_date" to endDate
                    )
                }

            }
            else if (isHomeObject) {
                mId = if (mode == "edit") {
                    product?.id!!
                } else {
                    if (mId.isNullOrEmpty()) {
                        db.collection("Products").document().id
                    } else {
                        mId
                    }
                }
                hashMapOf(
                    "id" to mId,
                    "image" to photoFilePath,
                    "authToken" to auth.currentUser?.uid,
                    "photoUrl" to avatar,
                    "latitude" to mLatitudeCustom,
                    "longitude" to mLongitudeCustom,
                    "address" to binding.etLocation.text.toString(),
                    "status" to "available",
                    "title" to "",
                    "description" to binding.getAirCraftType.text.toString(),
                    "createdDateTime" to cal.timeInMillis,
                    "is_object" to true,
                    "is_flight" to false,
                    "is_home" to true
                )
            } else {
                hashMapOf(
                    "id" to mId,
                    "image" to photoFilePath,
                    "authToken" to auth.currentUser?.uid,
                    "photoUrl" to avatar,
                    "latitude" to mLatitude,
                    "longitude" to mLongitude,
                    "address" to currAddress,
                    "status" to "available",
                    "title" to "",
                    "description" to binding.getAirCraftType.text.toString(),
                    "createdDateTime" to cal.timeInMillis,
                    "visibility" to 24,
                    "is_object" to false
                )
            }
//        println("Working:  "+mId+" "+avatar)

        db.collection("Products").document(mId)
            .set(product)
            .addOnSuccessListener {
                hideShowLoading(false)
//                println("passed")
                Log.d(
                    "FireStoreSuccess",
                    "DocumentSnapshot successfully written! $mId"
                )
                //Toast.makeText(applicationContext,"Updated", Toast.LENGTH_SHORT).show()
                if (applicationContext.getIsAdmin()) {
                    val body = if (mode == "edit") {
                        "Aircraft details updated"
                    } else {
                        "New object is added on map"
                    }
                    val data = Data(mId, R.drawable.new_logo, body, "")

                    val sender = if (mode == "edit") {
                        Sender(data, "/topics/${mId}")
                    } else {
                        Sender(data, "/topics/new_post")
                    }
                    apiService!!.sendNotification(sender)
                        ?.enqueue(object : Callback<MyResponse?> {
                            override fun onResponse(
                                call: Call<MyResponse?>,
                                response: Response<MyResponse?>
                            ) {
                                if (response.code() == 200) {
                                    if (response.body()!!.success !== 1) {
                                        //error
                                        Log.e("sendNotification->", response.message().toString())
//                                        val sIntent =
//                                            Intent(applicationContext, MainActivity::class.java)
//                                        sIntent.flags =
//                                            FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK
//                                        startActivity(sIntent)
//                                        finish()
                                        val sIntent = Intent()
                                        sIntent.putExtra(
                                            "lat",
                                            if (applicationContext.getIsAdmin()) {
                                                mLatitudeCustom
                                            } else {
                                                mLatitude
                                            }
                                        )
                                        setResult(RESULT_OK, sIntent)
                                        finish()
                                    } else {
                                        Log.e("sendNotification->", response.message().toString())
                                        val sIntent = Intent()
                                        sIntent.putExtra(
                                            "lat",
                                            if (applicationContext.getIsAdmin()) {
                                                mLatitudeCustom
                                            } else {
                                                mLatitude
                                            }
                                        )
                                        setResult(RESULT_OK, sIntent)
                                        finish()
                                    }
                                } else {
                                    val sIntent = Intent()
                                    sIntent.putExtra(
                                        "lat",
                                        if (applicationContext.getIsAdmin()) {
                                            mLatitudeCustom
                                        } else {
                                            mLatitude
                                        }
                                    )
                                    setResult(RESULT_OK, sIntent)
                                    finish()
                                }
                            }

                            override fun onFailure(call: Call<MyResponse?>, t: Throwable) {
                                Log.e("sendNotification->", t.message.toString())
                                val sIntent = Intent()
                                sIntent.putExtra(
                                    "lat", if (applicationContext.getIsAdmin()) {
                                        mLatitudeCustom
                                    } else {
                                        mLatitude
                                    }
                                )
                                setResult(RESULT_OK, sIntent)
                                finish()
                            }

                        })
                } else {
                    val sIntent = Intent()
                    sIntent.putExtra("lat", mLatitude)
                    setResult(RESULT_OK, sIntent)
                    finish()
                }
            }
            .addOnFailureListener { e ->
                hideShowLoading(false)
                Log.w(
                    "FireStoreFailure",
                    "Error writing document",
                    e
                )
            }
    }

    private fun validate(): Boolean {

        if (mLatitudeCustom.isNullOrEmpty() || mLongitudeCustom.isNullOrEmpty()) {
            Toast.makeText(applicationContext, "Please Select Location", Toast.LENGTH_LONG).show()
            return false
        }
//        if (binding.etLocation.text.toString().isEmpty()) {
//            Toast.makeText(applicationContext, "Please Enter Address", Toast.LENGTH_LONG).show()
//            return false
//        }
        if (binding.etPrice.text.toString().isEmpty() && (!isParkedObject && !isRegularObject)) {
            Toast.makeText(applicationContext, "Please Enter Price", Toast.LENGTH_LONG).show()
            return false
        }
        if (binding.etDepartureDate.text.toString().isEmpty() && (!isParkedObject && !isRegularObject)) {
            Toast.makeText(applicationContext, "Please Enter Departure Date", Toast.LENGTH_LONG)
                .show()
            return false
        }
//        if (binding.etTimeOfDeparture.text.toString().isEmpty()) {
//            Toast.makeText(applicationContext, "Please Enter Departure Time", Toast.LENGTH_LONG)
//                .show()
//            return false
//        }
        if (binding.etNumberOfSeats.text.toString().isEmpty()) {
            Toast.makeText(applicationContext, "Please Enter No of seats", Toast.LENGTH_LONG).show()
            return false
        }
        if (binding.etDestination.text.toString().isEmpty() && (!isParkedObject && !isRegularObject)) {
            Toast.makeText(applicationContext, "Please Enter Destination", Toast.LENGTH_LONG).show()
            return false
        }
        if (binding.etCallUsOnn.text.toString().isEmpty()) {
            Toast.makeText(applicationContext, "Please Enter Contact number", Toast.LENGTH_LONG)
                .show()
            return false
        }

        return true
    }

    private val TOP_LEFT = 1
    private val BOTTOM_LEFT = 2
    private val TOP_RIGHT = 3
    private val BOTTOM_RIGHT = 4
    private val BOTTOM = 5
    private val TOP = 6
    private val LEFT = 7
    private val RIGHT = 8
    /*private fun updateLatLang(
        lat: Double = 0.0,
        lang: Double = 0.0,
        latitude: Double = 0.0,
        longitude: Double = 0.0,
        type: Int = -1
    ) {
        if (applicationContext.getIsAdmin()) {
            addProductData()
        } else {
            db.collection("Products")
                .whereGreaterThanOrEqualTo("latitude", latitude.toString())
                .whereLessThanOrEqualTo("latitude", lat.toString())
                .whereGreaterThanOrEqualTo("latitude", lat.toString())
                .whereLessThanOrEqualTo("latitude", latitude.toString())
                .get()
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        if (it.result == null || it.result.isEmpty) {
                            db.collection("Products")
                                .whereGreaterThanOrEqualTo("longitude", longitude.toString())
                                .whereLessThanOrEqualTo("longitude", lang.toString())
                                .whereGreaterThanOrEqualTo("longitude", lang.toString())
                                .whereLessThanOrEqualTo("longitude", longitude.toString())
                                .get()
                                .addOnCompleteListener {
                                    if (it.isSuccessful) {
                                        if (it.result == null || it.result.isEmpty) {
                                            if (type == -1) {
                                                mLatitude = lat.toString()
                                                mLongitude = lang.toString()
                                            } else {
                                                mLatitude = latitude.toString()
                                                mLongitude = longitude.toString()
                                            }
                                            addProductData()
                                        } else {
                                            val meters: Double = 10.0

// number of km per degree = ~111km (111.32 in google maps, but range varies
// 1km in degree = 1 / 111.32km = 0.0089
// 1m in degree = 0.0089 / 1000 = 0.0000089
                                            val coef: Double = meters * 0.0000267

                                            val new_lat: Double = latitude + coef

// pi / 180 = 0.018
                                            val new_long: Double =
                                                longitude + coef / Math.cos(latitude * 0.008)
                                            updateLatLang(lat, lang, new_lat, new_long, LEFT)
//                                        when(type){
//                                            -1 -> {
//                                                updateLatLang(lat, lang, new_lat-COORDINATE_OFFSET, new_long, LEFT)
//                                            }
//                                            LEFT -> {updateLatLang(lat, lang, new_lat+COORDINATE_OFFSET, new_long, RIGHT)}
//                                            RIGHT -> {updateLatLang(lat, lang, new_lat, new_long+COORDINATE_OFFSET, TOP)}
//                                            TOP -> {updateLatLang(lat, lang, new_lat, new_long-COORDINATE_OFFSET, BOTTOM)}
//                                            BOTTOM -> {updateLatLang(lat, lang, new_lat-COORDINATE_OFFSET, new_long+COORDINATE_OFFSET, TOP_LEFT)}
//                                            TOP_LEFT -> {updateLatLang(lat, lang, new_lat+COORDINATE_OFFSET, new_long+COORDINATE_OFFSET, TOP_RIGHT)}
//                                            TOP_RIGHT -> {updateLatLang(lat, lang, new_lat-COORDINATE_OFFSET, new_long-COORDINATE_OFFSET, BOTTOM_LEFT)}
//                                            BOTTOM_LEFT -> {updateLatLang(lat, lang, new_lat+COORDINATE_OFFSET, new_long-COORDINATE_OFFSET, BOTTOM_RIGHT)}
//                                            BOTTOM_RIGHT-> {
//                                                updateLatLang(lat, lang, new_lat-COORDINATE_OFFSET, new_long, LEFT)
//                                            }
//                                        }
                                        }
                                    } else {
                                        mLatitude = lat.toString()
                                        mLongitude = lang.toString()
                                        addProductData()
                                    }
                                }
                                .addOnFailureListener {
                                    binding.rlProgressLoading.visibility = View.GONE
                                }
                        } else {
                            db.collection("Products")
                                .whereGreaterThanOrEqualTo(
                                    "longitude",
                                    (lang - COORDINATE_OFFSET).toString()
                                )
                                .whereLessThanOrEqualTo("longitude", lang.toString())
                                .whereGreaterThanOrEqualTo("longitude", lang.toString())
                                .whereLessThanOrEqualTo(
                                    "longitude",
                                    (lang + COORDINATE_OFFSET).toString()
                                )
                                .get()
                                .addOnCompleteListener {
                                    if (it.isSuccessful) {
                                        if (it.result == null || it.result.isEmpty) {
                                            mLatitude = latitude.toString()
                                            mLongitude = longitude.toString()
                                            addProductData()
                                        } else {

                                            when (type) {
                                                -1 -> {
                                                    updateLatLang(
                                                        lat,
                                                        lang,
                                                        lat - COORDINATE_OFFSET,
                                                        lang,
                                                        LEFT
                                                    )
                                                }
                                                LEFT -> {
                                                    updateLatLang(
                                                        lat,
                                                        lang,
                                                        lat + COORDINATE_OFFSET,
                                                        lang,
                                                        RIGHT
                                                    )
                                                }
                                                RIGHT -> {
                                                    updateLatLang(
                                                        lat,
                                                        lang,
                                                        lat,
                                                        lang + COORDINATE_OFFSET,
                                                        TOP
                                                    )
                                                }
                                                TOP -> {
                                                    updateLatLang(
                                                        lat,
                                                        lang,
                                                        lat,
                                                        lang - COORDINATE_OFFSET,
                                                        BOTTOM
                                                    )
                                                }
                                                BOTTOM -> {
                                                    updateLatLang(
                                                        lat,
                                                        lang,
                                                        lat - COORDINATE_OFFSET,
                                                        lang + COORDINATE_OFFSET,
                                                        TOP_LEFT
                                                    )
                                                }
                                                TOP_LEFT -> {
                                                    updateLatLang(
                                                        lat,
                                                        lang,
                                                        lat + COORDINATE_OFFSET,
                                                        lang + COORDINATE_OFFSET,
                                                        TOP_RIGHT
                                                    )
                                                }
                                                TOP_RIGHT -> {
                                                    updateLatLang(
                                                        lat,
                                                        lang,
                                                        lat - COORDINATE_OFFSET,
                                                        lang - COORDINATE_OFFSET,
                                                        BOTTOM_LEFT
                                                    )
                                                }
                                                BOTTOM_LEFT -> {
                                                    updateLatLang(
                                                        lat,
                                                        lang,
                                                        lat + COORDINATE_OFFSET,
                                                        lang - COORDINATE_OFFSET,
                                                        BOTTOM_RIGHT
                                                    )
                                                }
                                                BOTTOM_RIGHT -> {
                                                    COORDINATE_OFFSET = COORDINATE_OFFSET + 0.000085
                                                    updateLatLang(
                                                        lat,
                                                        lang,
                                                        lat - COORDINATE_OFFSET,
                                                        lang,
                                                        LEFT
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        mLatitude = latitude.toString()
                                        mLongitude = longitude.toString()
                                        addProductData()
                                        addProductData()
                                    }
                                }
                                .addOnFailureListener {
                                    binding.rlProgressLoading.visibility = View.GONE
                                }
                        }
                    } else {

                    }
                }.addOnFailureListener {
                    binding.rlProgressLoading.visibility = View.GONE
                }
        }
    }*/

    private fun updateLatLangUpload(
        lat: Double = 0.0,
        lang: Double = 0.0,
        latitude: Double = 0.0,
        longitude: Double = 0.0,
        type: Int = -1,
        offset: Double = OFFSET
    ) {
        var isOverlap = false
        for (item in list) {
            try {
                if (latitude >= item.latitude.toDouble() - OFFSET && latitude <= item.latitude.toDouble() + OFFSET && longitude >= item.longitude.toDouble() - OFFSET && longitude <= item.longitude.toDouble() + OFFSET) {
                    isOverlap = true
                    break
                }
            }catch (ex: Exception){

            }
        }
        if (!isOverlap) {
            mLatitude = latitude.toString()
            mLongitude = longitude.toString()
            return
        }
        when (type) {
            -1 -> {
                updateLatLangUpload(lat, lang, lat + offset, longitude, LEFT, offset)
            }
            LEFT -> {
                updateLatLangUpload(lat, lang, lat - offset, longitude, RIGHT, offset)
            }
            RIGHT -> {
                updateLatLangUpload(lat, lang, lat, lang + offset, TOP, offset)
            }
            TOP -> {
                updateLatLangUpload(lat, lang, lat, lang - offset, BOTTOM, offset)
            }
            BOTTOM -> {
                updateLatLangUpload(
                    lat,
                    lang,
                    lat + offset + offset,
                    longitude,
                    LEFT,
                    offset + OFFSET
                )
            }
        }
    }


    @Throws(IOException::class)
    private fun rotateImageIfRequired(img: Bitmap, selectedImage: File): Bitmap? {
        try {

            val ei = ExifInterface(selectedImage.getPath())
            val orientation: Int =
                ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            return when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> TransformationUtils.rotateImage(img, 90)
                ExifInterface.ORIENTATION_ROTATE_180 -> TransformationUtils.rotateImage(img, 180)
                ExifInterface.ORIENTATION_ROTATE_270 -> TransformationUtils.rotateImage(img, 270)
                else -> img
            }
        } catch (e: IOException) {
            return img
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
        val client: SettingsClient = LocationServices.getSettingsClient(this@AddProductActivity)
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
                        this@AddProductActivity,
                        REQUEST_CHECK_SETTINGS
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }
    }

    fun showLocationAccessPopup() {

        if (ContextCompat.checkSelfPermission(
                this@AddProductActivity,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission is not granted
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this@AddProductActivity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                // Show an explanation to the user
                val dialogBuilder = AlertDialog.Builder(this@AddProductActivity)
                dialogBuilder.setMessage("App needs location permission to associate location with your product. Do you want to grant location permission")
                    ?.setCancelable(false)
                    ?.setPositiveButton(
                        "Yes, Grant permission",
                        DialogInterface.OnClickListener { dialog, id ->
                            ActivityCompat.requestPermissions(
                                this@AddProductActivity,
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                                100
                            )
                        })
                    // negative button text and action
                    ?.setNegativeButton("Cancel", DialogInterface.OnClickListener { dialog, id ->
                        dialog.cancel()
                        currLocation = null;
                    })
                val alert = dialogBuilder.create()
                alert.setTitle("Location Permission")
                alert.show()

            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(
                    this@AddProductActivity,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    100
                )
            }
        } else {
            checkLocationSettings()
        }
    }

    /**
     * Handle Runtime Permissions
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
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
                        this@AddProductActivity,
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_CHECK_SETTINGS -> {
                // If request is cancelled, the result arrays are empty.
                if (resultCode == Activity.RESULT_OK) {
                    // permission was granted.
                    getLocation()
                } else {
                    // permission denied.
                    //viewModel.setCurrentLocation(Location(""))
                    currLocation = null
                }
                return
            }
            else -> {
                // Ignore all other requests.
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    @SuppressLint("MissingPermission")
    private fun getLocation() {

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
                        // viewModel.setCurrentLocation(location)
                        currLocation = location
                        mLatitude = currLocation?.latitude.toString()
                        mLongitude = currLocation?.longitude.toString()
                        if (list.isNullOrEmpty()) {
                            getProduct()
                        }
                        if (!Geocoder.isPresent()) {
                            /* Toast.makeText(this@AddProductActivity,
                                     "Geocoder service not available to get address.",
                                     Toast.LENGTH_LONG).show()*/
                            fusedLocationClient.removeLocationUpdates(locationCallback)
                            return
                        } else {
                            getAddressFromCurrentLocation(currLocation!!)
                        }

                    } else {
                        //viewModel.setCurrentLocation(Location(""))
                        currLocation = null
                        fusedLocationClient.removeLocationUpdates(locationCallback)
                        // binding.rlProgress.visibility = View.GONE
                        Toast.makeText(
                            this@AddProductActivity,
                            "Failed to get your location. Please check location services or internet connection.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    // viewModel.setCurrentLocation(Location(""))
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
                //binding.rlProgress.visibility = View.GONE
                Toast.makeText(
                    this@AddProductActivity,
                    "Failed to get your location. Please check location services/internet and try again.",
                    Toast.LENGTH_SHORT
                ).show()
            }

        /* fusedLocationClient.lastLocation
                 .addOnSuccessListener { location: Location? ->
                     val str: String = location?.latitude.toString()
                 }
                 .addOnFailureListener {
                     Toast.makeText(this@MainActivity, R.string.err_location_get_failed, Toast.LENGTH_SHORT).show()
                 }*/
    }

    private fun getAddressFromCurrentLocation(location: Location) {
        GlobalScope.launch {
            val geocoder = Geocoder(this@AddProductActivity, Locale.getDefault())
            var addressLocation = ""
            var addresses: List<Address> = emptyList()

            try {
                addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1) as List<Address>
            } catch (Exception: Exception) {
                currAddress = ""
            }

            // Handle case where no address was found.
            if (addresses.isEmpty()) {
                currAddress = ""
            } else {
                val address = addresses[0]
                if (address != null) {
                    if (addresses[0].getAddressLine(0) != null) {
                        addressLocation = addresses[0].getAddressLine(0)
                        currAddress = addressLocation
                    } else {
                        currAddress = ""
                    }
                } else {
                    currAddress = ""
                }
            }
        }
    }

    private fun getAddressFromCurrentLocation(lat: Double, lang: Double) {
        GlobalScope.launch {
            val geocoder = Geocoder(this@AddProductActivity, Locale.getDefault())
            var addressLocation = ""
            var addresses: List<Address> = emptyList()

            try {
                addresses = geocoder.getFromLocation(lat, lang, 1) as List<Address>
            } catch (Exception: Exception) {
                currCustomAddress = ""
            }

            // Handle case where no address was found.
            if (addresses.isEmpty()) {
                currCustomAddress = ""
            } else {
                val address = addresses[0]
                if (address != null) {
                    if (addresses[0].getAddressLine(0) != null) {
                        addressLocation = addresses[0].getAddressLine(0)
                        currCustomAddress = addressLocation
                    } else {
                        currCustomAddress = ""
                    }
                } else {
                    currCustomAddress = ""
                }
            }
        }
    }

    /*
   Utility Method to Show Dropdown to Any View
    */

    /*
    Interface/Callback for dropdown
     */
    internal interface ItemSelector {
        fun onItemSelected(value: Tag?)
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

    private fun getProduct() {
        hideShowLoading(true)
        val cal = Calendar.getInstance()
        val time = cal.timeInMillis
        Log.e("time->", time.toString())
        db.collection("Products")
//            .whereGreaterThanOrEqualTo("latitude", (mLatitude.toDouble()-0.0089))
//            .whereLessThanOrEqualTo("latitude", (mLatitude.toDouble()+0.0089)).get()
            .whereGreaterThanOrEqualTo("createdDateTime", time).get()
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    list.clear()
                    for (snapShot in it.result) {
                        try {
                            val product = snapShot.toObject(Product::class.java)
                            list.add(product)

                        } catch (e: Exception) {

                        }

                    }
                    hideShowLoading(false)
                }
            }.addOnFailureListener {
                hideShowLoading(false)
            }
    }

    private fun onRadioButtonClicked() {
        binding.radioGroup.check(R.id.radioFlight)
        binding.radioGroup.setOnCheckedChangeListener { _, checkId ->
            when (checkId) {
                R.id.radioFlight -> {
                    if(applicationContext.getIsAdmin()) {
                        binding.getAirCraftType.visibility = View.VISIBLE
                        binding.etPrice.visibility = View.VISIBLE
                        binding.etDestination.visibility = View.VISIBLE
                        binding.etDepartureDate.visibility = View.VISIBLE
                        binding.etTimeOfDeparture.visibility = View.VISIBLE
                        binding.etNumberOfSeats.visibility = View.VISIBLE
                        binding.tvLocation.visibility = View.VISIBLE
                        binding.radioGroupScheduleParked.visibility = View.VISIBLE
                        binding.etCallUsOnn.visibility = View.VISIBLE
                    }
                    else{
                        isHomeObject = false
                    }
                }
                R.id.radioHome -> {
                    binding.etPrice.visibility = View.GONE
                    binding.etDestination.visibility = View.GONE
                    binding.etDepartureDate.visibility = View.GONE
                    binding.etTimeOfDeparture.visibility = View.GONE
                    binding.etNumberOfSeats.visibility = View.GONE
                    binding.tvLocation.visibility = View.GONE
                    binding.radioGroupScheduleParked.visibility = View.GONE
                    binding.etCallUsOnn.visibility = View.INVISIBLE
                    binding.getAirCraftType.visibility = View.GONE
                    isHomeObject = true
                }

            }
        }
    }

//    private fun onRadioButtonClickedParked() {
//        binding.radioGroupScheduleParked.check(R.id.radioSchedule)
//        binding.radioGroupScheduleParked.setOnCheckedChangeListener { _, checkId ->
//            when (checkId) {
//                R.id.radioSchedule -> {
//                    if(applicationContext.getIsAdmin()) {
//                        binding.getAirCraftType.visibility = View.VISIBLE
//                        binding.etPrice.visibility = View.VISIBLE
//                        binding.etDestination.visibility = View.VISIBLE
//                        binding.etDepartureDate.visibility = View.VISIBLE
//                        binding.etTimeOfDeparture.visibility = View.VISIBLE
//                        binding.etNumberOfSeats.visibility = View.VISIBLE
//                        binding.tvLocation.visibility = View.VISIBLE
////                        binding.radioGroupScheduleParked.visibility = View.VISIBLE
//                        binding.etCallUsOnn.visibility = View.VISIBLE
//                    }
//                    else{
//                        isParkedObject = false
//                    }
//                }
//                R.id.radioParked -> {
//                    binding.etPrice.visibility = View.VISIBLE
//                    binding.etDestination.visibility = View.GONE
//                    binding.etDepartureDate.visibility = View.GONE
//                    binding.etTimeOfDeparture.visibility = View.GONE
//                    binding.etNumberOfSeats.visibility = View.VISIBLE
//                    binding.tvLocation.visibility = View.VISIBLE
////                    binding.radioGroupScheduleParked.visibility = View.G
//                    binding.etCallUsOnn.visibility = View.VISIBLE
//                    binding.getAirCraftType.visibility = View.VISIBLE
//                    isParkedObject = true
//                }
//
//            }
//        }
//    }

    override fun onBackPressed() {
        setResult(RESULT_CANCELED)
        finish()
    }
    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this, this, year, month, dayOfMonth)
        datePickerDialog.show()
    }

    override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, dayOfMonth,0,0,0)
        endDate = calendar.timeInMillis.toString()
        val currentCalendar = Calendar.getInstance()
        if(currentCalendar.timeInMillis >= endDate.toLong()) {
            Toast.makeText(this,"Kindly Select Correct Date",Toast.LENGTH_SHORT).show()
            showDatePicker()
        }
        else
            binding.etEndDate.setText(dayOfMonth.toString()+"/"+month+1+"/"+year)


        // Handle the selected date here
    }
}


