package com.example

import android.annotation.SuppressLint
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

class MainActivity : ComponentActivity() {

    private var webView: WebView? = null
    private var pendingFileCallback: ValueCallback<Array<Uri>>? = null

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        pendingFileCallback?.onReceiveValue(
            if (uri != null) arrayOf(uri) else emptyArray()
        )
        pendingFileCallback = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#031030"))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        ViewCompat.setOnApplyWindowInsetsListener(root) { v, windowInsets ->
            val bars = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            )
            v.updatePadding(
                left = bars.left,
                top = bars.top,
                right = bars.right,
                bottom = bars.bottom
            )
            windowInsets
        }

        val wv = createConfiguredWebView()
        webView = wv
        root.addView(wv)
        setContentView(root)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val activeWebView = webView
                if (activeWebView == null) {
                    finish()
                    return
                }
                activeWebView.evaluateJavascript(
                    """
                    (function() {
                        var shw = document.getElementById('shw');
                        if (shw && shw.classList.contains('open')) {
                            var sc = shw.querySelector('[data-sc]');
                            if (sc) sc.click(); else shw.classList.remove('open');
                            return 'handled';
                        }
                        var con = document.getElementById('con');
                        if (con && con.classList.contains('open')) {
                            var cc = con.querySelector('[data-cc]');
                            if (cc) cc.click(); else con.classList.remove('open');
                            return 'handled';
                        }
                        var np = document.getElementById('np');
                        if (np && np.classList.contains('open')) {
                            var npc = np.querySelector('[data-npc]');
                            if (npc) npc.click(); else np.classList.remove('open');
                            return 'handled';
                        }
                        if (typeof window.__jinBack === 'function' && window.__jinBack()) {
                            return 'handled';
                        }
                        var activeNav = document.querySelector('.nimbus-item[aria-current="page"]');
                        if (activeNav && activeNav.dataset.go && activeNav.dataset.go !== 'home') {
                            var homeBtn = document.querySelector('.nimbus-item[data-go="home"]');
                            if (homeBtn) homeBtn.click();
                            return 'handled';
                        }
                        return 'exit';
                    })();
                    """.trimIndent()
                ) { result ->
                    if (result?.contains("exit") == true) {
                        finish()
                    }
                }
            }
        })

        wv.loadUrl("https://appassets.androidplatform.net/assets/index.html")
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createConfiguredWebView(): WebView {
        return WebView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#031030"))
            // Keep Chromium's native Surface GPU compositor (avoid LAYER_TYPE_HARDWARE FBO copy stalls)
            setLayerType(View.LAYER_TYPE_NONE, null)
            overScrollMode = View.OVER_SCROLL_NEVER
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, false)
            }

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                mediaPlaybackRequiresUserGesture = false
                cacheMode = WebSettings.LOAD_DEFAULT
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            }

            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?
                ): WebResourceResponse? {
                    val url = request?.url?.toString() ?: return null
                    val cleanUrl = url.substringBefore('?').substringBefore('#')
                    val assetPath = when {
                        cleanUrl.startsWith("https://appassets.androidplatform.net/assets/") ->
                            cleanUrl.removePrefix("https://appassets.androidplatform.net/assets/")
                        url.contains("three@0.134.0/build/three.min.js") -> "vendor/three.min.js"
                        url.contains("vanta@0.5.24/dist/vanta.clouds.min.js") -> "vendor/vanta.clouds.min.js"
                        else -> null
                    }
                    if (!assetPath.isNullOrBlank()) {
                        val mime = when {
                            assetPath.endsWith(".html") -> "text/html"
                            assetPath.endsWith(".js") -> "application/javascript"
                            assetPath.endsWith(".css") -> "text/css"
                            assetPath.endsWith(".svg") -> "image/svg+xml"
                            assetPath.endsWith(".png") -> "image/png"
                            assetPath.endsWith(".webp") -> "image/webp"
                            assetPath.endsWith(".json") -> "application/json"
                            else -> "application/octet-stream"
                        }
                        return runCatching {
                            WebResourceResponse(
                                mime,
                                "UTF-8",
                                assets.open(assetPath)
                            ).apply {
                                responseHeaders = mapOf("Access-Control-Allow-Origin" to "*")
                            }
                        }.getOrNull()
                    }
                    return super.shouldInterceptRequest(view, request)
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean = false
            }

            webChromeClient = object : WebChromeClient() {
                override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback: ValueCallback<Array<Uri>>?,
                    fileChooserParams: FileChooserParams?
                ): Boolean {
                    if (filePathCallback == null) return false
                    pendingFileCallback?.onReceiveValue(emptyArray())
                    pendingFileCallback = filePathCallback
                    val accept = fileChooserParams?.acceptTypes
                        ?.firstOrNull { it.isNotBlank() }
                        ?: "*/*"
                    filePickerLauncher.launch(accept)
                    return true
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        webView?.onResume()
        webView?.resumeTimers()
    }

    override fun onPause() {
        webView?.onPause()
        webView?.pauseTimers()
        super.onPause()
    }

    override fun onDestroy() {
        pendingFileCallback?.onReceiveValue(emptyArray())
        pendingFileCallback = null
        webView?.apply {
            stopLoading()
            destroy()
        }
        webView = null
        super.onDestroy()
    }
}
