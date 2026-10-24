package lk.zenova.pawhope.fragment

import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.tasks.Tasks
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.storage.FirebaseStorage
import lk.zenova.pawhope.R
import lk.zenova.pawhope.activity.LocationPickerActivity
import lk.zenova.pawhope.activity.MainActivity
import lk.zenova.pawhope.adapter.PhotoAdapter
import lk.zenova.pawhope.helper.AuthHelper
import lk.zenova.pawhope.model.Animal
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Date
import kotlin.concurrent.thread
import kotlin.math.roundToInt

// සතෙක් report කරන screen එක
class ReportFragment : Fragment() {

    companion object {
        private const val MAX_PHOTOS = 4
    }

    // ---------- Views ----------
    private lateinit var addPhotoCard: View
    private lateinit var photoHint: TextView
    private lateinit var chipTypeGroup: ChipGroup
    private lateinit var chipCondGroup: ChipGroup
    private lateinit var chipNeedGroup: ChipGroup
    private lateinit var descriptionInput: TextInputEditText
    private lateinit var nameInput: TextInputEditText
    private lateinit var phoneInput: TextInputEditText
    private lateinit var locationText: TextView
    private lateinit var btnPickLocation: MaterialButton
    private lateinit var btnSubmit: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var photoAdapter: PhotoAdapter

    // ---------- State (form එකේ දැනට තියෙන දත්ත) ----------
    private val photos = mutableListOf<Uri>()
    private var cameraUri: Uri? = null
    private var pickedLat: Double? = null
    private var pickedLng: Double? = null
    private var pickedAddress = ""
    private var pickedCity = ""
    private var pickedDistrict = ""

    // ---------- Activity result launchers ----------
    // (මේවා Fragment එක හදද්දීම, property විදිහට register කරන්න ඕනි)

    // Camera එකෙන් photo එකක් ගන්න
    private val takePicture =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                cameraUri?.let { addPhoto(it) }
            }
        }

    // Gallery එකෙන් photos කිහිපයක් තෝරන්න (permission ඕනි නෑ)
    private val pickImages =
        registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(MAX_PHOTOS)) { uris ->
            // තව තියෙන ඉඩ ප්‍රමාණයට විතරක් ගන්නවා
            uris.take(MAX_PHOTOS - photos.size).forEach { photos.add(it) }
            refreshPhotos()
        }

    // Map screen එකෙන් තෝරපු තැන ආපහු ගන්න
    private val pickLocation =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data ?: return@registerForActivityResult
                pickedLat = data.getDoubleExtra(LocationPickerActivity.EXTRA_LAT, 0.0)
                pickedLng = data.getDoubleExtra(LocationPickerActivity.EXTRA_LNG, 0.0)
                pickedAddress = data.getStringExtra(LocationPickerActivity.EXTRA_ADDRESS) ?: ""
                pickedCity = data.getStringExtra(LocationPickerActivity.EXTRA_CITY) ?: ""
                pickedDistrict = data.getStringExtra(LocationPickerActivity.EXTRA_DISTRICT) ?: ""
                showPickedLocation()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_report, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        addPhotoCard = view.findViewById(R.id.addPhotoCard)
        photoHint = view.findViewById(R.id.photoHint)
        chipTypeGroup = view.findViewById(R.id.chipTypeGroup)
        chipCondGroup = view.findViewById(R.id.chipCondGroup)
        chipNeedGroup = view.findViewById(R.id.chipNeedGroup)
        descriptionInput = view.findViewById(R.id.descriptionInput)
        nameInput = view.findViewById(R.id.nameInput)
        phoneInput = view.findViewById(R.id.phoneInput)
        locationText = view.findViewById(R.id.locationText)
        btnPickLocation = view.findViewById(R.id.btnPickLocation)
        btnSubmit = view.findViewById(R.id.btnSubmit)
        progressBar = view.findViewById(R.id.progressBar)

        // Photos list එක (තිරස් අතට)
        photoAdapter = PhotoAdapter(requireContext()) { position ->
            photos.removeAt(position)
            refreshPhotos()
        }
        val photoRecycler = view.findViewById<RecyclerView>(R.id.photoRecycler)
        photoRecycler.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        photoRecycler.adapter = photoAdapter

        addPhotoCard.setOnClickListener { showPhotoOptions() }
        btnPickLocation.setOnClickListener {
            pickLocation.launch(Intent(requireContext(), LocationPickerActivity::class.java))
        }
        btnSubmit.setOnClickListener { submit() }

        refreshPhotos()
    }

    // ===================== Photos =====================

    private fun showPhotoOptions() {
        if (photos.size >= MAX_PHOTOS) {
            toast("You can add up to $MAX_PHOTOS photos")
            return
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add a photo")
            .setItems(arrayOf("Take a photo", "Choose from gallery")) { _, which ->
                if (which == 0) launchCamera()
                else pickImages.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
            .show()
    }

    // Camera photo එක cache folder එකේ තාවකාලික file එකකට save කරනවා
    private fun launchCamera() {
        val dir = File(requireContext().cacheDir, "camera").apply { mkdirs() }
        val file = File.createTempFile("photo_", ".jpg", dir)
        val uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            file
        )
        cameraUri = uri
        takePicture.launch(uri)
    }

    private fun addPhoto(uri: Uri) {
        if (photos.size < MAX_PHOTOS) {
            photos.add(uri)
            refreshPhotos()
        }
    }

    private fun refreshPhotos() {
        photoAdapter.setItems(photos)
        photoHint.text = "${photos.size}/$MAX_PHOTOS photos (at least 1)"
    }

    // ===================== Location =====================

    private fun showPickedLocation() {
        val lat = pickedLat ?: return
        val lng = pickedLng ?: return
        locationText.text = pickedAddress.ifEmpty { String.format("%.5f, %.5f", lat, lng) }
        locationText.setTextColor(requireContext().getColor(R.color.text_primary))
        btnPickLocation.setText(R.string.change_location)
    }

    // ===================== Chip තෝරාගැනීම් =====================

    private fun selectedType(): String = when (chipTypeGroup.checkedChipId) {
        R.id.chip_type_cat -> "cat"
        R.id.chip_type_other -> "other"
        else -> "dog"
    }

    // තෝරලා නැත්නම් null
    private fun selectedCondition(): String? = when (chipCondGroup.checkedChipId) {
        R.id.chip_cond_healthy -> "healthy"
        R.id.chip_cond_sick -> "sick"
        R.id.chip_cond_injured -> "injured"
        R.id.chip_cond_critical -> "critical"
        else -> null
    }

    private fun selectedNeed(): String = when (chipNeedGroup.checkedChipId) {
        R.id.chip_need_home -> "home"
        R.id.chip_need_both -> "both"
        else -> "help"
    }

    // ===================== Submit =====================

    // 1) form එක validate කරනවා
    private fun submit() {
        if (photos.isEmpty()) {
            toast("Please add at least one photo")
            return
        }
        val condition = selectedCondition() ?: run {
            toast("Please select how the animal is")
            return
        }
        val description = descriptionInput.text?.toString()?.trim().orEmpty()
        if (description.isEmpty()) {
            descriptionInput.error = "Please describe the animal"
            return
        }
        val lat = pickedLat
        val lng = pickedLng
        if (lat == null || lng == null) {
            toast("Please pin the animal's location on the map")
            return
        }

        setLoading(true)

        // 2) login නැත්නම් guest user කෙනෙක් හදලා, ඊළඟට photos ලෑස්ති කරනවා
        AuthHelper.ensureSignedIn(
            onReady = { user -> compressAndUpload(user, condition, description, lat, lng) },
            onError = { e ->
                setLoading(false)
                toast("Sign-in failed: ${e.message}")
            }
        )
    }

    // 3) Photos පොඩි කරනවා (background thread එකේ, මොකද මේක බර වැඩක්)
    private fun compressAndUpload(
        user: FirebaseUser,
        condition: String,
        description: String,
        lat: Double,
        lng: Double
    ) {
        val resolver = requireContext().contentResolver
        val uris = photos.toList()

        thread {
            val compressed: List<ByteArray>? = try {
                uris.map { compressImage(resolver, it) }
            } catch (e: Exception) {
                null
            }

            activity?.runOnUiThread {
                if (!isAdded) return@runOnUiThread
                if (compressed == null) {
                    setLoading(false)
                    toast("Couldn't read the photos. Please try again")
                    return@runOnUiThread
                }
                uploadPhotos(user, compressed, condition, description, lat, lng)
            }
        }
    }

    // 4) Photos Firebase Storage එකට upload කරනවා
    private fun uploadPhotos(
        user: FirebaseUser,
        compressed: List<ByteArray>,
        condition: String,
        description: String,
        lat: Double,
        lng: Double
    ) {
        val db = FirebaseFirestore.getInstance()
        val animalRef = db.collection("animals").document()   // අලුත් document id එක දැන්ම හදනවා
        val storage = FirebaseStorage.getInstance()

        // හැම photo එකකටම upload task එකක් (එකවර upload වෙනවා)
        val uploadTasks = compressed.mapIndexed { index, bytes ->
            val ref = storage.reference.child("animals/${animalRef.id}/img_${index + 1}.jpg")
            ref.putBytes(bytes).continueWithTask { task ->
                if (!task.isSuccessful) throw task.exception ?: Exception("Upload failed")
                ref.downloadUrl   // upload ඉවර වුණාම download URL එක ගන්නවා
            }
        }

        // photos ඔක්කොම සාර්ථකව ගියාම විතරක් ඊළඟ පියවරට යනවා
        Tasks.whenAllSuccess<Uri>(uploadTasks)
            .addOnSuccessListener { urls ->
                saveReport(user, animalRef, urls.map { it.toString() }, condition, description, lat, lng)
            }
            .addOnFailureListener { e ->
                setLoading(false)
                toast("Photo upload failed: ${e.message}")
            }
    }

    // 5) Firestore එකට report එක save කරනවා
    private fun saveReport(
        user: FirebaseUser,
        animalRef: DocumentReference,
        photoUrls: List<String>,
        condition: String,
        description: String,
        lat: Double,
        lng: Double
    ) {
        val db = FirebaseFirestore.getInstance()

        val animal = Animal(
            reporterId = user.uid,
            guest = user.isAnonymous,
            animalType = selectedType(),
            condition = condition,
            needType = selectedNeed(),
            description = description,
            photoUrls = photoUrls,
            location = GeoPoint(lat, lng),
            address = pickedAddress,
            city = pickedCity,
            district = pickedDistrict,
            status = "reported",
            updatedAt = Date()          // createdAt එක server එක ඉබේම දානවා (@ServerTimestamp)
        )

        // phone/name public document එකේ දාන්නේ නෑ, වෙනම private document එකක
        val privateData = mapOf(
            "reporterId" to user.uid,
            "reporterName" to nameInput.text?.toString()?.trim().orEmpty(),
            "reporterPhone" to phoneInput.text?.toString()?.trim().orEmpty(),
            "createdAt" to FieldValue.serverTimestamp()
        )

        // batch = document දෙකම එකවර save වෙනවා, නැත්නම් දෙකම නෑ
        val batch = db.batch()
        batch.set(animalRef, animal)
        batch.set(db.collection("animals_private").document(animalRef.id), privateData)

        batch.commit()
            .addOnSuccessListener {
                setLoading(false)
                toast("Thank you! The animal has been reported 🐾")
                resetForm()
                (activity as? MainActivity)?.goHome()
            }
            .addOnFailureListener { e ->
                setLoading(false)
                toast("Couldn't save the report: ${e.message}")
            }
    }

    // ===================== Helpers =====================

    // Photo එක උපරිම 1280px වලට පොඩි කරලා JPEG (quality 80) බවට හරවනවා.
    // ImageDecoder එක photo එක හරවලා තියෙන්නේ (EXIF) නම් ඒකත් හරියට හදනවා.
    private fun compressImage(resolver: ContentResolver, uri: Uri): ByteArray {
        val source = ImageDecoder.createSource(resolver, uri)
        val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val maxSide = 1280f
            val width = info.size.width
            val height = info.size.height
            val scale = minOf(1f, maxSide / maxOf(width, height))
            decoder.setTargetSize((width * scale).roundToInt(), (height * scale).roundToInt())
            decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE)
        }
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
        return out.toByteArray()
    }

    private fun setLoading(loading: Boolean) {
        btnSubmit.isEnabled = !loading
        btnSubmit.text = if (loading) "Uploading…" else getString(R.string.btn_report)
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
    }

    // report එක සාර්ථක වුණාට පස්සේ form එක හිස් කරනවා
    private fun resetForm() {
        photos.clear()
        refreshPhotos()
        descriptionInput.text = null
        nameInput.text = null
        phoneInput.text = null
        chipTypeGroup.check(R.id.chip_type_dog)
        chipNeedGroup.check(R.id.chip_need_help)
        chipCondGroup.clearCheck()
        pickedLat = null
        pickedLng = null
        pickedAddress = ""
        pickedCity = ""
        pickedDistrict = ""
        locationText.setText(R.string.no_location)
        locationText.setTextColor(requireContext().getColor(R.color.text_secondary))
        btnPickLocation.setText(R.string.pin_location)
    }

    // Fragment එක screen එකෙන් අයින් වුණාට පස්සේ app crash නොවෙන්න context? පාවිච්චි කරනවා
    private fun toast(message: String) {
        context?.let { Toast.makeText(it, message, Toast.LENGTH_SHORT).show() }
    }
}