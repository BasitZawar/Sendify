package com.smartswitch.presentation.sendData.videos

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
import com.smartswitch.databinding.FragmentVideosSolBinding
import com.smartswitch.domain.model.MediaInfoModel
import com.smartswitch.presentation.sendData.photos.PhotosAdapter
import com.smartswitch.utils.Constant.STORAGE_PERMISSION_CODE
import com.smartswitch.utils.Constant.checkPermission
import com.smartswitch.utils.Constant.requestStoragePermission11
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


@AndroidEntryPoint
class VideosSolFragment : Fragment(), OnMediaItemClickCallbackForSelectAll,
    OnMediaItemClickCallbackForDisplaying {
    private var _binding: FragmentVideosSolBinding? = null
    private val binding get() = _binding!!
    private lateinit var permissionViewModel: PermissionViewModel
    private lateinit var manageAllFilesPermissionLauncher: ActivityResultLauncher<Intent>
    var TAG = "TESTTAG"
    private var isDataLoaded = false
    private val viewModel: VideosSolFragmentViewModel by viewModels()
    private var adapter: VideosAdapter? = null

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
        _binding = FragmentVideosSolBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        permissionViewModel =
            ViewModelProvider(requireActivity()).get(PermissionViewModel::class.java)
        permissionViewModel.isPermissionGranted.observe(viewLifecycleOwner) { isGranted ->
            if (isGranted) {
                Log.e(TAG, "onViewCreated: observer videos $isGranted")
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
                    binding.rvVideos.visibility = View.GONE
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
                binding.rvVideos.visibility = View.GONE
                binding.storagePermissionCardView.visibility = View.VISIBLE
            }
        } else {
            if (checkPermission(requireContext())) {
                permissionViewModel.setPermissionGranted(true)
                startObserving("3")
            } else {
                Log.e(TAG, "onViewCreated: 33")
                binding.headerLayout.visibility = View.GONE
                binding.rvVideos.visibility = View.GONE
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
                fetchVideos()
            }
            binding.headerLayout.visibility = View.VISIBLE
            binding.rvVideos.visibility = View.VISIBLE
            binding.storagePermissionCardView.visibility = View.GONE
        }
    }

  /*  override fun onResume() {
        super.onResume()
        Log.d("VideoRecyclerView___", "onResume: VideosFragment is resumed")
        if (!isDataLoaded) {
            fetchVideos()
        }

//        if (::fullVideoView.isInitialized){
//            fullVideoView.start()
//        }
    }*/
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
        binding.rvVideos.visibility = View.GONE
        binding.storagePermissionCardView.visibility = View.VISIBLE
    }

//    override fun onPause() {
//        super.onPause()
//        Log.d("VideoRecyclerView___", "onPause: VideosFragment is paused")
//        if (::fullVideoView.isInitialized){
//            fullVideoView.pause()
//        }
//    }

    private fun fetchVideos() {
        Log.d(TAG, "fetchVideos: Fetching videos")
        lifecycleScope.launch {   // 👉 stay on Main
            viewModel.getAllVideos()
            Log.d(TAG, "fetchVideos: getAllVideos() called")

        }
    }

    @SuppressLint("RepeatOnLifecycleWrongUsage")
    private fun observeList() {
        Log.e(TAG, "observeList: called", )
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isFetchingComplete.collect { isComplete ->
                    updateUi(isComplete)
                    Log.e(TAG, "observeList isComplete: $isComplete", )
                }
            }
        }
    }

    private fun updateUi(isComplete: Boolean) {
        if (isComplete) {
            binding.dateTextView.text =
                "${getString(R.string.videos)} (${viewModel.videoList.size})"
            setupRecyclerView(viewModel.videoList)
            Log.e(TAG, "updateUi videoList: ${viewModel.videoList.size}", )
            isDataLoaded = true
        } else {
            binding.progressBar.visible()
            binding.rvVideos.gone()
            binding.headerLayout.gone()
            binding.tvNoData.gone()
        }
    }

    private fun setupRecyclerView(list: List<MediaInfoModel>) {
        Log.d("VideoRecyclerView___", "setupRecyclerView")
        if (list.isEmpty()) {
            binding.tvNoData.visible()
            binding.progressBar.gone()
            binding.rvVideos.gone()
            binding.headerLayout.gone()
        } else {
            adapter = VideosAdapter(list, this, this)
            val gridLayoutManager = GridLayoutManager(context, 3)
            val linearLayoutManager = LinearLayoutManager(context)
            gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return when (adapter?.getItemViewType(position)) {
                        PhotosAdapter.VIEW_TYPE_DATE_HEADER -> 3
                        PhotosAdapter.VIEW_TYPE_PHOTO_ITEM -> 1
                        else -> 1
                    }
                }
            }
            binding.progressBar.gone()
            binding.tvNoData.gone()
            binding.rvVideos.visible()
            binding.headerLayout.visible()
            binding.rvVideos.adapter = adapter
            binding.rvVideos.setItemAnimator(null)


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
            showPermissionUi()
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
               showPermissionUi()
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

        binding.rvVideos.layoutManager = layoutManager
        adapter?.isGridLayout = isGridLayout
        adapter?.notifyDataSetChanged()
    }

    private fun initListener() {
        Log.d("VideoRecyclerView___", "initListener: videoFragment")
        //updateSelectAllState(false)
        binding.apply {
            checkboxSelectAll.setOnCheckedChangeListener { _, isChecked ->
                if (!checkboxSelectAll.isPressed) return@setOnCheckedChangeListener

                adapter?.selectAllMedia(isChecked, viewModel.videoList, lifecycleScope) {
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
            SelectedListManager.getSelectedMediaList().containsAll(viewModel.videoList)
        updateSelectAllState(allSelected)
        binding.checkboxSelectAll.isChecked = allSelected
    }

    override fun onMediaItemClickedForDisplaying(mediaInfoModel: MediaInfoModel) {
//        openFullViewLiveWallpaper(mediaInfoModel.uri.toString())
        requireContext().openFileFromRecyclerView(mediaInfoModel.uri.toString())
    }

//    private fun showVideo(videoUri: String) {
//        // Path to your video file
//        val videoFile = File(videoUri)
//
//        if (videoFile.exists()) {
//            // Use FileProvider to get a content URI
//            val videoContentUri: Uri = FileProvider.getUriForFile(
//                requireContext(),
//                "${requireContext().packageName}.fileprovider",
//                videoFile
//            )
//            Log.d("VideoRecyclerView___", "showVideo: ${requireContext().packageName}")
//
//            // Create an intent to play the video
//            val intent = Intent(Intent.ACTION_VIEW).apply {
//                setDataAndType(videoContentUri, "video/*") // Specify the MIME type as "video/*"
//                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // Grant permission to read the file
//            }
//
//            // Start the activity with the intent
//            startActivity(Intent.createChooser(intent, "Play video with"))
//        } else {
//            println("File not found: $videoUri")
//        }
//    }
//
//
//
//
//    private lateinit var fullVideoView: VideoView
//    private fun openFullViewLiveWallpaper(video: String) {
//        isAlive {activityContext ->
//            val dialog = Dialog(activityContext, android.R.style.Theme_Black_NoTitleBar)
//            dialog.setContentView(R.layout.full_view_video)
//
//            fullVideoView = dialog.findViewById(R.id.viewVideo)
//            val closeButton: ImageView = dialog.findViewById(com.smartswitch.R.id.closeButton)
//            //Glide.with(this).load(video).into(videoView)
//
//
//            val mediaController = MediaController(activityContext)
//            mediaController.setAnchorView(fullVideoView);
//            fullVideoView.setMediaController(mediaController);
//
//
//            // Set the video file URI
//            fullVideoView.setVideoURI(Uri.parse(video))
//
//            // Start playing the video
//            fullVideoView.start()
//
//            fullVideoView.setOnCompletionListener {
//                fullVideoView.start()
//            }
//
//            closeButton.setOnClickListener {
//                dialog.dismiss()
//                fullVideoView.pause()
//            }
//            dialog.show()
//        }
//    }


}