package com.example.scuqrcoder

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Base64
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 创建 WebView 实例
        webView = WebView(this)

        // 配置 WebView 设置
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.allowFileAccess = true
        webView.settings.allowContentAccess = true
        webView.settings.setSupportZoom(true)
        webView.settings.builtInZoomControls = true
        webView.settings.displayZoomControls = false

        // 设置自定义 WebViewClient 来处理下载请求
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                url?.let {
                    // 处理自定义下载协议
                    if (it.startsWith("scuqrcoder://download")) {
                        handleImageDownload(it)
                        return true
                    }
                    // 处理普通 HTTP/HTTPS 链接
                    else if (it.startsWith("http://") || it.startsWith("https://")) {
                        view?.loadUrl(it)
                        return true
                    }
                }
                return false
            }
        }

        // 加载本地 HTML 文件
        webView.loadUrl("file:///android_asset/index.html")

        // 设置 WebView 为内容视图
        setContentView(webView)
    }

    private fun handleImageDownload(url: String) {
        try {
            val uri = Uri.parse(url)
            val dataParam = uri.getQueryParameter("data")
            val filenameParam = uri.getQueryParameter("filename")
            val titleParam = uri.getQueryParameter("title")

            if (dataParam != null && filenameParam != null) {
                // 解码 Base64 数据
                val base64Data = dataParam.substringAfter("data:image/png;base64,")
                val imageBytes = Base64.decode(base64Data, Base64.DEFAULT)

                // 创建 Bitmap
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

                // 保存图片到相册
                val success = saveBitmapToGallery(bitmap, filenameParam)

                // 显示保存成功消息
                if (success) {
                    val message = when (titleParam) {
                        "签到二维码" -> "签到二维码已保存到相册"
                        "签退二维码" -> "签退二维码已保存到相册"
                        else -> "二维码已保存到相册"
                    }
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun saveBitmapToGallery(bitmap: Bitmap, filename: String): Boolean {
        return try {
            // 获取 Pictures 目录
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val appDir = File(picturesDir, "SCU二维码")
            if (!appDir.exists()) {
                appDir.mkdirs()
            }

            // 创建文件
            val file = File(appDir, filename)
            val outputStream = FileOutputStream(file)

            // 压缩并保存为 PNG
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            outputStream.close()

            // 通知系统相册更新
            sendBroadcast(
                android.content.Intent(android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).apply {
                    data = Uri.fromFile(file)
                }
            )

            true
        } catch (e: Exception) {
            Toast.makeText(this, "保存失败，请检查存储权限", Toast.LENGTH_SHORT).show()
            false
        }
    }

    // 处理返回键
    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    // 在 Activity 销毁时清理 WebView
    override fun onDestroy() {
        if (::webView.isInitialized) {
            webView.stopLoading()
            webView.destroy()
        }
        super.onDestroy()
    }
}