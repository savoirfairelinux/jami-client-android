package cx.ring.linkpreview

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.util.LruCache
import androidx.core.util.PatternsCompat
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import net.jami.utils.HashUtils
import net.jami.utils.toHex
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.json.JSONObject
import java.io.File

object LinkPreview {
    private const val CACHE_SIZE = 256
    private const val CACHE_DIR_NAME = "link_preview"

    private var cacheDir: File? = null
    private val previewCache = LruCache<String, PreviewData>(CACHE_SIZE)

    fun init(context: Context) {
        val dir = File(context.cacheDir, CACHE_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        cacheDir = dir
    }

    fun clearCache() {
        synchronized(previewCache) {
            previewCache.evictAll()
        }
    }

    private fun getCachedPreview(url: String): PreviewData? = synchronized(previewCache) {
        previewCache.get(url) ?: getDiskPreview(url)?.also { previewCache.put(url, it) }
    }

    private fun putCachedPreview(url: String, preview: PreviewData) {
        synchronized(previewCache) {
            previewCache.put(url, preview)
        }
    }

    private fun getDiskPreview(url: String): PreviewData? {
        val file = cacheFileForUrl(url) ?: return null
        if (!file.exists()) return null
        return try {
            val payload = file.readText(Charsets.UTF_8)
            deserializePreview(payload, url)
        } catch (_: Exception) {
            file.delete()
            null
        }
    }

    private fun putDiskPreview(url: String, preview: PreviewData) {
        val file = cacheFileForUrl(url) ?: return
        val payload = serializePreview(preview)
        val tmpFile = File(file.parentFile, file.name + ".tmp")
        try {
            tmpFile.writeText(payload, Charsets.UTF_8)
            if (!tmpFile.renameTo(file)) {
                file.writeText(payload, Charsets.UTF_8)
                tmpFile.delete()
            }
        } catch (_: Exception) {
            tmpFile.delete()
        }
    }

    private fun cacheFileForUrl(url: String): File? {
        val dir = cacheDir ?: return null
        val key = HashUtils.sha256(url).toHex()
        return File(dir, "$key.json")
    }

    private fun serializePreview(preview: PreviewData): String {
        val json = JSONObject()
        json.put("title", preview.title)
        json.put("description", preview.description)
        json.put("imageUrl", preview.imageUrl)
        json.put("baseUrl", preview.baseUrl)
        return json.toString()
    }

    private fun deserializePreview(payload: String, fallbackUrl: String): PreviewData? {
        return try {
            val json = JSONObject(payload)
            PreviewData(
                title = json.optString("title", ""),
                description = json.optString("description", ""),
                imageUrl = json.optString("imageUrl", ""),
                baseUrl = json.optString("baseUrl", fallbackUrl)
            )
        } catch (_: Exception) {
            null
        }
    }

    @SuppressLint("RestrictedApi")
    private fun extractUrls(input: String): List<String> {
        val result: MutableList<String> = ArrayList()
        val matcher = PatternsCompat.AUTOLINK_WEB_URL.matcher(input)
        while (matcher.find()) {
            val word = matcher.group()
            val start = matcher.start()
            if (!word.startsWith("http://", ignoreCase = true) && !word.startsWith("https://", ignoreCase = true)) {
                if (start > 0 && input[start - 1] == '@') {
                    continue // Skip email addresses
                }
                result.add("https://$word")
            } else {
                result.add(word)
            }
        }
        return result
    }

    fun getFirstUrl(input: String): Maybe<String> = Maybe.fromCallable {
        extractUrls(input).firstOrNull()
    }

    private fun getTile(doc: Document): String {
        doc.selectFirst("meta[property=og:title]")
            ?.attr("content")
            ?.takeIf { it.isNotEmpty() }
            ?.let { return it }
        return doc.title()
    }

    private fun getDescription(doc: Document): String {
        doc.selectFirst("meta[property=og:description]")
            ?.attr("content")
            ?.takeIf { it.isNotEmpty() }
            ?.let { return it }
        doc.selectFirst("meta[name=description]")
            ?.attr("content")
            ?.takeIf { it.isNotEmpty() }
            ?.let { return it }
        return ""
    }

    private fun getImage(doc: Document): String {
        doc.selectFirst("meta[property=og:image]")
            ?.attr("content")
            ?.takeIf { it.isNotEmpty() }
            ?.let { return it }
        doc.selectFirst("link[rel=image_src]")
            ?.attr("href")
            ?.takeIf { it.isNotEmpty() }
            ?.let { return it }
        return ""
    }

    private fun loadUrl(url: String): Single<Document> = Single.create { e ->
        Log.w("LinkPreview", "load $url")
        try {
            e.onSuccess(Jsoup.connect(url)
                .userAgent("Mozilla")
                .get())
        } catch (ex: Exception) {
            if (!e.isDisposed)
                e.onError(ex)
        }
    }

    fun load(url: String): Maybe<PreviewData> {
        getCachedPreview(url)?.let { return Maybe.just(it) }
        return loadUrl(url)
            .map { doc -> PreviewData(getTile(doc), getDescription(doc), getImage(doc), url) }
            .filter { preview -> preview.isNotEmpty() }
            .doOnSuccess { preview ->
                putCachedPreview(url, preview)
                Schedulers.io().createWorker().schedule { putDiskPreview(url, preview) }
            }
            .subscribeOn(Schedulers.io())
    }
}
