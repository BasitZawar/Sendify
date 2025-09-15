package com.smartswitch.presentation.sendData.photos

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.smartswitch.R
import com.smartswitch.databinding.FragmentPhotosSolBinding
import com.smartswitch.domain.model.MediaInfoModel
import com.smartswitch.utils.Constant.STORAGE_PERMISSION_CODE
import com.smartswitch.utils.Constant.checkPermission
import com.smartswitch.utils.Constant.requestStoragePermission11
import com.smartswitch.utils.PermissionManager
import com.smartswitch.utils.PermissionViewModel
import com.smartswitch.utils.SelectedListManager
import com.smartswitch.utils.callback.OnMediaItemClickCallback
import com.smartswitch.utils.callback.OnMediaItemClickCallbackForDisplaying
import com.smartswitch.utils.callback.OnMediaItemClickCallbackForSelectAll
import com.smartswitch.utils.extensions.gone
import com.smartswitch.utils.extensions.isAlive
import com.smartswitch.utils.extensions.openFileFromRecyclerView
import com.smartswitch.utils.extensions.selectAllMedia
import com.smartswitch.utils.extensions.setSafeOnClickListener
import com.smartswitch.utils.extensions.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@AndroidEntryPoint
class PhotosSolFragment : Fragment(), OnMediaItemClickCallbackForSelectAll,
    OnMediaItemClickCallbackForDisplaying {
    private var _binding: FragmentPhotosSolBinding? = null
    private val binding get() = _binding!!
    private lateinit var permissionViewModel: PermissionViewModel
    private lateinit var manageAllFilesPermissionLauncher: ActivityResultLauncher<Intent>
    private var isDataLoaded = false
    var TAG = "TESTTAG"
    private val viewModel: PhotosSolFragmentViewModel by viewModels()
    private var adapter: PhotosAdapter? = null

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
        _binding = FragmentPhotosSolBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        permissionViewModel =
            ViewModelProvider(requireActivity()).get(PermissionViewModel::class.java)
        permissionViewModel.isPermissionGranted.observe(viewLifecycleOwner) { isGranted ->
            if (isGranted) {
                Log.e(TAG, "onViewCreated: observer Images $isGranted")
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
                    binding.headerLayout.visibility = View.GONE
                    binding.rvPhotos.visibility = View.GONE
                    binding.storagePermissionCardView.visibility = View.VISIBLE

                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                permissionViewModel.setPermissionGranted(true)
                startObserving("2")
            } else {
                Log.e(TAG, "onViewCreated: 22")
                binding.headerLayout.visibility = View.GONE
                binding.rvPhotos.visibility = View.GONE
                binding.storagePermissionCardView.visibility = View.VISIBLE
            }
        } else {
            if (checkPermission(requireContext())) {
                permissionViewModel.setPermissionGranted(true)
                startObserving("3")
            } else {
                Log.e(TAG, "onViewCreated: 33")
                binding.headerLayout.visibility = View.GONE
                binding.rvPhotos.visibility = View.GONE
                binding.storagePermissionCardView.visibility = View.VISIBLE
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
    }

    //    override fun onResume() {
//        super.onResume()
//        Log.e(TAG, "onResume: called isDataLoaded $isDataLoaded")
//        if (!isDataLoaded) {
//            fetchPhotos()
//        }
//    }
    override fun onResume() {
        super.onResume()
        Log.e(TAG, "onResume: called isDataLoaded $isDataLoaded")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                if (!isDataLoaded) {
                    permissionViewModel.setPermissionGranted(true)
                    startObserving("onResume-R")
                }
            } else {
                binding.headerLayout.visibility = View.GONE
                binding.rvPhotos.visibility = View.GONE
                binding.storagePermissionCardView.visibility = View.VISIBLE
            }
        } else {
            if (checkPermission(requireContext())) {
                if (!isDataLoaded) {
                    permissionViewModel.setPermissionGranted(true)
                    startObserving("onResume-Legacy")
                }
            } else {
                binding.headerLayout.visibility = View.GONE
                binding.rvPhotos.visibility = View.GONE
                binding.storagePermissionCardView.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun fetchPhotos() {
        Log.d(TAG, "fetchPhotos called")
        lifecycleScope.launch {   // 👉 stay on Main
            viewModel.getAllPhotos()
        }
    }

    @SuppressLint("RepeatOnLifecycleWrongUsage")
    private fun observeList() {
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isFetchingComplete.collect { isComplete ->
                    updateUi(isComplete)
                }
            }
        }
    }

    private fun updateUi(isComplete: Boolean) {
        if (isComplete) {
            binding.dateTextView.text =
                "${getString(R.string.photos)} (${viewModel.photoList.size})"
            setupRecyclerView(viewModel.photoList)
            isDataLoaded = true
        } else {
            binding.progressBar.visible()
            binding.rvPhotos.gone()
            binding.tvNoData.gone()
            binding.headerLayout.gone()
        }
    }

    private fun setupRecyclerView(list: List<MediaInfoModel>) {
        Log.d("PhotoRecyclerView___", "setupRecyclerView:photoShowed")
        if (list.isEmpty()) {
            binding.tvNoData.visible()
            binding.rvPhotos.gone()
            binding.progressBar.gone()
            binding.headerLayout.gone()
        } else {
            adapter = PhotosAdapter(list, this, this)
            val gridLayoutManager = GridLayoutManager(context, 3) // 3 columns for photos
            val linearLayoutManager = LinearLayoutManager(context)
            gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return when (adapter?.getItemViewType(position)) {
                        PhotosAdapter.VIEW_TYPE_DATE_HEADER -> 3 // Span across all 3 columns
                        PhotosAdapter.VIEW_TYPE_PHOTO_ITEM -> 1  // Each photo takes 1 column
                        else -> 1
                    }
                }
            }
            binding.progressBar.gone()
            binding.tvNoData.gone()
            binding.headerLayout.visible()
            binding.rvPhotos.visible()
            binding.rvPhotos.setItemAnimator(null)
            binding.rvPhotos.adapter = adapter

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

    @Deprecated("Deprecated in Java")
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (Environment.isExternalStorageManager()) {
            permissionViewModel.setPermissionGranted(true)
            startObserving("5")
        } else {
            Log.e(TAG, "onActivityResult: 55")
            binding.headerLayout.visibility = View.GONE
            binding.rvPhotos.visibility = View.GONE
            binding.storagePermissionCardView.visibility = View.VISIBLE
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                permissionViewModel.setPermissionGranted(true)
                startObserving("4")
            } else {
                Log.e(TAG, "onRequestPermissionsResult: 44")
                binding.headerLayout.visibility = View.GONE
                binding.rvPhotos.visibility = View.GONE
                binding.storagePermissionCardView.visibility = View.VISIBLE
            }
        }
    }

    private fun updateLayoutToggle(
        gridColor: Int,
        listColor: Int,
        layoutManager: RecyclerView.LayoutManager,
        isGridLayout: Boolean,
    ) {
        binding.gridView.backgroundTintList = ColorStateList.valueOf(gridColor)
        binding.listView.backgroundTintList = ColorStateList.valueOf(listColor)

        binding.rvPhotos.layoutManager = layoutManager
        adapter?.isGridLayout = isGridLayout
        adapter?.notifyDataSetChanged()
    }

    private fun initListener() {
        Log.d("PhotoRecyclerView___", "initListener: photoFragment")
        updateSelectAllState(false)
        binding.apply {
            checkboxSelectAll.setOnCheckedChangeListener { _, isChecked ->
                if (!checkboxSelectAll.isPressed) {
                    return@setOnCheckedChangeListener
                }
                adapter?.selectAllMedia(isChecked, viewModel.photoList, lifecycleScope) {
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
            SelectedListManager.getSelectedMediaList().containsAll(viewModel.photoList)
        updateSelectAllState(allSelected)
        binding.checkboxSelectAll.isChecked = allSelected
    }

    override fun onMediaItemClickedForDisplaying(mediaInfoModel: MediaInfoModel) {
        // openFullViewWallpaper(mediaInfoModel.name.toString(), mediaInfoModel.uri.toString())
        requireContext().openFileFromRecyclerView(mediaInfoModel.uri.toString())
    }

    private fun startObserving(source: String) {
        Log.e(TAG, "startObserving from: $source")

        viewLifecycleOwner.lifecycleScope.launch {
            // Always start observing first
            observeList()
            initListener()

            // Then trigger fetching (if not already loaded)
            if (!isDataLoaded) {
                fetchPhotos()
            }

            // UI updates must run on Main
            binding.headerLayout.visibility = View.VISIBLE
            binding.rvPhotos.visibility = View.VISIBLE
            binding.storagePermissionCardView.visibility = View.GONE
        }
    }


//    private fun showPhoto(photoUri: String) {
//        // Path to your image file
//        val imageFile = File(photoUri)
//
//        if (imageFile.exists()) {
//            // Use FileProvider to get a content URI
//            val imageUri: Uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", imageFile)
//            Log.d("PhotoRecyclerView___", "showPhoto: ${requireContext().packageName}")
//
//            // Create an intent to open the image
//            val intent = Intent(Intent.ACTION_VIEW).apply {
//                setDataAndType(imageUri, "image/*") // Specify the MIME type as "image/*"
//                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // Grant permission to read the file
//            }
//
//            // Start the activity with the intent
//            startActivity(Intent.createChooser(intent, "Open image with"))
//        } else {
//            println("File not found: $photoUri")
//        }
//    }
//    private fun openFullViewWallpaper(name: String ,image: String) {
//        isAlive { activityContext ->
//            val dialog = Dialog(activityContext, android.R.style.Theme_Black_NoTitleBar)
//            dialog.setContentView(R.layout.full_view_photos)
//            dialog.show()
//
//            val imageView: ImageView = dialog.findViewById(R.id.fullViewImage)
//            val textView: TextView = dialog.findViewById(R.id.name)
//            val closeButton: ImageView = dialog.findViewById(R.id.closeButton)
//
//            textView.text = name
//            Glide.with(activityContext).load(image).into(imageView)
//            closeButton.setOnClickListener {
//                dialog.dismiss()
//            }
//        }
//    }
}