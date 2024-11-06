package dev.brahmkshatriya.echo.playback.source

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.ResolvingDataSource.Resolver
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.playback.MediaItemUtils.toIdAndIndex
import kotlinx.coroutines.flow.MutableStateFlow

class MediaResolver(
    private val current: MutableStateFlow<Streamable.Media.Sources?>
) : Resolver {

    @UnstableApi
    override fun resolveDataSpec(dataSpec: DataSpec): DataSpec {
        val (_, _, index) = dataSpec.uri.toString().toIdAndIndex() ?: return dataSpec

        val current = current.value ?: return dataSpec
        val source = current.sources[index]
        return dataSpec.copy(
            customData = source
        )
    }

    companion object {

        @OptIn(UnstableApi::class)
        fun DataSpec.copy(
            uri: Uri? = null,
            uriPositionOffset: Long? = null,
            httpMethod: Int? = null,
            httpBody: ByteArray? = null,
            httpRequestHeaders: Map<String, String>? = null,
            position: Long? = null,
            length: Long? = null,
            key: String? = null,
            flags: Int? = null,
            customData: Any? = null
        ): DataSpec {
            return DataSpec.Builder()
                .setUri(uri ?: this.uri)
                .setUriPositionOffset(uriPositionOffset ?: this.uriPositionOffset)
                .setHttpMethod(httpMethod ?: this.httpMethod)
                .setHttpBody(httpBody ?: this.httpBody)
                .setHttpRequestHeaders(httpRequestHeaders ?: this.httpRequestHeaders)
                .setPosition(position ?: this.position)
                .setLength(length ?: this.length)
                .setKey(key ?: this.key)
                .setFlags(flags ?: this.flags)
                .setCustomData(customData ?: this.customData)
                .build()
        }
    }
}