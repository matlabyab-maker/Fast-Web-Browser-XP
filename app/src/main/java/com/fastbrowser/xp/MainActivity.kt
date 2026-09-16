package com.fastbrowser.xp

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.ProgressDialog
import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import org.json.JSONObject
import org.json.JSONArray
import org.json.JSONTokener
import android.net.Uri
import android.os.Environment
import android.os.Bundle
import android.os.Build
import android.view.MotionEvent
import android.widget.ImageView
import android.view.KeyEvent
import android.os.Handler
import android.os.Looper
import android.util.Xml
import java.io.BufferedInputStream
import java.net.HttpURLConnection
import java.net.URL
import android.view.View
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.view.Gravity
import android.view.animation.AlphaAnimation
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.DownloadListener
import android.webkit.URLUtil
import android.webkit.WebView.HitTestResult
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import android.widget.ProgressBar
import android.widget.FrameLayout
import android.content.Intent
import android.webkit.ValueCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.app.PictureInPictureParams
import android.util.Rational

class MainActivity : AppCompatActivity() {
    private lateinit var web: WebView
    private lateinit var address: EditText
    private lateinit var control: TextView
    private lateinit var selectIndicator: View
    private lateinit var control2Indicator: View
    private var control2Mode = false
    private var control2StartX = -1f
    private var control2StartY = -1f
    private var selectMode = false
    private var textScale = 100
    private var fileCallback: ValueCallback<Array<Uri>>? = null
    private val historyPrefs by lazy { getSharedPreferences("browser_history", MODE_PRIVATE) }
    private data class BrowserTab(var url: String = "about:blank", var title: String = "New Tab")
    private val tabs = mutableListOf<BrowserTab>()
    private var currentTab = 0
    private lateinit var tabBar: LinearLayout
    private lateinit var webArea: FrameLayout
    private lateinit var btnShowAll: TextView
    private lateinit var newsTicker: LinearLayout
    private lateinit var newsText: TextView
    private lateinit var btnNewsSource: TextView
    private lateinit var btnNewsTranslate: TextView
    private lateinit var btnHideNews: TextView
    private var chromeBarsHidden = false
    private var fullScreenMode = false
    private var newsHidden = false
    private var splitMode = false
    private lateinit var splitContainer: LinearLayout
    private var splitWeb: WebView? = null
    private var radioPlayer: MediaPlayer? = null
    private var radioIndex = 0
    private var radioSecondList = false
    private var radioPlaying = false
    private var radioKbps = 48
    private val radioStations = listOf(
        "BBC World Service" to "https://stream.live.vc.bbcmedia.co.uk/bbc_world_service",
        "VOA News Now" to "https://voanews.com/mp3/voanews.m3u",
        "Radio France Internationale" to "https://rfienanglais64k.ice.infomaniak.ch/rfienanglais-64.aac",
        "Radio Farda" to "https://rfe-channel-07.akacast.akamaized.net/stream01"
    )
    private val radioSecondStations = listOf(
        "Radio Iran (Radio Browser)" to "https://de1.api.radio-browser.info/json/stations/bycountryexact/Iran?limit=20",
        "Radio Afghanistan (Radio Browser)" to "https://de1.api.radio-browser.info/json/stations/bycountryexact/Afghanistan?limit=20"
    )
    private var transcoderTemplate = ""
    private var newsSourceIndex = 0
    private var currentNewsText = ""
    private var desktopModeActive = false
    private var downloader2Active = false
    private var stopwatchRunning = false
    private var stopwatchStartMs = 0L
    private var stopwatchElapsedMs = 0L
    private val stopwatchHandler = Handler(Looper.getMainLooper())
    private lateinit var stopwatchButton: TextView
    private var zoomLensButton: FrameLayout? = null
    private var zoomLensIndicator: View? = null
    private var zoomMagnifier: android.widget.Magnifier? = null
    private var zoomLensActive = false
    private val stopwatchTick = object : Runnable {
        override fun run() {
            if (stopwatchRunning) {
                stopwatchElapsedMs = System.currentTimeMillis() - stopwatchStartMs
                updateStopwatchButton()
                stopwatchHandler.postDelayed(this, 200)
            }
        }
    }
    private val activeLightAnimations = mutableMapOf<Int, android.animation.ValueAnimator>()
    private val subtitleSources = listOf(
        "OpenSubtitles" to "https://www.opensubtitles.com/en/search?query=",
        "SUBDL" to "https://subdl.com/search?query=",
        "Addic7ed" to "https://www.addic7ed.com/search.php?search="
    )
    private val mainHandler = Handler(Looper.getMainLooper())
    private val detectedMediaUrls = linkedSetOf<String>()
    private val newsSources = listOf(
        "DW فارسی/عربی" to "https://rss.dw.com/syndication/feeds/MENA_RSS_GNS_AR.42103-copypaste.html",
        "DW English" to "https://rss.dw.com/syndication/feeds/VAS_CB_Eng_OurVoice.31791-cb.html"
    )

    private val homeUrl = "https://chatgpt.com/"
    private val chatGptUrl = "https://chatgpt.com/"
    private val githubUrl = "https://github.com/matlabyab-maker/Fast-Browser-XP"
    private val tokenUrl = "https://github.com/settings/tokens"
    private val googleUrl = "https://www.google.com/"
    private val searchEngines = listOf(
        "Google" to "https://www.google.com/",
        "Bing" to "https://www.bing.com/",
        "DuckDuckGo" to "https://duckduckgo.com/",
        "Yahoo" to "https://search.yahoo.com/",
        "Brave Search" to "https://search.brave.com/",
        "Ecosia" to "https://www.ecosia.org/",
        "Startpage" to "https://www.startpage.com/",
        "ذره‌بین (Zarebin)" to "https://zarebin.ir/"
    )

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        web = findViewById(R.id.webView)
        address = findViewById(R.id.addressBar)
        control = findViewById(R.id.btnControl)
        selectIndicator = findViewById(R.id.selectIndicator)
        control2Indicator = findViewById(R.id.control2Indicator)
        selectIndicator.background = android.graphics.drawable.GradientDrawable().apply { shape = android.graphics.drawable.GradientDrawable.OVAL; setColor(Color.rgb(255,165,0)) }
        tabBar = findViewById(R.id.tabBar)
        webArea = findViewById(R.id.webArea)
        btnShowAll = findViewById(R.id.btnShowAll)
        newsTicker = findViewById(R.id.newsTicker)
        newsText = findViewById(R.id.newsText)
        btnNewsSource = findViewById(R.id.btnNewsSource)
        btnNewsTranslate = findViewById(R.id.btnNewsTranslate)
        btnHideNews = findViewById(R.id.btnHideNews)
        newsText.isSelected = true
        newsText.ellipsize = android.text.TextUtils.TruncateAt.MARQUEE
        setupFloatingWheel()
        setupTopScrollWheel()
        setupDesktopMode()
        setupMouseControl()
        setupNewsTicker()
        setupRadioPlayer()
        setupSplitScreen()
        setupToolbarGroups()
        setupStopwatch()
        setupZoomLens()
        tabs.add(BrowserTab(homeUrl, "Fast Browser XP"))
        refreshTabBar()

        val s = web.settings
        s.javaScriptEnabled = true
        s.domStorageEnabled = true
        s.databaseEnabled = true
        s.cacheMode = WebSettings.LOAD_DEFAULT

        // WebView compatibility:
        // Keep the full page viewport available and allow both zoom-in and zoom-out.
        // LOAD_WITH_OVERVIEW gives the user a smaller starting scale on wide pages
        // (such as GitHub) instead of locking the page to an unnecessarily large scale.
        s.useWideViewPort = true
        s.loadWithOverviewMode = true
        s.setSupportZoom(true)
        s.builtInZoomControls = true
        s.displayZoomControls = false

        // Modern sites such as GitHub rely on these WebView features.
        s.domStorageEnabled = true
        s.databaseEnabled = true
        s.loadsImagesAutomatically = true
        s.blockNetworkImage = false
        s.allowFileAccess = true
        s.allowContentAccess = true
        s.javaScriptCanOpenWindowsAutomatically = true
        s.setSupportMultipleWindows(false)
        s.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        s.mediaPlaybackRequiresUserGesture = false

        s.textZoom = textScale
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true)

        web.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                scanPageMedia(view)
                address.setText(url)
                if (tabs.isNotEmpty()) {
                    tabs[currentTab].url = url
                    tabs[currentTab].title = view.title?.takeIf { it.isNotBlank() } ?: "New Tab"
                    refreshTabBar()
                }
                saveHistory(url)
                if (selectMode) installSelectMode()
            }

            override fun onLoadResource(view: WebView, url: String) {
                rememberMediaUrl(url)
                super.onLoadResource(view, url)
            }
        }
        web.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView,
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: FileChooserParams
            ): Boolean {
                fileCallback?.onReceiveValue(null)
                fileCallback = filePathCallback
                return try {
                    startActivityForResult(fileChooserParams.createIntent(), FILE_CHOOSER_REQUEST)
                    true
                } catch (_: Exception) {
                    fileCallback?.onReceiveValue(null)
                    fileCallback = null
                    false
                }
            }
        }

        web.setDownloadListener(DownloadListener { _, _, _, _, _ ->
            // Never start a download just because a normal link was tapped.
            // Downloads are intentionally user-initiated through long-press / VIDEO DL / Downloads UI.
            Toast.makeText(this, "دانلود خودکار خاموش است؛ برای دانلود لینک را نگه دارید.", Toast.LENGTH_SHORT).show()
        })

        // Long-press an image (or downloadable link) to open our own download dialog.
        web.setOnLongClickListener {
            val hit = web.hitTestResult
            val type = hit?.type ?: HitTestResult.UNKNOWN_TYPE
            val extra = hit?.extra
            when {
                (type == HitTestResult.IMAGE_TYPE || type == HitTestResult.SRC_IMAGE_ANCHOR_TYPE) && !extra.isNullOrBlank() -> {
                    showDownloadChoiceDialog(extra, web.settings.userAgentString, null, guessMimeForName(URLUtil.guessFileName(extra, null, null)), web.url)
                    true
                }
                type == HitTestResult.SRC_ANCHOR_TYPE && !extra.isNullOrBlank() && isHttpUrl(extra) -> {
                    showDownloadChoiceDialog(extra, web.settings.userAgentString, null, guessMimeForName(URLUtil.guessFileName(extra, null, null)), web.url)
                    true
                }
                else -> false
            }
        }

        web.setOnTouchListener { _, event ->
            if (!control2Mode) return@setOnTouchListener false
            if (event.action == android.view.MotionEvent.ACTION_UP) {
                if (control2StartX < 0f) {
                    control2StartX = event.x
                    control2StartY = event.y
                    Toast.makeText(this, "نقطه اول ثبت شد؛ نقطه دوم را بزنید.", Toast.LENGTH_SHORT).show()
                } else {
                    val endX = event.x
                    val endY = event.y
                    copySelectedRegion(control2StartX, control2StartY, endX, endY)
                    control2StartX = -1f
                    control2StartY = -1f
                }
                return@setOnTouchListener true
            }
            true
        }

        findViewById<TextView>(R.id.btnBack).setOnClickListener { if (web.canGoBack()) web.goBack() }
        findViewById<TextView>(R.id.btnForward).setOnClickListener { if (web.canGoForward()) web.goForward() }
        findViewById<TextView>(R.id.btnReload).setOnClickListener { web.reload() }
        setupGoogleButton()
        findViewById<TextView>(R.id.btnHome).setOnClickListener { web.loadUrl(homeUrl) }
        findViewById<TextView>(R.id.btnChatGPT).setOnClickListener { web.loadUrl(chatGptUrl) }
        findViewById<TextView>(R.id.btnToken).setOnClickListener { web.loadUrl(tokenUrl) }
        findViewById<TextView>(R.id.btnRepo).setOnClickListener { web.loadUrl(githubUrl) }
        findViewById<TextView>(R.id.btnBookmark).setOnClickListener { showBookmarks() }
        findViewById<TextView>(R.id.btnHistory).setOnClickListener { showHistory() }
        findViewById<TextView>(R.id.btnDownloads).setOnClickListener { showDownloads() }
        findViewById<TextView>(R.id.btnApps).setOnClickListener { showAppSources() }
        findViewById<TextView>(R.id.btnSubs).setOnClickListener { showSubtitleSources() }
        findViewById<TextView>(R.id.btnSocial).setOnClickListener { showSocialSources() }
        findViewById<TextView>(R.id.btnVideoDownload).setOnClickListener { detectAndOfferVideoDownloads() }
        findViewById<TextView>(R.id.btnGo).setOnClickListener { openAddress() }
        address.setOnEditorActionListener { _, _, _ -> openAddress(); true }

        control.setOnClickListener { toggleSelectMode() }
        findViewById<TextView>(R.id.btnControl2).setOnClickListener { toggleControl2Mode() }
        findViewById<TextView>(R.id.btnSelect).setOnClickListener { toggleSelectMode() }
        findViewById<TextView>(R.id.btnWheel).setOnClickListener { showWheel() }
        findViewById<TextView>(R.id.btnFullscreen).setOnClickListener { toggleFullScreen() }
        findViewById<TextView>(R.id.btnFloat).setOnClickListener { enterFloatMode() }
        findViewById<TextView>(R.id.btnScreenshot).setOnClickListener {
            Toast.makeText(this, "برای ذخیره تصویر از قابلیت Screenshot دستگاه استفاده کنید.", Toast.LENGTH_SHORT).show()
        }
        findViewById<TextView>(R.id.btnSavePage).setOnClickListener {
            Toast.makeText(this, "ذخیره صفحه وب به‌صورت فایل در این نسخه فعال نیست.", Toast.LENGTH_SHORT).show()
        }
        findViewById<TextView>(R.id.btnUpload).setOnClickListener {
            openWebsiteFileChooser()
        }
        findViewById<TextView>(R.id.btnNewTab).setOnClickListener { createNewTab() }
        findViewById<TextView>(R.id.btnCloseAllTabs).setOnClickListener { closeAllTabs() }
        findViewById<TextView>(R.id.btnEnterTop).setOnClickListener { sendEnterToWeb() }
        findViewById<TextView>(R.id.btnEnterBottom).setOnClickListener { sendEnterToWeb() }

        web.loadUrl(savedInstanceState?.getString("last_url") ?: homeUrl)
    }

    private fun setupStopwatch() {
        stopwatchButton = findViewById(R.id.btnStopwatch)
        updateStopwatchButton()
        stopwatchButton.setOnClickListener {
            if (stopwatchRunning) {
                stopwatchElapsedMs = System.currentTimeMillis() - stopwatchStartMs
                stopwatchRunning = false
                stopwatchHandler.removeCallbacks(stopwatchTick)
                updateStopwatchButton()
            } else {
                stopwatchStartMs = System.currentTimeMillis() - stopwatchElapsedMs
                stopwatchRunning = true
                stopwatchHandler.removeCallbacks(stopwatchTick)
                stopwatchHandler.post(stopwatchTick)
            }
        }
        stopwatchButton.setOnLongClickListener {
            stopwatchRunning = false
            stopwatchHandler.removeCallbacks(stopwatchTick)
            stopwatchElapsedMs = 0L
            updateStopwatchButton()
            Toast.makeText(this, "کرنومتر صفر شد.", Toast.LENGTH_SHORT).show()
            true
        }
    }

    private fun updateStopwatchButton() {
        if (!::stopwatchButton.isInitialized) return
        val totalSeconds = stopwatchElapsedMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        stopwatchButton.text = String.format(
            java.util.Locale.US,
            "⏱ %02d:%02d",
            minutes, seconds
        )
    }

    private fun setupZoomLens() {
        val area = findViewById<FrameLayout>(R.id.webArea)
        val density = resources.displayMetrics.density
        val size = (52 * density).toInt()

        val lens = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(size, size, Gravity.END or Gravity.CENTER_VERTICAL).apply {
                marginEnd = (10 * density).toInt()
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.rgb(58, 110, 165))
                setStroke((2 * density).toInt(), Color.WHITE)
            }
            elevation = 10f
            contentDescription = "ذره‌بین بزرگنمایی؛ لمس برای فعال یا غیرفعال کردن و کشیدن روی صفحه"
        }

        val icon = TextView(this).apply {
            layoutParams = FrameLayout.LayoutParams(-1, -1)
            gravity = Gravity.CENTER
            text = "🔍"
            textSize = 22f
        }
        lens.addView(icon)

        val indicator = View(this).apply {
            layoutParams = FrameLayout.LayoutParams((10 * density).toInt(), (10 * density).toInt(), Gravity.CENTER)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.rgb(255, 165, 0))
            }
            visibility = View.GONE
        }
        lens.addView(indicator)
        area.addView(lens)
        zoomLensButton = lens
        zoomLensIndicator = indicator

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            zoomMagnifier = android.widget.Magnifier(web)
            zoomMagnifier?.zoom = 2.0f
        }

        lens.setOnClickListener {
            zoomLensActive = !zoomLensActive
            indicator.visibility = if (zoomLensActive) View.VISIBLE else View.GONE
            if (zoomLensActive) {
                val blink = AlphaAnimation(1f, 0.15f).apply {
                    duration = 450
                    repeatMode = android.view.animation.Animation.REVERSE
                    repeatCount = android.view.animation.Animation.INFINITE
                }
                indicator.startAnimation(blink)
                updateZoomLens()
            } else {
                indicator.clearAnimation()
                zoomMagnifier?.dismiss()
            }
        }

        lens.setOnTouchListener(object : View.OnTouchListener {
            var downX = 0f
            var downY = 0f
            var startX = 0f
            var startY = 0f
            var moved = false

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = event.rawX; downY = event.rawY
                        startX = v.x; startY = v.y; moved = false
                        return false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - downX
                        val dy = event.rawY - downY
                        if (kotlin.math.abs(dx) > 6 || kotlin.math.abs(dy) > 6) moved = true
                        if (moved) {
                            v.x = (startX + dx).coerceIn(0f, (area.width - v.width).toFloat())
                            v.y = (startY + dy).coerceIn(0f, (area.height - v.height).toFloat())
                            if (zoomLensActive) updateZoomLens()
                            return true
                        }
                    }
                    MotionEvent.ACTION_UP -> {
                        if (moved) {
                            if (zoomLensActive) updateZoomLens()
                            return true
                        }
                    }
                }
                return false
            }
        })
    }

    private fun updateZoomLens() {
        if (!zoomLensActive || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val lens = zoomLensButton ?: return
        val sourceX = (lens.x + lens.width / 2f).coerceIn(0f, web.width.toFloat())
        val sourceY = (lens.y + lens.height / 2f).coerceIn(0f, web.height.toFloat())
        zoomMagnifier?.show(sourceX, sourceY)
    }

    private fun setupGoogleButton() {
        val google = findViewById<TextView>(R.id.btnGoogle)
        google.setOnClickListener { web.loadUrl(googleUrl) }
        google.setOnLongClickListener {
            showSearchEngineDropdown(google)
            true
        }
    }

    private fun showSearchEngineDropdown(anchor: View) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(6, 6, 6, 6)
            setBackgroundColor(Color.rgb(236, 233, 216))
        }
        val popup = android.widget.PopupWindow(
            box,
            (190 * resources.displayMetrics.density).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )
        searchEngines.forEach { (name, url) ->
            val b = TextView(this).apply {
                text = name
                textSize = 14f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER_VERTICAL
                setPadding(20, 0, 20, 0)
                background = resources.getDrawable(R.drawable.xp_button, theme)
                minHeight = (42 * resources.displayMetrics.density).toInt()
                setOnClickListener {
                    web.loadUrl(url)
                    popup.dismiss()
                }
            }
            box.addView(b, LinearLayout.LayoutParams(-1, (46 * resources.displayMetrics.density).toInt()).apply {
                setMargins(2, 2, 2, 2)
            })
        }
        popup.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
        popup.isOutsideTouchable = true
        popup.elevation = 10f
        popup.showAsDropDown(anchor, 0, 4)
    }

    private fun setupToolbarGroups() {
        val links = findViewById<View>(R.id.linksPanel)
        val tools = findViewById<View>(R.id.toolsPanel)
        val media = findViewById<View>(R.id.mediaPanel)
        fun toggle(panel: View, button: TextView, label: String) {
            val open = panel.visibility != View.VISIBLE
            panel.visibility = if (open) View.VISIBLE else View.GONE
            button.text = if (open) "$label ▲" else "$label ▼"
        }
        findViewById<TextView>(R.id.btnLinksGroup).setOnClickListener { toggle(links, findViewById(R.id.btnLinksGroup), "LINKS") }
        findViewById<TextView>(R.id.btnToolsGroup).setOnClickListener { toggle(tools, findViewById(R.id.btnToolsGroup), "TOOLS") }
        findViewById<TextView>(R.id.btnMediaGroup).setOnClickListener { toggle(media, findViewById(R.id.btnMediaGroup), "MEDIA") }
    }

    private fun openWebsiteFileChooser() {
        web.evaluateJavascript("""(function(){
            var inputs = Array.from(document.querySelectorAll('input[type="file"]'));
            var visible = inputs.find(function(i){
                var r=i.getBoundingClientRect();
                var s=getComputedStyle(i);
                return s.display!=="none" && s.visibility!=="hidden" && r.width>0 && r.height>0;
            });
            if (visible) { visible.click(); return "ok"; }
            return "none";
        })()""") { result ->
            if (result != "\"ok\"") {
                Toast.makeText(this, "در این صفحه فیلد انتخاب فایل پیدا نشد.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun toggleFullScreen() {
        if (fullScreenMode) exitFullScreen() else enterFullScreen()
    }

    private fun enterFullScreen() {
        fullScreenMode = true
        setChromeBarsHidden(true)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
        findViewById<TextView>(R.id.btnExitFullscreen).visibility = View.VISIBLE
        setButtonActive(findViewById(R.id.btnFullscreen), true)
        Toast.makeText(this, "حالت تمام‌صفحه فعال شد", Toast.LENGTH_SHORT).show()
    }

    private fun exitFullScreen() {
        fullScreenMode = false
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.show(WindowInsetsCompat.Type.systemBars())
        setChromeBarsHidden(false)
        findViewById<TextView>(R.id.btnExitFullscreen).visibility = View.GONE
        setButtonActive(findViewById(R.id.btnFullscreen), false)
    }

    private fun enterFloatMode() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) {
            Toast.makeText(this, "حالت FLOAT به Android 8 یا بالاتر نیاز دارد.", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val ratio = Rational(web.width.coerceAtLeast(1), web.height.coerceAtLeast(1))
            val params = PictureInPictureParams.Builder().setAspectRatio(ratio).build()
            setButtonActive(findViewById(R.id.btnFloat), true)
            enterPictureInPictureMode(params)
        } catch (_: Exception) {
            Toast.makeText(this, "حالت FLOAT در این دستگاه در دسترس نیست.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: android.content.res.Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (isInPictureInPictureMode) {
            setChromeBarsHidden(true)
            findViewById<TextView>(R.id.btnExitFullscreen).visibility = View.GONE
        } else if (!fullScreenMode) {
            setChromeBarsHidden(false)
            setButtonActive(findViewById(R.id.btnFloat), false)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_F11) {
            toggleFullScreen()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun setupFloatingWheel() {
        // The two arrow buttons are now page-edge jumps.
        findViewById<TextView>(R.id.btnWheelUp).setOnClickListener {
            web.evaluateJavascript("window.scrollTo({top:0,left:0,behavior:'smooth'});") { }
        }
        findViewById<TextView>(R.id.btnWheelDown).setOnClickListener {
            web.evaluateJavascript("window.scrollTo({top:Math.max(document.body.scrollHeight,document.documentElement.scrollHeight),left:0,behavior:'smooth'});") { }
        }
        findViewById<TextView>(R.id.btnWheelReset).setOnClickListener {
            web.evaluateJavascript("window.scrollTo({top:0,left:0,behavior:'smooth'});") { }
        }
        findViewById<TextView>(R.id.btnHideAll).setOnClickListener { setChromeBarsHidden(true) }
        btnShowAll.setOnClickListener { setChromeBarsHidden(false); newsHidden = false; newsTicker.visibility = View.VISIBLE }
        findViewById<TextView>(R.id.btnExitFullscreen).setOnClickListener { exitFullScreen() }
    }

    private fun setupMouseControl() {
        val pointer = findViewById<MousePointerView>(R.id.mousePointer)
        val pad = findViewById<MousePadView>(R.id.mousePad)
        pad.onMoveDelta = { dx, dy ->
            pointer.moveBy(dx, dy)
        }
        pad.onPadClick = {
            val x = pointer.xPos().toInt()
            val y = pointer.yPos().toInt()
            val js = """(function(){var e=document.elementFromPoint($x,$y);if(!e)return;try{e.click();}catch(_){var r=e.getBoundingClientRect();var ev=new MouseEvent('click',{bubbles:true,cancelable:true,clientX:r.left+2,clientY:r.top+2});e.dispatchEvent(ev);}})()"""
            web.evaluateJavascript(js, null)
        }
    }

    private fun showAppSources() {
        val names = arrayOf(
            "F-Droid — نرم‌افزارهای آزاد",
            "GitHub Releases — برنامه و پلاگین",
            "Itch.io — بازی و نرم‌افزار"
        )
        val urls = arrayOf(
            "https://f-droid.org/",
            "https://github.com/releases",
            "https://itch.io/games"
        )
        AlertDialog.Builder(this)
            .setTitle("APPS / PLUGINS / GAMES")
            .setItems(names) { _, which -> web.loadUrl(urls[which]) }
            .setNeutralButton("افزودن منبع") { _, _ -> addAppSource() }
            .setNegativeButton("لغو", null)
            .show()
    }

    private fun showSubtitleSources() {
        val title = (web.title ?: "").trim()
        val seed = if (title.isNotBlank()) title else web.url?.substringAfterLast('/')?.replace("-", " ") ?: ""
        val names = subtitleSources.map { it.first }.toTypedArray() + arrayOf("جستجوی عنوان فعلی در همه منابع")
        AlertDialog.Builder(this)
            .setTitle("SUBTITLES / زیرنویس واقعی")
            .setMessage("منابع واقعی زیرنویس: OpenSubtitles، SUBDL و Addic7ed")
            .setItems(names) { _, which ->
                val q = Uri.encode(seed)
                if (which < subtitleSources.size) web.loadUrl(subtitleSources[which].second + q)
                else showSubtitleSearchDialog()
            }
            .setNegativeButton("لغو", null)
            .show()
    }

    private fun showSubtitleSearchDialog() {
        val input = EditText(this).apply {
            setText((web.title ?: "").trim())
            selectAll(); setSingleLine(true)
            hint = "نام فیلم یا سریال"
            setPadding(24, 8, 24, 8)
        }
        AlertDialog.Builder(this)
            .setTitle("جستجوی زیرنویس")
            .setView(input)
            .setPositiveButton("جستجو") { _, _ ->
                val q = Uri.encode(input.text.toString().trim())
                if (q.isNotBlank()) {
                    web.loadUrl(subtitleSources[0].second + q)
                }
            }
            .setNeutralButton("SUBDL") { _, _ ->
                val q = Uri.encode(input.text.toString().trim()); if (q.isNotBlank()) web.loadUrl(subtitleSources[1].second + q)
            }
            .setNegativeButton("لغو", null).show()
    }

    private fun addAppSource() {
        val input = EditText(this).apply {
            hint = "https://example.com"
            setSingleLine(true)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI
            setPadding(24, 8, 24, 8)
        }
        AlertDialog.Builder(this)
            .setTitle("منبع جدید")
            .setMessage("آدرس منبع برنامه، بازی یا پلاگین را وارد کنید:")
            .setView(input)
            .setPositiveButton("باز کردن") { _, _ ->
                val u = input.text.toString().trim()
                if (u.startsWith("http://") || u.startsWith("https://")) web.loadUrl(u)
                else Toast.makeText(this, "آدرس باید با http یا https شروع شود.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("لغو", null)
            .show()
    }

    private fun setupTopScrollWheel() {
        findViewById<ScrollWheelView>(R.id.topScrollWheel).onScrollDelta = { delta ->
            web.smoothScrollBy(0, delta)
        }
    }

    private fun setupDesktopMode() {
        val desktop = findViewById<TextView>(R.id.btnDesktop)
        desktop.setOnClickListener {
            desktopModeActive = !desktopModeActive
            web.settings.userAgentString = if (desktopModeActive) DESKTOP_USER_AGENT else WebSettings.getDefaultUserAgent(this)
            web.settings.useWideViewPort = true
            web.settings.loadWithOverviewMode = !desktopModeActive
            desktop.text = if (desktopModeActive) "MOBILE" else "DESKTOP"
            setButtonActive(desktop, desktopModeActive)
            web.reload()
        }
    }

    private fun setChromeBarsHidden(hidden: Boolean) {
        chromeBarsHidden = hidden
        val topTitle = findViewById<View>(R.id.titleBar)
        val topNav = topTitle.nextSiblingView()
        val tab = findViewById<View>(R.id.tabScroller)
        val addressRow = tab.nextSiblingView()
        topTitle.visibility = if (hidden) View.GONE else View.VISIBLE
        topNav?.visibility = if (hidden) View.GONE else View.VISIBLE
        tab.visibility = if (hidden) View.GONE else View.VISIBLE
        addressRow?.visibility = if (hidden) View.GONE else View.VISIBLE
        findViewById<View>(R.id.btnNewTab).parent?.parent?.let { (it as? View)?.visibility = if (hidden) View.GONE else View.VISIBLE }
        btnShowAll.visibility = if (hidden) View.VISIBLE else View.GONE
    }

    private fun View.nextSiblingView(): View? {
        val p = parent as? android.view.ViewGroup ?: return null
        val i = p.indexOfChild(this)
        return if (i >= 0 && i + 1 < p.childCount) p.getChildAt(i + 1) else null
    }

    private fun setupNewsTicker() {
        btnNewsSource.setOnClickListener {
            newsSourceIndex = (newsSourceIndex + 1) % newsSources.size
            loadNews()
        }
        btnNewsTranslate.setOnClickListener { translateCurrentNews() }
        btnHideNews.setOnClickListener {
            newsHidden = true
            newsTicker.visibility = View.GONE
        }
        loadNews()
    }

    private fun loadNews() {
        btnNewsSource.text = if (newsSourceIndex == 0) "DW AR" else "DW EN"
        val source = newsSources[newsSourceIndex]
        Thread {
            try {
                val conn = URL(source.second).openConnection() as HttpURLConnection
                conn.connectTimeout = 8000
                conn.readTimeout = 10000
                conn.setRequestProperty("User-Agent", "Fast-Browser-XP/1.0")
                val input = BufferedInputStream(conn.inputStream)
                val parser = Xml.newPullParser()
                parser.setInput(input, "UTF-8")
                val titles = mutableListOf<String>()
                var event = parser.eventType
                var insideTitle = false
                while (event != org.xmlpull.v1.XmlPullParser.END_DOCUMENT && titles.size < 12) {
                    if (event == org.xmlpull.v1.XmlPullParser.START_TAG && parser.name.equals("title", true)) insideTitle = true
                    else if (event == org.xmlpull.v1.XmlPullParser.TEXT && insideTitle) {
                        val t = parser.text.trim()
                        if (t.isNotBlank() && !titles.contains(t)) titles.add(t)
                        insideTitle = false
                    } else if (event == org.xmlpull.v1.XmlPullParser.END_TAG && parser.name.equals("title", true)) insideTitle = false
                    event = parser.next()
                }
                input.close(); conn.disconnect()
                val result = titles.filter { !it.equals("DW", true) && !it.equals("World News", true) }.take(8)
                mainHandler.post {
                    currentNewsText = if (result.isEmpty()) "NEWS: خبری دریافت نشد" else result.joinToString("     •     ")
                    newsText.text = currentNewsText
                    newsText.isSelected = true
                    newsText.scrollTo(0, 0)
                }
            } catch (_: Exception) {
                mainHandler.post { newsText.text = "NEWS: اتصال خبر در دسترس نیست" }
            }
        }.start()
    }

    private fun translateCurrentNews() {
        val text = currentNewsText.trim()
        if (text.isBlank()) return
        btnNewsTranslate.text = "…"
        Thread {
            try {
                val q = Uri.encode(text.take(1200))
                val u = URL("https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=fa&dt=t&q=$q")
                val conn = u.openConnection() as HttpURLConnection
                conn.connectTimeout = 8000
                conn.readTimeout = 10000
                conn.setRequestProperty("User-Agent", "Fast-Browser-XP/1.0")
                val body = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val root = JSONArray(body)
                val chunks = root.optJSONArray(0)
                val translated = buildString {
                    if (chunks != null) {
                        for (i in 0 until chunks.length()) {
                            val part = chunks.optJSONArray(i)
                            val t = part?.optString(0).orEmpty()
                            if (t.isNotBlank()) append(t)
                        }
                    }
                }
                mainHandler.post {
                    if (translated.isNotBlank()) {
                        currentNewsText = translated
                        newsText.text = translated
                        newsText.isSelected = true
                    } else Toast.makeText(this, "ترجمه دریافت نشد.", Toast.LENGTH_SHORT).show()
                    btnNewsTranslate.text = "→FA"
                }
                conn.disconnect()
            } catch (_: Exception) {
                mainHandler.post {
                    btnNewsTranslate.text = "→FA"
                    Toast.makeText(this, "ترجمه فعلاً در دسترس نیست.", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun saveHistory(url: String) {
        if (url.isBlank() || url == "about:blank") return
        val old = historyPrefs.getStringSet("urls", linkedSetOf())?.toMutableList() ?: mutableListOf()
        old.remove(url)
        old.add(0, url)
        val trimmed = old.take(250).toSet()
        historyPrefs.edit().putStringSet("urls", trimmed).apply()
    }

    private fun createNewTab() {
        // No artificial tab-count limit; Android memory remains the practical limit.
        if (tabs.isNotEmpty()) tabs[currentTab].url = web.url ?: tabs[currentTab].url
        tabs.add(BrowserTab("about:blank", "New Tab"))
        currentTab = tabs.lastIndex
        address.setText("")
        selectMode = false
        control.text = "CONTROL"
        setIndicator(false)
        refreshTabBar()
        web.loadUrl("about:blank")
    }

    private fun switchTab(index: Int) {
        if (index !in tabs.indices || index == currentTab) return
        if (tabs.isNotEmpty()) tabs[currentTab].url = web.url ?: tabs[currentTab].url
        currentTab = index
        refreshTabBar()
        val url = tabs[currentTab].url
        address.setText(if (url == "about:blank") "" else url)
        web.loadUrl(url)
    }

    private fun refreshTabBar() {
        if (!::tabBar.isInitialized) return
        tabBar.removeAllViews()
        tabs.forEachIndexed { index, tab ->
            val holder = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setBackgroundResource(if (index == currentTab) R.drawable.xp_button else R.drawable.xp_bar)
            }
            val title = TextView(this).apply {
                text = "${index + 1}: ${tab.title.take(14)}"
                gravity = android.view.Gravity.CENTER
                setTextColor(Color.WHITE)
                textSize = 10f
                setPadding(10, 0, 5, 0)
                setOnClickListener { switchTab(index) }
            }
            val close = TextView(this).apply {
                text = "×"
                gravity = android.view.Gravity.CENTER
                setTextColor(Color.WHITE)
                textSize = 18f
                setPadding(5, 0, 8, 0)
                setOnClickListener { closeTab(index) }
            }
            holder.addView(title, LinearLayout.LayoutParams(0, 34, 1f))
            holder.addView(close, LinearLayout.LayoutParams(34, 34))
            tabBar.addView(holder, LinearLayout.LayoutParams(154, 34).apply { setMargins(3, 3, 3, 3) })
        }
    }

    private fun closeAllTabs() {
        tabs.clear()
        tabs.add(BrowserTab("about:blank", "New Tab"))
        currentTab = 0
        address.setText("")
        selectMode = false
        control.text = "CONTROL"
        setIndicator(false)
        web.loadUrl("about:blank")
        refreshTabBar()
    }

    private fun closeTab(index: Int) {
        if (index !in tabs.indices) return
        tabs.removeAt(index)
        if (tabs.isEmpty()) {
            tabs.add(BrowserTab("about:blank", "New Tab"))
            currentTab = 0
            web.loadUrl("about:blank")
        } else {
            currentTab = currentTab.coerceIn(0, tabs.lastIndex)
            val url = tabs[currentTab].url
            address.setText(if (url == "about:blank") "" else url)
            web.loadUrl(url)
        }
        refreshTabBar()
    }

    private fun showHistory() {
        val items = historyPrefs.getStringSet("urls", emptySet())?.toList() ?: emptyList()
        if (items.isEmpty()) {
            AlertDialog.Builder(this).setTitle("History / سابقه")
                .setMessage("هنوز سابقه‌ای ذخیره نشده است.")
                .setPositiveButton("OK", null).show()
            return
        }
        val ordered = items
        AlertDialog.Builder(this).setTitle("History / سابقه")
            .setItems(ordered.toTypedArray()) { _, which -> web.loadUrl(ordered[which]) }
            .setNeutralButton("پاک کردن") { _, _ ->
                historyPrefs.edit().remove("urls").apply()
                Toast.makeText(this, "سابقه پاک شد.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("لغو", null).show()
    }

    private fun startDownload(url: String, userAgent: String, contentDisposition: String?, mimetype: String?) {
        showDownloadChoiceDialog(url, userAgent, contentDisposition, mimetype, web.url)
    }

    private val socialSources = linkedMapOf(
        "بله" to "https://web.bale.ai/",
        "روبیکا" to "https://rubika.ir/",
        "ایتا" to "https://eitaa.com/",
        "سروش پلاس" to "https://splus.ir/",
        "گپ" to "https://gap.im/",
        "Telegram" to "https://web.telegram.org/",
        "WhatsApp" to "https://web.whatsapp.com/",
        "Instagram" to "https://www.instagram.com/",
        "Facebook" to "https://www.facebook.com/",
        "X" to "https://x.com/",
        "YouTube" to "https://www.youtube.com/",
        "LinkedIn" to "https://www.linkedin.com/"
    )

    private fun showSocialSources() {
        val names = socialSources.keys.toTypedArray()
        AlertDialog.Builder(this).setTitle("پیام‌رسان‌ها و شبکه‌های اجتماعی")
            .setItems(names) { _, which -> web.loadUrl(socialSources[names[which]]!!) }
            .setPositiveButton("＋ افزودن") { _, _ -> addSocialSource() }
            .setNegativeButton("بستن", null).show()
    }

    private fun addSocialSource() {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 8, 24, 4) }
        val name = EditText(this).apply { hint = "نام سرویس"; setSingleLine(true) }
        val url = EditText(this).apply { hint = "https://..."; setSingleLine(true); inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI }
        box.addView(name); box.addView(url)
        AlertDialog.Builder(this).setTitle("افزودن شبکه") .setView(box)
            .setPositiveButton("ذخیره") { _, _ ->
                val n=name.text.toString().trim(); val u=url.text.toString().trim()
                if (n.isNotBlank() && isHttpUrl(u)) { socialSources[n]=u; Toast.makeText(this,"شبکه اضافه شد.",Toast.LENGTH_SHORT).show() }
            }.setNegativeButton("لغو",null).show()
    }

    private fun rememberMediaUrl(url: String?) {
        if (url.isNullOrBlank() || !isHttpUrl(url)) return
        val l=url.lowercase()
        if (listOf(".m3u8", ".mpd", ".mp4", ".webm", ".m4v", ".mov", ".ts", ".m4s", ".aac", ".mp3").any { l.contains(it) }) {
            detectedMediaUrls.add(url)
            if (detectedMediaUrls.size > 80) detectedMediaUrls.remove(detectedMediaUrls.first())
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun scanPageMedia(view: WebView) {
        view.evaluateJavascript("""(function(){
            var a=[];
            document.querySelectorAll('video,audio,source').forEach(function(e){ if(e.src) a.push(e.src); if(e.currentSrc) a.push(e.currentSrc); });
            try { performance.getEntriesByType('resource').forEach(function(e){ if(e.name) a.push(e.name); }); } catch(x){}
            return JSON.stringify(a.slice(-120));
        })()""") { raw ->
            try {
                val arr = JSONArray(JSONTokener(raw.removeSurrounding("\"", "\"").replace("\\\"", "\"")))
                for (i in 0 until arr.length()) rememberMediaUrl(arr.optString(i))
            } catch (_: Exception) { }
        }
    }

    private fun detectAndOfferVideoDownloads() {
        scanPageMedia(web)
        mainHandler.postDelayed({
            val candidates = detectedMediaUrls.filter { isHttpUrl(it) }.distinct().takeLast(20)
            if (candidates.isEmpty()) {
                Toast.makeText(this, "پخش رسانه‌ای قابل شناسایی پیدا نشد.", Toast.LENGTH_SHORT).show()
                return@postDelayed
            }
            val labels = candidates.map { URLUtil.guessFileName(it, null, null) + "\n" + it.take(100) }.toTypedArray()
            AlertDialog.Builder(this).setTitle("VIDEO DL — لینک‌های شناسایی‌شده")
                .setItems(labels) { _, which ->
                    val u=candidates[which]
                    showDownloadChoiceDialog(u, web.settings.userAgentString, null, guessMimeForName(URLUtil.guessFileName(u,null,null)), web.url)
                }.setMessage("فقط رسانه‌هایی که مرورگر از صفحه به‌صورت قابل‌دسترسی شناسایی کرده نشان داده می‌شوند. محتوای DRM یا پخش محافظت‌شده دور زده نمی‌شود.")
                .setNegativeButton("بستن",null).show()
        }, 350)
    }

    private fun showDownloadChoiceDialog(url: String, userAgent: String?, contentDisposition: String?, mimetype: String?, referer: String?) {
        if (!isHttpUrl(url)) return
        val guessed = URLUtil.guessFileName(url, contentDisposition, mimetype)
        val input = EditText(this).apply {
            setText(guessed); selectAll(); setSingleLine(true); hint = "نام فایل"
            setPadding(24, 8, 24, 8)
        }
        var choice = if (downloader2Active) 1 else 0
        val modes = arrayOf("Downloader 1 — سیستم Android", "Downloader 2 — Fast Browser XP")
        AlertDialog.Builder(this)
            .setTitle("دانلود فایل")
            .setMessage("نام فایل و موتور دانلود را انتخاب کن:")
            .setView(input)
            .setSingleChoiceItems(modes, choice) { _, which -> choice = which }
            .setPositiveButton("دانلود") { _, _ ->
                val name = safeFileName(input.text.toString(), guessed)
                downloader2Active = choice == 1
                if (choice == 1) {
                    setButtonActive(findViewById(R.id.btnDownloads), true)
                    Download2.start(this, url, name, userAgent, referer)
                    Toast.makeText(this, "Downloader 2 شروع شد: $name", Toast.LENGTH_SHORT).show()
                } else {
                    enqueueDownload(url, name, userAgent, mimetype, referer)
                }
            }
            .setNegativeButton("لغو", null)
            .show()
    }

    private fun enqueueDownload(
        url: String,
        fileName: String,
        userAgent: String?,
        mimetype: String?,
        referer: String? = web.url
    ) {
        try {
            val cleanName = safeFileName(fileName, "download")
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setTitle(cleanName)
                setDescription("Fast Browser XP")
                setMimeType(mimetype ?: "application/octet-stream")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
                userAgent?.let { addRequestHeader("User-Agent", it) }
                CookieManager.getInstance().getCookie(url)?.let { addRequestHeader("Cookie", it) }
                referer?.let { addRequestHeader("Referer", it) }
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, cleanName)
            }
            (getSystemService(DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
            Toast.makeText(this, "دانلود شروع شد: $cleanName", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            Toast.makeText(this, "دانلود شروع نشد.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDownloads() {
        val manager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        val cursor = manager.query(DownloadManager.Query().setFilterByStatus(
            DownloadManager.STATUS_PENDING or DownloadManager.STATUS_RUNNING or
                DownloadManager.STATUS_PAUSED or DownloadManager.STATUS_SUCCESSFUL or
                DownloadManager.STATUS_FAILED
        ))
        val rows = mutableListOf<Pair<String, Long>>()
        cursor.use { c ->
            val idCol = c.getColumnIndex(DownloadManager.COLUMN_ID)
            val titleCol = c.getColumnIndex(DownloadManager.COLUMN_TITLE)
            val statusCol = c.getColumnIndex(DownloadManager.COLUMN_STATUS)
            val sizeCol = c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
            val doneCol = c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
            while (c.moveToNext() && rows.size < 80) {
                val id = c.getLong(idCol)
                val title = c.getString(titleCol) ?: "Download"
                val status = c.getInt(statusCol)
                val total = if (sizeCol >= 0) c.getLong(sizeCol) else -1L
                val done = if (doneCol >= 0) c.getLong(doneCol) else 0L
                val progress = if (total > 0) " ${(done * 100 / total).coerceIn(0, 100)}%" else ""
                rows.add("${downloadStatus(status)}$progress  $title" to id)
            }
        }

        val labels = if (rows.isEmpty()) arrayOf("هنوز دانلودی ثبت نشده است.") else rows.map { it.first }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Downloads / دانلودها")
            .setItems(labels) { _, which ->
                if (rows.isNotEmpty()) {
                    val id = rows[which].second
                    try {
                        startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS))
                    } catch (_: Exception) {
                        Toast.makeText(this, "نمایش دانلودهای دستگاه در دسترس نیست.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNeutralButton("دانلودهای گوشی") { _, _ ->
                try { startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)) }
                catch (_: Exception) { Toast.makeText(this, "برنامه دانلودهای دستگاه پیدا نشد.", Toast.LENGTH_SHORT).show() }
            }
            .setNegativeButton("بستن", null)
            .show()
    }

    private fun downloadStatus(status: Int): String = when (status) {
        DownloadManager.STATUS_PENDING -> "⏳"
        DownloadManager.STATUS_RUNNING -> "⬇"
        DownloadManager.STATUS_PAUSED -> "⏸"
        DownloadManager.STATUS_SUCCESSFUL -> "✓"
        DownloadManager.STATUS_FAILED -> "✕"
        else -> "•"
    }

    private fun isHttpUrl(url: String): Boolean = url.startsWith("http://") || url.startsWith("https://")

    private fun safeFileName(value: String, fallback: String): String {
        val cleaned = value.trim().replace(Regex("[\\/:*?\"<>|]"), "_")
        return cleaned.take(180).ifBlank { fallback }
    }

    private fun guessMimeForName(name: String): String? {
        val lower = name.lowercase()
        return when {
            lower.endsWith(".jpg") || lower.endsWith(".jpeg") -> "image/jpeg"
            lower.endsWith(".png") -> "image/png"
            lower.endsWith(".webp") -> "image/webp"
            lower.endsWith(".gif") -> "image/gif"
            lower.endsWith(".svg") -> "image/svg+xml"
            else -> null
        }
    }

    private val extraTouchPx = 18

    override fun dispatchTouchEvent(event: android.view.MotionEvent): Boolean {
        if (event.action == android.view.MotionEvent.ACTION_UP) {
            val buttonIds = intArrayOf(
                R.id.btnControl, R.id.btnControl2, R.id.btnBack, R.id.btnForward, R.id.btnReload, R.id.btnGoogle, R.id.btnEnterTop,
                R.id.btnHome, R.id.btnChatGPT, R.id.btnToken, R.id.btnRepo, R.id.btnHistory, R.id.btnBookmark,
                R.id.btnGo, R.id.btnNewTab, R.id.btnCloseAllTabs, R.id.btnEnterBottom, R.id.btnSelect, R.id.btnWheel, R.id.btnScreenshot,
                R.id.btnDownloads, R.id.btnSubs, R.id.btnSavePage, R.id.btnWheelUp, R.id.btnWheelReset, R.id.btnWheelDown,
                R.id.btnHideAll, R.id.btnShowAll, R.id.btnSplit, R.id.btnRadioPrev, R.id.btnRadioPlay, R.id.btnRadioNext, R.id.btnRadioList, R.id.btnRadioKbps, R.id.btnRadioServer, R.id.btnUpload, R.id.btnNewsSource, R.id.btnNewsTranslate, R.id.btnHideNews
            )
            val x = event.rawX.toInt()
            val y = event.rawY.toInt()
            for (id in buttonIds) {
                val v = findViewById<View>(id) ?: continue
                if (v.visibility != View.VISIBLE || !v.isEnabled) continue
                val r = android.graphics.Rect()
                v.getGlobalVisibleRect(r)
                r.inset(-extraTouchPx, -extraTouchPx)
                if (r.contains(x, y)) {
                    v.performClick()
                    return true
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILE_CHOOSER_REQUEST) {
            val results = if (resultCode == RESULT_OK && data != null) WebChromeClient.FileChooserParams.parseResult(resultCode, data) else null
            fileCallback?.onReceiveValue(results)
            fileCallback = null
        }
    }

    private fun sendEnterToWeb() {
        // ENTER first behaves like GO when the address bar is focused. Otherwise it
        // acts on the web control the user is currently working with: focused buttons,
        // menu items, links and form fields receive an Enter-equivalent action.
        if (address.hasFocus()) {
            openAddress()
            return
        }
        web.evaluateJavascript("""(function(){
          var el=document.activeElement;
          if(!el || el===document.body || el===document.documentElement){
            var c=document.querySelector('[aria-expanded=\"true\"], [role=\"menuitem\"]:focus, button:focus, [role=\"button\"]:focus, a:focus');
            el=c||el;
          }
          if(!el) return 'none';
          var tag=(el.tagName||'').toLowerCase();
          var role=(el.getAttribute('role')||'').toLowerCase();
          if(tag==='button' || tag==='a' || tag==='select' || role==='button' || role==='menuitem' || role==='option' || el.getAttribute('aria-haspopup')==='true'){
            el.click(); return 'click';
          }
          var ev=new KeyboardEvent('keydown',{key:'Enter',code:'Enter',keyCode:13,which:13,bubbles:true,cancelable:true});
          el.dispatchEvent(ev);
          var ev2=new KeyboardEvent('keyup',{key:'Enter',code:'Enter',keyCode:13,which:13,bubbles:true,cancelable:true});
          el.dispatchEvent(ev2);
          return 'key';
        })()""".trimIndent()) { result ->
            if (result == "\"none\"" || result == "null") {
                Toast.makeText(this, "عنصر فعالی برای ENTER پیدا نشد.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openAddress(): Boolean {
        var u = address.text.toString().trim()
        if (u.isEmpty()) return true
        if (!u.startsWith("http://") && !u.startsWith("https://")) {
            u = if (u.contains(".") && !u.contains(" ")) "https://$u"
            else "https://www.google.com/search?q=" + Uri.encode(u)
        }
        web.loadUrl(u)
        return true
    }

    private fun setButtonActive(button: View, active: Boolean) {
        val key = button.id
        activeLightAnimations.remove(key)?.cancel()
        if (!active) {
            button.background = androidx.core.content.ContextCompat.getDrawable(this, R.drawable.xp_button)
            return
        }
        val base = androidx.core.content.ContextCompat.getDrawable(this, R.drawable.xp_button)!!.mutate()
        val light = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.rgb(255, 165, 0))
            cornerRadius = 1f
        }
        val layers = LayerDrawable(arrayOf(base, light))
        val size = (7 * resources.displayMetrics.density).toInt()
        val inset = (3 * resources.displayMetrics.density).toInt()
        layers.setLayerGravity(1, Gravity.TOP or Gravity.END)
        layers.setLayerSize(1, size, size)
        layers.setLayerInset(1, 0, inset, inset, 0)
        button.background = layers
        val anim = android.animation.ValueAnimator.ofInt(255, 40).apply {
            duration = 500
            repeatMode = android.animation.ValueAnimator.REVERSE
            repeatCount = android.animation.ValueAnimator.INFINITE
            addUpdateListener { light.alpha = it.animatedValue as Int; button.invalidate() }
        }
        activeLightAnimations[key] = anim
        anim.start()
    }

    private fun setIndicator(active: Boolean) {
        if (!active) {
            selectIndicator.clearAnimation()
            selectIndicator.visibility = View.GONE
            return
        }
        selectIndicator.visibility = View.VISIBLE
        val blink = AlphaAnimation(1.0f, 0.15f).apply {
            duration = 550
            repeatMode = android.view.animation.Animation.REVERSE
            repeatCount = android.view.animation.Animation.INFINITE
        }
        selectIndicator.startAnimation(blink)
    }

    private fun toggleSelectMode() {
        selectMode = !selectMode
        control.text = if (selectMode) "SELECT" else "CONTROL"
        setIndicator(selectMode)
        setButtonActive(control, selectMode)
        setButtonActive(findViewById(R.id.btnSelect), selectMode)
        if (selectMode) installSelectMode() else removeSelectMode()
    }

    private fun toggleControl2Mode() {
        control2Mode = !control2Mode
        control2StartX = -1f
        control2StartY = -1f
        setControl2Indicator(control2Mode)
        setButtonActive(findViewById(R.id.btnControl2), control2Mode)
        Toast.makeText(
            this,
            if (control2Mode) "CONTROL 2: نقطه اول و سپس نقطه دوم را در همان صفحه بزنید." else "CONTROL 2 خاموش شد.",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun setControl2Indicator(active: Boolean) {
        if (!active) {
            control2Indicator.clearAnimation()
            control2Indicator.visibility = View.GONE
            return
        }
        control2Indicator.visibility = View.VISIBLE
        val blink = AlphaAnimation(1.0f, 0.15f).apply {
            duration = 550
            repeatMode = android.view.animation.Animation.REVERSE
            repeatCount = android.view.animation.Animation.INFINITE
        }
        control2Indicator.startAnimation(blink)
    }

    private fun copySelectedRegion(x1: Float, y1: Float, x2: Float, y2: Float) {
        if (web.width <= 0 || web.height <= 0) return
        val left = minOf(x1, x2).toInt().coerceIn(0, web.width - 1)
        val top = minOf(y1, y2).toInt().coerceIn(0, web.height - 1)
        val right = maxOf(x1, x2).toInt().coerceIn(left + 1, web.width)
        val bottom = maxOf(y1, y2).toInt().coerceIn(top + 1, web.height)

        val full = Bitmap.createBitmap(web.width, web.height, Bitmap.Config.ARGB_8888)
        web.draw(Canvas(full))
        val crop = Bitmap.createBitmap(full, left, top, right - left, bottom - top)
        full.recycle()

        val file = File(cacheDir, "fbxp_selection_${System.currentTimeMillis()}.png")
        try {
            FileOutputStream(file).use { crop.compress(Bitmap.CompressFormat.PNG, 100, it) }
            crop.recycle()
            val uri = FileProvider.getUriForFile(this, "${applicationContext.packageName}.fileprovider", file)
            copyRegionTextAndHtml(left, top, right, bottom, uri)
        } catch (_: Exception) {
            crop.recycle()
            Toast.makeText(this, "کپی ناحیه انجام نشد.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun copyRegionTextAndHtml(left: Int, top: Int, right: Int, bottom: Int, imageUri: Uri) {
        web.evaluateJavascript(
            """(function(){
              const sx=${left}, sy=${top}, ex=${right}, ey=${bottom};
              const vw=Math.max(1, document.documentElement.clientWidth);
              const vh=Math.max(1, document.documentElement.clientHeight);
              const scaleX=vw/${web.width.toDouble()};
              const scaleY=vh/${web.height.toDouble()};
              const l=sx*scaleX, t=sy*scaleY, r=ex*scaleX, b=ey*scaleY;
              const els=[...document.querySelectorAll('body *')];
              const picked=els.filter(el=>{
                const z=el.getBoundingClientRect();
                return z.width>0 && z.height>0 && z.right>l && z.left<r && z.bottom>t && z.top<b;
              });
              const texts=[]; const htmls=[];
              picked.forEach(el=>{
                const tx=(el.innerText||el.textContent||'').trim();
                if(tx && tx.length<20000 && !texts.includes(tx)) texts.push(tx);
                if(el.children.length===0){ const h=el.outerHTML||''; if(h && h.length<30000) htmls.push(h); }
              });
              return JSON.stringify({text:texts.join('\n'),html:htmls.join('\n')});
            })()""".trimIndent()
        ) { raw ->
            try {
                val decoded = JSONTokener(raw).nextValue() as? String ?: ""
                val obj = JSONObject(decoded)
                val text = obj.optString("text", "")
                val html = obj.optString("html", text)
                val clip = ClipData.newHtmlText("Fast Browser XP", text, html)
                clip.addItem(ClipData.Item(imageUri))
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(clip)
                grantUriPermission("com.android.systemui", imageUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                Toast.makeText(this, "ناحیه با متن و تصویر کپی شد.", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Fast Browser XP", "ناحیه انتخاب شد؛ تصویر در کلیپ‌بورد قرار گرفت."))
                Toast.makeText(this, "تصویر ناحیه کپی شد.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupRadioPlayer() {
        findViewById<TextView>(R.id.btnRadioPrev).setOnClickListener { changeRadio(-1) }
        findViewById<TextView>(R.id.btnRadioPlay).setOnClickListener { toggleRadio() }
        findViewById<TextView>(R.id.btnRadioNext).setOnClickListener { changeRadio(1) }
        findViewById<TextView>(R.id.btnRadioList).setOnClickListener { showRadioList() }
        findViewById<TextView>(R.id.btnRadioKbps).setOnClickListener { showRadioKbpsWheel() }
        findViewById<TextView>(R.id.btnRadioServer).setOnClickListener { showTranscoderSettings() }
        updateRadioLabel()
    }

    private fun updateRadioLabel() {
        val list = if (radioSecondList) radioSecondStations else radioStations
        val name = list.getOrNull(radioIndex)?.first ?: "Radio"
        findViewById<TextView>(R.id.radioName).text = "$name • ${radioKbps} kbps"
    }

    private fun radioSourceUrl(): String? {
        val list = if (radioSecondList) radioSecondStations else radioStations
        return list.getOrNull(radioIndex)?.second
    }

    private fun resolvedRadioUrl(raw: String): String {
        if (transcoderTemplate.isBlank()) return raw
        return transcoderTemplate.replace("{URL}", Uri.encode(raw)).replace("{KBPS}", radioKbps.toString())
    }

    private fun toggleRadio() {
        if (radioPlaying) {
            radioPlayer?.pause()
            radioPlaying = false
            findViewById<TextView>(R.id.btnRadioPlay).text = "▶"
            setButtonActive(findViewById(R.id.btnRadioPlay), false)
            return
        }
        playRadio()
    }

    private fun playRadio() {
        val raw = radioSourceUrl() ?: return
        radioPlayer?.release()
        radioPlayer = MediaPlayer().apply {
            setAudioAttributes(AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).setUsage(AudioAttributes.USAGE_MEDIA).build())
            setDataSource(resolvedRadioUrl(raw))
            setOnPreparedListener {
                start()
                radioPlaying = true
                findViewById<TextView>(R.id.btnRadioPlay).text = "⏸"
                setButtonActive(findViewById(R.id.btnRadioPlay), true)
                updateRadioLabel()
            }
            setOnCompletionListener { radioPlaying = false }
            setOnErrorListener { _, _, _ ->
                radioPlaying = false
                findViewById<TextView>(R.id.btnRadioPlay).text = "▶"
                Toast.makeText(this@MainActivity, "پخش این ایستگاه برقرار نشد.", Toast.LENGTH_SHORT).show()
                true
            }
            try { prepareAsync() } catch (_: Exception) {
                Toast.makeText(this@MainActivity, "آدرس استریم قابل پخش نیست.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun changeRadio(delta: Int) {
        val list = if (radioSecondList) radioSecondStations else radioStations
        if (list.isEmpty()) return
        radioIndex = (radioIndex + delta + list.size) % list.size
        radioPlayer?.release()
        radioPlayer = null
        radioPlaying = false
        findViewById<TextView>(R.id.btnRadioPlay).text = "▶"
        updateRadioLabel()
        playRadio()
    }

    private fun showRadioList() {
        val items = (if (radioSecondList) radioSecondStations else radioStations).map { it.first }.toMutableList()
        items.add("＋ لیست دوم / منابع آنلاین")
        AlertDialog.Builder(this).setTitle("Radio / رادیو")
            .setItems(items.toTypedArray()) { _, which ->
                if (which == items.lastIndex) {
                    radioSecondList = !radioSecondList
                    radioIndex = 0
                    updateRadioLabel()
                    Toast.makeText(this, if (radioSecondList) "لیست دوم فعال شد." else "لیست اصلی فعال شد.", Toast.LENGTH_SHORT).show()
                } else {
                    radioIndex = which
                    updateRadioLabel()
                    playRadio()
                }
            }.setNegativeButton("بستن", null).show()
    }

    private fun showRadioKbpsWheel() {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 12, 24, 8) }
        val label = TextView(this).apply { text = "Bitrate: $radioKbps kbps"; textSize = 18f }
        val seek = SeekBar(this).apply { min = 1; max = 100; progress = radioKbps }
        box.addView(label); box.addView(seek)
        seek.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) { radioKbps=p.coerceIn(1,100); label.text="Bitrate: $radioKbps kbps"; updateRadioLabel() }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })
        AlertDialog.Builder(this).setTitle("RADIO KBPS WHEEL").setMessage("مقدار هدف 1 تا 100 kbps است. برای تبدیل واقعی باید URL یک سرور Transcoder وارد شود.")
            .setView(box).setPositiveButton("OK", null).show()
    }

    private fun showTranscoderSettings() {
        val input=EditText(this).apply {
            setSingleLine(true); inputType=android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI
            hint="https://server/stream?url={URL}&kbps={KBPS}"
            setText(transcoderTemplate)
        }
        AlertDialog.Builder(this).setTitle("Low-Kbps Transcoder")
            .setMessage("این برنامه خودش کیفیت استریم اینترنتی را کم نمی‌کند؛ سرور باید تبدیل را انجام دهد. {URL} و {KBPS} را پشتیبانی کنید.")
            .setView(input)
            .setPositiveButton("ذخیره") { _, _ -> transcoderTemplate=input.text.toString().trim(); Toast.makeText(this, if (transcoderTemplate.isBlank()) "تبدیل سروری خاموش شد." else "سرور Transcoder ذخیره شد.", Toast.LENGTH_SHORT).show() }
            .setNegativeButton("لغو", null).show()
    }

    private fun setupSplitScreen() {
        findViewById<TextView>(R.id.btnSplit).setOnClickListener { toggleSplitScreen() }
    }

    private fun makeSplitWebView(): WebView = WebView(this).apply {
        layoutParams=LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.MATCH_PARENT,1f)
        settings.javaScriptEnabled=true
        settings.domStorageEnabled=true
        settings.databaseEnabled=true
        settings.useWideViewPort=true
        settings.loadWithOverviewMode=true
        settings.setSupportZoom(true)
        settings.builtInZoomControls=true
        settings.displayZoomControls=false
        settings.loadsImagesAutomatically=true
        settings.blockNetworkImage=false
        settings.javaScriptCanOpenWindowsAutomatically=true
        settings.setSupportMultipleWindows(false)
        settings.mixedContentMode=WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        webViewClient=WebViewClient()
        setBackgroundColor(Color.WHITE)
    }

    private fun toggleSplitScreen() {
        if (splitMode) {
            splitContainer.removeAllViews()
            (splitContainer.parent as? ViewGroup)?.let { it.removeView(splitContainer) }
            splitWeb?.destroy()
            splitWeb=null
            splitMode=false
            web.visibility=View.VISIBLE
            findViewById<TextView>(R.id.btnSplit).text="SPLIT"
            setButtonActive(findViewById(R.id.btnSplit),false)
            return
        }
        splitContainer=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; layoutParams=FrameLayout.LayoutParams(-1,-1) }
        val left=FrameLayout(this)
        val right=FrameLayout(this)
        left.layoutParams=LinearLayout.LayoutParams(0,-1,1f)
        right.layoutParams=LinearLayout.LayoutParams(0,-1,1f)
        val closeLeft=TextView(this).apply { text="×"; textSize=22f; gravity=Gravity.CENTER; setTextColor(Color.WHITE); background=getDrawable(R.drawable.xp_button); layoutParams=FrameLayout.LayoutParams(42,42,Gravity.TOP or Gravity.END).apply { setMargins(0,8,8,0) }; setOnClickListener { toggleSplitScreen() } }
        val closeRight=TextView(this).apply { text="×"; textSize=22f; gravity=Gravity.CENTER; setTextColor(Color.WHITE); background=getDrawable(R.drawable.xp_button); layoutParams=FrameLayout.LayoutParams(42,42,Gravity.TOP or Gravity.END).apply { setMargins(0,8,8,0) }; setOnClickListener { toggleSplitScreen() } }
        val second=makeSplitWebView()
        splitWeb=second
        (web.parent as? ViewGroup)?.removeView(web)
        left.addView(web); left.addView(closeLeft)
        right.addView(second); right.addView(closeRight)
        splitContainer.addView(left)
        val divider=View(this).apply { setBackgroundColor(Color.rgb(120,120,120)); layoutParams=LinearLayout.LayoutParams(3,-1) }
        splitContainer.addView(divider); splitContainer.addView(right)
        webArea.addView(splitContainer,0)
        web.visibility=View.VISIBLE
        second.loadUrl("https://www.google.com/")
        splitMode=true
        findViewById<TextView>(R.id.btnSplit).text="CLOSE SPLIT"
        setButtonActive(findViewById(R.id.btnSplit),true)
    }

    private fun showWheel() {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 8, 24, 8) }
        val label = TextView(this).apply { text = "اندازه متن صفحه: $textScale%"; textSize = 17f; setTextColor(Color.BLACK) }
        val seek = SeekBar(this).apply { min = 50; max = 250; progress = textScale }
        box.addView(label)
        box.addView(seek)
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) {
                textScale = p.coerceIn(50, 250)
                label.text = "اندازه متن صفحه: $textScale%"
                web.settings.textZoom = textScale
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })
        AlertDialog.Builder(this).setTitle("WHEEL").setView(box).setPositiveButton("OK", null).show()
    }

    private fun showBookmarks() {
        val items = arrayOf("ChatGPT", "GitHub مخزن", "GitHub Token")
        val urls = arrayOf(chatGptUrl, githubUrl, tokenUrl)
        AlertDialog.Builder(this).setTitle("Bookmarks / نشانک‌ها")
            .setItems(items) { _, which -> web.loadUrl(urls[which]) }
            .setNegativeButton("لغو", null).show()
    }

    private fun installSelectMode() {
        val js = """
            (function(){
              if(window.__fbxp_style) return;
              window.__fbxp_style=document.createElement('style');
              window.__fbxp_style.innerHTML='*{cursor:crosshair !important}';
              document.head.appendChild(window.__fbxp_style);
              window.__fbxp_click=function(e){
                e.preventDefault(); e.stopPropagation();
                var el=e.target;
                var text=(el.innerText || el.textContent || '').trim();
                if(!text) text=(el.outerHTML || '').trim();
                try{navigator.clipboard.writeText(text);}catch(x){
                  var ta=document.createElement('textarea'); ta.value=text;
                  document.body.appendChild(ta); ta.select(); document.execCommand('copy'); ta.remove();
                }
                return false;
              };
              document.addEventListener('click',window.__fbxp_click,true);
            })();
        """.trimIndent()
        web.evaluateJavascript(js, null)
        Toast.makeText(this, "روی بخش موردنظر صفحه بزنید.", Toast.LENGTH_SHORT).show()
    }

    private fun removeSelectMode() {
        web.evaluateJavascript("""
            (function(){
              if(window.__fbxp_click) document.removeEventListener('click',window.__fbxp_click,true);
              if(window.__fbxp_style){window.__fbxp_style.remove();window.__fbxp_style=null;}
            })();
        """.trimIndent(), null)
    }

    override fun onDestroy() { radioPlayer?.release(); radioPlayer=null; splitWeb?.destroy(); super.onDestroy() }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("last_url", web.url)
        super.onSaveInstanceState(outState)
    }

    @Deprecated("Deprecated in Android API")
    override fun onBackPressed() {
        if (fullScreenMode) { exitFullScreen(); return }
        if (chromeBarsHidden) { setChromeBarsHidden(false); return }
        if (web.canGoBack()) { web.goBack(); return }
        super.onBackPressed()
    }

    companion object {
        private const val FILE_CHOOSER_REQUEST = 4101
        private const val DESKTOP_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36"
    }
}
