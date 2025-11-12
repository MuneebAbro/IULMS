package com.freaky.iulms.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.freaky.iulms.R
import com.freaky.iulms.model.DashboardItem

class DashboardAdapter(
    private val items: List<DashboardItem>,
    private val onItemSelected: (DashboardItem) -> Unit
) : RecyclerView.Adapter<DashboardAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_dashboard_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item, onItemSelected)
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val icon: ImageView = itemView.findViewById(R.id.dashboard_icon)
        private val title: TextView = itemView.findViewById(R.id.dashboard_title)

        fun bind(item: DashboardItem, onItemSelected: (DashboardItem) -> Unit) {
            icon.setImageResource(item.iconResId)
            title.text = item.title
            itemView.setOnClickListener {
                onItemSelected(item)
            }
        }
    }
}