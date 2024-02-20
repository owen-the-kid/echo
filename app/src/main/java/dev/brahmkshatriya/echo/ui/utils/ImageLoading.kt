package dev.brahmkshatriya.echo.ui.utils

import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.button.MaterialButton
import dev.brahmkshatriya.echo.common.models.ImageHolder

fun ImageHolder.loadInto(imageView: ImageView, placeholder: Int? = null, error: Int? = null) {
    val builder = when (this) {
        is ImageHolder.BitmapHolder -> Glide.with(imageView).load(this.bitmap)
        is ImageHolder.UrlHolder -> Glide.with(imageView).load(GlideUrl(this.url) { this.headers })
        is ImageHolder.UriHolder -> Glide.with(imageView).load(this.uri)
    }
    placeholder?.let { builder.placeholder(it) }
    error?.let { builder.error(it) }
    builder.into(imageView)
}

fun ImageHolder.loadInto(button: MaterialButton, placeholder: Int? = null, error: Int? = null) {
    val builder = when (this) {
        is ImageHolder.BitmapHolder -> Glide.with(button).load(this.bitmap)
        is ImageHolder.UrlHolder -> Glide.with(button).load(GlideUrl(this.url) { this.headers })
        is ImageHolder.UriHolder -> Glide.with(button).load(this.uri)
    }
    placeholder?.let { builder.placeholder(it) }
    error?.let { builder.error(it) }
    builder.into(MaterialButtonTarget(button))
}

class MaterialButtonTarget(private val button: MaterialButton)
    : CustomViewTarget<MaterialButton, Drawable>(button) {
    override fun onLoadFailed(errorDrawable: Drawable?) { button.icon = errorDrawable }
    override fun onResourceCleared(placeholder: Drawable?) { button.icon = placeholder }
    override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
        button.icon = resource
    }
}