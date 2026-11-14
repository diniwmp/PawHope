package lk.zenova.pawhope.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import lk.zenova.pawhope.R
import lk.zenova.pawhope.adapter.CareAdapter
import lk.zenova.pawhope.model.Animal
import lk.zenova.pawhope.model.CareItem
import lk.zenova.pawhope.model.HelpRequest

class MyCareActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var adapter: CareAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyState: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_care)

        progressBar = findViewById(R.id.progressBar)
        emptyState = findViewById(R.id.emptyState)
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        adapter = CareAdapter(this) { item ->
            startActivity(
                Intent(this, CareDetailActivity::class.java)
                    .putExtra(CareDetailActivity.EXTRA_REQUEST_ID, item.request.id)
            )
        }
        val recycler = findViewById<RecyclerView>(R.id.careRecycler)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter
    }

    override fun onStart() {
        super.onStart()
        loadMyCare()
    }

    private fun loadMyCare() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null || user.isAnonymous) {
            finish()
            return
        }

        progressBar.visibility = View.VISIBLE

        db.collection("helpRequests").whereEqualTo("helperId", user.uid).get()
            .addOnSuccessListener { snap ->
                val requests = snap.documents
                    .mapNotNull { runCatching { it.toObject(HelpRequest::class.java) }.getOrNull() }
                    .sortedByDescending { it.createdAt }

                if (requests.isEmpty()) {
                    showItems(emptyList())
                } else {
                    loadAnimals(requests)
                }
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Couldn't load: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun loadAnimals(requests: List<HelpRequest>) {
        val ids = requests.map { it.animalId }.distinct()
        val tasks = ids.chunked(10).map { chunk ->
            db.collection("animals").whereIn(FieldPath.documentId(), chunk).get()
        }

        Tasks.whenAllSuccess<QuerySnapshot>(tasks)
            .addOnSuccessListener { results ->
                val animals = results
                    .flatMap { it.documents }
                    .mapNotNull { runCatching { it.toObject(Animal::class.java) }.getOrNull() }
                    .associateBy { it.id }

                showItems(requests.map { CareItem(it, animals[it.animalId]) })
            }
            .addOnFailureListener {
                showItems(requests.map { CareItem(it, null) })
            }
    }

    private fun showItems(items: List<CareItem>) {
        progressBar.visibility = View.GONE
        adapter.setItems(items)
        emptyState.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
    }
}