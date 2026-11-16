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
import lk.zenova.pawhope.model.CareUpdate
import lk.zenova.pawhope.util.CareUi

class UpdateAdapter(private val context: Context) :
    RecyclerView.Adapter<UpdateAdapter.VH>() {

    private val items = mutableListOf<CareUpdate>()

    @SuppressLint("NotifyDataSetChanged")
    fun setItems(newItems: List<CareUpdate>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val iconCircle: View = view.findViewById(R.id.updIconCircle)
        val title: TextView = view.findViewById(R.id.updTitle)
        val date: TextView = view.findViewById(R.id.updDate)
        val details: TextView = view.findViewById(R.id.updDetails)
        val text: TextView = view.findViewById(R.id.updText)
        val symptoms: TextView = view.findViewById(R.id.updSymptoms)
        val severity: TextView = view.findViewById(R.id.updSeverity)
        val status: TextView = view.findViewById(R.id.updStatus)
        val photosRow: View = view.findViewById(R.id.updPhotos)
        val photoViews: List<ImageView> = listOf(
            view.findViewById(R.id.updPhoto1),
            view.findViewById(R.id.updPhoto2),
            view.findViewById(R.id.updPhoto3)
        )
        val replyBox: View = view.findViewById(R.id.updReplyBox)
        val replyText: TextView = view.findViewById(R.id.updReplyText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(context).inflate(R.layout.item_update, parent, false)
        return VH(view)
    }

    override fun getItemCount() = items.size

    private fun tint(view: View, colorRes: Int) {
        view.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, colorRes))
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val u = items[position]
        val isHealth = u.type == "health"

        holder.title.text = CareUi.reportTitle(u.type)
        tint(holder.iconCircle, CareUi.reportColor(u.type))

        val createdAt = u.createdAt
        holder.date.text = if (createdAt != null) {
            DateUtils.getRelativeTimeSpanString(
                createdAt.time, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS
            )
        } else {
            context.getString(R.string.just_now)
        }

        val lines = CareUi.detailLabels.mapNotNull { (key, label) ->
            u.details[key]?.takeIf { it.isNotBlank() }?.let { "$label: $it" }
        }
        holder.details.text = lines.joinToString("\n")
        holder.details.visibility = if (lines.isEmpty()) View.GONE else View.VISIBLE

        holder.text.text = u.text
        holder.text.visibility = if (u.text.isEmpty()) View.GONE else View.VISIBLE

        if (u.symptoms.isNotEmpty()) {
            holder.symptoms.text = "Noticed: ${u.symptoms.joinToString(", ")}"
            holder.symptoms.visibility = View.VISIBLE
        } else {
            holder.symptoms.visibility = View.GONE
        }

        if (isHealth) {
            if (u.severity.isNotEmpty()) {
                holder.severity.text = u.severity.replaceFirstChar { it.uppercase() }
                tint(
                    holder.severity, when (u.severity) {
                        "severe" -> R.color.red
                        "moderate" -> R.color.limit_stock_color
                        else -> R.color.dark_blue
                    }
                )
                holder.severity.visibility = View.VISIBLE
            } else {
                holder.severity.visibility = View.GONE
            }
            val resolved = u.status == "resolved"
            holder.status.text = if (resolved) "Resolved" else "Open"
            tint(holder.status, if (resolved) R.color.online_green else R.color.limit_stock_color)
            holder.status.visibility = View.VISIBLE
        } else {
            holder.severity.visibility = View.GONE
            holder.status.visibility = View.GONE
        }

        val urls = if (u.photoUrls.isNotEmpty()) u.photoUrls else listOfNotNull(u.photoUrl.ifEmpty { null })
        holder.photosRow.visibility = if (urls.isEmpty()) View.GONE else View.VISIBLE
        holder.photoViews.forEachIndexed { i, imageView ->
            val url = urls.getOrNull(i)
            if (url != null) {
                imageView.visibility = View.VISIBLE
                Glide.with(context).load(url).into(imageView)
            } else {
                Glide.with(context).clear(imageView)
                imageView.visibility = View.GONE
            }
        }

        if (u.adminReply.isNotEmpty()) {
            holder.replyText.text = u.adminReply
            holder.replyBox.visibility = View.VISIBLE
        } else {
            holder.replyBox.visibility = View.GONE
        }
    }
}