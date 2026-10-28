package lk.zenova.pawhope.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class HelpRequest(
    @DocumentId var id: String = "",
    var animalId: String = "",
    var helperId: String = "",
    var helperName: String = "",
    var helperEmail: String = "",
    var type: String = "",                 // adopt | foster | food | medical | visit
    var message: String = "",
    var status: String = "pending",        // pending | approved | rejected | completed
    @ServerTimestamp var createdAt: Date? = null
)