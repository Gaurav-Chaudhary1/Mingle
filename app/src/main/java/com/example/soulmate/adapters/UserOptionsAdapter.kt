package com.example.soulmate.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.soulmate.R
import com.example.soulmate.models.OptionItem

class UserOptionsAdapter(
    private val options: List<OptionItem>,  // Updated to use a data class for title and icon
    private val onItemClick: (OptionItem) -> Unit
) : RecyclerView.Adapter<UserOptionsAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val optionText: TextView = itemView.findViewById(R.id.tv_option_title)
        val optionIcon: ImageView = itemView.findViewById(R.id.iv_option_icon)
        val arrowIcon: ImageView = itemView.findViewById(R.id.iv_arrow_icon)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(options[position])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_option, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val option = options[position]
        holder.optionText.text = option.title
        holder.optionIcon.setImageResource(option.iconResId)
    }

    override fun getItemCount(): Int = options.size
}
