package com.freaky.iulms.adapter

import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.freaky.iulms.R
import com.freaky.iulms.VoucherPrintActivity
import com.freaky.iulms.model.VoucherItem

class VoucherAdapter(private val items: List<VoucherItem>) : RecyclerView.Adapter<VoucherAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_voucher_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val descriptionTextView: TextView = itemView.findViewById(R.id.description_textview)
        private val amountTextView: TextView = itemView.findViewById(R.id.amount_textview)
        private val voucherNumberTextView: TextView = itemView.findViewById(R.id.voucher_number_textview)
        private val dueDateTextView: TextView = itemView.findViewById(R.id.due_date_textview)
        private val printButton: Button = itemView.findViewById(R.id.print_button)
        private val cardView: CardView = itemView.findViewById(R.id.voucher_card)

        fun bind(item: VoucherItem) {
            descriptionTextView.text = item.description
            amountTextView.text = item.amount
            voucherNumberTextView.text = item.voucherNumber
            dueDateTextView.text = item.dueDate

            // Set text color for late payments
            if (item.isLate) {
                cardView.setCardBackgroundColor(Color.parseColor("#FFF5E1")) // Light yellow
                dueDateTextView.setTextColor(Color.RED)
            } else {
                cardView.setCardBackgroundColor(Color.WHITE)
                dueDateTextView.setTextColor(Color.GRAY)
            }

            // Handle print button click
            printButton.setOnClickListener {
                val context = itemView.context
                val intent = Intent(context, VoucherPrintActivity::class.java).apply {
                    putExtra("VOUCHER_NUMBER", item.printableVoucherNumber)
                    putExtra("STUDENT_ID", item.printableStudentId)
                }
                context.startActivity(intent)
            }
        }
    }
}