package cx.ring.linkpreview

data class PreviewData(
    val title: String,
    val description: String,
    val imageUrl: String,
    val baseUrl: String
) {
    fun isEmpty(): Boolean = title.isEmpty() && description.isEmpty() && imageUrl.isEmpty()

    fun isNotEmpty(): Boolean = !isEmpty()
}
