package com.freaky.iulms.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.freaky.iulms.R
import com.freaky.iulms.model.SavedAccount

class SavedAccountsAdapter(
    private val accounts: List<SavedAccount>,
    private val onAccountSelected: (SavedAccount) -> Unit
) : RecyclerView.Adapter<SavedAccountsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_saved_account, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val account = accounts[position]
        holder.bind(account, onAccountSelected)
    }

    override fun getItemCount(): Int = accounts.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val usernameTextView: TextView = itemView.findViewById(R.id.saved_username_textview)
        private val loginButton: Button = itemView.findViewById(R.id.login_as_button)

        fun bind(account: SavedAccount, onAccountSelected: (SavedAccount) -> Unit) {
            usernameTextView.text = account.username
            loginButton.setOnClickListener {
                onAccountSelected(account)
            }
        }
    }
}