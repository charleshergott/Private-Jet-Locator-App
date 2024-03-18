package app.blinkshare.android.model

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import java.sql.Timestamp

data class Comments(
    var userName: String? = "",
    var comments: String? = "",
    var productId:String? = "",
    var timestamp: String? = "",
    var imageUri: String? = "",
    var commentId: String? = "",
    var liked: Int? = 0
)