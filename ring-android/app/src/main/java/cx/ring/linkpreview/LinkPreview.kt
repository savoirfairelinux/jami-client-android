package cx.ring.linkpreview

import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

object LinkPreview {

    fun loadPreviewData(url: String) : Single<PreviewData> {
        return Single.fromCallable {
            val doc = Jsoup.connect(url)
                .userAgent("Mozilla")
                .get()
            val imageElements = doc.select("meta[property=og:image]")
            if (imageElements.size > 0) {
                var it = 0
                var chosen: String? = ""
                while ((chosen == null || chosen.isEmpty()) && it < imageElements.size) {
                    chosen = imageElements[it].attr("content")
                    it += 1
                }
                PreviewData(doc.title(), chosen ?: "", url)
            } else {
                PreviewData("", "", "")
            }
        }.subscribeOn(Schedulers.io())
    }

}
