package lk.zenova.pawhope.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import lk.zenova.pawhope.R
import lk.zenova.pawhope.model.CareItem
import lk.zenova.pawhope.model.HelpRequest
import lk.zenova.pawhope.util.CareUi
import kotlin.math.ceil
import kotlin.math.roundToInt

// "My care" list එකේ සතා කාඩ් එක
class CareAdapter(
    private val context: Context,
    private val onClick: (CareItem) -> Unit
) : RecyclerView.Adapter<CareAdapter.VH>() {

    private val items = mutableListOf<CareItem>()

    @SuppressLint("NotifyDataSetChanged")
    fun setItems(newItems: List<CareItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.careImage)
        val title: TextView = view.findViewById(R.id.careTitle)
        val status: TextView = view.findViewById(R.id.careStatus)
        val offer: TextView = view.findViewById(R.id.careOffer)
        val hint: TextView = view.findViewById(R.id.careDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(context).inflate(R.layout.item_care, parent, false)
        return VH(view)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        val request = item.request
        val animal = item.animal

        // සතාගේ වර්ගය (සතාව delete වෙලා නම් "Animal")
        holder.title.setText(CareUi.animalTypeRes(animal?.animalType ?: ""))

        // Photo
        val photo = animal?.photoUrls?.firstOrNull()
        if (!photo.isNullOrEmpty()) {
            holder.image.clearColorFilter()
            holder.image.setPadding(0, 0, 0, 0)
            holder.image.scaleType = ImageView.ScaleType.CENTER_CROP
            Glide.with(context).load(photo).into(holder.image)
        } else {
            Glide.with(context).clear(holder.image)
            val pad = (24 * context.resources.displayMetrics.density).roundToInt()
            holder.image.setPadding(pad, pad, pad, pad)
            holder.image.scaleType = ImageView.ScaleType.CENTER_INSIDE
            holder.image.setImageResource(R.drawable.ic_paw)
            holder.image.setColorFilter(ContextCompat.getColor(context, R.color.primary_color))
        }

        // තත්වය: Details needed / In your care / Under review ...
        holder.status.text = CareUi.careStateLabel(request)
        holder.status.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(context, CareUi.careStateColor(request))
        )

        holder.offer.text = "Your offer: ${CareUi.helpTypeLabel(request.type)}"

        // ඊළඟට කළ යුතු දේ පිළිබඳ කෙටි උපදෙසක්
        val (hintText, overdue) = hintFor(request)
        holder.hint.text = hintText
        holder.hint.setTextColor(
            ContextCompat.getColor(context, if (overdue) R.color.red else R.color.text_hint)
        )

        holder.itemView.setOnClickListener { onClick(item) }
    }

    // (උපදෙස් text එක, පරක්කුද කියලා) යුගලයක් දෙනවා
    private fun hintFor(r: HelpRequest): Pair<String, Boolean> = when {
        r.status == "approved" && !r.handoverDone ->
            "Tap to complete your adoption details" to false
        r.status == "approved" -> {
            val due = r.nextCheckInDue
            if (due == null) {
                "Tap to open your care dashboard" to false
            } else {
                val days = ceil((due.time - System.currentTimeMillis()) / 86_400_000.0).toInt()
                if (days < 0) "Monthly check-in is overdue" to true
                else "Next check-in in $days days" to false
            }
        }
        r.status == "pending" -> "Waiting for the PawHope team to review" to false
        r.status == "rejected" -> "This offer wasn't approved" to false
        else -> "Care journey complete" to false
    }
}