package dev.brahmkshatriya.echo.newui

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.common.models.EchoMediaItem

class MediaItemAdapter(
    val listener: Listener,
    val list: List<EchoMediaItem>
) : RecyclerView.Adapter<MediaItemViewHolder>() {

    interface Listener {
        fun onClick(item: EchoMediaItem, transitionView: View)
        fun onLongClick(item: EchoMediaItem, transitionView: View): Boolean
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaItemViewHolder {
        return when (viewType) {
            0 -> MediaItemViewHolder.Lists.create(parent)
            1 -> MediaItemViewHolder.Profile.create(parent)
            2 -> MediaItemViewHolder.Track.create(parent)
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun getItemViewType(position: Int) = when (list[position]) {
        is EchoMediaItem.Lists -> 0
        is EchoMediaItem.Profile -> 1
        is EchoMediaItem.TrackItem -> 2
    }

    override fun getItemCount() = list.size

    override fun onBindViewHolder(holder: MediaItemViewHolder, position: Int) {
        val item = list[position]
        holder.bind(item)
        holder.itemView.setOnClickListener {
            listener.onClick(item, holder.transitionView)
        }
        holder.itemView.setOnLongClickListener {
            listener.onLongClick(item, holder.transitionView)
        }
    }
}
