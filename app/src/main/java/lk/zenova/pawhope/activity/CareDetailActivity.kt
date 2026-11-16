package lk.zenova.pawhope.activity

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import lk.zenova.pawhope.R
import lk.zenova.pawhope.adapter.UpdateAdapter
import lk.zenova.pawhope.adapter.VaccinationAdapter
import lk.zenova.pawhope.model.Animal
import lk.zenova.pawhope.model.CareUpdate
import lk.zenova.pawhope.model.HelpRequest
import lk.zenova.pawhope.model.Vaccination
import lk.zenova.pawhope.util.CareUi
import kotlin.math.ceil


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
    private lateinit var handoverCard: View
    private lateinit var checkInBanner: MaterialCardView
    private lateinit var checkInText: TextView
    private lateinit var careImage: ImageView
    private lateinit var careTitle: TextView
    private lateinit var careOffer: TextView
    private lateinit var careStatus: TextView
    private lateinit var careMessage: TextView
    private lateinit var vaccineEmpty: TextView
    private lateinit var updateEmpty: TextView

    private lateinit var vaccineAdapter: VaccinationAdapter
    private lateinit var updateAdapter: UpdateAdapter

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
        handoverCard = findViewById(R.id.handoverCard)
        checkInBanner = findViewById(R.id.checkInBanner)
        checkInText = findViewById(R.id.checkInText)
        careImage = findViewById(R.id.careImage)
        careTitle = findViewById(R.id.careTitle)
        careOffer = findViewById(R.id.careOffer)
        careStatus = findViewById(R.id.careStatus)
        careMessage = findViewById(R.id.careMessage)
        vaccineEmpty = findViewById(R.id.vaccineEmpty)
        updateEmpty = findViewById(R.id.updateEmpty)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        // Adoption details form එකට
        findViewById<View>(R.id.btnHandover).setOnClickListener {
            startActivity(
                Intent(this, HandoverActivity::class.java)
                    .putExtra(HandoverActivity.EXTRA_REQUEST_ID, requestId)
            )
        }

        findViewById<View>(R.id.tileMonthly).setOnClickListener { openReport("monthly") }
        findViewById<View>(R.id.btnCheckIn).setOnClickListener { openReport("monthly") }
        findViewById<View>(R.id.tileVaccine).setOnClickListener { openReport("vaccine") }
        findViewById<View>(R.id.tileVet).setOnClickListener { openReport("vet") }
        findViewById<View>(R.id.tileHealth).setOnClickListener { openReport("health") }
        findViewById<View>(R.id.btnQuickUpdate).setOnClickListener { openReport("update") }

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
    }

    override fun onStart() {
        super.onStart()
        loadRequest()
    }

    override fun onStop() {
        super.onStop()
        stopListeners()
    }

    private fun openReport(type: String) {
        val req = helpRequest ?: return
        startActivity(
            Intent(this, CareReportActivity::class.java)
                .putExtra(CareReportActivity.EXTRA_REQUEST_ID, requestId)
                .putExtra(CareReportActivity.EXTRA_ANIMAL_ID, req.animalId)
                .putExtra(CareReportActivity.EXTRA_TYPE, type)
        )
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

        val approved = r.status == "approved"

        careMessage.text = when {
            approved && !r.handoverDone ->
                "Congratulations! Please complete your adoption details so we can start " +
                        "following your animal's journey."
            approved ->
                "You're caring for this animal. Please send a check-in every month, and " +
                        "report any health problem right away."
            r.status == "rejected" ->
                "Thank you for offering to help. We couldn't match this offer this time, " +
                        "but you can still help other animals."
            r.status == "completed" ->
                "This care journey is complete. Thank you for giving this animal a safe home."
            else -> "The PawHope team is reviewing your offer and will contact you soon."
        }

        // approve වෙලා, details පුරවලා නැත්නම් පමණක් form card එක
        handoverCard.visibility = if (approved && !r.handoverDone) View.VISIBLE else View.GONE

        if (approved && r.handoverDone) {
            careGroup.visibility = View.VISIBLE
            updateCheckInBanner(r)
            startListeners()
        } else {
            careGroup.visibility = View.GONE
            stopListeners()
        }
    }

    private fun updateCheckInBanner(r: HelpRequest) {
        val due = r.nextCheckInDue
        if (due == null) {
            checkInBanner.visibility = View.GONE
            return
        }
        checkInBanner.visibility = View.VISIBLE

        val days = ceil((due.time - System.currentTimeMillis()) / 86_400_000.0).toInt()
        val (message, colorRes) = when {
            days < 0 -> "Monthly check-in is overdue by ${-days} day(s)" to R.color.red
            days <= 5 -> "Monthly check-in due in $days day(s)" to R.color.limit_stock_color
            else -> "Next monthly check-in in $days days" to R.color.primary_dark
        }
        checkInText.text = message
        checkInBanner.strokeColor = ContextCompat.getColor(this, colorRes)
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
        stopListeners()
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

    private fun stopListeners() {
        updatesReg?.remove()
        vaccinesReg?.remove()
        updatesReg = null
        vaccinesReg = null
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

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}