package com.samiuysal.keyboard.features.password

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.samiuysal.keyboard.R
import com.samiuysal.keyboard.data.password.Password

class PasswordSettingsAdapter(
        private val onEditClick: (Password) -> Unit,
        private val onDeleteClick: (Password) -> Unit
) : RecyclerView.Adapter<PasswordSettingsAdapter.ViewHolder>() {

    private var passwords: List<Password> = emptyList()

    fun setPasswords(list: List<Password>) {
        passwords = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
                LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_password_settings, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(passwords[position])
    }

    override fun getItemCount() = passwords.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtSiteName: TextView = itemView.findViewById(R.id.txtSiteName)
        private val txtUsername: TextView = itemView.findViewById(R.id.txtUsername)
        private val txtPassword: TextView = itemView.findViewById(R.id.txtPassword)
        private val btnTogglePassword: ImageButton = itemView.findViewById(R.id.btnTogglePassword)
        private val btnEdit: ImageButton = itemView.findViewById(R.id.btnEdit)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)

        private var isPasswordVisible = false
        private var currentPassword: String = ""

        fun bind(password: Password) {
            txtSiteName.text = password.siteName
            txtUsername.text =
                    if (password.username.isNotEmpty()) password.username else "(kullanıcı adı yok)"
            currentPassword = password.password

            // Initially show masked password
            isPasswordVisible = false
            updatePasswordDisplay()

            btnTogglePassword.setOnClickListener {
                isPasswordVisible = !isPasswordVisible
                updatePasswordDisplay()
            }

            btnEdit.setOnClickListener { onEditClick(password) }
            btnDelete.setOnClickListener { onDeleteClick(password) }
        }

        private fun updatePasswordDisplay() {
            if (currentPassword.isEmpty()) {
                txtPassword.text = "(şifre yok)"
                btnTogglePassword.setImageResource(R.drawable.ic_visibility_off)
            } else if (isPasswordVisible) {
                txtPassword.text = currentPassword
                btnTogglePassword.setImageResource(R.drawable.ic_visibility)
            } else {
                txtPassword.text = "•".repeat(currentPassword.length.coerceAtMost(12))
                btnTogglePassword.setImageResource(R.drawable.ic_visibility_off)
            }
        }
    }
}
