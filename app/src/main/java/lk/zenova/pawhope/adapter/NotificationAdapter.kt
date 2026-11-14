package lk.zenova.pawhope.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import lk.zenova.pawhope.R
import lk.zenova.pawhope.model.AppNotification

class NotificationAdapter(
    private val context: Context,
    private val onClick: (AppNotification) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.VH>() {

    private val items = mutableListOf<AppNotification>()

    @SuppressLint("NotifyDataSetChanged")
    fun setItems(newItems: List<AppNotification>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val iconCircle: View = view.findViewById(R.id.notifIconCircle)
        val title: TextView = view.findViewById(R.id.notifTitle)
        val body: TextView = view.findViewById(R.id.notifBody)
        val time: TextView = view.findViewById(R.id.notifTime)
        val unreadDot: View = view.findViewById(R.id.notifUnreadDot)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(context).inflate(R.layout.item_notification, parent, false)
        return VH(view)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val n = items[position]

        val colorRes = when (n.type) {
            "vaccine" -> R.color.dark_blue
            "health" -> R.color.red
            "adoption" -> R.color.online_green
            else -> R.color.primary_dark
        }
        holder.iconCircle.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(context, colorRes))

        holder.title.text = n.title
        holder.body.text = n.body

        val createdAt = n.createdAt
        holder.time.text = if (createdAt != null) {
            DateUtils.getRelativeTimeSpanString(
                createdAt.time, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS
            )
        } else {
            context.getString(R.string.just_now)
        }

        holder.unreadDot.visibility = if (n.read) View.INVISIBLE else View.VISIBLE
        holder.title.alpha = if (n.read) 0.75f else 1f

        holder.itemView.setOnClickListener { onClick(n) }
    }
}