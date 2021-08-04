package cx.ring.linkpreview
interface LinkListener {

    /**
     * Called when there was an error in loading the image from url, recommended to hide the view
     */
    fun onError()

    /**
     * Called when image from url is loaded successfully
     *
     * @param link to url image
     */
    fun onSuccess(link: PreviewData)
}