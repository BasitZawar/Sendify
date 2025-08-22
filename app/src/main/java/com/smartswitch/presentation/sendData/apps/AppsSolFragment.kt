package com.smartswitch.presentation.sendData.apps

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.smartswitch.R
import com.smartswitch.databinding.FragmentAppsSolBinding
import com.smartswitch.domain.model.MediaInfoModel
import com.smartswitch.presentation.sendData.photos.PhotosAdapter
import com.smartswitch.utils.SelectedListManager
import com.smartswitch.utils.callback.OnMediaItemClickCallback
import com.smartswitch.utils.callback.OnMediaItemClickCallbackForSelectAll
import com.smartswitch.utils.extensions.gone
import com.smartswitch.utils.extensions.isAlive
import com.smartswitch.utils.extensions.selectAllMedia
import com.smartswitch.utils.extensions.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AppsSolFragment : Fragment(), OnMediaItemClickCallbackForSelectAll {
    private var _binding: FragmentAppsSolBinding? = null
    private val binding get() = _binding!!

    private lateinit var onMediaItemClickCallback: OnMediaItemClickCallback

    private val viewModel: AppsSolFragmentViewModel by viewModels()

    private var isDataLoaded = false
    var adapter: AppsAdapter? = null


    override fun onAttach(context: Context) {
        super.onAttach(context)
        isAlive {
            // Ensure the parent fragment implements the interface
            val parentFragment = parentFragment
            if (parentFragment is OnMediaItemClickCallback) {
                onMediaItemClickCallback = parentFragment
            } else {
                throw IllegalStateException("Parent fragment must implement OnMediaItemClickCallback")
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?, ): View {
        _binding = FragmentAppsSolBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isAlive {
            observeList()
            initListener()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isDataLoaded) {
            fetchApps()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun fetchApps() {
        Log.d("fetch___", "fetchApps")
        lifecycleScope.launch(Dispatchers.IO) {
            activity?.let { act ->
                viewModel.getApps()
            }
        }
    }

    private fun observeList() {
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isFetchingComplete.collect { isComplete ->
                    updateUi(isComplete)

                }
            }
        }
    }

    private fun initListener() {
        updateSelectAllState(false)
        binding.apply {
            checkboxSelectAll.setOnCheckedChangeListener { _, isChecked ->
                if (!checkboxSelectAll.isPressed) {
                    checkboxSelectAll.isChecked = false
                    return@setOnCheckedChangeListener
                }

                adapter?.selectAllMedia(isChecked, viewModel.appsList, lifecycleScope) {
                    onMediaItemClickCallback.onMediaItemClicked()
                    updateSelectAllState(isChecked)
                }
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

    private fun updateUi(isComplete: Boolean) {
        if (isComplete) {
            binding.dateTextView.text = "${getString(R.string.apps)} (${viewModel.appsList.size})"
            setupRecyclerView(viewModel.appsList)
            isDataLoaded = true

        } else {
            binding.progressBar.visible()
            binding.rvApps.gone()
            binding.tvNoData.gone()
            binding.headerLayout.gone()
        }
    }

    private fun setupRecyclerView(list: List<MediaInfoModel>) {
        Log.d("setRecyclerView", "setupRecyclerView")
        if (list.isEmpty()) {
            binding.tvNoData.visible()
            binding.rvApps.gone()
            binding.progressBar.gone()
            binding.headerLayout.gone()

        } else {
            adapter = AppsAdapter(list, this)
            val linearLayoutManager = LinearLayoutManager(context)
            val gridLayoutManager = GridLayoutManager(context, 4) // 3 columns for photos

            gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return when (adapter?.getItemViewType(position)) {
                        PhotosAdapter.VIEW_TYPE_PHOTO_LIST_ITEM -> 2 // Span across all 3 columns
                        PhotosAdapter.VIEW_TYPE_PHOTO_ITEM -> 1  // Each photo takes 1 column
                        else -> 1
                    }
                }
            }
            binding.tvNoData.gone()
            binding.progressBar.gone()
            binding.rvApps.visible()
            binding.headerLayout.visible()
            binding.rvApps.setItemAnimator(null)
            binding.rvApps.adapter = adapter

            // For Layout Toggle
            val gridListColor = ContextCompat.getColor(requireContext(), R.color.grid_list_color)
            val gridListSelectedColor =
                ContextCompat.getColor(requireContext(), R.color.grid_list_selected_color)

            updateLayoutToggle(gridListSelectedColor, gridListColor, gridLayoutManager, true)
            binding.gridView.setOnClickListener {
                updateLayoutToggle(gridListSelectedColor, gridListColor, gridLayoutManager, true)
            }

            binding.listView.setOnClickListener {
                updateLayoutToggle(gridListColor, gridListSelectedColor, linearLayoutManager, false)
            }
        }
    }

    private fun updateLayoutToggle(gridColor: Int, listColor: Int, layoutManager: RecyclerView.LayoutManager, isGridLayout: Boolean,
    ) {
        binding.gridView.backgroundTintList = ColorStateList.valueOf(gridColor)
        binding.listView.backgroundTintList = ColorStateList.valueOf(listColor)

        binding.rvApps.layoutManager = layoutManager
        adapter?.isGridLayout = isGridLayout
        adapter?.notifyDataSetChanged()
    }

    override fun onMediaItemClickedForSelectAll() {
        onMediaItemClickCallback.onMediaItemClicked()
        val allSelected = SelectedListManager.getSelectedMediaList().containsAll(viewModel.appsList)
        updateSelectAllState(allSelected)
        binding.checkboxSelectAll.isChecked = allSelected
    }
}