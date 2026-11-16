package lk.zenova.pawhope.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import lk.zenova.pawhope.R
import lk.zenova.pawhope.adapter.PhotoAdapter
import lk.zenova.pawhope.util.DateUtil
import lk.zenova.pawhope.util.ImageUtils
import lk.zenova.pawhope.util.PhotoPicker
import java.util.Date

// සතාව ගත්තාට පස්සේ පුරවන "Adoption details" form එක.
// සාර්ථකව save වුණාම helpRequests/{id} එකේ handover විස්තර + handoverDone = true වෙනවා.
class HandoverActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_REQUEST_ID = "request_id"
    }

    private lateinit var requestId: String
    private lateinit var photoPicker: PhotoPicker
    private lateinit var photoAdapter: PhotoAdapter

    private lateinit var photoHint: TextView
    private lateinit var petNameInput: TextInputEditText
    private lateinit var homeAddressInput: TextInputEditText
    private lateinit var noteInput: TextInputEditText
    private lateinit var btnTakenDate: MaterialButton
    private lateinit var chipPickupCondGroup: ChipGroup
    private lateinit var chipVaccGroup: ChipGroup
    private lateinit var chipHousingGroup: ChipGroup
    private lateinit var chipPetsGroup: ChipGroup
    private lateinit var checkAgree: MaterialCheckBox
    private lateinit var btnSubmit: MaterialButton
    private lateinit var progressBar: ProgressBar

    private var takenDate = DateUtil.today()
    private var homeLat: Double? = null
    private var homeLng: Double? = null

    // Map එකෙන් ගෙදර තැන තෝරද්දී ලැබෙන ප්‍රතිඵලය
    private val pinHome =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data ?: return@registerForActivityResult
                homeLat = data.getDoubleExtra(LocationPickerActivity.EXTRA_LAT, 0.0)
                homeLng = data.getDoubleExtra(LocationPickerActivity.EXTRA_LNG, 0.0)
                val address = data.getStringExtra(LocationPickerActivity.EXTRA_ADDRESS).orEmpty()
                if (address.isNotEmpty()) homeAddressInput.setText(address)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_handover)

        val id = intent.getStringExtra(EXTRA_REQUEST_ID)
        if (id.isNullOrEmpty()) {
            finish()
            return
        }
        requestId = id

        photoHint = findViewById(R.id.photoHint)
        petNameInput = findViewById(R.id.petNameInput)
        homeAddressInput = findViewById(R.id.homeAddressInput)
        noteInput = findViewById(R.id.noteInput)
        btnTakenDate = findViewById(R.id.btnTakenDate)
        chipPickupCondGroup = findViewById(R.id.chipPickupCondGroup)
        chipVaccGroup = findViewById(R.id.chipVaccGroup)
        chipHousingGroup = findViewById(R.id.chipHousingGroup)
        chipPetsGroup = findViewById(R.id.chipPetsGroup)
        checkAgree = findViewById(R.id.checkAgree)
        btnSubmit = findViewById(R.id.btnSubmit)
        progressBar = findViewById(R.id.progressBar)

        // Photos (උපරිම 2ක්)
        photoAdapter = PhotoAdapter(this) { position -> photoPicker.removeAt(position) }
        photoPicker = PhotoPicker(this, 2) { refreshPhotos() }
        val photoRecycler = findViewById<RecyclerView>(R.id.photoRecycler)
        photoRecycler.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        photoRecycler.adapter = photoAdapter
        findViewById<View>(R.id.addPhotoCard).setOnClickListener { photoPicker.show() }
        refreshPhotos()

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        // ගත් දිනය තෝරන්න
        btnTakenDate.text = "Date you took the animal: $takenDate"
        btnTakenDate.setOnClickListener {
            DateUtil.pick(this, "Date you took the animal") { picked ->
                takenDate = picked
                btnTakenDate.text = "Date you took the animal: $takenDate"
            }
        }

        findViewById<View>(R.id.btnPinHome).setOnClickListener {
            pinHome.launch(Intent(this, LocationPickerActivity::class.java))
        }
        btnSubmit.setOnClickListener { submit() }
    }

    private fun refreshPhotos() {
        photoAdapter.setItems(photoPicker.photos)
        photoHint.text = "${photoPicker.photos.size}/2 photos (at least 1)"
    }

    // 1) form එක validate කරනවා
    private fun submit() {
        val petName = petNameInput.text?.toString()?.trim().orEmpty()
        val address = homeAddressInput.text?.toString()?.trim().orEmpty()
        val note = noteInput.text?.toString()?.trim().orEmpty()

        if (photoPicker.photos.isEmpty()) {
            toast("Please add a photo of your animal")
            return
        }
        if (petName.isEmpty()) {
            petNameInput.error = "Please enter a name"
            return
        }
        if (address.isEmpty()) {
            homeAddressInput.error = "Please enter your home address"
            return
        }
        if (!checkAgree.isChecked) {
            toast("Please accept the promise to continue")
            return
        }

        setLoading(true)

        // 2) Photos upload කරනවා
        ImageUtils.uploadAll(this, photoPicker.photos.toList(), "care/$requestId", "handover") { urls, error ->
            if (urls == null) {
                setLoading(false)
                toast(error ?: "Upload failed")
                return@uploadAll
            }
            saveHandover(petName, address, note, urls)
        }
    }

    // 3) විස්තර helpRequests document එකට save කරනවා
    private fun saveHandover(petName: String, address: String, note: String, urls: List<String>) {
        val handover = hashMapOf<String, Any>(
            "petName" to petName,
            "takenDate" to takenDate,
            "housing" to when (chipHousingGroup.checkedChipId) {
                R.id.chip_house_nogarden -> "House, no garden"
                R.id.chip_apartment -> "Apartment"
                R.id.chip_housing_other -> "Other"
                else -> "House with garden"
            },
            "otherPets" to if (chipPetsGroup.checkedChipId == R.id.chip_pets_yes) "Yes" else "No",
            "homeAddress" to address,
            "conditionAtPickup" to when (chipPickupCondGroup.checkedChipId) {
                R.id.chip_pc_sick -> "Sick"
                R.id.chip_pc_injured -> "Injured"
                else -> "Healthy"
            },
            "vaccinatedBefore" to when (chipVaccGroup.checkedChipId) {
                R.id.chip_v_yes -> "Yes"
                R.id.chip_v_no -> "No"
                else -> "Not sure"
            },
            "note" to note,
            "photoUrls" to urls,
            "agreed" to true
        )
        homeLat?.let { handover["homeLat"] = it }
        homeLng?.let { handover["homeLng"] = it }

        FirebaseFirestore.getInstance().collection("helpRequests").document(requestId)
            .update(
                mapOf(
                    "handover" to handover,
                    "handoverDone" to true,
                    "handoverAt" to FieldValue.serverTimestamp(),
                    // ඊළඟ මාසික check-in දිනය = දින 30කින්
                    "nextCheckInDue" to Date(System.currentTimeMillis() + 30L * 24 * 3600 * 1000)
                )
            )
            .addOnSuccessListener {
                setLoading(false)
                toast("Thank you! You can now send care reports 🐾")
                finish()
            }
            .addOnFailureListener { e ->
                setLoading(false)
                toast("Couldn't save: ${e.message}")
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