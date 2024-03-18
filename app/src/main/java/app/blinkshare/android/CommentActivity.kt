package app.blinkshare.android

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction

class CommentActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comment)
//        val productId = intent.getStringExtra("productId")
//
//        val bundle = Bundle()
//        bundle.putString("productId", productId)
//        val fragment = CommentsFragment()
//        fragment.setArguments(bundle)
//        val fragmentManager: FragmentManager = supportFragmentManager
//        val fragmentTransaction: FragmentTransaction =
//            fragmentManager.beginTransaction()
//        fragmentTransaction.replace(R.id.container_view_test, fragment)
//        fragmentTransaction.commit()
    }
}