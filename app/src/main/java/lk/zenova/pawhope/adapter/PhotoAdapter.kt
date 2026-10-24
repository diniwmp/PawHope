package lk.zenova.pawhope.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import lk.zenova.pawhope.R

// Report form එකේ තෝරපු photos පෙන්නන small list එක
// onRemove: X බටන් එක එබුවාම "කී වෙනි photo එකද" කියලා Fragment එකට කියනවා
class PhotoAdapter(
    private val context: Context,
    private val onRemove: (Int) -> Unit
) : RecyclerView.Adapter<PhotoAdapter.VH>() {

    private val items = mutableListOf<Uri>()

    @SuppressLint("NotifyDataSetChanged")
    fun setItems(newItems: List<Uri>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.photoImage)
        val remove: ImageView = view.findViewById(R.id.removePhoto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(context).inflate(R.layout.item_photo, parent, false)
        return VH(view)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        Glide.with(context).load(items[position]).into(holder.image)

        holder.remove.setOnClickListener {
            // position එක scroll/remove නිසා වෙනස් වෙන්න පුළුවන්, ඒ නිසා දැන් තියෙන තැන අහනවා
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) onRemove(pos)
        }
    }
}