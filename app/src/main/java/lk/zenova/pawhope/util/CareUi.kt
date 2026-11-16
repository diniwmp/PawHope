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


    fun reportTitle(type: String): String = when (type) {
        "monthly" -> "Monthly check-in"
        "vaccine" -> "Vaccination"
        "vet" -> "Vet visit"
        "health" -> "Health concern"
        else -> "Care update"
    }

    fun reportColor(type: String): Int = when (type) {
        "monthly" -> R.color.primary_dark
        "vaccine" -> R.color.dark_blue
        "vet" -> R.color.online_green
        "health" -> R.color.red
        else -> R.color.primary_dark
    }

    val detailLabels: Map<String, String> = linkedMapOf(
        "weight" to "Weight (kg)",
        "bodyCondition" to "Condition",
        "eating" to "Eating",
        "behavior" to "Behavior",
        "vaccineName" to "Vaccine",
        "dateGiven" to "Given on",
        "nextDue" to "Next due",
        "clinic" to "Clinic / vet",
        "visitDate" to "Visit date",
        "reason" to "Reason",
        "treatment" to "Treatment / medicine",
        "cost" to "Cost (LKR)"
    )
}