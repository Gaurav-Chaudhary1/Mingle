package com.example.soulmate.utils

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.view.Window
import android.view.WindowManager

class ProgressDialogUtil {
    private var progressDialog: AlertDialog? = null

    fun showProgressDialog(context: Context, message: String = "Loading...") {
        // Create a Linear Layout
        val llPadding = 30
        val ll = LinearLayout(context)
        ll.orientation = LinearLayout.HORIZONTAL
        ll.setPadding(llPadding, llPadding, llPadding, llPadding)
        ll.gravity = Gravity.CENTER
        val llParam = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        ll.layoutParams = llParam

        // Create a ProgressBar
        val progressBar = ProgressBar(context)
        progressBar.isIndeterminate = true
        progressBar.setPadding(0, 0, llPadding, 0)
        progressBar.layoutParams = llParam

        // Create a TextView
        val tvText = TextView(context)
        tvText.text = message
        tvText.setTextColor(Color.parseColor("#000000"))
        tvText.textSize = 20f
        tvText.layoutParams = llParam

        // Add ProgressBar and TextView to Linear Layout
        ll.addView(progressBar)
        ll.addView(tvText)

        // Build AlertDialog
        val builder = AlertDialog.Builder(context)
        builder.setCancelable(false)
        builder.setView(ll)
        progressDialog = builder.create()

        // Set dialog window parameters
        progressDialog?.window?.let { window: Window ->
            val layoutParams = WindowManager.LayoutParams()
            layoutParams.copyFrom(window.attributes)
            layoutParams.width = LinearLayout.LayoutParams.WRAP_CONTENT
            layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT
            window.attributes = layoutParams
            window.setFlags(
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            )
        }

        progressDialog?.show()
    }

    fun hideProgressDialog() {
        progressDialog?.dismiss()
    }
}
