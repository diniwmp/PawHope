package lk.zenova.pawhope.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Animal(
    @DocumentId var id: String = "",
    var reporterId: String = "",
    var guest: Boolean = false,
    var animalType: String = "",              // dog | cat | other
    var condition: String = "",               // healthy | needs_help | injured | critical
    var needType: String = "",
    var description: String = "",
    var photoUrls: List<String> = emptyList(),
    var location: GeoPoint? = null,
    var address: String = "",
    var city: String = "",
    var district: String = "",
    var status: String = "reported",
    @ServerTimestamp var createdAt: Date? = null,
    var updatedAt: Date? = null
)