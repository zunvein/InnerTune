package com.zionhuang.music.ui.fragments

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.annotation.IdRes
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.transition.MaterialFadeThrough
import com.zionhuang.music.R
import com.zionhuang.music.constants.MediaConstants.QUEUE_ALL_SONG
import com.zionhuang.music.constants.MediaConstants.QUEUE_DESC
import com.zionhuang.music.constants.MediaConstants.QUEUE_ORDER
import com.zionhuang.music.constants.MediaConstants.QUEUE_TYPE
import com.zionhuang.music.constants.ORDER_ARTIST
import com.zionhuang.music.constants.ORDER_CREATE_DATE
import com.zionhuang.music.constants.ORDER_NAME
import com.zionhuang.music.constants.SongSortType
import com.zionhuang.music.databinding.LayoutRecyclerviewBinding
import com.zionhuang.music.download.DownloadHandler
import com.zionhuang.music.extensions.addFastScroller
import com.zionhuang.music.extensions.addOnClickListener
import com.zionhuang.music.ui.adapters.SongsAdapter
import com.zionhuang.music.ui.fragments.base.BindingFragment
import com.zionhuang.music.ui.listeners.SortMenuListener
import com.zionhuang.music.viewmodels.PlaybackViewModel
import com.zionhuang.music.viewmodels.SongsViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SongsFragment : BindingFragment<LayoutRecyclerviewBinding>() {
    private val playbackViewModel by activityViewModels<PlaybackViewModel>()
    private val songsViewModel by activityViewModels<SongsViewModel>()
    private lateinit var songsAdapter: SongsAdapter
    private val downloadHandler = DownloadHandler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialFadeThrough().apply { duration = 300L }
        exitTransition = MaterialFadeThrough().apply { duration = 300L }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        songsViewModel.downloadServiceConnection.addDownloadListener(downloadHandler.downloadListener)
        songsAdapter = SongsAdapter(songsViewModel.songPopupMenuListener, downloadHandler).apply {
            sortMenuListener = this@SongsFragment.sortMenuListener
        }
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = songsAdapter
            addOnClickListener { pos, _ ->
                if (pos == 0) return@addOnClickListener
                playbackViewModel.playMedia(requireActivity(), songsAdapter.getItemByPosition(pos)!!.id, bundleOf(
                        QUEUE_TYPE to QUEUE_ALL_SONG,
                        QUEUE_ORDER to sortMenuListener.sortType(),
                        QUEUE_DESC to sortMenuListener.sortDescending()
                ))
            }
            addFastScroller { useMd2Style() }
        }
        lifecycleScope.launch {
            songsViewModel.allSongsFlow.collectLatest {
                songsAdapter.submitData(it)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> findNavController().navigate(SettingsFragmentDirections.openSettingsFragment())
        }
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_search_and_settings, menu)
    }

    override fun onDestroy() {
        super.onDestroy()
        songsViewModel.downloadServiceConnection.removeDownloadListener(downloadHandler.downloadListener)
    }

    private val sortMenuListener = object : SortMenuListener {
        @IdRes
        override fun sortType(): Int = songsViewModel.sortType
        override fun sortDescending(): Boolean = songsViewModel.sortDescending
        override fun sortByCreateDate() = updateSortType(ORDER_CREATE_DATE)
        override fun sortByName() = updateSortType(ORDER_NAME)
        override fun sortByArtist() = updateSortType(ORDER_ARTIST)
        override fun toggleSortOrder() {
            songsViewModel.sortDescending = !songsViewModel.sortDescending
            songsAdapter.refresh()
        }
    }

    private fun updateSortType(@SongSortType sortType: Int) {
        songsViewModel.sortType = sortType
        songsAdapter.refresh()
    }

    companion object {
        private const val TAG = "SongsFragment"
    }
}