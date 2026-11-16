package lk.zenova.pawhope.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class CareUpdate(
    @DocumentId var id: String = "",
    var type: String = "update",           // update | monthly | vaccine | vet | health
    var text: String = "",
    var symptoms: List<String> = emptyList(),
    var severity: String = "",
    var photoUrl: String = "",
    var photoUrls: List<String> = emptyList(),
    var details: Map<String, String> = emptyMap(),
    var authorId: String = "",
    var animalId: String = "",
    var status: String = "info",           // info | open | resolved
    var adminReply: String = "",
    var repliedAt: Date? = null,
    @ServerTimestamp var createdAt: Date? = null
)