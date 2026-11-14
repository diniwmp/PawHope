package lk.zenova.pawhope.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.text.format.DateUtils
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
import lk.zenova.pawhope.util.CareUi
import kotlin.math.roundToInt

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
        val date: TextView = view.findViewById(R.id.careDate)
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

        holder.title.setText(CareUi.animalTypeRes(animal?.animalType ?: ""))

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

        holder.status.text = CareUi.requestStatusLabel(request.status)
        holder.status.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(context, CareUi.requestStatusColor(request.status))
        )

        holder.offer.text = "Your offer: ${CareUi.helpTypeLabel(request.type)}"

        val createdAt = request.createdAt
        holder.date.text = if (createdAt != null) {
            DateUtils.getRelativeTimeSpanString(
                createdAt.time, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS
            )
        } else {
            context.getString(R.string.just_now)
        }

        holder.itemView.setOnClickListener { onClick(item) }
    }
}