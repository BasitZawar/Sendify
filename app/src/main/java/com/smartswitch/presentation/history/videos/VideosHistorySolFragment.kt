package com.smartswitch.presentation.history.videos

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.smartswitch.R
import com.smartswitch.databinding.FragmentVideosHistorySolBinding
import com.smartswitch.domain.model.HistoryViewModel
import com.smartswitch.domain.model.MediaInfoModel
import com.smartswitch.utils.SelectedListManagerForDeletion
import com.smartswitch.utils.callback.OnMediaItemClickCallbackForSelectAll
import com.smartswitch.utils.extensions.gone
import com.smartswitch.utils.extensions.visible
import kotlinx.coroutines.launch

class VideosHistorySolFragment : Fragment(), OnMediaItemClickCallbackForSelectAll {
    private var _binding: FragmentVideosHistorySolBinding? = null
    private val binding get() = _binding!!

    private val videoHistoryViewModel: HistoryViewModel by activityViewModels()
    private lateinit var adapter: VideosHistoryAdapter
    private var isSent = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentVideosHistorySolBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeSentAndReceivedHistory()
        initListeners()

        binding.progressBar.visible()
        binding.tvNoData.gone()
        binding.headerLayout.gone()
        binding.rvHistoryVideos.gone()
    }

    private fun setupRecyclerView() {
        adapter = VideosHistoryAdapter(this)
        val linearLayoutManager = LinearLayoutManager(context)
        binding.rvHistoryVideos.layoutManager = linearLayoutManager
        binding.rvHistoryVideos.adapter = adapter
    }


    private fun observeSentAndReceivedHistory() {
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    videoHistoryViewModel.filteredVideoHistory.collect { sentHistoryList ->
                        Log.d(
                            "VideosHistoryFragment",
                            "Received sent video history list: $sentHistoryList"
                        )
                        updateRecyclerView(sentHistoryList)
                    }
                }
            }
        }
    }

    private fun initListeners() {
        updateSelectAllState(false)
        binding.apply {
            binding.checkboxSelectAll.setOnCheckedChangeListener { _, isChecked ->
                if (!binding.checkboxSelectAll.isPressed) return@setOnCheckedChangeListener
                val currentVideoList = videoHistoryViewModel.filteredVideoHistory.value
                if (isChecked) {
                    adapter?.selectAllMedia(true, currentVideoList, lifecycleScope)
                } else {
                    adapter?.selectAllMedia(false, currentVideoList, lifecycleScope)
                }
                updateSelectAllState(isChecked)
            }
        }
    }

    private fun updateSelectAllState(isChecked: Boolean) {
        binding.checkboxSelectAll.setBackgroundResource(
            if (isChecked) R.drawable.check_circle else R.drawable.uncheck_circle
        )
        binding.selectTv.text = if (isChecked) getString(R.string.de_select_all) else getString(R.string.select_all)
        binding.selectTv.setTextColor(resources.getColor(R.color.sub_heading_text_color, null))
    }

    private fun updateRecyclerView(historyList: List<MediaInfoModel>) {
        binding.progressBar.gone()
        // Log the size of the list
        Log.d("UpdateRecyclerView___", "History list size: ${historyList.size}")

        // Log the contents of the list
        Log.d("UpdateRecyclerView___", "History list contents: $historyList")

        if (historyList.isEmpty()) {
            binding.rvHistoryVideos.gone()
            binding.headerLayout.gone()
            binding.tvNoData.visible()
        } else {
            binding.tvNoData.gone()
            binding.headerLayout.visible()
            binding.rvHistoryVideos.visible()
            adapter.updateData(historyList)
        }
    }

    override fun onMediaItemClickedForSelectAll() {
        lifecycleScope.launch {
            val combinedVideoList = videoHistoryViewModel.filteredVideoHistory.value
            val allSelected =
                combinedVideoList.all { SelectedListManagerForDeletion.isItemSelected(it) }
            Log.d("VideoHistoryUseCase___", "History list contents: ${combinedVideoList.size}")
            binding.checkboxSelectAll.isChecked = allSelected
            updateSelectAllState(allSelected)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}