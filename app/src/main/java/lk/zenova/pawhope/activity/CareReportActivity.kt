package lk.zenova.pawhope.activity

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import lk.zenova.pawhope.R
import lk.zenova.pawhope.adapter.PhotoAdapter
import lk.zenova.pawhope.util.CareUi
import lk.zenova.pawhope.util.DateUtil
import lk.zenova.pawhope.util.ImageUtils
import lk.zenova.pawhope.util.PhotoPicker
import java.util.Date


class CareReportActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_REQUEST_ID = "request_id"
        const val EXTRA_ANIMAL_ID = "animal_id"
        const val EXTRA_TYPE = "type"
    }

    private lateinit var requestId: String
    private lateinit var animalId: String
    private lateinit var type: String

    private lateinit var photoPicker: PhotoPicker
    private lateinit var photoAdapter: PhotoAdapter
    private lateinit var photoHint: TextView
    private lateinit var btnSubmit: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var notesInput: TextInputEditText

    private lateinit var weightInput: TextInputEditText
    private lateinit var chipBodyGroup: ChipGroup
    private lateinit var chipEatGroup: ChipGroup
    private lateinit var chipBehaviorGroup: ChipGroup

    private lateinit var vaccineNameInput: TextInputEditText
    private lateinit var vacClinicInput: TextInputEditText
    private lateinit var btnVacDate: MaterialButton
    private lateinit var btnVacNext: MaterialButton
    private var vacDate = DateUtil.today()
    private var vacNext = ""

    private lateinit var vetClinicInput: TextInputEditText
    private lateinit var vetReasonInput: TextInputEditText
    private lateinit var vetTreatmentInput: TextInputEditText
    private lateinit var vetCostInput: TextInputEditText
    private lateinit var btnVetDate: MaterialButton
    private var vetDate = DateUtil.today()

    private lateinit var chipSymptoms: ChipGroup
    private lateinit var chipSeverity: ChipGroup

    private val photoRequired get() = type == "monthly" || type == "vaccine"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_care_report)

        val reqId = intent.getStringExtra(EXTRA_REQUEST_ID)
        val aId = intent.getStringExtra(EXTRA_ANIMAL_ID)
        if (reqId.isNullOrEmpty() || aId.isNullOrEmpty()) {
            finish()
            return
        }
        requestId = reqId
        animalId = aId
        type = intent.getStringExtra(EXTRA_TYPE) ?: "update"

        bindViews()
        setupPhotos()
        setupForType()

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        btnSubmit.setOnClickListener { submit() }
    }

    private fun bindViews() {
        photoHint = findViewById(R.id.photoHint)
        btnSubmit = findViewById(R.id.btnSubmit)
        progressBar = findViewById(R.id.progressBar)
        notesInput = findViewById(R.id.notesInput)

        weightInput = findViewById(R.id.weightInput)
        chipBodyGroup = findViewById(R.id.chipBodyGroup)
        chipEatGroup = findViewById(R.id.chipEatGroup)
        chipBehaviorGroup = findViewById(R.id.chipBehaviorGroup)

        vaccineNameInput = findViewById(R.id.vaccineNameInput)
        vacClinicInput = findViewById(R.id.vacClinicInput)
        btnVacDate = findViewById(R.id.btnVacDate)
        btnVacNext = findViewById(R.id.btnVacNext)

        vetClinicInput = findViewById(R.id.vetClinicInput)
        vetReasonInput = findViewById(R.id.vetReasonInput)
        vetTreatmentInput = findViewById(R.id.vetTreatmentInput)
        vetCostInput = findViewById(R.id.vetCostInput)
        btnVetDate = findViewById(R.id.btnVetDate)

        chipSymptoms = findViewById(R.id.chipSymptoms)
        chipSeverity = findViewById(R.id.chipSeverity)
    }

    private fun setupPhotos() {
        photoAdapter = PhotoAdapter(this) { position -> photoPicker.removeAt(position) }
        photoPicker = PhotoPicker(this, 3) { refreshPhotos() }

        val recycler = findViewById<RecyclerView>(R.id.photoRecycler)
        recycler.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recycler.adapter = photoAdapter
        findViewById<View>(R.id.addPhotoCard).setOnClickListener { photoPicker.show() }
        refreshPhotos()
    }

    private fun refreshPhotos() {
        photoAdapter.setItems(photoPicker.photos)
        val need = if (photoRequired) " (at least 1)" else " (optional)"
        photoHint.text = "${photoPicker.photos.size}/3 photos$need"
    }

    private fun setupForType() {
        findViewById<TextView>(R.id.titleText).text = CareUi.reportTitle(type)
        findViewById<TextView>(R.id.subtitleText).text = when (type) {
            "monthly" -> "Show us how your animal is doing this month"
            "vaccine" -> "Add the vaccine details and a photo as proof"
            "vet" -> "Tell us about the visit and treatment"
            "health" -> "The PawHope team will be alerted and will reply"
            else -> "Share how your animal is doing"
        }

        findViewById<View>(R.id.monthlyGroup).visibility = if (type == "monthly") View.VISIBLE else View.GONE
        findViewById<View>(R.id.vaccineGroup).visibility = if (type == "vaccine") View.VISIBLE else View.GONE
        findViewById<View>(R.id.vetGroup).visibility = if (type == "vet") View.VISIBLE else View.GONE
        findViewById<View>(R.id.healthGroup).visibility = if (type == "health") View.VISIBLE else View.GONE

        findViewById<TextInputLayout>(R.id.notesLayout).hint = when (type) {
            "monthly" -> "Anything else to share? (optional)"
            "vaccine" -> "Notes (optional)"
            "vet" -> "Notes (optional)"
            "health" -> "Describe what you noticed"
            else -> "What's new?"
        }

        btnVacDate.text = "Date given: $vacDate"
        btnVacDate.setOnClickListener {
            DateUtil.pick(this, "Date given") { vacDate = it; btnVacDate.text = "Date given: $it" }
        }
        btnVacNext.text = "Next vaccine due: not set"
        btnVacNext.setOnClickListener {
            DateUtil.pick(this, "Next vaccine due") { vacNext = it; btnVacNext.text = "Next vaccine due: $it" }
        }
        btnVetDate.text = "Visit date: $vetDate"
        btnVetDate.setOnClickListener {
            DateUtil.pick(this, "Visit date") { vetDate = it; btnVetDate.text = "Visit date: $it" }
        }
    }

    private fun checkedText(group: ChipGroup): String {
        val id = group.checkedChipId
        return if (id == View.NO_ID) "" else findViewById<Chip>(id).text.toString()
    }

    private fun text(input: TextInputEditText): String = input.text?.toString()?.trim().orEmpty()


    private fun submit() {
        val notes = text(notesInput)
        val details = linkedMapOf<String, String>()
        var symptoms = emptyList<String>()
        var severity = ""

        when (type) {
            "monthly" -> {
                val weight = text(weightInput)
                if (weight.isNotEmpty() && weight.toDoubleOrNull() == null) {
                    weightInput.error = "Enter a valid number"
                    return
                }
                details["weight"] = weight
                details["bodyCondition"] = checkedText(chipBodyGroup)
                details["eating"] = checkedText(chipEatGroup)
                details["behavior"] = checkedText(chipBehaviorGroup)
            }
            "vaccine" -> {
                val name = text(vaccineNameInput)
                if (name.isEmpty()) {
                    vaccineNameInput.error = "Enter the vaccine name"
                    return
                }
                details["vaccineName"] = name
                details["dateGiven"] = vacDate
                details["clinic"] = text(vacClinicInput)
                details["nextDue"] = vacNext
            }
            "vet" -> {
                val reason = text(vetReasonInput)
                if (reason.isEmpty()) {
                    vetReasonInput.error = "Tell us the reason for the visit"
                    return
                }
                details["clinic"] = text(vetClinicInput)
                details["visitDate"] = vetDate
                details["reason"] = reason
                details["treatment"] = text(vetTreatmentInput)
                details["cost"] = text(vetCostInput)
            }
            "health" -> {
                symptoms = chipSymptoms.checkedChipIds.map { findViewById<Chip>(it).text.toString() }
                severity = when (chipSeverity.checkedChipId) {
                    R.id.chip_sev_mild -> "mild"
                    R.id.chip_sev_moderate -> "moderate"
                    R.id.chip_sev_severe -> "severe"
                    else -> ""
                }
                if (symptoms.isEmpty() && notes.isEmpty()) {
                    toast("Please select a symptom or describe what you noticed")
                    return
                }
                if (severity.isEmpty()) {
                    toast("Please tell us how serious it is")
                    return
                }
            }
            else -> {
                if (notes.isEmpty() && photoPicker.photos.isEmpty()) {
                    toast("Please add a note or a photo")
                    return
                }
            }
        }

        if (photoRequired && photoPicker.photos.isEmpty()) {
            toast(if (type == "vaccine") "Please add a photo of the vaccination proof" else "Please add a photo of your animal")
            return
        }

        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            toast("Please sign in again")
            return
        }

        setLoading(true)

        ImageUtils.uploadAll(this, photoPicker.photos.toList(), "care/$requestId", type) { urls, error ->
            if (urls == null) {
                setLoading(false)
                toast(error ?: "Upload failed")
                return@uploadAll
            }
            saveReport(user.uid, notes, details, symptoms, severity, urls)
        }
    }

    private fun saveReport(
        uid: String,
        notes: String,
        details: Map<String, String>,
        symptoms: List<String>,
        severity: String,
        urls: List<String>
    ) {
        val db = FirebaseFirestore.getInstance()
        val reqRef = db.collection("helpRequests").document(requestId)

        val data = hashMapOf<String, Any>(
            "type" to type,
            "text" to notes,
            "symptoms" to symptoms,
            "severity" to severity,
            "photoUrl" to (urls.firstOrNull() ?: ""),
            "photoUrls" to urls,
            "details" to details,
            "authorId" to uid,
            "animalId" to animalId,
            "status" to if (type == "health") "open" else "info",
            "adminReply" to "",
            "createdAt" to FieldValue.serverTimestamp()
        )

        val batch = db.batch()
        batch.set(reqRef.collection("updates").document(), data)
        if (type == "monthly") {
            batch.update(
                reqRef,
                mapOf(
                    "lastCheckInAt" to FieldValue.serverTimestamp(),
                    "nextCheckInDue" to Date(System.currentTimeMillis() + 30L * 24 * 3600 * 1000)
                )
            )
        }

        batch.commit()
            .addOnSuccessListener {
                setLoading(false)
                toast(
                    if (type == "health" && severity == "severe")
                        "The PawHope team has been alerted urgently"
                    else "Sent. Thank you for keeping us updated 🐾"
                )
                finish()
            }
            .addOnFailureListener { e ->
                setLoading(false)
                toast("Couldn't send: ${e.message}")
            }
    }

    private fun setLoading(loading: Boolean) {
        btnSubmit.isEnabled = !loading
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}