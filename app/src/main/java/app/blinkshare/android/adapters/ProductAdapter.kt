package app.blinkshare.android.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.blinkshare.android.*
import app.blinkshare.android.databinding.ItemObjectBinding
import app.blinkshare.android.databinding.ItemProductBinding
import app.blinkshare.android.model.Comments
import app.blinkshare.android.model.Product
import app.blinkshare.android.model.User
import com.astritveliu.boom.Boom
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import org.ocpsoft.prettytime.PrettyTime
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.abs

const val Flight = 0
const val Image = 1

class ProductAdapter(
    private val list: List<Product>,
    private val userId: String,
    private val mListener: OnItemClickListeners,
    private val activity: ViewProductActivity
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private lateinit var url:String
    private lateinit var context: Context
    private val format: NumberFormat = DecimalFormat("00")
    private var storageRef: StorageReference = FirebaseStorage.getInstance().reference
    var duration: Long = 0 // add this variable to hold the duration
    private var productId = ""
    private lateinit var auth: FirebaseAuth
    private var userName = ""


    inner class ViewHolder(val binding: ItemProductBinding, val listener: OnItemClickListeners) :
        RecyclerView.ViewHolder(binding.root) {
        private lateinit var timer: CountDownTimer

        @SuppressLint("SetTextI18n")
        fun onBind(position: Int) {


            binding.imgPhoto.visibility = View.VISIBLE
            if (!list[position].is_home) {
                val imagesRef = storageRef.child(list[position].image!!)
                imagesRef.downloadUrl.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val downloadUri = task.result
//                        println("1: "+downloadUri)
                        Glide.with(context).load(downloadUri).into(binding.imgPhoto)
                    }
                }
            } else {
                if (!list[position].image.isNullOrEmpty()) {
                    val imagesRef = storageRef.child(list[position].image!!)
                    imagesRef.downloadUrl.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val downloadUri = task.result
//                            println("2: "+downloadUri)
                            Glide.with(context).load(downloadUri).into(binding.imgPhoto)
                        }
                    }
                }else {
                    binding.cvProfilePic.visibility = View.GONE
                }
                binding.llLikeDislike.visibility = View.GONE
            }
            binding.tvDescription.text = "Aircraft Type: "+list[position].description


//            binding.tvComments.text = "Comments: "+list[position].comments
            binding.tvDate.visibility = View.VISIBLE
            if (!::timer.isInitialized && !list[position].is_parked) {
                val date1 = Date(list[position].createdDateTime).toUTCTime()
                val date2 = Date().toUTCTime()
                val diff: Long = date1?.time!! - date2?.time!! + duration // add the duration
//                val diff: Long = date1?.time!! - date2?.time!!
                timer = object: CountDownTimer(diff, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        var seconds = millisUntilFinished / 1000
                        var minutes = seconds / 60
                        var hours = minutes / 60
                        minutes %= 60
                        seconds %= 60
                        if (hours < 0) {
                            hours = 0
                        }
                        if (minutes < 0) {
                            minutes = 0
                        }
                        if (seconds < 0) {
                            seconds = 0
                        }
                        binding.tvDate.text =
                            "${format.format(hours)}:${format.format(minutes % 60)}:${
                                format.format(seconds % 60)
                            }"
                    }

                    override fun onFinish() {
                        binding.tvDate.text = "00:00:00"
                    }
                }
                timer.start()
            }
            else if(::timer.isInitialized && list[position].isTimeUpdated){
                timer.cancel()
                list[position].isTimeUpdated = false
                val date1 = Date(list[position].createdDateTime).toUTCTime()
                val date2 = Date().toUTCTime()
                val diff: Long = date1?.time!! - date2?.time!! + duration // add the duration

//                val diff: Long = date1?.time!! - date2?.time!!
                timer = object: CountDownTimer(diff, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        var seconds = millisUntilFinished / 1000
                        var minutes = seconds / 60
                        var hours = minutes / 60
                        minutes %= 60
                        seconds %= 60
                        if (hours < 0) {
                            hours = 0
                        }
                        if (minutes < 0) {
                            minutes = 0
                        }
                        if (seconds < 0) {
                            seconds = 0
                        }
                        binding.tvDate.text =
                            "${format.format(hours)}:${format.format(minutes % 60)}:${
                                format.format(seconds % 60)
                            }"
                    }

                    override fun onFinish() {
                        binding.tvDate.text = "00:00:00"
                    }
                }
                timer.start()
            }


            if (userId == "") {
                binding.imgLikes.visibility = View.GONE
                binding.imgDisLikes.visibility = View.GONE
                binding.ivReport.visibility = View.GONE
                binding.ivDelete.visibility = View.GONE
            } else {
                binding.imgLikes.visibility = View.VISIBLE
                binding.imgDisLikes.visibility = View.VISIBLE
            }
            if (userId == list[position].authToken) {
                binding.ivDelete.visibility = View.VISIBLE
                binding.ivReport.visibility = View.GONE
                binding.ivDelete.setOnClickListener {
                    timer.cancel()
                    listener.onDeleteClick(position)
                }
            } else {
                binding.ivDelete.visibility = View.GONE
//                binding.ivReport.visibility = View.VISIBLE
            }
            if (list[position].is_like) {
                binding.imgLikes.imageTintList
                binding.imgLikes.setColorFilter(
                    ContextCompat.getColor(
                        context,
                        R.color.colorPrimary
                    ), android.graphics.PorterDuff.Mode.SRC_IN
                )
            } else {
                binding.imgLikes.setColorFilter(
                    ContextCompat.getColor(context, R.color.colorWhite),
                    android.graphics.PorterDuff.Mode.SRC_IN
                )
            }

            if (list[position].is_dis_like) {
                binding.imgDisLikes.setColorFilter(
                    ContextCompat.getColor(
                        context,
                        R.color.colorPrimary
                    ), android.graphics.PorterDuff.Mode.SRC_IN
                )
            } else {
                binding.imgDisLikes.setColorFilter(
                    ContextCompat.getColor(
                        context,
                        R.color.colorWhite
                    ), android.graphics.PorterDuff.Mode.SRC_IN
                )
            }
            binding.imgLikes.setOnClickListener {
                listener.onLikeClick(position)
            }
            binding.imgDisLikes.setOnClickListener {
                listener.onDisLikeClick(position)
            }
            binding.ivReport.setOnClickListener {
                listener.onReportClick(position)
            }
//            listener.sendData(position)
        }

    }

    inner class ViewHolderObject(
        val binding: ItemObjectBinding,
        val listener: OnItemClickListeners
    ) :
        RecyclerView.ViewHolder(binding.root) {
        private lateinit var timer: CountDownTimer


        @RequiresApi(Build.VERSION_CODES.O)
        @SuppressLint("SetTextI18n")
        fun onBind(position: Int) {
            val database = FirebaseDatabase.getInstance()
            val db = FirebaseFirestore.getInstance()
            auth = FirebaseAuth.getInstance()
            val itemListMain = mutableListOf<Comments>()
//            listener.sendData(position)
            db.collection("Users").document(auth.currentUser?.uid.toString()).get().addOnCompleteListener {
                try {
                    val user_comment = it.result.toObject(User::class.java)
                    userName = user_comment?.userName ?: ""
                }catch (e:Exception){

                }
            }


            val ref = database.getReference("comments")
            ref.addValueEventListener(object: ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {
                    itemListMain.clear()
                    println("size: "+list.size)
//                    list[position].
                    if(!list.isEmpty()) {
                        if (snapshot.hasChild(list[position].id)) {
                            for (childSnapshot1 in snapshot.children)
                                for (childSnapshot2 in childSnapshot1.children)
                                    for (childSnapshot3 in childSnapshot2.children) {
                                        val data = childSnapshot3.getValue(Comments::class.java)
                                        if (data != null && data.productId.equals(list[position].id)) {
                                            itemListMain.add(0, data)
                                        }
                                    }
                            itemListMain.sortByDescending { it.timestamp } // Sort the list by timestamp in descending order
                        }
                    }
                    println(itemListMain.size)
                    if (!itemListMain.isEmpty()) {
//                        binding.commentingLayout.visibility = View.VISIBLE
                        binding.lastCommentName.visibility = View.VISIBLE
                        binding.lastCommentComment.visibility = View.VISIBLE
                        binding.totalComments.visibility = View.VISIBLE
                        binding.cvProfilePicLastComment.visibility = View.VISIBLE
                        binding.lastCommentComment.text = itemListMain.get(0).comments
                        binding.lastCommentName.text = itemListMain.get(0).userName
                        binding.totalComments.text = "View all ${itemListMain.size} comments"
                        binding.totalComments.setCompoundDrawables(null,null,null,null)
                        binding.totalComments.setBackgroundResource(0)
                        val imagesRef = storageRef.child("Avatars/${itemListMain.get(0).imageUri}.jpg")

                        imagesRef.downloadUrl.addOnCompleteListener { task->
                            if(task.isSuccessful){
//                    hideShowLoading(false)
                                val downloadUri = task.result
                                if(!activity.isDestroyed && itemView.context.applicationContext != null && activity != null) {
//                                    println("ho ja")
//                                    println("Hola "+activity.isDestroyed +" "+activity !=null)
                                    Glide.with(itemView.context.applicationContext).load(downloadUri)
                                        .into(binding.imgProfilePicLastComment)
//                                    binding.imgProfilePicLastComment.setImageURI(downloadUri)
                                }
                            }
                        }

//                        Glide.with(context).load(itemListMain.get(0).imageUri).into(binding.imgProfilePicLastComment)
                    }else{
                        // Load the drawable from R.drawable.icon
                        val iconDrawable = ContextCompat.getDrawable(context, R.drawable.comment_fill0_wght400_grad0_opsz48)

// Create a new drawable with padding
                        val padding = 4 // Set your desired padding value here
                        val paddedDrawable = iconDrawable?.let {
                            it.setBounds(padding, 0, it.intrinsicWidth + padding, it.intrinsicHeight)
                            it
                        }
                        binding.totalComments.visibility = View.VISIBLE
                        binding.lastCommentName.visibility = View.GONE
                        binding.lastCommentComment.visibility = View.GONE
//                        binding.totalComments.visibility = View.GONE
                        binding.cvProfilePicLastComment.visibility = View.GONE
                        binding.totalComments.text = "Be the first to comment"
                        binding.totalComments.setCompoundDrawables(paddedDrawable,null,null,null)
                        binding.totalComments.setBackgroundResource(R.drawable.comment_background)

//                        binding.commentingLayout.visibility = View.GONE
                    }




//                itemList.reverse()


                }


                override fun onCancelled(error: DatabaseError) {
//                Log.w(TAG, "Failed to read value.", error.toException())
                }

            })
            productId = list[position].id

            val myRef = database.getReference("comments").child(productId).child(auth.currentUser?.uid.toString())
            val itemList = mutableListOf<Comments>()
//
//            val recyclerView = view.findViewById<RecyclerView>(R.id.comments_recyclerview)
            val adapter = CommentsAdapter(itemList) { item ->
//            Toast.makeText(this, "Clicked on $item", Toast.LENGTH_SHORT).show()
            }
//            binding.commentsRecyclerview.layoutManager = LinearLayoutManager(context)
//            binding.commentsRecyclerview.adapter = adapter

//            val ref = database.getReference("comments")
//            binding.replyBtn.setOnClickListener{
//                Toast.makeText(context,binding.etReply.text.toString(), Toast.LENGTH_SHORT).show()
//                val comments = Comments(userName,binding.etReply.text.toString(),productId, DateTimeFormatter.ISO_INSTANT.format(
//                    Instant.now()))
//                myRef.child("Comments "+UUID.randomUUID().toString()).setValue(comments)
//                binding.etReply.setText("")
//            }
//            ref.addValueEventListener(object: ValueEventListener {
//
//                override fun onDataChange(snapshot: DataSnapshot) {
//                    itemList.clear()
//                    if(snapshot.hasChild(productId)) {
//                        for (childSnapshot1 in snapshot.children)
//                            for (childSnapshot2 in childSnapshot1.children)
//                                for (childSnapshot3 in childSnapshot2.children) {
//                                    val data = childSnapshot3.getValue(Comments::class.java)
//                                    if (data != null && data.productId.equals(productId)) {
//                                        itemList.add(0,data)
//                                    }
//                                }
//                        itemList.sortByDescending { it.timestamp } // Sort the list by timestamp in descending order
//                    }

//                itemList.reverse()
//                    binding.commentsRecyclerview.adapter?.notifyDataSetChanged()


//                }
//
//
//                override fun onCancelled(error: DatabaseError) {
////                Log.w(TAG, "Failed to read value.", error.toException())
//                }
//
//            })

//            imagesRef.downloadUrl.addOnCompleteListener { task ->
//                if(task.isSuccessful){
////                    hideShowLoading(false)
//                    val downloadUri = task.result
//                    Glide.with(context).load(downloadUri).into(binding.imgProfilePicComment)
//                }
//            }.addOnFailureListener {
////                hideShowLoading(false)
//            }



//        val reply_btn = view.findViewById<Button>(R.id.reply_btn)

//            val etReply = view.findViewById<EditText>(R.id.etReply)
//            val close_reply = view.findViewById<Button>(R.id.close_btn_reply)
//            val etReplyBtn = view.findViewById<Button>(R.id.reply_btn)

            binding.imgPhoto.visibility = View.VISIBLE
            binding.tvDescription.text = "Aircraft Type: "+list[position].description
//            binding.commentItemObject.text = list[position].
            binding.tvComments.text = "Comments: "+list[position].comments
            if (list[position].is_regular){
                binding.tvDate.visibility = View.GONE
                binding.tvComments.visibility = View.VISIBLE
                binding.tvTitle.text = "Regular Charter"
            }
            else if (!list[position].is_parked) {
                binding.tvDate.visibility = View.VISIBLE
                binding.tvComments.visibility = View.GONE
                binding.tvTitle.text = "Empty Leg"
            }

            else {
                binding.tvComments.text = "Tentative Destination: "+list[position].comments
                binding.tvComments.visibility = View.VISIBLE
                binding.tvTitle.text = "Parked Aircraft"
            }
            if(!list[position].photoUrl.isNullOrEmpty()) {
                val imagesRef = storageRef.child(list[position].image!!)
                imagesRef.downloadUrl.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val downloadUri = task.result
//                        println("3: "+downloadUri)
                        url = downloadUri.path.toString()
                        Glide.with(context).load(downloadUri).into(binding.imgPhoto)
                    }
                }
                binding.cvProfilePic.visibility = View.VISIBLE
            }
            else{
                binding.cvProfilePic.visibility = View.GONE
            }
//            binding.addTime.setOnClickListener{
//                duration += 24 * 60 * 60 * 1000 // 24 hours in milliseconds
//                println("AddTime is running!!")
//                list[position].isTimeUpdated = true
//                if (!::timer.isInitialized) {
//                    val date1 = Date(list[position].createdDateTime).toUTCTime()
//                    val date2 = Date().toUTCTime()
//                    val diff: Long = date1?.time!! - date2?.time!! + duration // add the duration
////                val diff: Long = date1?.time!! - date2?.time!!
//                    timer = object: CountDownTimer(diff, 1000) {
//                        override fun onTick(millisUntilFinished: Long) {
//                            var seconds = millisUntilFinished / 1000
//                            var minutes = seconds / 60
//                            var hours = minutes / 60
//                            minutes %= 60
//                            seconds %= 60
//                            if (hours < 0) {
//                                hours = 0
//                            }
//                            if (minutes < 0) {
//                                minutes = 0
//                            }
//                            if (seconds < 0) {
//                                seconds = 0
//                            }
//                            binding.tvDate.text =
//                                "${format.format(hours)}:${format.format(minutes % 60)}:${
//                                    format.format(seconds % 60)
//                                }"
//                        }
//
//                        override fun onFinish() {
//                            binding.tvDate.text = "00:00:00"
//                        }
//                    }
//                    timer.start()
//                }
//                else if(::timer.isInitialized && list[position].isTimeUpdated){
//                    timer.cancel()
//                    list[position].isTimeUpdated = false
//                    val date1 = Date(list[position].createdDateTime).toUTCTime()
//                    val date2 = Date().toUTCTime()
//                    val diff: Long = date1?.time!! - date2?.time!! + duration // add the duration
//
////                val diff: Long = date1?.time!! - date2?.time!!
//                    timer = object: CountDownTimer(diff, 1000) {
//                        override fun onTick(millisUntilFinished: Long) {
//                            var seconds = millisUntilFinished / 1000
//                            var minutes = seconds / 60
//                            var hours = minutes / 60
//                            minutes %= 60
//                            seconds %= 60
//                            if (hours < 0) {
//                                hours = 0
//                            }
//                            if (minutes < 0) {
//                                minutes = 0
//                            }
//                            if (seconds < 0) {
//                                seconds = 0
//                            }
//                            binding.tvDate.text =
//                                "${format.format(hours)}:${format.format(minutes % 60)}:${
//                                    format.format(seconds % 60)
//                                }"
//                        }
//
//                        override fun onFinish() {
//                            binding.tvDate.text = "00:00:00"
//                        }
//                    }
//                    timer.start()
//                }
//
//            }

            if (binding.tvDate.text.toString() == "" && !list[position].is_parked && !list[position].is_regular) {
                if (!::timer.isInitialized) {
                    val date1 = Date(list[position].createdDateTime).toUTCTime()
                    val date2 = Date().toUTCTime()
                    val diff: Long = date1?.time!! - date2?.time!! + duration // add the duration

//                    val diff: Long = date1?.time!! - date2?.time!!
                    timer = object: CountDownTimer(diff, 1000) {
                        override fun onTick(millisUntilFinished: Long) {
                            var seconds = millisUntilFinished / 1000
                            var minutes = seconds / 60
                            var hours = minutes / 60
                            minutes %= 60
                            seconds %= 60
                            if (hours < 0) {
                                hours = 0
                            }
                            if (minutes < 0) {
                                minutes = 0
                            }
                            if (seconds < 0) {
                                seconds = 0
                            }
                            binding.tvDate.text =
                                "${format.format(hours)}:${format.format(minutes % 60)}:${
                                    format.format(seconds % 60)
                                }"
                        }

                        override fun onFinish() {
                            binding.tvDate.text = "00:00:00"
                        }
                    }
                    timer.start()
                }
            }

            if (userId == "") {
                binding.ivReport.visibility = View.GONE
                binding.tvCallUsOn.visibility = View.INVISIBLE
                binding.shareBtn.visibility = View.GONE
                binding.commentingBtn.visibility = View.GONE
                binding.ivDelete.visibility = View.GONE
                binding.ivEdit.visibility = View.GONE
//                binding.commentItemObject.visibility = View.GONE
//                binding.commentNameItemObject.visibility = View.GONE
            }
            if (!userId.isNullOrEmpty() && userId == list[position].authToken) {
                binding.ivDelete.visibility = View.VISIBLE
                binding.ivEdit.visibility = View.VISIBLE
                binding.ivReport.visibility = View.GONE
                binding.tvCallUsOn.visibility = View.INVISIBLE
                binding.shareBtn.visibility = View.GONE
                binding.commentingBtn.visibility = View.VISIBLE
//                binding.commentItemObject.visibility=View.VISIBLE
//                binding.commentNameItemObject.visibility = View.VISIBLE
                if (!list[position].is_parked && !list[position].is_regular)
                    binding.tvDate.visibility =View.VISIBLE
                else
                    binding.tvDate.visibility = View.GONE
                binding.ivDelete.setOnClickListener {
                    if(!list[position].is_parked && !list[position].is_regular)
                        timer.cancel()
                    listener.onDeleteClick(position)
                }
                binding.ivEdit.setOnClickListener {
                    listener.onEditClick(position,url)
                }
            } else if (!userId.isNullOrEmpty()) {
                binding.ivDelete.visibility = View.GONE
//                binding.ivReport.visibility = View.VISIBLE
                binding.tvCallUsOn.visibility = View.VISIBLE
                binding.shareBtn.visibility = View.VISIBLE
                binding.commentingBtn.visibility = View.VISIBLE
                if (list[position].is_parked && !list[position].is_regular)
                    binding.tvDate.visibility = View.GONE
                else
                    binding.tvDate.visibility = View.VISIBLE
                binding.tvSubscribe.visibility = View.VISIBLE
                val filledDrawable = ContextCompat.getDrawable(context, R.drawable.ic_baseline_favorite_24)
                val unfilledDrawable = ContextCompat.getDrawable(context, R.drawable.ic_outline_favorite_border_24)

                if (list[position].isSubscribed) {
                    binding.tvSubscribe.setImageDrawable(filledDrawable)
//                    binding.tvSubscribe.text = "UnSubscribe"
                } else {
                    binding.tvSubscribe.setImageDrawable(unfilledDrawable)

//                    binding.tvSubscribe.text = "Subscribe"
                }
            }

//            binding.flightNote.visibility = View.VISIBLE
            binding.tvDepartureDate.text = "Departure Date: " + list[position].departure_date
            if(list[position].time_of_departure.isNullOrEmpty() || list[position].is_parked) {
                binding.tvDepartureTime.visibility = View.GONE
            }
            else {
                binding.tvDepartureTime.text =
                    "Time of Departure: " + list[position].time_of_departure
                binding.tvDepartureTime.visibility = View.VISIBLE
            }
            binding.tvSeats.text = "Average Seating Capacity: " + list[position].number_of_seats
            if (!list[position].is_parked && !list[position].is_regular) {
                binding.tvDestination.text = "Scheduled to go to: " + list[position].destination
                binding.tvPrice.text = "Estimated Price: €" + list[position].price
                binding.tvEndDate.visibility = View.GONE
            }else{
                binding.tvDepartureDate.visibility = View.GONE
                binding.tvDestination.visibility = View.GONE
                binding.tvPrice.visibility = View.GONE
                if (!list[position].end_date.equals("")) {
                    val milliseconds = list[position].end_date?.toLong() // Example milliseconds value
                    val sdf = SimpleDateFormat("dd/MM/yyyy") // Date format
                    val date = milliseconds?.let { Date(it) } // Create a Date object using milliseconds
                    val formattedDate = sdf.format(date) // Format the Date object as a String
                    //                println(formattedDate)
                    if(list[position].is_parked)
                        binding.tvEndDate.text = "End Date: " + formattedDate
                    else
                        binding.tvEndDate.text = "Proposed Date: " + formattedDate
                }
            }
//            binding.tvCallUsOn.text =
//                "To book, call us on " + if (list[position].call_us_on.isNullOrEmpty()) {
//                    "+41763718903"
//                } else {
//                    list[position].call_us_on
//                }
//            binding.tvCallUsOn.paintFlags = Paint.UNDERLINE_TEXT_FLAG
            binding.ivReport.setOnClickListener {
                listener.onReportClick(position)
            }
            binding.tvSubscribe.setOnClickListener {
                listener.onSubscribeClick(position)
            }
            Boom(binding.tvFindOnMap)
            binding.tvFindOnMap.setOnClickListener {
                listener.onFindAircraft(position)
            }
            Boom(binding.tvCallUsOn)
            binding.tvCallUsOn.setOnClickListener {
                listener.onNumberClicked(position)
            }
            binding.shareBtn.setOnClickListener {
                listener.onShareBtnClicked(position)
            }
            binding.commentingBtn.setOnClickListener{
                listener.onCommentingBtnClicked(position)
            }
            binding.totalComments.setOnClickListener{
                listener.onViewAllComments(position)
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        context = parent.context
        if (viewType == Flight) {
            val binding: ItemObjectBinding = DataBindingUtil.inflate(
                LayoutInflater.from(parent.context), R.layout.item_object,
                parent, false
            )
            return ViewHolderObject(binding, mListener)
        } else {
            val binding: ItemProductBinding = DataBindingUtil.inflate(
                LayoutInflater.from(parent.context), R.layout.item_product,
                parent, false
            )

            return ViewHolder(binding, mListener)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ViewHolderObject -> {
                holder.onBind(position)
            }
            is ViewHolder -> {
                holder.onBind(position)
            }
        }
    }

    override fun getItemCount(): Int = list.size

    override fun getItemViewType(position: Int): Int {
        if (list[position].is_flight) {
            return Flight
        } else {
            return Image
        }
    }

    interface OnItemClickListeners {
        fun onLikeClick(position: Int)
        fun onDisLikeClick(position: Int)
        fun onDeleteClick(position: Int)
        fun onReportClick(position: Int)
        fun onEditClick(position: Int,url:String)
        fun onSubscribeClick(position: Int)
        fun onFindAircraft(position: Int)
        fun onNumberClicked(position: Int)
        fun onShareBtnClicked(position: Int)
        fun onCommentingBtnClicked(position: Int)
        fun onViewAllComments(position: Int)
//        fun sendData(position: Int)
    }
}