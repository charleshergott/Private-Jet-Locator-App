package app.blinkshare.android.adapters

import android.graphics.Color
import android.text.Html
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import app.blinkshare.android.R
import app.blinkshare.android.model.Comments
import com.bumptech.glide.Glide
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import app.blinkshare.android.databinding.CommentsRecyclerviewBinding
import app.blinkshare.android.model.Likes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

class CommentsAdapter(private val itemList: List<Comments>, private val clickListener: (Comments) -> Unit) : RecyclerView.Adapter<CommentsAdapter.ViewHolder>() {
    private var storageRef: StorageReference = FirebaseStorage.getInstance().reference
    private var database = Firebase.database
    private var auth = FirebaseAuth.getInstance()
    val dateFormat = SimpleDateFormat("yyyy-MM-dd")
    private var valueEventListenerLikes: ValueEventListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            // Handle onDataChange event
        }

        override fun onCancelled(error: DatabaseError) {
            // Handle onCancelled event
        }
    }
    private var valueEventListener: ValueEventListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            // Handle onDataChange event
        }

        override fun onCancelled(error: DatabaseError) {
            // Handle onCancelled event
        }
    }


//    inner class ViewHolder(val binding: CommentsRecyclerviewBinding) : RecyclerView.ViewHolder(binding.root)


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            CommentsRecyclerviewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
//        val view = LayoutInflater.from(parent.context).inflate(R.layout.comments_recyclerview, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
//        with(holder){
//            with(itemList[position]){
//                binding.commentName.text = Html.fromHtml("<font color='#CBC3E3'><b>" + this.userName + "</b></font>: " + "<font color='#FFFFFF'>" + this.comments + "</font>")
//            }
//            this.binding.executePendingBindings()
//        }
        val item = itemList[position]
//        holder.binding.item = item
        holder.binding.executePendingBindings()
//        holder.binding.executePendingBindings()
        holder.bind(itemList[position], clickListener)
    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    inner class ViewHolder(val binding: CommentsRecyclerviewBinding) :
        RecyclerView.ViewHolder(binding.root) {
        //        private val commentsName: TextView = itemView.findViewById(R.id.commentName)
//        private val commentsComment: TextView = itemView.findViewById(R.id.commentComment)
//        private val imageComments: ImageView = itemView.findViewById(R.id.imgProfilePicComment)
        private var imagesRef: StorageReference = storageRef


        fun bind(item: Comments, clickListener: (Comments) -> Unit) {
            val myRef = database.getReference("comments").child(item.productId!!)
                .child(auth.currentUser?.uid.toString())
            val myRefLikes = database.getReference("likes").child(item.productId!!)
                .child(auth.currentUser?.uid.toString()).child(item.commentId!!)
            var key: String? = null
            var childKey: String? = null

//            binding.commentName.text = Html.fromHtml("<font color='#CBC3E3'><b>" + item.userName + "</b></font>: " + "<font color='#FFFFFF'>" + item.comments + "</font>")
            binding.commentName.text = item.userName
            binding.commentComment.text = item.comments
            val date = dateFormat.parse(item.timestamp!!)
            val calendar = Calendar.getInstance()
            calendar.time = date

//            val currentDate = simpleDate.format(Date(item.timestamp))
//            println("Date: "+"${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH)}-${calendar.get(Calendar.DATE)}")
//            binding.timestampComment.text = "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH)}-${calendar.get(Calendar.DATE)}"
            binding.timestampComment.text = "${calendar.get(Calendar.DATE)}/${calendar.get(Calendar.MONTH)+1}/${calendar.get(Calendar.YEAR)}"
//            if (item.liked!! > 0)
//                binding.likeComment.setImageResource(R.drawable.liked_filled)
//            else
//                binding.likeComment.setImageResource(R.drawable.like_border)

//            binding.likeComment.setOnClickListener {
//                println("checking like functionality")
//                var checkLike = true
//                val checkLikeNum = true
//                myRef.removeEventListener(valueEventListener)
//                myRefLikes.removeEventListener(valueEventListenerLikes)
//                if (item.liked == 0) {
//                    val data = Likes(auth.currentUser?.uid.toString(), true)
//                    item.liked = item.liked!! + 1
//                    myRefLikes.setValue(data).addOnSuccessListener {
//                        println("Success")
//                        valueEventListener = myRef.addValueEventListener(object : ValueEventListener {
//                            override fun onDataChange(snapshot: DataSnapshot) {
//                                if (checkLike) {
//                                    checkLike = false
//                                    for (childSnapshot in snapshot.children) {
////                            val data = Comments(childSnapshot.children)
//                                        val data =
//                                            childSnapshot.getValue(Comments::class.java)
//                                        if (data?.comments.equals(item.comments) && data?.productId.equals(
//                                                item.productId
//                                            ) && data?.userName.equals(item.userName) && data?.timestamp.equals(
//                                                item.timestamp
//                                            )
//                                        ) {
//                                            val updatedCommentVal = Comments(
//                                                item.userName,
//                                                item.comments,
//                                                item.productId,
//                                                item.timestamp,
//                                                item.imageUri,
//                                                item.commentId,
//                                                item.liked
//                                            )
//                                            myRef.child(childSnapshot.key!!)
//                                                .setValue(updatedCommentVal)
//                                        }
//                                    }
//                                }
//                            }
//
//
//                            override fun onCancelled(error: DatabaseError) {
//                                println("chceking")
//                            }
//
//
//                        })
////                        myRef.removeEventListener(valueEventListener)
//
//                    }
//                        .addOnFailureListener { error ->
//                            println("Error: " + error.message)
//                        }
//                }
//                else {
//                    println("next")
//                    valueEventListenerLikes = myRefLikes.addValueEventListener(object : ValueEventListener {
//                        override fun onDataChange(snapshot: DataSnapshot) {
//                            if(checkLike) {
//                                checkLike = false
//                                val dataLike = snapshot.getValue(Likes::class.java)
//                                if (dataLike?.liked == true) {
//                                    binding.likeComment.setImageResource(R.drawable.like_border)
//                                    val valueLikes = Likes(dataLike.userId, false)
//                                    item.liked = item.liked!! - 1
//                                    myRefLikes.setValue(valueLikes).addOnSuccessListener {
//                                        println("Success")
//                                        valueEventListener = myRef.addValueEventListener(object : ValueEventListener {
//                                            override fun onDataChange(snapshot: DataSnapshot) {
//                                                for (childSnapshot in snapshot.children) {
////                            val data = Comments(childSnapshot.children)
//                                                    val data =
//                                                        childSnapshot.getValue(Comments::class.java)
//                                                    if (data?.comments.equals(item.comments) && data?.productId.equals(
//                                                            item.productId
//                                                        ) && data?.userName.equals(item.userName) && data?.timestamp.equals(
//                                                            item.timestamp
//                                                        )
//                                                    ) {
//                                                        val updatedCommentVal = Comments(
//                                                            item.userName,
//                                                            item.comments,
//                                                            item.productId,
//                                                            item.timestamp,
//                                                            item.imageUri,
//                                                            item.commentId,
//                                                            item.liked
//                                                        )
//                                                        myRef.child(childSnapshot.key!!)
//                                                            .setValue(updatedCommentVal)
//                                                    }
//                                                }
//
//                                            }
//
//
//                                            override fun onCancelled(error: DatabaseError) {
//                                                println("chceking")
//                                            }
//
//
//                                        })
////                                        myRef.removeEventListener(valueEventListener)
//
//                                    }
//                                        .addOnFailureListener { error ->
//                                            println("Error: " + error.message)
//                                        }
//                                } else
//                                {
//                                    binding.likeComment.setImageResource(R.drawable.liked_filled)
//                                    val data = Likes(auth.currentUser?.uid.toString(), true)
//                                    item.liked = item.liked!! + 1
//                                    myRefLikes.setValue(data).addOnSuccessListener {
//                                        println("Success")
//                                        valueEventListener = myRef.addValueEventListener(object : ValueEventListener {
//                                            override fun onDataChange(snapshot: DataSnapshot) {
//                                                for (childSnapshot in snapshot.children) {
////                            val data = Comments(childSnapshot.children)
//                                                    val data =
//                                                        childSnapshot.getValue(Comments::class.java)
//                                                    if (data?.comments.equals(item.comments) && data?.productId.equals(
//                                                            item.productId
//                                                        ) && data?.userName.equals(item.userName) && data?.timestamp.equals(
//                                                            item.timestamp
//                                                        )
//                                                    ) {
//                                                        val updatedCommentVal = Comments(
//                                                            item.userName,
//                                                            item.comments,
//                                                            item.productId,
//                                                            item.timestamp,
//                                                            item.imageUri,
//                                                            item.commentId,
//                                                            item.liked
//                                                        )
//                                                        myRef.child(childSnapshot.key!!)
//                                                            .setValue(updatedCommentVal)
//                                                    }
//                                                }
//
//                                            }
//
//
//                                            override fun onCancelled(error: DatabaseError) {
//                                                println("chceking")
//                                            }
//
//
//                                        })
////                                        myRef.removeEventListener(valueEventListener)
//
//                                    }
//                                        .addOnFailureListener { error ->
//                                            println("Error: " + error.message)
//                                        }
//                                }
//                            }
//                        }
//
//                        override fun onCancelled(error: DatabaseError) {
//
//                        }
//
//                    })
////                    myRefLikes.removeEventListener(valueEventListenerLikes)
//
////                    myRefLikes.addValueEventListener(object : ValueEventListener {
////                        override fun onDataChange(snapshot: DataSnapshot) {
////                            println("LIked: " + item.liked)
////                            if (item.liked == 0) {
////                                item.liked = item.liked!! + 1
////                            }
////                            else {
////                                println(snapshot.value.toString())
////                                for (childSnapshot in snapshot.children) {
////                                    val data = childSnapshot.getValue<Likes>(Likes::class.java)
////                                    println("Testing: " + data?.liked)
////                                }
////                                if (item.liked!! > 0) {
////                                    binding.likeComment.setImageResource(R.drawable.ic_outline_favorite_border_24)
////                                    item.liked = item.liked!! + 1
////                                    myRef.addValueEventListener(object : ValueEventListener {
////                                        override fun onDataChange(snapshot: DataSnapshot) {
////                                            for (childSnapshot in snapshot.children) {
//////                            val data = Comments(childSnapshot.children)
////                                                val data =
////                                                    childSnapshot.getValue(Comments::class.java)
////                                                if (data?.comments.equals(item.comments) && data?.productId.equals(
////                                                        item.productId
////                                                    ) && data?.userName.equals(item.userName) && data?.timestamp.equals(
////                                                        item.timestamp
////                                                    )
////                                                ) {
////                                                    val updatedCommentVal = Comments(
////                                                        item.userName,
////                                                        item.comments,
////                                                        item.productId,
////                                                        item.timestamp,
////                                                        item.imageUri,
////                                                        item.commentId,
////                                                        item.liked
////                                                    )
////                                                    myRef.child(childSnapshot.key!!)
////                                                        .setValue(updatedCommentVal)
////                                                }
////                                            }
////
////                                        }
////
////
////                                        override fun onCancelled(error: DatabaseError) {
////                                            println("chceking")
////                                        }
////
////
////                                    })
////                                }
////                            }
////
////
////                        }
////
////                        override fun onCancelled(error: DatabaseError) {
////
////                        }
////
////                    })
//                }
//            }
                binding.deleteComment.setOnClickListener {
                    myRef.addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            for (childSnapshot in snapshot.children) {
//                            val data = Comments(childSnapshot.children)
                                val data = childSnapshot.getValue(Comments::class.java)
                                if (data?.comments.equals(item.comments) && data?.productId.equals(
                                        item.productId
                                    ) && data?.userName.equals(item.userName) && data?.timestamp.equals(
                                        item.timestamp
                                    )
                                ) {
                                    println("delete:" + childSnapshot.key)

//                                println("data: "+data?.comments+" "+snapshot.key +" "+childSnapshot.key)
                                    key = snapshot.key.toString()
                                    childKey = childSnapshot.key.toString()
                                    childSnapshot.ref.removeValue()
//                                if(childSnapshot.children.)
//
//
                                }
                            }
//                        if (key !=null && childKey !=null) {
//                            println("key:"+ key)
//                            val myRef_remove = database.getReference("comments").child(item.productId!!).child(auth.currentUser?.uid.toString()).orderByChild(key!!).equalTo(childKey!!)
//                            myRef_remove.addValueEventListener(object : ValueEventListener {
//                                override fun onDataChange(dataSnapshot: DataSnapshot) {
//                                    println(snapshot.key+"working")
//
//                                    for (snapshotRemove in dataSnapshot.children) {
//                                        println("multiple")
//                                        snapshotRemove.ref.removeValue().addOnCompleteListener {
//                                            println("completed")
//                                        }.addOnFailureListener{
//                                            println("failure")
//                                        }.addOnCanceledListener {
//                                            println("cancel")
//                                        }.addOnSuccessListener {
//                                            println("success")
//                                        }
//                                    }
//                                    if(!dataSnapshot.hasChildren() && childKey.equals(item.commentId)){
//                                        println("single "+dataSnapshot.key+"size: "+dataSnapshot.childrenCount)
//                                        dataSnapshot.ref.removeValue().addOnCompleteListener {
//                                            println("completed")
//                                        }.addOnFailureListener{
//                                            println("failure")
//                                        }.addOnCanceledListener {
//                                            println("cancel")
//                                        }.addOnSuccessListener {
//                                            println("success")
//                                        }
//                                    }
//                                }
//                                override fun onCancelled(databaseError: DatabaseError) {
//                                    // handle error
//                                    println(databaseError.message)
//                                }
//                            })
//                            println("done")
//                        }

                        }


                        override fun onCancelled(error: DatabaseError) {
                            println("chceking")
                        }


                    })
                }
//            binding.replyTxt.setOnClickListener{
//
//            }
//            commentsName.text = item.userName +": "+item.comments
//            commentsName.text = item.userName
//            commentsComment.text = item.comments
                imagesRef = storageRef.child("Avatars/${item.imageUri}.jpg")

                imagesRef.downloadUrl.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
//                    hideShowLoading(false)
                        val downloadUri = task.result
                        Glide.with(itemView.context).load(downloadUri)
                            .into(binding.imgProfilePicComment)
                    }
                }.addOnFailureListener {
//                hideShowLoading(false)
                }
//            imageComments.setImageURI(item.imageUri)
                itemView.setOnClickListener { clickListener(item) }
//            itemView.setOnClickListener { clickListener(item.comments) }

            }
        }
    }
