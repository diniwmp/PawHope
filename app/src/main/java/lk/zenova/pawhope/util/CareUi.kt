package lk.zenova.pawhope.util

import lk.zenova.pawhope.R

object CareUi {

    fun requestStatusLabel(status: String): String = when (status) {
        "approved" -> "Approved"
        "rejected" -> "Not approved"
        "completed" -> "Completed"
        else -> "Under review"
    }

    fun requestStatusColor(status: String): Int = when (status) {
        "approved" -> R.color.online_green
        "rejected" -> R.color.red
        "completed" -> R.color.dark_blue
        else -> R.color.limit_stock_color
    }

    fun helpTypeLabel(type: String): String = when (type) {
        "foster" -> "Foster"
        "food" -> "Food & water"
        "medical" -> "Vet / medical"
        "visit" -> "Visit & check"
        else -> "Adopt"
    }

    fun animalTypeRes(type: String): Int = when (type) {
        "dog" -> R.string.type_dog
        "cat" -> R.string.type_cat
        else -> R.string.type_other
    }
}