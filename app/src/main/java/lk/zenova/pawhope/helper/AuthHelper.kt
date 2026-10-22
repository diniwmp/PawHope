package lk.zenova.pawhope.helper

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

object AuthHelper {


    fun ensureSignedIn(onReady: (FirebaseUser) -> Unit, onError: (Exception) -> Unit) {
        val auth = FirebaseAuth.getInstance()
        val current = auth.currentUser

        if (current != null) {
            onReady(current)
            return
        }

        auth.signInAnonymously()
            .addOnSuccessListener { result -> result.user?.let { onReady(it) } }
            .addOnFailureListener { onError(it) }
    }

    fun isGuest(): Boolean {
        val user = FirebaseAuth.getInstance().currentUser
        return user == null || user.isAnonymous
    }
}