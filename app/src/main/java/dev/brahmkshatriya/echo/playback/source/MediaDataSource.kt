package dev.brahmkshatriya.echo.playback.source

import android.content.Context
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.playback.source.MediaResolver.Companion.copy

@UnstableApi
class MediaDataSource(
    private val defaultDataSourceFactory: Lazy<DefaultDataSource.Factory>,
    private val byteStreamDataSourceFactory: Lazy<ByteStreamDataSource.Factory>,
    private val byteChannelDataSourceFactory: Lazy<ByteChannelDataSource.Factory>,
) : BaseDataSource(true) {

    class Factory(
        context: Context,
    ) : DataSource.Factory {
        private val defaultDataSourceFactory = lazy { DefaultDataSource.Factory(context) }
        private val byteStreamDataSourceFactory = lazy { ByteStreamDataSource.Factory() }
        private val byteChannelDataSourceFactory = lazy { ByteChannelDataSource.Factory() }
        override fun createDataSource() = MediaDataSource(
            defaultDataSourceFactory, byteStreamDataSourceFactory, byteChannelDataSourceFactory
        )
    }

    private var source: DataSource? = null

    override fun getUri() = source?.uri

    override fun read(buffer: ByteArray, offset: Int, length: Int) =
        source?.read(buffer, offset, length) ?: throw Exception("Source not opened")

    override fun close() {
        source?.close()
        source = null
    }

    override fun open(dataSpec: DataSpec): Long {
        val streamable = dataSpec.customData as? Streamable.Source
        val (factory, spec) = when (streamable) {
            null -> defaultDataSourceFactory to dataSpec
            is Streamable.Source.ByteStream -> byteStreamDataSourceFactory to dataSpec
            is Streamable.Source.Channel -> byteChannelDataSourceFactory to dataSpec
            is Streamable.Source.Http -> {
                val spec = streamable.request.run {
                    dataSpec.copy(uri = url.toUri(), httpRequestHeaders = headers)
                }
                defaultDataSourceFactory to spec
            }
        }
        val source = factory.value.createDataSource()
        this.source = source
        return source.open(spec)
    }
}