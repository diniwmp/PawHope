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
import lk.zenova.pawhope.model.Animal
import kotlin.math.roundToInt


class AnimalAdapter(
    private val context: Context,
    private val onAnimalClick: (Animal) -> Unit
) : RecyclerView.Adapter<AnimalAdapter.VH>() {

    private val items = mutableListOf<Animal>()

    @SuppressLint("NotifyDataSetChanged")
    fun setItems(newItems: List<Animal>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.animalImage)
        val badge: TextView = view.findViewById(R.id.conditionBadge)
        val title: TextView = view.findViewById(R.id.animalTitle)
        val need: TextView = view.findViewById(R.id.needPill)
        val description: TextView = view.findViewById(R.id.animalDescription)
        val location: TextView = view.findViewById(R.id.animalLocation)
        val time: TextView = view.findViewById(R.id.animalTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(context).inflate(R.layout.item_animal, parent, false)
        return VH(view)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val animal = items[position]

        // ---------- Photo ----------
        val firstPhoto = animal.photoUrls.firstOrNull()
        if (!firstPhoto.isNullOrEmpty()) {
            holder.image.clearColorFilter()
            holder.image.setPadding(0, 0, 0, 0)
            holder.image.scaleType = ImageView.ScaleType.CENTER_CROP
            Glide.with(context).load(firstPhoto).into(holder.image)
        } else {
            Glide.with(context).clear(holder.image)
            val pad = dp(44)
            holder.image.setPadding(pad, pad, pad, pad)
            holder.image.scaleType = ImageView.ScaleType.CENTER_INSIDE
            holder.image.setImageResource(R.drawable.ic_paw)
            holder.image.setColorFilter(ContextCompat.getColor(context, R.color.primary_color))
        }

        // ---------- Condition badge ----------

        val (condColor, condLabel) = when (animal.condition) {
            "critical" -> R.color.red to R.string.cond_critical
            "injured" -> R.color.limit_stock_color to R.string.cond_injured
            "healthy" -> R.color.online_green to R.string.cond_healthy
            else -> R.color.dark_blue to R.string.cond_needs_help
        }
        holder.badge.setText(condLabel)
        holder.badge.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(context, condColor))

        // ---------- Title ----------
        holder.title.setText(
            when (animal.animalType) {
                "dog" -> R.string.type_dog
                "cat" -> R.string.type_cat
                else -> R.string.type_other
            }
        )

        // ---------- Need pill ----------
        holder.need.setText(
            when (animal.needType) {
                "home" -> R.string.need_home
                "both" -> R.string.need_both
                else -> R.string.need_help
            }
        )

        // ---------- Description / Location ----------
        holder.description.text = animal.description
        holder.location.text = buildLocation(animal)

        val createdAt = animal.createdAt
        if (createdAt != null) {
            holder.time.text = DateUtils.getRelativeTimeSpanString(
                createdAt.time,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
            )
        } else {
            holder.time.setText(R.string.just_now)
        }

        holder.itemView.setOnClickListener { onAnimalClick(animal) }
    }

    private fun buildLocation(animal: Animal): String {
        val city = animal.city
        val district = animal.district
        return when {
            city.isNotEmpty() && district.isNotEmpty() &&
                    !city.equals(district, ignoreCase = true) -> "$city, $district"   // "$city" = string template
            city.isNotEmpty() -> city
            district.isNotEmpty() -> district
            animal.address.isNotEmpty() -> animal.address
            else -> context.getString(R.string.location_pinned)
        }
    }

    private fun dp(value: Int): Int =
        (value * context.resources.displayMetrics.density).roundToInt()
}