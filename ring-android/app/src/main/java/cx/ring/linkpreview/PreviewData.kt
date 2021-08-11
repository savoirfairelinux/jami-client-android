package cx.ring.linkpreview

data class PreviewData(val title: String, val imageUrl: String, val baseUrl: String) {

    fun isEmpty(): Boolean = title.isEmpty() && imageUrl.isEmpty() && baseUrl.isEmpty()

    fun isNotEmpty(): Boolean = !isEmpty()

    companion object  {
        val EMPTY_DATA = PreviewData("", "", "")
    }
}
