package lk.zenova.pawhope.fragment
import lk.zenova.pawhope.activity.MyCareActivity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import lk.zenova.pawhope.R
import lk.zenova.pawhope.activity.AuthActivity
import lk.zenova.pawhope.model.User

class ProfileFragment : Fragment() {

    private lateinit var memberCard: View
    private lateinit var guestCard: View
    private lateinit var menuCard: View
    private lateinit var avatarText: TextView
    private lateinit var profileName: TextView
    private lateinit var profileEmail: TextView
    private lateinit var profileMobile: TextView

    private val authLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { refresh() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_profile, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        memberCard = view.findViewById(R.id.memberCard)
        guestCard = view.findViewById(R.id.guestCard)
        menuCard = view.findViewById(R.id.menuCard)
        avatarText = view.findViewById(R.id.avatarText)
        profileName = view.findViewById(R.id.profileName)
        profileEmail = view.findViewById(R.id.profileEmail)
        profileMobile = view.findViewById(R.id.profileMobile)

        view.findViewById<View>(R.id.btnGuestSignIn).setOnClickListener {
            authLauncher.launch(
                Intent(requireContext(), AuthActivity::class.java)
                    .putExtra(AuthActivity.Companion.EXTRA_MODE, AuthActivity.Companion.MODE_SIGN_IN)
            )
        }
        view.findViewById<View>(R.id.rowMyCare).setOnClickListener {
            startActivity(Intent(requireContext(), MyCareActivity::class.java))
        }
        view.findViewById<View>(R.id.rowSignOut).setOnClickListener { confirmSignOut() }
    }

    override fun onStart() {
        super.onStart()
        refresh()
    }

    private fun refresh() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null || user.isAnonymous) {
            memberCard.visibility = View.GONE
            menuCard.visibility = View.GONE
            guestCard.visibility = View.VISIBLE
            return
        }
        guestCard.visibility = View.GONE
        memberCard.visibility = View.VISIBLE
        menuCard.visibility = View.VISIBLE
        showUser(user, null)

        FirebaseFirestore.getInstance().collection("users").document(user.uid).get()
            .addOnSuccessListener { doc ->
                if (!isAdded) return@addOnSuccessListener
                showUser(user, doc.toObject(User::class.java))
            }
    }

    private fun showUser(user: FirebaseUser, profile: User?) {
        val name = profile?.name?.ifEmpty { null } ?: user.displayName ?: "PawHope member"
        profileName.text = name
        avatarText.text = name.firstOrNull()?.uppercase() ?: "P"
        profileEmail.text = profile?.email?.ifEmpty { null } ?: user.email ?: ""
        profileMobile.text = profile?.mobile ?: ""
        profileMobile.visibility = if (profileMobile.text.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun confirmSignOut() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Sign out?")
            .setMessage("You can sign in again anytime to follow the animals you help.")
            .setPositiveButton("Sign out") { _, _ ->
                FirebaseAuth.getInstance().signOut()
                refresh()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}