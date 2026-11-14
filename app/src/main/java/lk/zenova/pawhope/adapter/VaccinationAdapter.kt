package lk.zenova.pawhope.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import lk.zenova.pawhope.R
import lk.zenova.pawhope.model.Vaccination

class VaccinationAdapter(
    private val context: Context,
    private val onMarkDone: (Vaccination) -> Unit
) : RecyclerView.Adapter<VaccinationAdapter.VH>() {

    private val items = mutableListOf<Vaccination>()
    private val dateFormat = DateFormat.getMediumDateFormat(context)

    @SuppressLint("NotifyDataSetChanged")
    fun setItems(newItems: List<Vaccination>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.vacName)
        val due: TextView = view.findViewById(R.id.vacDue)
        val status: TextView = view.findViewById(R.id.vacStatus)
        val btnDone: MaterialButton = view.findViewById(R.id.btnVacDone)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(context).inflate(R.layout.item_vaccine, parent, false)
        return VH(view)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val v = items[position]
        holder.name.text = v.name

        val dueDate = v.dueDate
        val doneAt = v.doneAt
        val now = System.currentTimeMillis()
        val dueMillis = dueDate?.time

        val (label, colorRes) = when {
            v.status == "done" -> "Done" to R.color.online_green
            dueMillis != null && dueMillis < now -> "Overdue" to R.color.red
            dueMillis != null && dueMillis - now < 7 * 24 * 3600 * 1000L ->
                "Due soon" to R.color.limit_stock_color
            else -> "Upcoming" to R.color.dark_blue
        }
        holder.status.text = label
        holder.status.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(context, colorRes))

        holder.due.text = when {
            v.status == "done" && doneAt != null -> "Given on ${dateFormat.format(doneAt)}"
            dueDate != null -> "Due ${dateFormat.format(dueDate)}"
            else -> ""
        }

        holder.btnDone.visibility = if (v.status == "done") View.GONE else View.VISIBLE
        holder.btnDone.setOnClickListener { onMarkDone(v) }
    }
}