package app.blinkshare.android

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.blinkshare.android.adapters.CommentsAdapter
import app.blinkshare.android.databinding.CommentsRecyclerviewBinding
import app.blinkshare.android.model.Comments
import app.blinkshare.android.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*
import app.blinkshare.android.databinding.FragmentCommentsBinding


class CommentsFragment : Fragment() {
    private var database:FirebaseDatabase? = null
    private lateinit var auth: FirebaseAuth
    private var userName = ""
    private lateinit var db: FirebaseFirestore
    private var _binding : FragmentCommentsBinding? = null
    private var binding  = _binding



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCommentsBinding.inflate(inflater, container, false)
        db = FirebaseFirestore.getInstance()

        val productId = requireArguments().getString("productId")
        database = FirebaseDatabase.getInstance()
        auth = FirebaseAuth.getInstance()
        db.collection("Users").document(auth.currentUser?.uid.toString()).get().addOnCompleteListener {
            try {
                val user_comment = it.result.toObject(User::class.java)
                userName = user_comment?.userName ?: ""
            }catch (e:Exception){

            }
        }



        println("CommentsFragment:"+productId)
        val myRef = productId?.let { database!!.getReference("comments").child(it).child(auth.currentUser?.uid.toString()) }
//
//
//        val reply_btn = findViewById<Button>(R.id.reply_btn)
//        val etReply = view.findViewById<EditText>(R.id.etReply)
//        val close_reply = view.findViewById<Button>(R.id.close_btn_reply)
//        val etReplyBtn = view.findViewById<Button>(R.id.reply_btn)
        val itemList = mutableListOf<Comments>()
//        lateinit var recyclerView: RecyclerView

//        val recyclerView = view.findViewById<RecyclerView>(R.id.comments_recyclerview)
//        binding?.commentsRecyclerview?.setNestedScrollingEnabled(false);
        val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(requireContext())

//        binding?.commentsRecyclerview?.setLayoutManager(layoutManager)
        val adapter = CommentsAdapter(itemList) { item ->
//            Toast.makeText(this, "Clicked on $item", Toast.LENGTH_SHORT).show()
        }
//        binding?.commentsRecyclerview?.adapter = adapter
        val ref = database!!.getReference("comments")
//        binding!!.replyBtn.setOnClickListener{
//            Toast.makeText(context, binding!!.etReply.text.toString(),Toast.LENGTH_SHORT).show()
//            val comments = Comments(userName,
//                binding!!.etReply.text.toString(),productId, DateTimeFormatter.ISO_INSTANT.format(
//                Instant.now()))
//            myRef?.child("Comments "+ UUID.randomUUID().toString())?.setValue(comments)
//            binding!!.etReply.setText("")
//        }
//        ref.addValueEventListener(object: ValueEventListener {

//            override fun onDataChange(snapshot: DataSnapshot) {
//                itemList.clear()
//                if(productId?.let { snapshot.hasChild(it) } == true) {
//                    for (childSnapshot1 in snapshot.children)
//                        for (childSnapshot2 in childSnapshot1.children)
//                            for (childSnapshot3 in childSnapshot2.children) {
//                                val data = childSnapshot3.getValue(Comments::class.java)
//                                if (data != null && data.productId.equals(productId)) {
//                                    itemList.add(0,data)
//                                }
//                            }
//                    itemList.sortByDescending { it.timestamp } // Sort the list by timestamp in descending order
//                }
//                itemList.reverse()
//                binding?.commentsRecyclerview?.adapter?.notifyDataSetChanged()

//
//            }
//
//
//            override fun onCancelled(error: DatabaseError) {
//                Log.w(TAG, "Failed to read value.", error.toException())
//            }
//
//        })


        // Inflate the layout for this fragment
        return binding?.root
    }

}