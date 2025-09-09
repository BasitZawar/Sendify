package com.smartswitch.presentation.sendData.documents

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.smartswitch.R
import com.smartswitch.databinding.FragmentDocumentsSolBinding
import com.smartswitch.domain.model.MediaInfoModel
import com.smartswitch.utils.Constant.STORAGE_PERMISSION_CODE
import com.smartswitch.utils.Constant.checkPermission
import com.smartswitch.utils.Constant.requestStoragePermission11
import com.smartswitch.utils.PermissionViewModel
import com.smartswitch.utils.SelectedListManager
import com.smartswitch.utils.callback.OnMediaItemClickCallback
import com.smartswitch.utils.callback.OnMediaItemClickCallbackForSelectAll
import com.smartswitch.utils.extensions.gone
import com.smartswitch.utils.extensions.isAlive
import com.smartswitch.utils.extensions.selectAllMedia
import com.smartswitch.utils.extensions.setSafeOnClickListener
import com.smartswitch.utils.extensions.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DocumentsSendifyFragment : Fragment(), OnMediaItemClickCallbackForSelectAll {
    private var _binding: FragmentDocumentsSolBinding? = null
    private val binding get() = _binding!!
    private lateinit var permissionViewModel: PermissionViewModel
    private lateinit var manageAllFilesPermissionLauncher: ActivityResultLauncher<Intent>
    var TAG = "TESTTAG"
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
        permissionViewModel =
            ViewModelProvider(requireActivity()).get(PermissionViewModel::class.java)
        permissionViewModel.isPermissionGranted.observe(viewLifecycleOwner) { isGranted ->
            if (isGranted) {
                Log.e(TAG, "onViewCreated: observer documents $isGranted")
                startObserving("0")
            }
        }

        manageAllFilesPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    permissionViewModel.setPermissionGranted(true)
                    startObserving("1")
                } else {
                    Log.e(TAG, "onViewCreated: 11")
                    showPermissionUi()
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                permissionViewModel.setPermissionGranted(true)
                startObserving("2")
            } else {
                Log.e(TAG, "onViewCreated: 22")
                showPermissionUi()
            }
        } else {
            if (checkPermission(requireContext())) {
                permissionViewModel.setPermissionGranted(true)
                startObserving("3")
            } else {
                Log.e(TAG, "onViewCreated: 33")
                showPermissionUi()
            }
        }
        binding.allowButton.setSafeOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (!Environment.isExternalStorageManager()) {
                    requestStoragePermission11(requireContext(), manageAllFilesPermissionLauncher)
                }
            } else {
                if (!checkPermission(requireContext())) {
                    requestPermissions(
                        arrayOf(
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        ), STORAGE_PERMISSION_CODE
                    )
                }
            }
        }
//        isAlive {
//            observeList()
//            initListener()
//        }
    }

    private fun startObserving(source: String) {
        Log.e(TAG, "startObserving from: $source")
        viewLifecycleOwner.lifecycleScope.launch {
            observeList()
            initListener()
            if (!isDataLoaded) {
                fetchDocuments()
            }
            binding.headerLayout.visibility = View.VISIBLE
            binding.rvDocs.visibility = View.VISIBLE
            binding.storagePermissionCardView.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        Log.e(TAG, "onResume: called isDataLoaded $isDataLoaded")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                permissionViewModel.setPermissionGranted(true)
                if (!isDataLoaded) {
                    startObserving("onResume-R")
                }
            } else {
                showPermissionUi()
            }
        } else {
            if (checkPermission(requireContext())) {
                permissionViewModel.setPermissionGranted(true)
                if (!isDataLoaded) {
                    startObserving("onResume-Legacy")
                }
            } else {
                showPermissionUi()
            }
        }
    }

    private fun showPermissionUi() {
        binding.headerLayout.visibility = View.GONE
        binding.rvDocs.visibility = View.GONE
        binding.storagePermissionCardView.visibility = View.VISIBLE
    }
//    override fun onResume() {
//        super.onResume()
//        if (!isDataLoaded) {
//            fetchDocuments()
//        }
//    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun fetchDocuments() {
        Log.d("fetch___", "fetchApps")
        lifecycleScope.launch {
            activity?.let { act ->
                viewModel.getDocs()
            }
        }
    }

    @SuppressLint("RepeatOnLifecycleWrongUsage")
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
            binding.dateTextView.text =
                "${getString(R.string.documents)} (${viewModel.documentsList.size})"
            setupRecyclerView(viewModel.documentsList)
            isDataLoaded = true

        } else {
            binding.progressBar.visible()
            binding.rvDocs.gone()
            binding.tvNoData.gone()
            binding.headerLayout.gone()

        }
    }

    private fun setupRecyclerView(list: List<MediaInfoModel>) {
        Log.d("setRecyclerView", "setupRecyclerView")
        if (list.isEmpty()) {
            binding.tvNoData.visible()
            binding.progressBar.gone()
            binding.rvDocs.gone()
            binding.headerLayout.gone()
        } else {

            adapter = DocumentsAdapter(list, this)
            binding.progressBar.gone()
            binding.tvNoData.gone()
            binding.rvDocs.visible()
            binding.headerLayout.visible()
            binding.rvDocs.setItemAnimator(null)
            binding.rvDocs.adapter = adapter
        }

    }

    private fun initListener() {
        binding.apply {
            checkboxSelectAll.setOnCheckedChangeListener { _, isChecked ->
                if (!checkboxSelectAll.isPressed) return@setOnCheckedChangeListener

                adapter?.selectAllMedia(isChecked, viewModel.documentsList, lifecycleScope) {
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
        binding.selectTv.text =
            if (isChecked) getString(R.string.de_select_all) else getString(R.string.select_all)
        binding.selectTv.setTextColor(resources.getColor(R.color.sub_heading_text_color, null))
    }

    override fun onMediaItemClickedForSelectAll() {
        onMediaItemClickCallback.onMediaItemClicked()
        val allSelected =
            SelectedListManager.getSelectedMediaList().containsAll(viewModel.documentsList)
        updateSelectAllState(allSelected)
        binding.checkboxSelectAll.isChecked = allSelected
    }
}