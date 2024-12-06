package com.example.soulmate.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.soulmate.R
import android.webkit.WebView
import android.webkit.WebViewClient

class WebViewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_view)

        val webView: WebView = findViewById(R.id.webView)
        val url = intent.getStringExtra("url")
        val title = intent.getStringExtra("title")

        supportActionBar?.title = title
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        webView.webViewClient = WebViewClient()
        webView.settings.javaScriptEnabled = true
        webView.loadUrl(url ?: "https://www.example.com")
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
