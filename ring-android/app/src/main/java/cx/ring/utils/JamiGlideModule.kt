package cx.ring.utils

import android.content.Context
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.load.DecodeFormat

@GlideModule
class JamiGlideModule : AppGlideModule() {
    override fun applyOptions(context: Context, builder: GlideBuilder) {
        builder.setDefaultRequestOptions(RequestOptions().format(DecodeFormat.PREFER_ARGB_8888))
    }
}