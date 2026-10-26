package lk.zenova.pawhope.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class User(
    var uid: String = "",
    var name: String = "",
    var mobile: String = "",
    var email: String = "",
    var profilePicUrl: String = "",
    var fcmToken: String = "",
    @ServerTimestamp var createdAt: Date? = null
)