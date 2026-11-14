package lk.zenova.pawhope.model

import com.google.firebase.firestore.DocumentId
import java.util.Date

data class Vaccination(
    @DocumentId var id: String = "",
    var name: String = "",
    var dueDate: Date? = null,
    var status: String = "upcoming",
    var doneAt: Date? = null,
    var doneBy: String = "",
    var note: String = ""
)