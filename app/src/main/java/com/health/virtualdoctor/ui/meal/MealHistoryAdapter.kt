package com.health.virtualdoctor.ui.meal

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.health.virtualdoctor.R

class MealHistoryAdapter(
    private val items: List<MealHistoryItem>,
    private val onItemClick: (MealHistoryItem) -> Unit
) : RecyclerView.Adapter<MealHistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardView: CardView = view.findViewById(R.id.cardMealHistory)
        val ivMealImage: ImageView = view.findViewById(R.id.ivMealImage)
        val tvFoodName: TextView = view.findViewById(R.id.tvFoodName)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvCalories: TextView = view.findViewById(R.id.tvCalories)
        val tvProteins: TextView = view.findViewById(R.id.tvProteins)
        val tvCarbs: TextView = view.findViewById(R.id.tvCarbs)
        val tvFats: TextView = view.findViewById(R.id.tvFats)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_meal_history, parent, false)
        return ViewHolder(view)
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.tvFoodName.text = item.foodName
        holder.tvDate.text = item.date
        holder.tvCalories.text = "${item.calories.toInt()} kcal"
        holder.tvProteins.text = "P: ${item.proteins}g"
        holder.tvCarbs.text = "G: ${item.carbs}g"
        holder.tvFats.text = "L: ${item.fats}g"

        // ✅ CORRECTION : Utilisez la vraie IP de MinIO
        val correctedImageUrl = item.imageUrl.replace(
            "http://localhost:9000",
            "http://192.168.0.132:9000"
        )

        android.util.Log.d("MealHistoryAdapter", "Original URL: ${item.imageUrl}")
        android.util.Log.d("MealHistoryAdapter", "Corrected URL: $correctedImageUrl")

        if (correctedImageUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(correctedImageUrl)
                .placeholder(R.drawable.ic_meal_placeholder)
                .error(R.drawable.ic_meal_placeholder)
                .listener(object : com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
                    override fun onLoadFailed(
                        e: com.bumptech.glide.load.engine.GlideException?,
                        model: Any?,
                        target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        android.util.Log.e("MealHistoryAdapter", "Failed to load: $correctedImageUrl", e)
                        return false
                    }

                    override fun onResourceReady(
                        resource: android.graphics.drawable.Drawable,
                        model: Any,
                        target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?,
                        dataSource: com.bumptech.glide.load.DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        android.util.Log.d("MealHistoryAdapter", "Image loaded successfully: $correctedImageUrl")
                        return false
                    }
                })
                .centerCrop()
                .into(holder.ivMealImage)
        } else {
            holder.ivMealImage.setImageResource(R.drawable.ic_meal_placeholder)
        }

        holder.cardView.setOnClickListener {
            onItemClick(item)
        }
    }
    override fun getItemCount() = items.size
}