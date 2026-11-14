package lk.zenova.pawhope.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class AppNotification(
    @DocumentId var id: String = "",
    var title: String = "",
    var body: String = "",
    var type: String = "general",          // vaccine | health | adoption | general
    var animalId: String = "",
    var read: Boolean = false,
    @ServerTimestamp var createdAt: Date? = null
)