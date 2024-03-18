package app.blinkshare.android

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.blinkshare.android.adapters.CommentsAdapter
import app.blinkshare.android.adapters.ProductAdapter
import app.blinkshare.android.databinding.ActivityViewProductBinding
import app.blinkshare.android.model.Comments
import app.blinkshare.android.model.Product
import app.blinkshare.android.model.User
import app.blinkshare.android.notification.*
import app.blinkshare.android.utills.isNetworkAvailable
import app.blinkshare.android.utills.toast
import com.astritveliu.boom.Boom
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*


class ViewProductActivity : AppCompatActivity() {
    private lateinit var binding: ActivityViewProductBinding
    private var list = ArrayList<Product>()
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storageRef: StorageReference
    private var lat = ""
    private var lang = ""
    private var isFromProfile = false
    private var productId = ""
    private var apiService: APIService? = null
    private var database:FirebaseDatabase? = null
    private var userName = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewProductBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = FirebaseDatabase.getInstance()
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storageRef = FirebaseStorage.getInstance().reference
        apiService = Client.getClient("https://fcm.googleapis.com")?.create(APIService::class.java)
        lat = intent.getStringExtra("latitude") ?: ""
        lang = intent.getStringExtra("longitude") ?: ""
        productId = intent.getStringExtra("product_id") ?: ""
        isFromProfile = intent.getBooleanExtra("isFromProfile", false)

//        println("2: "+productId)


//        val myFragment = MyFragment()

//        val bundle = Bundle()
//        bundle.putString("myKey", "myValue")
//        myFragment.arguments = bundle
//
//        supportFragmentManager.beginTransaction()
//            .add(R.id.container, myFragment, "myFragmentTag")
//            .commit()

        initAdapter()
        if (isNetworkAvailable()) {
            getProducts()
        } else {
            hideShowLoading(false)
            val dialog = NetworkPopUp()
            dialog.show(supportFragmentManager, "NetworkPopUp")
        }
//        println("Chekcing Product ID: "+list)
        initListeners()

    }

    private fun initListeners() {
        binding.ivBack.setOnClickListener {
            finish()
        }
    }

    private fun initAdapter() {
        binding.rv.adapter = ProductAdapter(
            list,
            auth.currentUser?.uid ?: "",
            object : ProductAdapter.OnItemClickListeners {
                @SuppressLint("NotifyDataSetChanged")
                override fun onLikeClick(position: Int) {
                    if (list[position].is_like) {
                        return
                    }
                    if (!isNetworkAvailable()) {
                        hideShowLoading(false)
                        val dialog = NetworkPopUp()
                        dialog.show(supportFragmentManager, "NetworkPopUp")
                        return
                    }
                    val map = hashMapOf(
                        auth.currentUser?.uid to 1
                    )
                    db.collection("LikeDislike").document(list[position].id)
                        .set(map)
                        .addOnCompleteListener {
                            if (it.isSuccessful) {
                                val cal = Calendar.getInstance()
                                val date = Date(list[position].createdDateTime)
                                cal.time = date
                                //cal.add(Calendar.DAY_OF_MONTH, 1)
                                if (list[position].is_dis_like) {
                                    cal.add(Calendar.HOUR_OF_DAY, 2)
                                } else {
                                    cal.add(Calendar.HOUR_OF_DAY, 1)
                                }
                                val time = cal.timeInMillis
                                db.collection("Products").document(list[position].id)
                                    .update("createdDateTime", time).addOnCompleteListener {
                                        list[position].createdDateTime = time
                                        list[position].is_like = true
                                        list[position].is_dis_like = false
                                        list[position].isTimeUpdated = true
                                        binding.rv.adapter?.notifyDataSetChanged()
                                    }
                            }
                        }
                }

                @SuppressLint("NotifyDataSetChanged")
                override fun onDisLikeClick(position: Int) {
                    if (list[position].is_dis_like) {
                        return
                    }
                    if (!isNetworkAvailable()) {
                        hideShowLoading(false)
                        val dialog = NetworkPopUp()
                        dialog.show(supportFragmentManager, "NetworkPopUp")
                        return
                    }
                    val map = hashMapOf(
                        auth.currentUser?.uid to 0
                    )
                    db.collection("LikeDislike").document(list[position].id)
                        .set(map)
                        .addOnCompleteListener {
                            if (it.isSuccessful) {
                                val cal = Calendar.getInstance()
                                val date = Date(list[position].createdDateTime)
                                cal.time = date
                                //cal.add(Calendar.DAY_OF_MONTH, -1)
                                if (list[position].is_like) {
                                    cal.add(Calendar.HOUR_OF_DAY, -2)
                                } else {
                                    cal.add(Calendar.HOUR_OF_DAY, -1)
                                }

                                val time = cal.timeInMillis
                                db.collection("Products").document(list[position].id)
                                    .update("createdDateTime", time).addOnCompleteListener {
                                        list[position].createdDateTime = time
                                        list[position].is_dis_like = true
                                        list[position].is_like = false
                                        list[position].isTimeUpdated = true
                                        binding.rv.adapter?.notifyDataSetChanged()
                                    }
                            }
                        }
                }

                @SuppressLint("NotifyDataSetChanged")
                override fun onDeleteClick(position: Int) {
                    if (!isNetworkAvailable()) {
                        hideShowLoading(false)
                        val dialog = NetworkPopUp()
                        dialog.show(supportFragmentManager, "NetworkPopUp")
                        return
                    }
                    hideShowLoading(true)
                    db.collection("Products").document(list[position].id).delete()
                        .addOnCompleteListener {
                            if (it.isSuccessful) {
                                hideShowLoading(false)
                                list.removeAt(position)
                                binding.rv.adapter?.notifyDataSetChanged()
                                if (list.isNullOrEmpty()) {
//                                    val sIntent =
//                                        Intent(applicationContext, MainActivity::class.java)
//                                    sIntent.flags =
//                                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//                                    startActivity(sIntent)
//                                    finish()
                                    setResult(RESULT_OK)
                                    finish()
                                }
                            }
                        }.addOnFailureListener {
                            hideShowLoading(false)
                        }
                }

                override fun onNumberClicked(position: Int) {
                    val contact = "+41767337484" // phone number with country code
                    val message = "Hello, I would like to know more about the ${list[position].description} leaving on ${list[position].departure_date} for ${list[position].destination}"
                    val url = "https://api.whatsapp.com/send?phone=$contact&text=${Uri.encode(message)}"
                    val i = Intent(Intent.ACTION_VIEW)
                    i.data = Uri.parse(url)
                    startActivity(i)
                }

                @RequiresApi(Build.VERSION_CODES.O)
                override fun onCommentingBtnClicked(position: Int) {
//                    val intent = Intent(this@ViewProductActivity,CommentActivity::class.java)
//                    intent.putExtra("productId",productId)
//                    startActivity(intent)
//                    Toast.makeText(applicationContext,"Work in Progress. Stay Tuned",Toast.LENGTH_SHORT).show()
                    showCustomView(position)
                }

                @RequiresApi(Build.VERSION_CODES.O)
                override fun onViewAllComments(position: Int) {
                    showCustomView(position)
                }

//                override fun sendData(position: Int) {
//                    productId = list[position].id
//                    val bundle = Bundle()
//                    bundle.putString("productId", productId)
//                    val fragment = CommentsFragment()
//                    fragment.setArguments(bundle)
//                    val fragmentManager: FragmentManager = supportFragmentManager
//                    val fragmentTransaction: FragmentTransaction =
//                        fragmentManager.beginTransaction()
//                    fragmentTransaction.replace(R.id.container_view, fragment)
//                    fragmentTransaction.commit()
////                    println("print product ID: "+productId)
//                }

                override fun onShareBtnClicked(position: Int) {
                    val lat = list[position].latitude
                    val lng = list[position].longitude
                    val zoom = 15
                    val mapUrl = "https://www.google.com/maps/search/?api=1&query=$lat,$lng&zoom=$zoom"

                    if (list[position].image != null) {

//                        val localFile = File.createTempFile("image", "jpg")
//                        println(list[position].id)
//                        println(localFile.absolutePath)
//                        println(localFile.absoluteFile)
//                            val imageUri = FileProvider.getUriForFile(this@ViewProductActivity, "com.example.fileprovider", localFile)
                        val shareBody = "Aircraft Type: "+list[position].description+
                                "\nDeparture Date: " + list[position].departure_date +
                                "\nTime of Departure: " + list[position].time_of_departure +
                                "\nNo. of Seats: " + list[position].number_of_seats +
                                "\nDestination: " + list[position].destination +
                                "\nPrice: €" + list[position].price +
                                "\nTo book, call us on: " + list[position].call_us_on+
                                "\nPosition: "+mapUrl
//                                    "\nImage Uri: \n" + imageUri
                            val sharingIntent = Intent(Intent.ACTION_SEND)
                            sharingIntent.type = "text/plain"
                            sharingIntent.putExtra(Intent.EXTRA_SUBJECT, "Private Jet Locator")
                            sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody)
//                            sharingIntent.putExtra(Intent.EXTRA_STREAM,imageUri)
                            sharingIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            startActivity(Intent.createChooser(sharingIntent, "Share via"))

                    }else{
                        val shareBody = "Aircraft Type: "+list[position].description+
                                "\nDeparture Date: " + list[position].departure_date +
                                "\nTime of Departure: " + list[position].time_of_departure +
                                "\nNo. of Seats: " + list[position].number_of_seats +
                                "\nDestination: " + list[position].destination +
                                "\nPrice: €" + list[position].price +
                                "\nTo book, call us on: " + list[position].call_us_on+
                                "\nPosition: "+mapUrl
                        val sharingIntent = Intent(Intent.ACTION_SEND)
                        sharingIntent.type = "text/plain"
                        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, "Private Jet Locator")
                        sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody)
//                    sharingIntent.putExtra(Intent.EXTRA_STREAM,Uri.parse(list[position].photoUrl))
                        sharingIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        startActivity(Intent.createChooser(sharingIntent, "Share via"))
                    }



                }

                override fun onReportClick(position: Int) {
                    if (list.size > position) {
                        val dialog =
                            BlockProductFragment(object : BlockProductFragment.OnItemClickListener {
                                override fun onReportClick(text: String) {
                                    hideShowLoading(true)
                                    val map = hashMapOf(
                                        "id" to list[position].id,
                                        "why" to text,
                                        "date" to Calendar.getInstance().timeInMillis
                                    )
                                    db.collection("ReportedProducts")
                                        .document(auth.currentUser?.uid.toString())
                                        .collection("products")
                                        .document(list[position].id)
                                        .set(map)
                                        .addOnCompleteListener {
                                            hideShowLoading(false)
                                            if (it.isSuccessful) {
                                                this@ViewProductActivity.toast("Picture reported successfully.")
                                                setResult(RESULT_OK)
                                                finish()
                                            } else {
                                                this@ViewProductActivity.toast(it.exception?.message.toString())
                                            }
                                        }
                                        .addOnFailureListener {
                                            hideShowLoading(false)
                                            this@ViewProductActivity.toast(it.message.toString())
                                        }
                                }

                                override fun onCancelClick() {
                                }

                            })
                        dialog.show(supportFragmentManager, "BlockProductFragment")
                    }
                }

                override fun onEditClick(position: Int,url:String) {
                    val sIntent = Intent(applicationContext, AddProductActivity::class.java)
                    sIntent.putExtra("product", list[position])
                    sIntent.putExtra("url",url)
                    startActivity(sIntent)
                }

                override fun onSubscribeClick(position: Int) {
                    hideShowLoading(true)

                    val map = hashMapOf(
                        "${list[position].id}" to if(list[position].isSubscribed){"0"}else{"1"},
                    )

                    db.collection("Subscriptions").document(auth.currentUser?.uid!!)
                        .set(map)
                        .addOnCompleteListener {
                            if(list[position].isSubscribed){
                                unSubscribe(list[position].id)

                            }
                            else {
                                subscribe(list[position].id, list[position].authToken)
                            }
                            list[position].isSubscribed = !list[position].isSubscribed
                        }
                        .addOnFailureListener {
                            hideShowLoading(false)
                            Toast.makeText(applicationContext,"Subscribe failed\n"+it.message.toString(),Toast.LENGTH_LONG).show()
                        }

                }

                override fun onFindAircraft(position: Int) {
                    try {
                        val uri =
                            java.lang.String.format(
                                Locale.ENGLISH,
                                "geo:%f,%f",
                                list[position].latitude.toDouble(),
                                list[position].longitude.toDouble()
                            )
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                        startActivity(intent)
                    }catch (ex: Exception){
                        Toast.makeText(applicationContext, ex.message.toString(), Toast.LENGTH_LONG).show()
                    }
//                    val sIntent = Intent()
//                    sIntent.putExtra("latitude", list[position].latitude.toDouble())
//                    sIntent.putExtra("longitude", list[position].longitude.toDouble())
//                    sIntent.putExtra("zoom", true)
//                    setResult(RESULT_OK, sIntent)
//                    finish()
                }

            },ViewProductActivity())
    }

    private fun getProducts() {
        hideShowLoading(true)
        val ref = if (isFromProfile) {
            db.collection("Products").whereEqualTo("authToken", auth.currentUser?.uid ?: "")
        } else {
            if(!productId.isNullOrEmpty()){
                db.collection("Products").whereEqualTo("id", productId)
            }else {
                db.collection("Products").whereEqualTo("latitude", lat).whereEqualTo("longitude", lang)
            }
//            db.collection("Products").whereEqualTo("latitude", lat).whereEqualTo("longitude", lang)
        }
        ref.get().addOnCompleteListener {
            if (it.isSuccessful) {
                hideShowLoading(false)
                var isProfileLoaded = false
                val cal = Calendar.getInstance()
                val time = cal.timeInMillis
                for (snapShot in it.result) {
                    val product = snapShot.toObject(Product::class.java)
                    if (!isProfileLoaded) {
                        try {
                            getProfile(product.authToken)
                        } catch (ex: Exception) {

                        }
                    }
                    isProfileLoaded = true
                    if(product.createdDateTime > time || product.is_parked || product.is_regular) {
                        product.image = "Products/${product.authToken}/${snapShot.id}.jpg"
                        list.add(product)
                        try {
                            if (auth.currentUser?.uid ?: "" != "") {
                                db.collection("Subscriptions").document(auth.currentUser?.uid!!)
                                    .get().addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            if (task.result.exists() && task.result.contains(
                                                    product.id
                                                )
                                            ) {
                                                if (task.result.get(product.id)
                                                        .toString() == "1"
                                                ) {
                                                    product.isSubscribed = true
                                                    binding.rv.adapter?.notifyDataSetChanged()
                                                } else {
                                                    product.isSubscribed = false
                                                    binding.rv.adapter?.notifyDataSetChanged()
                                                }

                                            } else {
                                                product.isSubscribed = false
                                                binding.rv.adapter?.notifyDataSetChanged()
                                            }

                                        }
                                    }
                                db.collection("LikeDislike").document(product.id)
                                    .get().addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            if (task.result.exists() && task.result.contains(
                                                    auth.currentUser?.uid ?: ""
                                                )
                                            ) {
                                                if (task.result.get(auth.currentUser?.uid ?: "")
                                                        .toString() == "1"
                                                ) {
                                                    product.is_like = true
                                                    product.is_dis_like = false
                                                    binding.rv.adapter?.notifyDataSetChanged()
                                                } else {
                                                    product.is_like = false
                                                    product.is_dis_like = true
                                                    binding.rv.adapter?.notifyDataSetChanged()
                                                }
                                            }

                                        }
                                    }
                            }
                        } catch (ex: Exception) {

                        }
                    }
                    else{
                        db.collection("Products").document(product.id).delete()
                        storageRef.child("Products/${product.authToken}/${snapShot.id}.jpg").delete()

                        /*
                        db.collection("Products").document(product.id).delete().addOnCompleteListener { taskDelete ->
                            if(taskDelete.isSuccessful){
                                Log.i("ViewProductDelete","${product.id}is deleted Successfully")
                                storageRef.child("Products/${product.authToken}/${snapShot.id}.jpg").delete().addOnCompleteListener { taskImage->
                                    if(taskImage.isSuccessful){
                                        Log.i("image deleted", "image deleted Successfully")
                                    }
                                }
                            }
                        }
                         */

                    }
                }
                list.sortByDescending { item -> item.createdDateTime }
                binding.rv.adapter?.notifyDataSetChanged()
                binding.rlProgressLoading.visibility = View.GONE
            }
        }.addOnFailureListener {
            hideShowLoading(false)
            binding.rlProgressLoading.visibility = View.GONE
        }
    }

    @SuppressLint("SetTextI18n")
    private fun getProfile(uid: String) {
        storageRef.child("Avatars/$uid.jpg").downloadUrl.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val downloadUri = task.result
                Glide.with(applicationContext).load(downloadUri).into(binding.ivProfile)
            }
        }
        db.collection("Users").document(uid).get().addOnCompleteListener {
            try {
                val user = it.result.toObject(User::class.java)
                //binding.tvName.text = "${user?.firstName?:""} ${user?.lastName?:""}"
                binding.tvName.text = user?.userName ?: ""
//                userName = user?.userName ?: ""
            } catch (ex: Exception) {

            }
        }
        db.collection("Users").document(auth.currentUser?.uid.toString()).get().addOnCompleteListener {
            try {
                val user_comment = it.result.toObject(User::class.java)
                userName = user_comment?.userName ?: ""
            }catch (e:Exception){

            }
        }
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

    private fun subscribe(topic: String, uid: String) {

        FirebaseMessaging.getInstance().subscribeToTopic(topic)
            .addOnCompleteListener { task ->
                hideShowLoading(false)
                binding.rv.adapter?.notifyDataSetChanged()
                var msg = "Subscribed"
                if (!task.isSuccessful) {
                    msg = "Subscribe failed"
                }
                Toast.makeText(this@ViewProductActivity, msg, Toast.LENGTH_SHORT).show()
                db.collection("Tokens").document(uid).get()
                    .addOnCompleteListener {
                        if(it.isSuccessful && it.result.exists()){
                            try {
                                val token = it.result.get("token").toString()
                                val body = "Some one subscribed your aircraft"
                                val data = Data(topic, R.mipmap.ic_launcher, body, "")

                                val sender = Sender(data, token)

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

                                                } else {
                                                    Log.e("sendNotification->", response.message().toString())

                                                }
                                            }
                                        }

                                        override fun onFailure(call: Call<MyResponse?>, t: Throwable) {
                                            Log.e("sendNotification->", t.message.toString())
                                        }

                                    })
                            }catch (ex: Exception){

                            }

                        }
                    }
                    .addOnFailureListener {

                    }
            }
            .addOnFailureListener {
                hideShowLoading(false)
                Toast.makeText(this@ViewProductActivity, it.message.toString(), Toast.LENGTH_SHORT)
                    .show()
            }
    }

    private fun unSubscribe(topic: String) {

        FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
            .addOnCompleteListener { task ->
                hideShowLoading(false)
                binding.rv.adapter?.notifyDataSetChanged()
                var msg = "UnSubscribed"
                if (!task.isSuccessful) {
                    msg = "UnSubscribe failed"
                }
                Toast.makeText(this@ViewProductActivity, msg, Toast.LENGTH_SHORT).show()

            }
            .addOnFailureListener {
                hideShowLoading(false)
                Toast.makeText(this@ViewProductActivity, it.message.toString(), Toast.LENGTH_SHORT)
                    .show()
            }
    }
    fun showAlertDialog(context: Context, title: String, message: String) {
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
        alertDialog.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        println("Destroyed")
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun showCustomView(position:Int) {
        val parentLayout: ViewGroup = findViewById(R.id.viewproductactivity)
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.custom_layout,null)
        productId = list[position].id

        val myRef = database!!.getReference("comments").child(productId).child(auth.currentUser?.uid.toString())

        val etComment = view.findViewById<EditText>(R.id.etComment)
        val close_comment = view.findViewById<Button>(R.id.close_btn_comments)
        val etCommentBtn = view.findViewById<Button>(R.id.comment_btn)
        val info = view.findViewById<ImageView>(R.id.comment_info);
        val itemList = mutableListOf<Comments>()

        val popupWindow = PopupWindow(view, ViewGroup.LayoutParams.MATCH_PARENT, (getScreenHeight() / 2.0).toInt(), true)
        popupWindow.isOutsideTouchable = false
        popupWindow.isFocusable = true

        close_comment.setOnClickListener{
            popupWindow.dismiss()
        }
        info.setOnClickListener{
            showAlertDialog(view.context,"Info",getString(R.string.comment_description))
        }


        val recyclerView = view.findViewById<RecyclerView>(R.id.comments_recyclerview)
        recyclerView.layoutManager = LinearLayoutManager(this@ViewProductActivity)
        val adapter = CommentsAdapter(itemList) { item ->
            // Handle click event
        }
        recyclerView.adapter = adapter

        etCommentBtn.setOnClickListener{
            Toast.makeText(applicationContext,etComment.text.toString(),Toast.LENGTH_SHORT).show()
            val uniqueCommentId ="Comments "+UUID.randomUUID().toString()
            val comments = Comments(userName,etComment.text.toString(),productId, DateTimeFormatter.ISO_INSTANT.format(
                Instant.now()),auth.currentUser?.uid,uniqueCommentId,0)

            myRef.child(uniqueCommentId).setValue(comments)
            etComment.setText("")
        }

        val ref = database!!.getReference("comments")
        ref.addValueEventListener(object: ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                itemList.clear()
                if(snapshot.hasChild(productId)) {
                    for (childSnapshot1 in snapshot.children)
                        for (childSnapshot2 in childSnapshot1.children)
                            for (childSnapshot3 in childSnapshot2.children) {
                                val data = childSnapshot3.getValue(Comments::class.java)
                                if (data != null && data.productId.equals(productId)) {
                                    itemList.add(0,data)
                                }
                            }
                    itemList.sortByDescending { it.timestamp } // Sort the list by timestamp in descending order
                }
                recyclerView.adapter?.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }

        })

        // Show the popup window
        popupWindow.showAtLocation(view, Gravity.BOTTOM, 0, 0)

        // Set constraints for the layout
        val layout = view.findViewById<ConstraintLayout>(R.id.comments_main_layout)
        val constraints = ConstraintSet()
        constraints.clone(layout)
        constraints.connect(layout.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        constraints.connect(layout.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        constraints.applyTo(layout)
    }

    private fun getScreenHeight(): Int {
        val displayMetrics = resources.displayMetrics
        return displayMetrics.heightPixels
    }




    override fun onBackPressed() {
        setResult(RESULT_CANCELED)
        finish()
    }
}