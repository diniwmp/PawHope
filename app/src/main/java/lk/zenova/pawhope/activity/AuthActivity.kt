package lk.zenova.pawhope.activity

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import lk.zenova.pawhope.R
import lk.zenova.pawhope.model.User

// Sign up සහ Sign in එකම screen එකේ. mode එක මාරු කරන්න පුළුවන්.
// සාර්ථක වුණාම RESULT_OK එක්ක ආපහු යනවා (Detail screen එකට)
class AuthActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_MODE = "mode"
        const val MODE_SIGN_UP = "signup"
        const val MODE_SIGN_IN = "signin"
    }

    private var isSignUp = true

    private lateinit var titleText: TextView
    private lateinit var subtitleText: TextView
    private lateinit var switchModeText: TextView
    private lateinit var btnAuth: MaterialButton
    private lateinit var progressBar: ProgressBar

    private lateinit var nameLayout: TextInputLayout
    private lateinit var mobileLayout: TextInputLayout
    private lateinit var emailLayout: TextInputLayout
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var confirmLayout: TextInputLayout

    private lateinit var nameInput: TextInputEditText
    private lateinit var mobileInput: TextInputEditText
    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var confirmInput: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        isSignUp = intent.getStringExtra(EXTRA_MODE) != MODE_SIGN_IN

        titleText = findViewById(R.id.titleText)
        subtitleText = findViewById(R.id.subtitleText)
        switchModeText = findViewById(R.id.switchModeText)
        btnAuth = findViewById(R.id.btnAuth)
        progressBar = findViewById(R.id.progressBar)

        nameLayout = findViewById(R.id.nameLayout)
        mobileLayout = findViewById(R.id.mobileLayout)
        emailLayout = findViewById(R.id.emailLayout)
        passwordLayout = findViewById(R.id.passwordLayout)
        confirmLayout = findViewById(R.id.confirmLayout)

        nameInput = findViewById(R.id.nameInput)
        mobileInput = findViewById(R.id.mobileInput)
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        confirmInput = findViewById(R.id.confirmInput)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        btnAuth.setOnClickListener { submit() }
        switchModeText.setOnClickListener {
            isSignUp = !isSignUp
            applyMode()
        }

        applyMode()
    }

    private fun applyMode() {
        titleText.setText(if (isSignUp) R.string.auth_signup_title else R.string.auth_signin_title)
        subtitleText.setText(if (isSignUp) R.string.auth_signup_sub else R.string.auth_signin_sub)
        btnAuth.setText(if (isSignUp) R.string.btn_signup else R.string.btn_signin)
        switchModeText.setText(if (isSignUp) R.string.switch_to_signin else R.string.switch_to_signup)

        val signUpOnly = if (isSignUp) View.VISIBLE else View.GONE
        nameLayout.visibility = signUpOnly
        mobileLayout.visibility = signUpOnly
        confirmLayout.visibility = signUpOnly
    }

    private fun submit() {
        val name = nameInput.text?.toString()?.trim().orEmpty()
        val mobile = mobileInput.text?.toString()?.trim().orEmpty()
        val email = emailInput.text?.toString()?.trim().orEmpty()
        val password = passwordInput.text?.toString().orEmpty()
        val confirm = confirmInput.text?.toString().orEmpty()

        if (!validate(name, mobile, email, password, confirm)) return

        setLoading(true)
        if (isSignUp) signUp(name, mobile, email, password) else signIn(email, password)
    }

    private fun validate(
        name: String, mobile: String, email: String, password: String, confirm: String
    ): Boolean {
        listOf(nameLayout, mobileLayout, emailLayout, passwordLayout, confirmLayout)
            .forEach { it.error = null }

        var ok = true
        if (isSignUp && name.length < 2) {
            nameLayout.error = "Please enter your name"
            ok = false
        }
        if (isSignUp && !mobile.matches(Regex("^\\+?[0-9]{9,13}$"))) {
            mobileLayout.error = "Enter a valid mobile number"
            ok = false
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.error = "Enter a valid email"
            ok = false
        }
        if (password.length < 6) {
            passwordLayout.error = "At least 6 characters"
            ok = false
        }
        if (isSignUp && confirm != password) {
            confirmLayout.error = "Passwords don't match"
            ok = false
        }
        return ok
    }

    private fun signUp(name: String, mobile: String, email: String, password: String) {
        val auth = FirebaseAuth.getInstance()
        val current = auth.currentUser

        if (current != null && current.isAnonymous) {

            val credential = EmailAuthProvider.getCredential(email, password)
            current.linkWithCredential(credential)
                .addOnSuccessListener { result ->
                    result.user?.let { onSignedUp(it, name, mobile, email) }
                }
                .addOnFailureListener { handleAuthError(it) }
        } else {
            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    result.user?.let { onSignedUp(it, name, mobile, email) }
                }
                .addOnFailureListener { handleAuthError(it) }
        }
    }

    private fun signIn(email: String, password: String) {
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                result.user?.let { refreshTokenAndFinish(it) }
            }
            .addOnFailureListener { handleAuthError(it) }
    }

    private fun onSignedUp(user: FirebaseUser, name: String, mobile: String, email: String) {
        user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name).build())

        val profile = User(uid = user.uid, name = name, mobile = mobile, email = email)
        FirebaseFirestore.getInstance().collection("users").document(user.uid).set(profile)
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    toast("Account created, but saving your profile failed")
                }
                refreshTokenAndFinish(user)
            }
    }


    private fun refreshTokenAndFinish(user: FirebaseUser) {
        user.getIdToken(true).addOnCompleteListener {
            setLoading(false)
            setResult(RESULT_OK)
            finish()
        }
    }

    private fun handleAuthError(e: Exception) {
        setLoading(false)
        when (e) {
            is FirebaseAuthUserCollisionException -> {
                toast("This email is already registered. Please sign in.")
                isSignUp = false
                applyMode()
            }
            is FirebaseAuthInvalidCredentialsException,
            is FirebaseAuthInvalidUserException -> toast("Incorrect email or password")
            else -> toast(e.message ?: "Something went wrong")
        }
    }

    private fun setLoading(loading: Boolean) {
        btnAuth.isEnabled = !loading
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}