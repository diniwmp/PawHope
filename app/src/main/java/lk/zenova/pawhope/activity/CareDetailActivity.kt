package lk.zenova.pawhope.activity

import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import lk.zenova.pawhope.R
import lk.zenova.pawhope.adapter.UpdateAdapter
import lk.zenova.pawhope.adapter.VaccinationAdapter
import lk.zenova.pawhope.model.Animal
import lk.zenova.pawhope.model.CareUpdate
import lk.zenova.pawhope.model.HelpRequest
import lk.zenova.pawhope.model.Vaccination
import lk.zenova.pawhope.util.CareUi
import lk.zenova.pawhope.util.ImageUtils
import kotlin.concurrent.thread

class CareDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_REQUEST_ID = "request_id"
    }

    private val db = FirebaseFirestore.getInstance()
    private lateinit var requestId: String
    private var helpRequest: HelpRequest? = null

    private var updatesReg: ListenerRegistration? = null
    private var vaccinesReg: ListenerRegistration? = null

    private lateinit var progressBar: ProgressBar
    private lateinit var contentScroll: View
    private lateinit var careGroup: View
    private lateinit var careImage: ImageView
    private lateinit var careTitle: TextView
    private lateinit var careOffer: TextView
    private lateinit var careStatus: TextView
    private lateinit var careMessage: TextView
    private lateinit var vaccineEmpty: TextView
    private lateinit var updateEmpty: TextView

    private lateinit var vaccineAdapter: VaccinationAdapter
    private lateinit var updateAdapter: UpdateAdapter

    private var onPhotoChosen: ((Uri) -> Unit)? = null
    private val pickPhoto =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) onPhotoChosen?.invoke(uri)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_care_detail)

        val id = intent.getStringExtra(EXTRA_REQUEST_ID)
        if (id.isNullOrEmpty()) {
            finish()
            return
        }
        requestId = id

        progressBar = findViewById(R.id.progressBar)
        contentScroll = findViewById(R.id.contentScroll)
        careGroup = findViewById(R.id.careGroup)
        careImage = findViewById(R.id.careImage)
        careTitle = findViewById(R.id.careTitle)
        careOffer = findViewById(R.id.careOffer)
        careStatus = findViewById(R.id.careStatus)
        careMessage = findViewById(R.id.careMessage)
        vaccineEmpty = findViewById(R.id.vaccineEmpty)
        updateEmpty = findViewById(R.id.updateEmpty)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<View>(R.id.btnPostUpdate).setOnClickListener { showUpdateSheet(false) }
        findViewById<View>(R.id.btnReportHealth).setOnClickListener { showUpdateSheet(true) }

        vaccineAdapter = VaccinationAdapter(this) { v -> confirmVaccineDone(v) }
        val vaccineRecycler = findViewById<RecyclerView>(R.id.vaccineRecycler)
        vaccineRecycler.layoutManager = LinearLayoutManager(this)
        vaccineRecycler.adapter = vaccineAdapter
        vaccineRecycler.isNestedScrollingEnabled = false

        updateAdapter = UpdateAdapter(this)
        val updateRecycler = findViewById<RecyclerView>(R.id.updateRecycler)
        updateRecycler.layoutManager = LinearLayoutManager(this)
        updateRecycler.adapter = updateAdapter
        updateRecycler.isNestedScrollingEnabled = false

        loadRequest()
    }

    override fun onDestroy() {
        super.onDestroy()
        updatesReg?.remove()
        vaccinesReg?.remove()
    }


    private fun loadRequest() {
        db.collection("helpRequests").document(requestId).get()
            .addOnSuccessListener { doc ->
                val request = doc.toObject(HelpRequest::class.java)
                if (request == null) {
                    toast("This offer is no longer available")
                    finish()
                    return@addOnSuccessListener
                }
                helpRequest = request
                bindRequest(request)
                loadAnimal(request.animalId)

                if (request.status == "approved") {
                    careGroup.visibility = View.VISIBLE
                    startListeners()
                }
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                toast("Couldn't load: ${e.message}")
                finish()
            }
    }

    private fun loadAnimal(animalId: String) {
        db.collection("animals").document(animalId).get()
            .addOnSuccessListener { doc ->
                progressBar.visibility = View.GONE
                contentScroll.visibility = View.VISIBLE
                bindAnimal(doc.toObject(Animal::class.java))
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                contentScroll.visibility = View.VISIBLE
                bindAnimal(null)
            }
    }


    private fun bindRequest(r: HelpRequest) {
        careOffer.text = "Your offer: ${CareUi.helpTypeLabel(r.type)}"
        careStatus.text = CareUi.requestStatusLabel(r.status)
        careStatus.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(this, CareUi.requestStatusColor(r.status))
        )

        careMessage.text = when (r.status) {
            "approved" ->
                "You're caring for this animal. Please keep the PawHope team updated, " +
                        "and report any health problem right away."
            "rejected" ->
                "Thank you for offering to help. We couldn't match this offer this time, " +
                        "but you can still help other animals."
            "completed" ->
                "This care journey is complete. Thank you for giving this animal a safe home."
            else ->
                "The PawHope team is reviewing your offer and will contact you soon."
        }
    }

    private fun bindAnimal(a: Animal?) {
        careTitle.setText(CareUi.animalTypeRes(a?.animalType ?: ""))

        val photo = a?.photoUrls?.firstOrNull()
        if (!photo.isNullOrEmpty()) {
            Glide.with(this).load(photo).into(careImage)
        } else {
            careImage.scaleType = ImageView.ScaleType.CENTER_INSIDE
            careImage.setImageResource(R.drawable.ic_paw)
            careImage.setColorFilter(ContextCompat.getColor(this, R.color.primary_color))
        }
    }


    private fun startListeners() {
        val reqRef = db.collection("helpRequests").document(requestId)

        updatesReg = reqRef.collection("updates")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, error ->
                if (error != null || snap == null) return@addSnapshotListener
                val list = snap.documents.mapNotNull {
                    runCatching { it.toObject(CareUpdate::class.java) }.getOrNull()
                }
                updateAdapter.setItems(list)
                updateEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            }

        vaccinesReg = reqRef.collection("vaccinations")
            .orderBy("dueDate")
            .addSnapshotListener { snap, error ->
                if (error != null || snap == null) return@addSnapshotListener
                val list = snap.documents.mapNotNull {
                    runCatching { it.toObject(Vaccination::class.java) }.getOrNull()
                }
                vaccineAdapter.setItems(list)
                vaccineEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            }
    }


    private fun confirmVaccineDone(v: Vaccination) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Vaccine given?")
            .setMessage("Mark \"${v.name}\" as given? The PawHope team will see this.")
            .setPositiveButton("Yes, given") { _, _ ->
                db.collection("helpRequests").document(requestId)
                    .collection("vaccinations").document(v.id)
                    .update(
                        mapOf(
                            "status" to "done",
                            "doneAt" to FieldValue.serverTimestamp(),
                            "doneBy" to "adopter"
                        )
                    )
                    .addOnFailureListener { e -> toast("Couldn't update: ${e.message}") }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    private fun showUpdateSheet(isHealth: Boolean) {
        val req = helpRequest ?: return

        val sheet = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.sheet_care_update, null)
        sheet.setContentView(view)
        sheet.behavior.state = BottomSheetBehavior.STATE_EXPANDED

        val healthGroup = view.findViewById<View>(R.id.healthGroup)
        val chipSymptoms = view.findViewById<ChipGroup>(R.id.chipSymptoms)
        val chipSeverity = view.findViewById<ChipGroup>(R.id.chipSeverity)
        val textInput = view.findViewById<TextInputEditText>(R.id.updateTextInput)
        val btnAddPhoto = view.findViewById<MaterialButton>(R.id.btnAddPhoto)
        val photoPreview = view.findViewById<ImageView>(R.id.photoPreview)
        val btnSubmit = view.findViewById<MaterialButton>(R.id.btnSubmitUpdate)

        view.findViewById<TextView>(R.id.sheetTitle).text =
            if (isHealth) "Report a health problem" else "Share an update"
        view.findViewById<TextView>(R.id.sheetSubtitle).text =
            if (isHealth) "Tell us what you noticed. The PawHope team will be alerted and will reply with advice."
            else "Let us know how your animal is doing."
        healthGroup.visibility = if (isHealth) View.VISIBLE else View.GONE

        var photoUri: Uri? = null
        btnAddPhoto.setOnClickListener {
            onPhotoChosen = { uri ->
                photoUri = uri
                photoPreview.visibility = View.VISIBLE
                Glide.with(this).load(uri).into(photoPreview)
            }
            pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        btnSubmit.setOnClickListener {
            val text = textInput.text?.toString()?.trim().orEmpty()

            val symptoms = chipSymptoms.checkedChipIds.map { id ->
                view.findViewById<Chip>(id).text.toString()
            }
            val severity = when (chipSeverity.checkedChipId) {
                R.id.chip_sev_mild -> "mild"
                R.id.chip_sev_moderate -> "moderate"
                R.id.chip_sev_severe -> "severe"
                else -> ""
            }

            if (isHealth) {
                if (symptoms.isEmpty() && text.isEmpty()) {
                    toast("Please select a symptom or describe what you noticed")
                    return@setOnClickListener
                }
                if (severity.isEmpty()) {
                    toast("Please tell us how serious it is")
                    return@setOnClickListener
                }
            } else if (text.isEmpty() && photoUri == null) {
                toast("Please add a note or a photo")
                return@setOnClickListener
            }

            btnSubmit.isEnabled = false
            sendUpdate(req, isHealth, text, symptoms, severity, photoUri) { success ->
                if (success) sheet.dismiss() else btnSubmit.isEnabled = true
            }
        }

        sheet.show()
    }

    private fun sendUpdate(
        req: HelpRequest,
        isHealth: Boolean,
        text: String,
        symptoms: List<String>,
        severity: String,
        photoUri: Uri?,
        onFinished: (Boolean) -> Unit
    ) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            onFinished(false)
            return
        }

        fun save(photoUrl: String) {
            val data = hashMapOf<String, Any>(
                "type" to if (isHealth) "health" else "update",
                "text" to text,
                "symptoms" to symptoms,
                "severity" to severity,
                "photoUrl" to photoUrl,
                "authorId" to user.uid,
                "animalId" to req.animalId,
                "status" to if (isHealth) "open" else "info",
                "adminReply" to "",
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("helpRequests").document(requestId).collection("updates").add(data)
                .addOnSuccessListener {
                    if (isHealth && severity == "severe") {
                        toast("The PawHope team has been alerted urgently")
                    } else {
                        toast("Sent. Thank you for keeping us updated 🐾")
                    }
                    onFinished(true)
                }
                .addOnFailureListener { e ->
                    toast("Couldn't send: ${e.message}")
                    onFinished(false)
                }
        }

        if (photoUri == null) {
            save("")
            return
        }

        val resolver = contentResolver
        thread {
            val bytes = try {
                ImageUtils.compress(resolver, photoUri)
            } catch (e: Exception) {
                null
            }
            runOnUiThread {
                if (bytes == null) {
                    toast("Couldn't read the photo")
                    onFinished(false)
                    return@runOnUiThread
                }
                val ref = FirebaseStorage.getInstance().reference
                    .child("care/$requestId/${System.currentTimeMillis()}.jpg")
                ref.putBytes(bytes)
                    .continueWithTask { task ->
                        if (!task.isSuccessful) throw task.exception ?: Exception("Upload failed")
                        ref.downloadUrl
                    }
                    .addOnSuccessListener { url -> save(url.toString()) }
                    .addOnFailureListener { e ->
                        toast("Photo upload failed: ${e.message}")
                        onFinished(false)
                    }
            }
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}