package app.blinkshare.android

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.URLUtil
import android.widget.Toast
import androidx.fragment.app.Fragment
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

fun Context.setOnBoarding(value: Boolean) {
    getSharedPreferences("blink_share", Context.MODE_PRIVATE)
        .edit().apply { putBoolean("on_boarding", value); apply() }
}

fun Context.getOnBoarding(): Boolean {
    getSharedPreferences("blink_share", Context.MODE_PRIVATE)
        ?.getBoolean("on_boarding", true)?.let { return it }
    return true
}

fun Context.setIsAdmin(value: Boolean) {
    getSharedPreferences("blink_share", Context.MODE_PRIVATE)
        .edit().apply { putBoolean("is_admin", value); apply() }
}

fun Context.getIsAdmin(): Boolean {
    getSharedPreferences("blink_share", Context.MODE_PRIVATE)
        ?.getBoolean("is_admin", true)?.let { return it }
    return false
}

@SuppressLint("SimpleDateFormat")
fun timeWithUTC(date_string: String): String {
    val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    format.timeZone = TimeZone.getTimeZone("UTC")
    var myDate: Date? = null
    try {
        myDate = format.parse(date_string)
//        println(myDate)
    } catch (e: ParseException) {
        e.printStackTrace()
    }

    val timeFormat = SimpleDateFormat("MM dd yyyy - hh:mm aa")

    return timeFormat.format(myDate)

}

@SuppressLint("SimpleDateFormat")
fun Date.toUTCTime(): Date? {
    val date_string: String = this.toFormat("yyyy-MM-dd HH:mm:ss")
    val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    format.timeZone = TimeZone.getTimeZone("UTC")
    try {
        return format.parse(date_string)
    } catch (e: ParseException) {
        e.printStackTrace()
    }
    return null
}

fun String.toDate(format: String = "yyyy-MM-dd HH:mm:ss"): Date? {
    val format = SimpleDateFormat(format)
    return try {
        format.parse(this)
    } catch (e: Exception) {
        null
    }
}

fun Date.toFormat(format: String = "yyyy-MM-dd"): String {
    val format = SimpleDateFormat(format)
    return try {
        format.format(this)
    } catch (e: Exception) {
        ""
    }
}

fun Context.shareLink(text: String) {

    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }

    val shareIntent = Intent.createChooser(sendIntent, null)
    this.startActivity(shareIntent)
}

fun Context.openLink(url: String?) {
    try {
        if (!URLUtil.isValidUrl(url)) {
            Toast.makeText(this, " This is not a valid link", Toast.LENGTH_LONG).show()
        } else {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            startActivity(intent)
        }
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(
            this,
            " You don't have any browser to open web page",
            Toast.LENGTH_LONG
        ).show()
    }
}

fun Context.openYoutubeLink(youtubeID: String) {
    val intentApp = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:" + youtubeID))
    val intentBrowser = Intent(Intent.ACTION_VIEW, Uri.parse("http://www.youtube.com/watch?v=" + youtubeID))
    try {
        this.startActivity(intentApp)
    } catch (ex: ActivityNotFoundException) {
        this.startActivity(intentBrowser)
    }

}