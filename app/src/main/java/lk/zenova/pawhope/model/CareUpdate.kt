package lk.zenova.pawhope.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class CareUpdate(
    @DocumentId var id: String = "",
    var type: String = "update",
    var text: String = "",
    var symptoms: List<String> = emptyList(),
    var severity: String = "",
    var photoUrl: String = "",
    var authorId: String = "",
    var animalId: String = "",
    var status: String = "info",
    var adminReply: String = "",
    var repliedAt: Date? = null,
    @ServerTimestamp var createdAt: Date? = null
)