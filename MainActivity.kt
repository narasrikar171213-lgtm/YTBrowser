package com.yt.browser

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.*
import android.widget.*
import android.graphics.Color
import android.graphics.Typeface
import android.content.Intent
import android.provider.Browser
import java.net.URI
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var web: WebView
    private lateinit var address: EditText
    private lateinit var status: TextView
    private var adBlockEnabled = true
    private val bookmarks = linkedSetOf<String>()
    private val blockedHosts = setOf(
        "doubleclick.net", "googlesyndication.com", "googleadservices.com",
        "adservice.google.com", "adsystem.com", "adnxs.com", "taboola.com",
        "outbrain.com", "scorecardresearch.com", "criteo.com", "ads.yahoo.com"
    )
    private val homeUrl = "https://www.google.com"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        configureWebView()
        showHome()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(248, 250, 252))
            setPadding(12, 12, 12, 0)
        }
        val brand = TextView(this).apply {
            text = "🔷 YT Browser"
            textSize = 22f
            setTextColor(Color.rgb(25, 85, 180))
            typeface = Typeface.DEFAULT_BOLD
            setPadding(4, 8, 4, 12)
        }
        root.addView(brand)
        address = EditText(this).apply {
            hint = "Search Google or enter website"
            singleLine = true
            textSize = 16f
            setPadding(16, 8, 16, 8)
            setBackgroundColor(Color.WHITE)
            imeOptions = EditorInfo.IME_ACTION_GO
            setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_search, 0, 0, 0)
        }
        address.setOnEditorActionListener { _, action, _ ->
            if (action == EditorInfo.IME_ACTION_GO || action == EditorInfo.IME_ACTION_DONE) {
                navigate(address.text.toString()); true
            } else false
        }
        root.addView(address, LinearLayout.LayoutParams(-1, 54))
        status = TextView(this).apply {
            text = "Protected browsing enabled"
            textSize = 12f
            setTextColor(Color.rgb(70, 100, 80))
            setPadding(4, 6, 4, 6)
        }
        root.addView(status)
        web = WebView(this)
        root.addView(web, LinearLayout.LayoutParams(-1, 0, 1f))

        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 6, 0, 10)
        }
        fun navButton(label: String, action: () -> Unit) {
            val b = Button(this).apply { text = label; textSize = 11f; isAllCaps = false }
            nav.addView(b, LinearLayout.LayoutParams(0, 48, 1f))
            b.setOnClickListener { action() }
        }
        navButton("←") { if (web.canGoBack()) web.goBack() }
        navButton("Home") { showHome() }
        navButton("★ Save") { web.url?.let { bookmarks.add(it); toast("Bookmark saved") } }
        navButton("Bookmarks") { showBookmarks() }
        navButton("Downloads") { showDownloadsInfo() }
        navButton("Ad block: ON") {
            adBlockEnabled = !adBlockEnabled
            status.text = if (adBlockEnabled) "Ad/tracker filter enabled (basic list)" else "Ad/tracker filter disabled"
            toast(status.text.toString())
            web.reload()
        }
        root.addView(nav)
        setContentView(root)
    }

    @Suppress("SetJavaScriptEnabled")
    private fun configureWebView() {
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.loadsImagesAutomatically = true
        web.settings.javaScriptCanOpenWindowsAutomatically = false
        web.settings.setSupportMultipleWindows(false)
        web.settings.allowFileAccess = false
        web.settings.allowContentAccess = false
        web.settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, false)
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url ?: return false
                val scheme = url.scheme?.lowercase(Locale.ROOT)
                if (scheme != "https" && scheme != "http") {
                    return true
                }
                address.setText(url.toString())
                return false
            }
            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                if (!adBlockEnabled) return super.shouldInterceptRequest(view, request)
                val host = request?.url?.host?.lowercase(Locale.ROOT) ?: return super.shouldInterceptRequest(view, request)
                val blocked = blockedHosts.any { host == it || host.endsWith(".$it") }
                if (blocked) {
                    return WebResourceResponse("text/plain", "utf-8", 204, "No Content",
                        mapOf("Cache-Control" to "no-store"), java.io.ByteArrayInputStream(ByteArray(0)))
                }
                return super.shouldInterceptRequest(view, request)
            }
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                if (url != null) address.setText(url)
                status.text = if (adBlockEnabled) "Loading · basic tracker filter on" else "Loading · filter off"
            }
            override fun onPageFinished(view: WebView?, url: String?) {
                status.text = "Secure connection indicator · verify site address"
            }
            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: android.net.http.SslError?) {
                handler?.cancel() // Never bypass certificate errors.
                toast("Blocked an invalid or untrusted HTTPS certificate")
            }
        }
        web.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            startDownload(url, userAgent, contentDisposition, mimeType)
        }
        web.webChromeClient = WebChromeClient()
    }

    private fun navigate(input: String) {
        val text = input.trim()
        if (text.isEmpty()) return
        val url = when {
            text.startsWith("https://") || text.startsWith("http://") -> text
            text.contains(" ") || !text.contains(".") -> "https://www.google.com/search?q=" + Uri.encode(text)
            else -> "https://$text"
        }
        val parsed = runCatching { URI(url) }.getOrNull()
        if (parsed == null || parsed.scheme !in listOf("http", "https")) {
            toast("Please enter a valid website or search")
            return
        }
        web.loadUrl(url)
    }

    private fun showHome() {
        address.setText(homeUrl)
        web.loadUrl(homeUrl)
    }

    private fun showBookmarks() {
        if (bookmarks.isEmpty()) { toast("No bookmarks saved yet"); return }
        val choices = bookmarks.toTypedArray()
        AlertDialog.Builder(this).setTitle("Bookmarks").setItems(choices) { _, which ->
            web.loadUrl(choices[which])
        }.setNegativeButton("Close", null).show()
    }

    private fun showDownloadsInfo() {
        AlertDialog.Builder(this).setTitle("Downloads")
            .setMessage("Downloads are saved to the device Downloads folder. Use the Android Files app to view them. Only download audio or files you are authorized to save.")
            .setPositiveButton("OK", null).show()
    }

    private fun startDownload(url: String, userAgent: String?, disposition: String?, mimeType: String?) {
        val uri = Uri.parse(url)
        if (uri.scheme != "https") {
            toast("For safety, downloads must use HTTPS")
            return
        }
        val host = uri.host?.lowercase(Locale.ROOT) ?: ""
        if (host == "youtube.com" || host.endsWith(".youtube.com") ||
            host == "music.youtube.com" || host.endsWith(".googlevideo.com")) {
            toast("YouTube media extraction is not supported. Use official download options or authorized audio links.")
            return
        }
        val filename = URLUtil.guessFileName(url, disposition, mimeType)
        val request = DownloadManager.Request(uri)
            .setTitle(filename)
            .setDescription("YT Browser download — only save files you have permission to download")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)
        if (!userAgent.isNullOrBlank()) request.addRequestHeader("User-Agent", userAgent)
        if (mimeType != null) request.setMimeType(mimeType)
        val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.enqueue(request)
        toast("Download started: $filename")
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()

    override fun onBackPressed() {
        if (::web.isInitialized && web.canGoBack()) web.goBack() else super.onBackPressed()
    }

    override fun onDestroy() {
        if (::web.isInitialized) {
            web.stopLoading()
            web.destroy()
        }
        super.onDestroy()
    }
}
