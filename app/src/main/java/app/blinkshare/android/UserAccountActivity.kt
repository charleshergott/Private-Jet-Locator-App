package app.blinkshare.android

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResult
import androidx.appcompat.app.AlertDialog
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.customtabs.CustomTabsSession
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import app.blinkshare.android.databinding.ActivityUserAccountBinding
import app.blinkshare.android.utills.AppUtils
import app.blinkshare.android.utills.isNetworkAvailable
import com.bumptech.glide.Glide
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class UserAccountActivity : AppCompatActivity() {
    lateinit var binding: ActivityUserAccountBinding
    val CUSTOM_TAB_PACKAGE_NAME = "com.android.chrome"
    private var mCustomTabsServiceConnection: CustomTabsServiceConnection? = null
    private var mClient: CustomTabsClient? = null
    private var mCustomTabsSession: CustomTabsSession? = null
    private lateinit var storageRef: StorageReference
    private lateinit var auth: FirebaseAuth
    val activityLauncher: BaseActivityResult<Intent, ActivityResult> =
        BaseActivityResult.registerActivityForResult(this)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserAccountBinding.inflate(layoutInflater)


        auth = FirebaseAuth.getInstance()
        storageRef = FirebaseStorage.getInstance().reference
        setContentView(binding.root)
        initCustomTab()
        initListeners()
        if(isNetworkAvailable()) {
            getAvatar()
        }
        else{
            hideShowLoading(false)
            val dialog = NetworkPopUp()
            dialog.show(supportFragmentManager, "NetworkPopUp")
        }
    }

    private fun getAvatar(){
        hideShowLoading(true)
        val imagesRef = storageRef.child("Avatars/${auth.currentUser?.uid}.jpg")
        imagesRef.downloadUrl.addOnCompleteListener { task ->
            if(task.isSuccessful){
                hideShowLoading(false)
                val downloadUri = task.result
                Glide.with(applicationContext).load(downloadUri).into(binding.imgProfilePic)
            }
        }.addOnFailureListener {
            hideShowLoading(false)
        }
    }

    private fun initListeners(){
        binding.llAccountSettings.setOnClickListener {
            startActivity(Intent(this@UserAccountActivity, AccountSettingsActivity::class.java))
        }
        binding.llMyProducts.setOnClickListener {
            val myIntent = Intent(this@UserAccountActivity, ViewProductActivity::class.java)
            myIntent.putExtra("isFromProfile",true)
            activityLauncher.launch(
                myIntent,
                object : BaseActivityResult.OnActivityResult<ActivityResult> {
                    @SuppressLint("NotifyDataSetChanged")
                    override fun onActivityResult(result: ActivityResult) {
                        if (result.resultCode == Activity.RESULT_OK) {
//                                        val mLatitudeCustom =
//                                            result.data!!.getDoubleExtra("lat", 0.0).toString()
//                                        getSingleProduct(mLatitudeCustom)
                            if (result.data != null && result.data!!.hasExtra("zoom")) {
                                val sIntent = Intent()
                                sIntent.putExtra("latitude", result.data!!.getDoubleExtra("latitude", 0.0))
                                sIntent.putExtra("longitude",result.data!!.getDoubleExtra("longitude", 0.0))
                                sIntent.putExtra("zoom", true)
                                setResult(RESULT_OK, sIntent)
                                finish()

                            } else {
                                setResult(RESULT_CANCELED)
                                finish()
                            }
                        }
                    }
                })
        }
//        binding.climateJetClubBtn.setOnClickListener {
//            startActivity(Intent(applicationContext, ClimateJetClubActivity::class.java))
//        }
        binding.btnLogout.setOnClickListener {
            val builder = AlertDialog.Builder(this@UserAccountActivity)
            builder.setTitle("Private Jet Locator")
            builder.setMessage("Are you sure you want to logout?")
            builder.setPositiveButton("Yes") { dialog, which ->
                FirebaseAuth.getInstance().signOut()
                AppUtils().logoutUser(this@UserAccountActivity)
                this.setOnBoarding(true)
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags =
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            builder.setNegativeButton("No") { dialog, which ->
            }
            builder.show()
        }
        binding.tvFaq.setOnClickListener {
            val customTabsIntent = CustomTabsIntent.Builder(mCustomTabsSession)
                .setToolbarColor(resources.getColor(R.color.colorBg))
                .setShowTitle(true)
                .build()

            customTabsIntent.launchUrl(this, Uri.parse("https://fly-c2lean.com/faq"))
        }
        binding.tvTermsConditions.setOnClickListener {
            val customTabsIntent = CustomTabsIntent.Builder(mCustomTabsSession)
                .setToolbarColor(resources.getColor(R.color.colorBg))
                .setShowTitle(true)
                .build()

            customTabsIntent.launchUrl(
                this,
                Uri.parse("https://climatejet.club/app-terms-and-conditions")
            )
        }
        binding.tvPrivacyPolicy.setOnClickListener {
            val customTabsIntent = CustomTabsIntent.Builder(mCustomTabsSession)
                .setToolbarColor(resources.getColor(R.color.colorBg))
                .setShowTitle(true)
                .build()

            customTabsIntent.launchUrl(
                this,
                Uri.parse("https://fly-c2lean.com/privacy-policy")
            )
        }
        binding.tvContacts.setOnClickListener {
            val customTabsIntent = CustomTabsIntent.Builder(mCustomTabsSession)
                .setToolbarColor(resources.getColor(R.color.colorBg))
                .setShowTitle(true)
                .build()

            customTabsIntent.launchUrl(
                this,
                Uri.parse("http://www.climatejet.club")
            )
        }
        binding.tvShareWithFriends.setOnClickListener {
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.type = "text/plain"
            shareIntent.putExtra(Intent.EXTRA_TEXT, "https://fly-c2lean.com/");
            startActivity(Intent.createChooser(shareIntent, "Select an app to share"))
        }
        binding.tvInstagramLink.setOnClickListener {
            openLink("https://www.instagram.com/fly_c2lean/")
        }
        binding.tvLinkedInLink.setOnClickListener{
            openLink("https://www.linkedin.com/company/c2leanbrokers/")
        }
        binding.tvFacebookLink.setOnClickListener{
            openLink("https://www.facebook.com/flyc2lean")
        }
        binding.rateApp.setOnClickListener{
            val uri: Uri = Uri.parse("market://details?id=$packageName")
            val goToMarket = Intent(Intent.ACTION_VIEW, uri)
            // To count with Play market backstack, After pressing back button,
            // to taken back to our application, we need to add following flags to intent.
            goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or
                    Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            try {
                startActivity(goToMarket)
            } catch (e: ActivityNotFoundException) {
                startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse("http://play.google.com/store/apps/details?id=$packageName")))
            }
        }
        binding.imgBackBtn.setOnClickListener {
            onBackPressed()
        }
    }
    private fun initCustomTab() {
        mCustomTabsServiceConnection = object : CustomTabsServiceConnection() {
            override fun onCustomTabsServiceConnected(
                componentName: ComponentName,
                customTabsClient: CustomTabsClient
            ) {
                //Pre-warming
                mClient = customTabsClient
                mClient?.warmup(0L)
                mCustomTabsSession = mClient?.newSession(null)
            }

            override fun onServiceDisconnected(name: ComponentName) {
                mClient = null
            }
        }

        CustomTabsClient.bindCustomTabsService(
            this, CUSTOM_TAB_PACKAGE_NAME,
            mCustomTabsServiceConnection as CustomTabsServiceConnection
        )
    }

    override fun onResume() {
        super.onResume()
        if(isNetworkAvailable()) {
            getAvatar()
        }
        else{
            hideShowLoading(false)
            val dialog = NetworkPopUp()
            dialog.show(supportFragmentManager, "NetworkPopUp")
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
}