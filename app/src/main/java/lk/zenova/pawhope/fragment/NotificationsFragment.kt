package lk.zenova.pawhope.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import lk.zenova.pawhope.R
import lk.zenova.pawhope.adapter.NotificationAdapter
import lk.zenova.pawhope.model.AppNotification

class NotificationsFragment : Fragment() {

    private var registration: ListenerRegistration? = null
    private lateinit var adapter: NotificationAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyState: View
    private lateinit var emptyTitle: TextView
    private lateinit var emptySub: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_notifications, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progressBar = view.findViewById(R.id.progressBar)
        emptyState = view.findViewById(R.id.emptyState)
        emptyTitle = view.findViewById(R.id.emptyTitle)
        emptySub = view.findViewById(R.id.emptySub)

        adapter = NotificationAdapter(requireContext()) { n -> markAsRead(n) }
        val recycler = view.findViewById<RecyclerView>(R.id.notifRecycler)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter
    }

    override fun onStart() {
        super.onStart()
        startListening()
    }

    override fun onStop() {
        super.onStop()
        registration?.remove()
        registration = null
    }

    private fun startListening() {
        val user = FirebaseAuth.getInstance().currentUser

        if (user == null || user.isAnonymous) {
            adapter.setItems(emptyList())
            emptyTitle.setText(R.string.profile_guest_title)
            emptySub.setText(R.string.notif_guest)
            emptyState.visibility = View.VISIBLE
            return
        }

        emptyTitle.setText(R.string.notif_empty)
        emptySub.setText(R.string.notif_empty_sub)
        progressBar.visibility = View.VISIBLE

        registration = FirebaseFirestore.getInstance()
            .collection("users").document(user.uid).collection("notifications")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (!isAdded) return@addSnapshotListener
                progressBar.visibility = View.GONE
                if (error != null || snapshot == null) return@addSnapshotListener

                val list = snapshot.documents.mapNotNull { doc ->
                    runCatching { doc.toObject(AppNotification::class.java) }.getOrNull()
                }
                adapter.setItems(list)
                emptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            }
    }

    private fun markAsRead(n: AppNotification) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        if (n.read) return
        FirebaseFirestore.getInstance()
            .collection("users").document(user.uid)
            .collection("notifications").document(n.id)
            .update("read", true)
    }
}