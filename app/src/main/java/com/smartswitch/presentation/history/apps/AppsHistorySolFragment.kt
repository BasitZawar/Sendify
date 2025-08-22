package com.smartswitch.presentation.history.apps

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
import com.smartswitch.databinding.FragmentAppsHistorySolBinding
import com.smartswitch.domain.model.HistoryViewModel
import com.smartswitch.domain.model.MediaInfoModel
import com.smartswitch.utils.SelectedListManagerForDeletion
import com.smartswitch.utils.callback.OnMediaItemClickCallbackForSelectAll
import com.smartswitch.utils.extensions.gone
import com.smartswitch.utils.extensions.visible
import kotlinx.coroutines.launch

class AppsHistorySolFragment : Fragment(), OnMediaItemClickCallbackForSelectAll {
    private var _binding : FragmentAppsHistorySolBinding? = null
    private val binding get() = _binding!!

    private val appsHistoryViewModel: HistoryViewModel by activityViewModels()
    private lateinit var adapter: AppsHistoryAdapter
    private var isSent = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAppsHistorySolBinding.inflate(inflater,container,false)
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
        binding.rvHistoryApps.gone()

    }

    private fun setupRecyclerView() {
        adapter = AppsHistoryAdapter(this)
        binding.rvHistoryApps.layoutManager = LinearLayoutManager(context)
        binding.rvHistoryApps.adapter = adapter
    }

    private fun observeSentAndReceivedHistory() {
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    appsHistoryViewModel.filteredAppsHistory.collect { sentHistoryList ->
                        Log.d("AppsHistoryFragment", "Received sent app history list: $sentHistoryList")
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
                val currentAppsList = appsHistoryViewModel.filteredAppsHistory.value
                if (isChecked) {
                    adapter?.selectAllMedia(true, currentAppsList, lifecycleScope)
                } else {
                    adapter?.selectAllMedia(false, currentAppsList, lifecycleScope)
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
            binding.rvHistoryApps.gone()
            binding.tvNoData.visible()
            binding.headerLayout.gone()
            Log.d("AppsHistoryFragment___", "No app history available.")
        } else {
            binding.rvHistoryApps.visible()
            binding.tvNoData.gone()
            binding.headerLayout.visible()
            Log.d("AppsHistoryFragment___", "Updating RecyclerView with ${historyList.size} items.")
            adapter.updateData(historyList)
            Log.d("AppsHistoryFragment___", "Updated RecyclerView with app history data.")
        }
    }


    override fun onMediaItemClickedForSelectAll() {
        lifecycleScope.launch {
            val currentAppsList = appsHistoryViewModel.filteredAppsHistory.value
            val allSelected = currentAppsList.all { SelectedListManagerForDeletion.isItemSelected(it) }

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