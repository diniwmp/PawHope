package lk.zenova.pawhope.activity

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.text.format.DateUtils
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import lk.zenova.pawhope.R
import lk.zenova.pawhope.adapter.PhotoPagerAdapter
import lk.zenova.pawhope.model.Animal
import kotlin.math.roundToInt

// සතෙක්ගේ සම්පූර්ණ විස්තර බලන screen එක (GoMart වල single product view එක වගේ)
class AnimalDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ANIMAL_ID = "animal_id"
    }

    private var animal: Animal? = null

    private lateinit var contentLayout: View
    private lateinit var progressBar: ProgressBar
    private lateinit var photoPager: ViewPager2
    private lateinit var dotsLayout: LinearLayout
    private lateinit var conditionBadge: TextView
    private lateinit var titleText: TextView
    private lateinit var timeText: TextView
    private lateinit var needPill: TextView
    private lateinit var infoType: TextView
    private lateinit var infoCondition: TextView
    private lateinit var infoStatus: TextView
    private lateinit var descriptionText: TextView
    private lateinit var addressText: TextView
    private lateinit var btnHelp: MaterialButton
    private lateinit var noteText: TextView
    private lateinit var photoAdapter: PhotoPagerAdapter

    // Sign up / Sign in screen එකෙන් ආපහු ආවම: සාර්ථක නම් කෙලින්ම help sheet එක පෙන්නනවා
    private val authLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                animal?.let { showHelpSheet(it) }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_animal_detail)

        val animalId = intent.getStringExtra(EXTRA_ANIMAL_ID)
        if (animalId.isNullOrEmpty()) {
            finish()
            return
        }

        contentLayout = findViewById(R.id.contentLayout)
        progressBar = findViewById(R.id.progressBar)
        photoPager = findViewById(R.id.photoPager)
        dotsLayout = findViewById(R.id.dotsLayout)
        conditionBadge = findViewById(R.id.conditionBadge)
        titleText = findViewById(R.id.titleText)
        timeText = findViewById(R.id.timeText)
        needPill = findViewById(R.id.needPill)
        infoType = findViewById(R.id.infoType)
        infoCondition = findViewById(R.id.infoCondition)
        infoStatus = findViewById(R.id.infoStatus)
        descriptionText = findViewById(R.id.descriptionText)
        addressText = findViewById(R.id.addressText)
        btnHelp = findViewById(R.id.btnHelp)
        noteText = findViewById(R.id.noteText)

        photoAdapter = PhotoPagerAdapter(this)
        photoPager.adapter = photoAdapter

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<View>(R.id.btnDirections).setOnClickListener {
            animal?.let { openDirections(it) }
        }

        loadAnimal(animalId)
    }

    // Firestore එකෙන් ඒ සතාගේ document එක ගන්නවා
    private fun loadAnimal(animalId: String) {
        FirebaseFirestore.getInstance().collection("animals").document(animalId).get()
            .addOnSuccessListener { doc ->
                progressBar.visibility = View.GONE
                val loaded = doc.toObject(Animal::class.java)
                if (loaded == null) {
                    toast("This animal is no longer available")
                    finish()
                    return@addOnSuccessListener
                }
                animal = loaded
                contentLayout.visibility = View.VISIBLE
                bindAnimal(loaded)
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                toast("Couldn't load: ${e.message}")
                finish()
            }
    }

    // ===================== UI පුරවනවා =====================

    private fun bindAnimal(a: Animal) {
        // Photos + dots
        photoAdapter.setItems(a.photoUrls)
        setupDots(a.photoUrls.size)

        // සතාගේ වර්ගය
        val typeRes = when (a.animalType) {
            "dog" -> R.string.type_dog
            "cat" -> R.string.type_cat
            else -> R.string.type_other
        }
        titleText.setText(typeRes)
        infoType.setText(typeRes)

        // Urgency (කලින් adapter එකේ වගේම)
        val (condColor, condLabel) = when (a.condition) {
            "critical" -> R.color.red to R.string.cond_critical
            "injured" -> R.color.limit_stock_color to R.string.cond_injured
            "sick" -> R.color.dark_blue to R.string.cond_sick
            "healthy" -> R.color.online_green to R.string.cond_healthy
            else -> R.color.dark_blue to R.string.cond_needs_help
        }
        val color = ContextCompat.getColor(this, condColor)
        conditionBadge.setText(condLabel)
        conditionBadge.backgroundTintList = ColorStateList.valueOf(color)
        infoCondition.setText(condLabel)
        infoCondition.setTextColor(color)

        // අවශ්‍යතාවය
        needPill.setText(
            when (a.needType) {
                "home" -> R.string.need_home
                "both" -> R.string.need_both
                else -> R.string.need_help
            }
        )

        infoStatus.text = statusLabel(a.status)

        // Report කරපු වෙලාව
        val createdAt = a.createdAt
        timeText.text = if (createdAt != null) {
            DateUtils.getRelativeTimeSpanString(
                createdAt.time, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS
            )
        } else {
            getString(R.string.just_now)
        }

        descriptionText.text = a.description
        addressText.text = a.address.ifEmpty { getString(R.string.location_pinned) }

        showMap(a)
        setupHelpButton(a)
    }

    private fun statusLabel(status: String): String = when (status) {
        "rescued" -> "Rescued"
        "under_treatment" -> "Under treatment"
        "ready_for_adoption" -> "Ready for adoption"
        "adopted" -> "Adopted"
        else -> "Reported"
    }

    // ===================== Photo dots =====================

    private fun setupDots(count: Int) {
        dotsLayout.removeAllViews()
        if (count <= 1) return

        repeat(count) {
            val dot = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp(8), dp(8)).apply {
                    marginStart = dp(4)
                    marginEnd = dp(4)
                }
                setBackgroundResource(R.drawable.bg_dot)
            }
            dotsLayout.addView(dot)
        }
        updateDots(0)

        // photo එක swipe කරද්දී දැනට ඉන්න dot එක highlight කරනවා
        photoPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) = updateDots(position)
        })
    }

    private fun updateDots(selected: Int) {
        for (i in 0 until dotsLayout.childCount) {
            dotsLayout.getChildAt(i).alpha = if (i == selected) 1f else 0.4f
        }
    }

    // ===================== Map preview + directions =====================

    // Lite mode map එකක් (සැහැල්ලුයි, tap කළාම Google Maps app එක open වෙනවා)
    private fun showMap(a: Animal) {
        val geo = a.location ?: return
        val position = LatLng(geo.latitude, geo.longitude)

        val options = GoogleMapOptions().liteMode(true).mapToolbarEnabled(false)
        val mapFragment = SupportMapFragment.newInstance(options)
        supportFragmentManager.beginTransaction()
            .replace(R.id.mapContainer, mapFragment)
            .commit()

        mapFragment.getMapAsync { googleMap ->
            googleMap.addMarker(MarkerOptions().position(position))
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 16f))
        }
    }

    // Google Maps app එකේ navigation එක open කරනවා. app එක නැත්නම් browser එකෙන්
    private fun openDirections(a: Animal) {
        val geo = a.location ?: return
        val navIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("google.navigation:q=${geo.latitude},${geo.longitude}")
        ).setPackage("com.google.android.apps.maps")

        try {
            startActivity(navIntent)
        } catch (e: ActivityNotFoundException) {
            val webUri = Uri.parse(
                "https://www.google.com/maps/dir/?api=1&destination=${geo.latitude},${geo.longitude}"
            )
            startActivity(Intent(Intent.ACTION_VIEW, webUri))
        }
    }

    // ===================== Help flow =====================

    // පහළ බටන් එක: ඔයාගේම report එකක්ද, adopt වෙලාද, නැත්නම් help කරන්න පුළුවන්ද
    private fun setupHelpButton(a: Animal) {
        val user = FirebaseAuth.getInstance().currentUser
        when {
            a.status == "adopted" -> {
                btnHelp.visibility = View.GONE
                noteText.setText(R.string.adopted_note)
                noteText.visibility = View.VISIBLE
            }
            user != null && user.uid == a.reporterId -> {
                btnHelp.visibility = View.GONE
                noteText.setText(R.string.own_report_note)
                noteText.visibility = View.VISIBLE
            }
            else -> {
                noteText.visibility = View.GONE
                btnHelp.visibility = View.VISIBLE
                btnHelp.setOnClickListener { onHelpClicked(a) }
            }
        }
    }

    private fun onHelpClicked(a: Animal) {
        val user = FirebaseAuth.getInstance().currentUser
        // ලියාපදිංචි නැති (guest) නම් register වෙන්න කියනවා
        if (user == null || user.isAnonymous) {
            showRegisterDialog()
        } else {
            showHelpSheet(a)
        }
    }

    private fun showRegisterDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Join PawHope to help")
            .setMessage(
                "To keep every animal safe, only registered members can adopt or help. " +
                        "We track each animal's care, so we need to know who is helping. " +
                        "It takes less than a minute."
            )
            .setPositiveButton("Sign up") { _, _ -> launchAuth(AuthActivity.MODE_SIGN_UP) }
            .setNeutralButton("I have an account") { _, _ -> launchAuth(AuthActivity.MODE_SIGN_IN) }
            .setNegativeButton("Not now", null)
            .show()
    }

    private fun launchAuth(mode: String) {
        authLauncher.launch(
            Intent(this, AuthActivity::class.java).putExtra(AuthActivity.EXTRA_MODE, mode)
        )
    }

    // Help වර්ගය + message එක තෝරන bottom sheet එක
    private fun showHelpSheet(a: Animal) {
        val sheet = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.sheet_help_request, null)
        sheet.setContentView(view)

        val chipGroup = view.findViewById<ChipGroup>(R.id.chipHelpGroup)
        val messageInput = view.findViewById<TextInputEditText>(R.id.helpMessageInput)
        val btnSend = view.findViewById<MaterialButton>(R.id.btnSendHelp)

        btnSend.setOnClickListener {
            val type = when (chipGroup.checkedChipId) {
                R.id.chip_h_foster -> "foster"
                R.id.chip_h_food -> "food"
                R.id.chip_h_medical -> "medical"
                R.id.chip_h_visit -> "visit"
                else -> "adopt"
            }
            val message = messageInput.text?.toString()?.trim().orEmpty()

            btnSend.isEnabled = false
            submitHelpRequest(a, type, message) { success ->
                if (success) sheet.dismiss() else btnSend.isEnabled = true
            }
        }
        sheet.show()
    }

    // helpRequests/{animalId}_{uid} ලෙස save කරනවා (එකම කෙනා එකම සතාට දෙපාරක් යවන්න බෑ)
    private fun submitHelpRequest(
        a: Animal,
        type: String,
        message: String,
        onFinished: (Boolean) -> Unit
    ) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null || user.isAnonymous) {
            onFinished(false)
            return
        }

        val ref = FirebaseFirestore.getInstance()
            .collection("helpRequests")
            .document("${a.id}_${user.uid}")

        ref.get()
            .addOnSuccessListener { existing ->
                if (existing.exists()) {
                    toast("You've already offered to help this animal. We'll contact you soon.")
                    onFinished(true)
                    return@addOnSuccessListener
                }

                val data = hashMapOf(
                    "animalId" to a.id,
                    "helperId" to user.uid,
                    "helperName" to (user.displayName ?: ""),
                    "helperEmail" to (user.email ?: ""),
                    "type" to type,
                    "message" to message,
                    "status" to "pending",           // admin approve කරනකම් pending
                    "createdAt" to FieldValue.serverTimestamp()
                )
                ref.set(data)
                    .addOnSuccessListener {
                        toast("Thank you! Your offer has been sent 🐾")
                        onFinished(true)
                    }
                    .addOnFailureListener { e ->
                        toast("Couldn't send: ${e.message}")
                        onFinished(false)
                    }
            }
            .addOnFailureListener { e ->
                toast("Couldn't send: ${e.message}")
                onFinished(false)
            }
    }

    // ===================== Helpers =====================

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).roundToInt()

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}