package cx.ring.linkpreview

import android.util.Log
import androidx.core.util.PatternsCompat
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

object LinkPreview {
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

    fun load(url: String): Maybe<PreviewData> = loadUrl(url)
        .map { doc -> PreviewData(getTile(doc), getDescription(doc), getImage(doc), url) }
        .filter { preview -> preview.isNotEmpty() }
        .subscribeOn(Schedulers.io())
}
