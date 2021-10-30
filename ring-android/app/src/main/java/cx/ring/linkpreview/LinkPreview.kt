package cx.ring.linkpreview

import android.util.Log
import android.util.Patterns
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

object LinkPreview {
    private fun extractUrls(input: String): List<String> {
        val result: MutableList<String> = ArrayList()
        val words = input.split("\\s+".toRegex())
        for (word in words)
            if (Patterns.WEB_URL.matcher(word).find()) {
                if (!word.lowercase().contains("http://") && !word.lowercase().contains("https://")) {
                    result.add("https://$word")
                } else {
                    result.add(word)
                }
            }
        return result
    }

    fun getFirstUrl(input: String) : Maybe<String> {
        return Maybe.fromCallable { extractUrls(input).firstOrNull() }
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

    private fun loadUrl(url: String): Single<Document> {
        return Single.create { e ->
            try {
                e.onSuccess(Jsoup.connect(url)
                    .userAgent("Mozilla")
                    .get())
            } catch (ex: Exception) {
                if (!e.isDisposed)
                    e.onError(ex)
            }
        }
    }

    fun load(url: String): Maybe<PreviewData> {
        Log.w("LinkPreview", "load $url")
        return loadUrl(url)
            .map { doc -> PreviewData(getTile(doc), getDescription(doc), getImage(doc), url) }
            .filter { preview -> preview.isNotEmpty() }
            .subscribeOn(Schedulers.io())
    }

}
