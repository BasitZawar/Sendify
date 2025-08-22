package com.smartswitch.presentation.sendData.photos

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
import com.smartswitch.databinding.FragmentPhotosSolBinding
import com.smartswitch.domain.model.MediaInfoModel
import com.smartswitch.utils.SelectedListManager
import com.smartswitch.utils.callback.OnMediaItemClickCallback
import com.smartswitch.utils.callback.OnMediaItemClickCallbackForDisplaying
import com.smartswitch.utils.callback.OnMediaItemClickCallbackForSelectAll
import com.smartswitch.utils.extensions.gone
import com.smartswitch.utils.extensions.isAlive
import com.smartswitch.utils.extensions.openFileFromRecyclerView
import com.smartswitch.utils.extensions.selectAllMedia
import com.smartswitch.utils.extensions.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


@AndroidEntryPoint
class PhotosSolFragment : Fragment(), OnMediaItemClickCallbackForSelectAll,OnMediaItemClickCallbackForDisplaying {
    private var _binding: FragmentPhotosSolBinding? = null
    private val binding get() = _binding!!

    private var isDataLoaded = false
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

        isAlive {
            observeList()
            initListener()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isDataLoaded) {
            fetchPhotos()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    private fun fetchPhotos() {
        Log.d("fetch___", "fetchPhotos")
        lifecycleScope.launch(Dispatchers.IO) {
            activity?.let { _ ->
                viewModel.getAllPhotos()
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

    private fun updateUi(isComplete: Boolean) {
        if (isComplete) {
            binding.dateTextView.text = "${getString(R.string.photos)} (${viewModel.photoList.size})"
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
            adapter = PhotosAdapter(list, this,this)
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
            val gridListSelectedColor = ContextCompat.getColor(requireContext(), R.color.grid_list_selected_color)

            updateLayoutToggle(gridListSelectedColor, gridListColor, gridLayoutManager, true)
            binding.gridView.setOnClickListener {
                updateLayoutToggle(gridListSelectedColor, gridListColor, gridLayoutManager, true)
            }

            binding.listView.setOnClickListener {
                updateLayoutToggle(gridListColor, gridListSelectedColor, linearLayoutManager, false)
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
                if (!binding.checkboxSelectAll.isPressed) return@setOnCheckedChangeListener
                adapter?.selectAllMedia(isChecked, viewModel.photoList, lifecycleScope){
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
        val allSelected = SelectedListManager.getSelectedMediaList().containsAll(viewModel.photoList )
        updateSelectAllState(allSelected)
        binding.checkboxSelectAll.isChecked = allSelected
    }

    override fun onMediaItemClickedForDisplaying(mediaInfoModel: MediaInfoModel) {
        // openFullViewWallpaper(mediaInfoModel.name.toString(), mediaInfoModel.uri.toString())

        requireContext().openFileFromRecyclerView(mediaInfoModel.uri.toString())


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