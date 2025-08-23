package com.smartswitch.presentation.sendData.audios

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
import com.smartswitch.databinding.FragmentAudiosSolBinding
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
class AudiosSolFragment : Fragment(), OnMediaItemClickCallbackForSelectAll ,OnMediaItemClickCallbackForDisplaying{
    private var _binding: FragmentAudiosSolBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AudiosSolFragmentViewModel by viewModels()

    private var isDataLoaded = false
    var adapter: AudiosAdapter? = null

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
        _binding = FragmentAudiosSolBinding.inflate(inflater, container, false)
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
            fetchAudios()
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun fetchAudios() {
        Log.d("fetch___", "fetchApps")
        lifecycleScope.launch(Dispatchers.IO) {
            activity?.let { act ->
                viewModel.getAudios()
            }
        }
    }

    private fun observeList() {
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isFetchingComplete.collect { isComplete ->
                    Log.d("refresh_behav","observelist isComplete : $isComplete")
                    updateUi(isComplete)
                }
            }
        }
    }

    private fun updateUi(isComplete: Boolean) {
        if (isComplete) {
            binding.dateTextView.text = "${getString(R.string.audio)} (${viewModel.audioList.size})"
            setupRecyclerView(viewModel.audioList)
            isDataLoaded = true

        } else {
            binding.progressBar.visible()
            binding.rvAudios.gone()
            binding.tvNoData.gone()
            binding.headerLayout.gone()

        }
    }

    private fun setupRecyclerView(list: List<MediaInfoModel>) {
        Log.v("refresh_behav","setupRecyclerView")
        if (list.isEmpty()) {
            binding.tvNoData.visible()
            binding.rvAudios.gone()
            binding.progressBar.gone()
            binding.headerLayout.gone()

        } else {
            adapter = AudiosAdapter(list, this,this)
            binding.progressBar.gone()
            binding.tvNoData.gone()
            binding.rvAudios.visible()
            binding.headerLayout.visible()
            binding.rvAudios.setItemAnimator(null)
            binding.rvAudios.adapter = adapter
        }

    }

    private fun initListener() {
        updateSelectAllState(false)
        binding.apply {
            checkboxSelectAll.setOnCheckedChangeListener { _, isChecked ->
                if (!checkboxSelectAll.isPressed) return@setOnCheckedChangeListener

                adapter?.selectAllMedia(isChecked, viewModel.audioList, lifecycleScope){
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
        val allSelected = SelectedListManager.getSelectedMediaList().containsAll(viewModel.audioList )
        updateSelectAllState(allSelected)
        binding.checkboxSelectAll.isChecked = allSelected
    }

    override fun onMediaItemClickedForDisplaying(mediaInfoModel: MediaInfoModel) {
        // openFullViewLiveWallpaper(mediaInfoModel.uri.toString())
        //ActivitiesSmart.view(requireContext(), mediaInfoModel)
        //playAudio(mediaInfoModel.uri.toString())
        requireContext().openFileFromRecyclerView(mediaInfoModel.uri.toString())
    }
//
//    private fun playAudio(audioUri: String) {
//        // Path to your audio file
//        val audioFile = File(audioUri)
//
//        if (audioFile.exists()) {
//            // Use FileProvider to get a content URI
//            val audioContentUri: Uri = FileProvider.getUriForFile(
//                requireContext(),
//                "${requireContext().packageName}.fileprovider",
//                audioFile
//            )
//            Log.d("AudioPlayer___", "playAudio: ${requireContext().packageName}")
//
//            // Create an intent to play the audio
//            val intent = Intent(Intent.ACTION_VIEW).apply {
//                setDataAndType(audioContentUri, "audio/*") // Specify the MIME type as "audio/*"
//                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // Grant permission to read the file
//            }
//
//            // Start the activity with the intent
//            startActivity(Intent.createChooser(intent, "Play audio with"))
//        } else {
//            println("File not found: $audioUri")
//        }
//    }
//
//
//
//    private lateinit var mediaPlayer: MediaPlayer
//    private lateinit var playPauseButton: ImageView
//    private lateinit var seekBar: SeekBar
//    private var isPlaying = false
//    private var updateSeekBarRunnable: Runnable? = null
//    private val handler = Handler(Looper.getMainLooper())
//
//    private fun openFullViewLiveWallpaper(audio: String) {
//        isAlive { activityContext ->
//            val dialog = Dialog(activityContext)
//            dialog.setContentView(R.layout.audio_player_dialog)
//
//            playPauseButton = dialog.findViewById(R.id.playPauseButton)
//            seekBar = dialog.findViewById(R.id.audioSeekBar)
//            val closeButton: ImageView = dialog.findViewById(R.id.closeButton)
//
//            mediaPlayer = MediaPlayer().apply {
//                setDataSource(activityContext, Uri.parse(audio))
//                prepare()
//            }
//
//            // Set seek bar max value to audio duration
//            seekBar.max = mediaPlayer.duration
//
//            // Play/pause button functionality
//            playPauseButton.setOnClickListener {
//                if (isPlaying) {
//                    pauseAudio()
//                } else {
//                    playAudio()
//                }
//            }
//
//            // Update the seek bar as audio plays
//            updateSeekBarRunnable = Runnable {
//                if (mediaPlayer.isPlaying) {
//                    seekBar.progress = mediaPlayer.currentPosition
//                    handler.postDelayed(updateSeekBarRunnable!!, 100)
//                }
//            }
//
//            // Update MediaPlayer's position when the seek bar is adjusted
//            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
//                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
//                    if (fromUser) {
//                        mediaPlayer.seekTo(progress)
//                    }
//                }
//
//                override fun onStartTrackingTouch(seekBar: SeekBar) {}
//                override fun onStopTrackingTouch(seekBar: SeekBar) {}
//            })
//
//            // Dismiss dialog and release resources on close button click
//            closeButton.setOnClickListener {
//                stopAudio()
//                dialog.dismiss()
//            }
//
//            dialog.setOnDismissListener {
//                stopAudio()
//            }
//
//            dialog.show()
//        }
//    }
//
//    private fun playAudio() {
//        mediaPlayer.start()
//        isPlaying = true
//        playPauseButton.setImageResource(R.drawable.pause) // Replace with your pause icon
//        handler.post(updateSeekBarRunnable!!)
//    }
//
//    private fun pauseAudio() {
//        mediaPlayer.pause()
//        isPlaying = false
//        playPauseButton.setImageResource(R.drawable.ic_play) // Replace with your play icon
//        handler.removeCallbacks(updateSeekBarRunnable!!)
//    }
//
//    private fun stopAudio() {
//        try {
//            if (::mediaPlayer.isInitialized && mediaPlayer.isPlaying) {
//                mediaPlayer.stop()
//                mediaPlayer.release()
//                handler.removeCallbacks(updateSeekBarRunnable!!)
//                isPlaying = false
//            } else if (::mediaPlayer.isInitialized) {
//                mediaPlayer.release()
//            }
//        } catch (e: IllegalStateException) {
//            e.printStackTrace()
//        }
//    }
//

}