package com.smartswitch.presentation.history.audios

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
import com.smartswitch.databinding.FragmentAudiosHistorySolBinding
import com.smartswitch.domain.model.HistoryViewModel
import com.smartswitch.domain.model.MediaInfoModel
import com.smartswitch.utils.SelectedListManagerForDeletion
import com.smartswitch.utils.callback.OnMediaItemClickCallbackForSelectAll
import com.smartswitch.utils.extensions.gone
import com.smartswitch.utils.extensions.visible
import kotlinx.coroutines.launch

class AudiosHistorySolFragment : Fragment(), OnMediaItemClickCallbackForSelectAll {
    private var _binding : FragmentAudiosHistorySolBinding? = null
    private val binding get() = _binding!!

    private val audioHistoryViewModel: HistoryViewModel by activityViewModels()
    private lateinit var adapter: AudiosHistoryAdapter
    private var isSent = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAudiosHistorySolBinding.inflate(inflater,container,false)
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
        binding.rvHistoryAudios.gone()
    }

    private fun setupRecyclerView() {
        adapter = AudiosHistoryAdapter(this)
        binding.rvHistoryAudios.layoutManager = LinearLayoutManager(context)
        binding.rvHistoryAudios.adapter = adapter
    }

    private fun observeSentAndReceivedHistory() {
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    audioHistoryViewModel.filteredAudioHistory.collect { sentHistoryList ->
                        Log.d("AudiosHistoryFragment", "Received sent audio history list: $sentHistoryList")
                        if (isSent) updateRecyclerView(sentHistoryList)
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
                val currentAudioList = audioHistoryViewModel.filteredAudioHistory.value
                if (isChecked) {
                    adapter?.selectAllMedia(true, currentAudioList, lifecycleScope)
                } else {
                    adapter?.selectAllMedia(false, currentAudioList, lifecycleScope)
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

        if (historyList.isEmpty()) {
            binding.rvHistoryAudios.gone()
            binding.tvNoData.visible()
            binding.headerLayout.gone()
            adapter.updateData(emptyList())
            Log.d("AudiosHistoryFragment", "No audio history available.")
        } else {
            binding.rvHistoryAudios.visible()
            binding.tvNoData.gone()
            binding.headerLayout.visible()
            adapter.updateData(historyList)
            Log.d("AudiosHistoryFragment", "Updated RecyclerView with audio history data.")
        }
    }

    override fun onMediaItemClickedForSelectAll() {
        lifecycleScope.launch {
            val currentAudioList = audioHistoryViewModel.filteredAudioHistory.value
            val allSelected = currentAudioList.all { SelectedListManagerForDeletion.isItemSelected(it) }
            Log.d("AudioHistoryUseCase___", "Fetched video history list size: ${currentAudioList.size}")
            // Update the checkbox state
            binding.checkboxSelectAll.isChecked = allSelected
            updateSelectAllState(allSelected)
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}