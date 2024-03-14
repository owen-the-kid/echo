package dev.brahmkshatriya.echo.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.search.SearchBar
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.databinding.ItemSearchHeaderBinding
import dev.brahmkshatriya.echo.ui.extension.ExtensionDialogFragmentDirections
import dev.brahmkshatriya.echo.ui.settings.SettingsFragmentDirections


class SearchHeaderAdapter(
    private val setup : (SearchBar) -> Unit
) : RecyclerView.Adapter<SearchHeaderAdapter.SearchHeaderViewHolder>() {
    class SearchHeaderViewHolder(val binding: ItemSearchHeaderBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = SearchHeaderViewHolder(
        ItemSearchHeaderBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
    )
    override fun getItemCount(): Int = 1

    override fun onBindViewHolder(holder: SearchHeaderViewHolder, position: Int) {
        val binding = holder.binding

        setup(binding.catSearchBar)

        searchbar = binding.catSearchBar

        binding.topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_settings -> {
                    val action = SettingsFragmentDirections.actionSettings()
                    binding.root.findNavController().navigate(action)
                    true
                }

                R.id.menu_extensions -> {
                    val action = ExtensionDialogFragmentDirections.actionExtension()
                    binding.root.findNavController().navigate(action)
                    true
                }

                else -> false
            }
        }
    }

    private var searchbar : SearchBar? = null
    fun setText(it: String) {
        searchbar?.setText(it)
    }
}