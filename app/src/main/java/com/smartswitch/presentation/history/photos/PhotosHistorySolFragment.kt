package com.smartswitch.presentation.history.photos

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
import com.smartswitch.databinding.FragmentPhotosHistorySolBinding
import com.smartswitch.domain.model.HistoryViewModel
import com.smartswitch.domain.model.MediaInfoModel
import com.smartswitch.utils.SelectedListManagerForDeletion
import com.smartswitch.utils.callback.OnMediaItemClickCallbackForSelectAll
import com.smartswitch.utils.extensions.gone
import com.smartswitch.utils.extensions.visible
import kotlinx.coroutines.launch

class PhotosHistorySolFragment : Fragment(), OnMediaItemClickCallbackForSelectAll {
    private var _binding : FragmentPhotosHistorySolBinding?=null
    private val binding get() = _binding!!
    private val photoHistoryViewModel: HistoryViewModel by activityViewModels()
    private var adapter: PhotosHistoryAdapter? = null

    private var isGridLayout = true
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentPhotosHistorySolBinding.inflate(inflater,container,false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerview()
        observeSentAndReceivedHistory()
        initListeners()

        binding.progressBar.visible()
        binding.headerLayout.gone()
    }

    private fun setupRecyclerview() {
        adapter = PhotosHistoryAdapter(this)
        binding.rvHistoryPhotos.adapter = adapter
        binding.rvHistoryPhotos.setItemAnimator(null)

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                photoHistoryViewModel.isGridLayout.collect { layoutMode ->
                    isGridLayout = layoutMode
                    setupLayoutManager()
                }
            }
        }
    }

    private fun setupLayoutManager() {
        val linearLayoutManager = LinearLayoutManager(context)
        binding.rvHistoryPhotos.layoutManager = linearLayoutManager
        adapter?.notifyDataSetChanged()
    }

    private fun observeSentAndReceivedHistory() {
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                photoHistoryViewModel.filteredPhotoHistory.collect { historyList ->
                    Log.d("PhotosHistoryFragmentZai___", "History received: $historyList")
                    updateRecyclerView(historyList)
                }
            }
        }
    }

    private fun initListeners() {
        updateSelectAllState(false)
        binding.apply {
            binding.checkboxSelectAll.setOnCheckedChangeListener { _, isChecked ->
                if (!binding.checkboxSelectAll.isPressed) return@setOnCheckedChangeListener
                val currentPhotoList = photoHistoryViewModel.filteredPhotoHistory.value
                if (isChecked) {
                    adapter?.selectAllMedia(true, currentPhotoList, lifecycleScope)
                } else {
                    adapter?.selectAllMedia(false, currentPhotoList, lifecycleScope)
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
        Log.d("UpdateRecyclerView", "History list size: ${historyList.size}")

        // Log the contents of the list
        Log.d("UpdateRecyclerView", "History list contents: $historyList")

        if (historyList.isEmpty()) {
            binding.rvHistoryPhotos.gone()
            binding.headerLayout.gone()
            binding.tvNoData.visible()
        } else {
            binding.rvHistoryPhotos.visible()
            binding.headerLayout.visible()
            binding.tvNoData.gone()
            adapter?.updateData(historyList)
        }
    }


    override fun onMediaItemClickedForSelectAll() {
        lifecycleScope.launch {
            val combinedPhotoList = photoHistoryViewModel.filteredPhotoHistory.value
            val allSelected = combinedPhotoList.all { SelectedListManagerForDeletion.isItemSelected(it) }
            Log.d("testng",allSelected.toString())

            binding.checkboxSelectAll.isChecked = allSelected
            updateSelectAllState(allSelected)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}