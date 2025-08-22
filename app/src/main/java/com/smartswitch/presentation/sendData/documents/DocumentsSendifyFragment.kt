package com.smartswitch.presentation.sendData.documents

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.smartswitch.R
import com.smartswitch.databinding.FragmentDocumentsSolBinding
import com.smartswitch.domain.model.MediaInfoModel
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
class DocumentsSendifyFragment : Fragment(), OnMediaItemClickCallbackForSelectAll {
    private var _binding: FragmentDocumentsSolBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DocumentsSolFragmentViewModel by viewModels()

    private var isDataLoaded = false
    var adapter: DocumentsAdapter? = null

    private lateinit var onMediaItemClickCallback: OnMediaItemClickCallback

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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentDocumentsSolBinding.inflate(inflater, container, false)
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
            fetchDocuments()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun fetchDocuments() {
        Log.d("fetch___", "fetchApps")
        lifecycleScope.launch(Dispatchers.IO) {
            activity?.let { act ->
                viewModel.getDocs()
            }
        }
    }

    private fun observeList() {
        Log.d("observeList", "observeList() called: Starting observation.")

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                Log.d("observeList", "repeatOnLifecycle STARTED: Collecting isFetchingComplete.")
                viewModel.isFetchingComplete.collect { isComplete ->
                    Log.d("observeList", "isFetchingComplete value received: $isComplete")
                    updateUi(isComplete)
                }
            }
        }
    }

    private fun updateUi(isComplete: Boolean) {
        if (isComplete) {
            binding.dateTextView.text = "${getString(R.string.documents)} (${viewModel.documentsList.size})"
            setupRecyclerView(viewModel.documentsList)
            isDataLoaded = true

        } else {
            binding.progressBar.visible()
            binding.rvAudios.gone()
            binding.tvNoData.gone()
            binding.headerLayout.gone()

        }
    }

    private fun setupRecyclerView(list: List<MediaInfoModel>) {
        Log.d("setRecyclerView", "setupRecyclerView")
        if (list.isEmpty()) {
            binding.tvNoData.visible()
            binding.progressBar.gone()
            binding.rvAudios.gone()
            binding.headerLayout.gone()
        } else {

            adapter = DocumentsAdapter(list, this)
            binding.progressBar.gone()
            binding.tvNoData.gone()
            binding.rvAudios.visible()
            binding.headerLayout.visible()
            binding.rvAudios.setItemAnimator(null)
            binding.rvAudios.adapter = adapter
        }

    }

    private fun initListener() {
        binding.apply {
            checkboxSelectAll.setOnCheckedChangeListener { _, isChecked ->
                if (!checkboxSelectAll.isPressed) return@setOnCheckedChangeListener

                adapter?.selectAllMedia(isChecked, viewModel.documentsList, lifecycleScope){
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

    override fun onMediaItemClickedForSelectAll() {
        onMediaItemClickCallback.onMediaItemClicked()
        val allSelected = SelectedListManager.getSelectedMediaList().containsAll(viewModel.documentsList)
        updateSelectAllState(allSelected)
        binding.checkboxSelectAll.isChecked = allSelected
    }
}