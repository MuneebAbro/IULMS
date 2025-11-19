package com.freaky.iulms.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import com.freaky.iulms.R

class InstallDialog(private val context: Context) {

    private var dialog: Dialog? = null
    private var progressBar: ProgressBar? = null
    private var statusText: TextView? = null
    private var progressText: TextView? = null

    fun show(title: String = "Installing Update") {
        // Wrap context with light Material theme
        val themedContext = ContextThemeWrapper(
            context,
            com.google.android.material.R.style.Theme_MaterialComponents_Light_Dialog
        )

        dialog = Dialog(themedContext).apply {
            setContentView(createDialogView())
            setCancelable(false)
            // Make the window background transparent if you want rounded corners etc.
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        // Set the status text
        statusText?.text = title

        dialog?.show()
    }


    fun updateProgress(progress: Int) {
        progressBar?.progress = progress
        progressText?.text = "$progress%"
    }

    fun updateStatus(status: String) {
        statusText?.text = status
    }

    fun dismiss() {
        dialog?.dismiss()
        dialog = null
    }

    private fun createDialogView(): View {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.dialog_install, null)

        progressBar = view.findViewById(R.id.install_progress_bar)
        statusText = view.findViewById(R.id.install_status_text)
        progressText = view.findViewById(R.id.install_progress_text)

        return view
    }
}