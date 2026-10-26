package lk.zenova.pawhope.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import lk.zenova.pawhope.R

class PhotoPagerAdapter(private val context: Context) :
    RecyclerView.Adapter<PhotoPagerAdapter.VH>() {

    private val urls = mutableListOf<String>()

    @SuppressLint("NotifyDataSetChanged")
    fun setItems(newUrls: List<String>) {
        urls.clear()
        urls.addAll(newUrls)
        notifyDataSetChanged()
    }

    class VH(val image: ImageView) : RecyclerView.ViewHolder(image)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val image = ImageView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(ContextCompat.getColor(context, R.color.primary_light))
        }
        return VH(image)
    }

    override fun getItemCount() = if (urls.isEmpty()) 1 else urls.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val image = holder.image
        if (urls.isEmpty()) {
            Glide.with(context).clear(image)
            image.scaleType = ImageView.ScaleType.CENTER_INSIDE
            val pad = (80 * context.resources.displayMetrics.density).toInt()
            image.setPadding(pad, pad, pad, pad)
            image.setImageResource(R.drawable.ic_paw)
            image.setColorFilter(ContextCompat.getColor(context, R.color.primary_color))
        } else {
            image.clearColorFilter()
            image.setPadding(0, 0, 0, 0)
            image.scaleType = ImageView.ScaleType.CENTER_CROP
            Glide.with(context).load(urls[position]).into(image)
        }
    }
}