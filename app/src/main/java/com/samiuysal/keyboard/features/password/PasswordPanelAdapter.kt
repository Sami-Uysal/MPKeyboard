package com.samiuysal.keyboard.features.password

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.samiuysal.keyboard.R
import com.samiuysal.keyboard.data.password.Password

class PasswordPanelAdapter(
        private val onFillUsername: (String) -> Unit,
        private val onFillPassword: (String) -> Unit
) : RecyclerView.Adapter<PasswordPanelAdapter.ViewHolder>() {

    private var passwords: List<Password> = emptyList()

    fun setPasswords(list: List<Password>) {
        passwords = list
        notifyDataSetChanged()
    }

    private var textColor: Int? = null

    fun setTheme(txtColor: Int) {
        textColor = txtColor
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
                LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_password_panel, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(passwords[position])
    }

    override fun getItemCount() = passwords.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtSiteName: TextView = itemView.findViewById(R.id.txtSiteName)
        private val txtUsername: TextView = itemView.findViewById(R.id.txtUsername)
        private val btnFillUsername: ImageButton = itemView.findViewById(R.id.btnFillUsername)
        private val btnFillPassword: ImageButton = itemView.findViewById(R.id.btnFillPassword)

        fun bind(password: Password) {
            txtSiteName.text = password.siteName
            txtUsername.text = password.username

            btnFillUsername.setOnClickListener { onFillUsername(password.username) }
            btnFillPassword.setOnClickListener { onFillPassword(password.password) }

            if (textColor != null) {
                txtSiteName.setTextColor(textColor!!)
                txtUsername.setTextColor(textColor!!)
                btnFillUsername.setColorFilter(textColor!!)
                btnFillPassword.setColorFilter(textColor!!)
            }
        }
    }
}
