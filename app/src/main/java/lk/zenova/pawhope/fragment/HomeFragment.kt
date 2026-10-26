package lk.zenova.pawhope.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.chip.ChipGroup
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import lk.zenova.pawhope.R
import lk.zenova.pawhope.adapter.AnimalAdapter
import lk.zenova.pawhope.model.Animal
import android.content.Intent
import lk.zenova.pawhope.activity.AnimalDetailActivity

class HomeFragment : Fragment() {

    // Firestore එකෙන් එන සියලුම සත්තු
    private val allAnimals = mutableListOf<Animal>()

    // ListenerRegistration? = null වෙන්න පුළුවන් ("?"). listener එක නැවැත්වීමට ඕනි
    private var registration: ListenerRegistration? = null

    private lateinit var adapter: AnimalAdapter
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyState: View
    private lateinit var countText: TextView
    private lateinit var chipGroup: ChipGroup

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        progressBar = view.findViewById(R.id.progressBar)
        emptyState = view.findViewById(R.id.emptyState)
        countText = view.findViewById(R.id.countText)
        chipGroup = view.findViewById(R.id.chipGroup)
        val recycler = view.findViewById<RecyclerView>(R.id.animalRecycler)

        recycler.layoutManager = GridLayoutManager(requireContext(), 2)

        adapter = AnimalAdapter(requireContext()) { animal ->
            val intent = Intent(requireContext(), AnimalDetailActivity::class.java)
            intent.putExtra(AnimalDetailActivity.EXTRA_ANIMAL_ID, animal.id)
            startActivity(intent)
        }
        recycler.adapter = adapter

        swipeRefresh.setColorSchemeResources(R.color.primary_dark)
        swipeRefresh.setProgressBackgroundColorSchemeResource(R.color.surface_white)
        swipeRefresh.setOnRefreshListener {
            stopListening()
            startListening()
        }

        chipGroup.setOnCheckedStateChangeListener { _, _ -> applyFilter() }
    }

    override fun onStart() {
        super.onStart()
        startListening()
    }

    override fun onStop() {
        super.onStop()
        stopListening()
    }

    // Firestore එකෙන් real-time data අහන්න පටන් ගන්නවා
    private fun startListening() {
        if (allAnimals.isEmpty()) {
            progressBar.visibility = View.VISIBLE
        }

        registration = FirebaseFirestore.getInstance()
            .collection("animals")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                // lambda එකෙන් එළියට යන්න "return@addSnapshotListener" කියනවා
                if (!isAdded) return@addSnapshotListener

                progressBar.visibility = View.GONE
                swipeRefresh.isRefreshing = false

                if (error != null) {
                    Toast.makeText(
                        requireContext(),
                        "Couldn't load animals: ${error.message}",   // ${ } = string template
                        Toast.LENGTH_LONG
                    ).show()
                    applyFilter()
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                val animals = snapshot.documents.mapNotNull { doc ->
                    runCatching { doc.toObject(Animal::class.java) }.getOrNull()
                }

                allAnimals.clear()
                allAnimals.addAll(animals)
                applyFilter()
            }
    }

    private fun stopListening() {
        registration?.remove()   // ?. = null නැත්නම් විතරක් remove() කරනවා
        registration = null
    }

    // තෝරපු chip එකට අනුව list එක filter කරනවා
    private fun applyFilter() {
        val checked = chipGroup.checkedChipId

        // filter { } = Java වල for loop + if + add කරන කොටස එක පේළියකින්
        val filtered = allAnimals.
        filter { matches(it, checked) }
            .sortedWith(
                compareBy<Animal> { urgencyRank(it.condition) }
                    .thenByDescending { it.createdAt })

        adapter.setItems(filtered)

        val empty = filtered.isEmpty()
        emptyState.visibility = if (empty) View.VISIBLE else View.GONE
        countText.visibility = if (empty) View.GONE else View.VISIBLE
        countText.text = getString(R.string.home_count, filtered.size)
    }

    // අඩු අගය = වැඩි හදිසිය
    private fun urgencyRank(condition: String): Int = when (condition) {
        "critical" -> 0
        "injured" -> 1
        "sick", "needs_help" -> 2
        else -> 3   // healthy
    }

    private fun matches(animal: Animal, checkedChipId: Int): Boolean = when (checkedChipId) {
        R.id.chip_dogs -> animal.animalType == "dog"
        R.id.chip_cats -> animal.animalType == "cat"
        R.id.chip_urgent -> animal.condition == "critical" || animal.condition == "injured"
        R.id.chip_home -> animal.needType == "home" || animal.needType == "both"
        else -> true   // All
    }
}