package com.smartswitch.presentation.history

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import com.smartswitch.interfaces.OnFileClick
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.smartswitch.R
import com.smartswitch.databinding.HistoryFragmentBinding
import com.smartswitch.presentation.adapter.HistoryFilesAdapter
import com.smartswitch.utils.Constant
import com.smartswitch.utils.PaperDB
import com.smartswitch.utils.formatLength
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class HistoryFragment : Fragment() {

    var binding: HistoryFragmentBinding? = null
    lateinit var adapter: HistoryFilesAdapter
    var receivedFiles: ArrayList<HistoryModel> = ArrayList()
    var sentFiles: ArrayList<HistoryModel> = ArrayList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = HistoryFragmentBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.tabLayout?.visibility = View.VISIBLE


        binding?.tabLayout?.getTabAt(0)?.text = getString(R.string.received).lowercase()
        binding?.tabLayout?.getTabAt(1)?.text = getString(R.string.sent).lowercase()


        CoroutineScope(Dispatchers.IO).launch {
            try {
                PaperDB.readHistory().apply {
                    this.map {
                        it.path?.let { path ->
                            File(path).let { file ->
                                HistoryModel(file.name, file.length().formatLength(), path)
                            }
                        }
                    }.apply {
                        this.filterNotNull().apply {
                            receivedFiles.clear()
                            receivedFiles.addAll(this)
                        }
                    }
                }
            } catch (e: Exception) {
            }
            try {
                PaperDB.readSendHistory().apply {
                    this.map {
                        it.path?.let { path ->
                            File(path).let { file ->
                                HistoryModel(file.name, file.length().formatLength(), path)
                            }
                        }
                    }.apply {
                        this.filterNotNull().apply {
                            sentFiles.clear()
                            sentFiles.addAll(this)

                        }
                    }
                }
            } catch (e: Exception) {
            }

            withContext(Dispatchers.Main) {
                context?.let {
//                    binding?.tabLayout?.tabRippleColor = null
                    if (receivedFiles.isEmpty()) {
                        binding?.rvReceivedFiles?.visibility = View.GONE
                        binding?.noItemFound?.visibility = View.VISIBLE
                        if (binding?.noItemFound!!.isVisible) {
                            binding?.constraintLoader?.visibility = View.GONE
                        }

                    } else {
                        binding?.rvReceivedFiles?.visibility = View.VISIBLE
                        binding?.noItemFound?.visibility = View.GONE
                        binding?.constraintLoader?.visibility = View.GONE
                    }

                    binding?.rvReceivedFiles?.layoutManager =
                        LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                    adapter = HistoryFilesAdapter(receivedFiles, it, object : OnFileClick {
                        override fun onFileClick(pos: Int) {
                        }

                        override fun onFileCheckUncheck(position: Int, selectedOrNot: Boolean) {
                        }

                        override fun onHistoryClick(pos: Int, type: Int) {
                            Log.e("TESTTAG", "TYPE CLICKED onHistoryClick $type")
                            if (type == 0) {
                                val authority = "${requireContext().packageName}.provider"
                                val file = File(receivedFiles[pos].path)
                                val imageUri: Uri =
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                        FileProvider.getUriForFile(
                                            requireContext(), authority, file
                                        )
                                    } else {
                                        Uri.fromFile(File(receivedFiles[pos].path))
                                    }
                                view(
                                    requireContext(),
                                    imageUri,
                                    DocumentFile.fromFile(File(receivedFiles[pos].path)).type
                                )
                            } else {
                                val authority = "${requireContext().packageName}.provider"
                                var imageUri: Uri? = null
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    try {
                                        imageUri = FileProvider.getUriForFile(
                                            requireContext(), authority, File(sentFiles[pos].path)
                                        )
                                    } catch (e: Exception) {
                                        Constant.showSnackBar(
                                            requireActivity(),
                                            "Already installed"
                                        )
                                    }

                                } else {
                                    imageUri = Uri.fromFile(File(sentFiles[pos].path))
                                }
                                imageUri?.let {
                                    view(
                                        requireContext(),
                                        it,
                                        DocumentFile.fromFile(File(sentFiles[pos].path)).type
                                    )
                                }
                            }
                        }
                    })
                    binding?.rvReceivedFiles?.adapter = adapter
                    binding?.tabLayout?.addOnTabSelectedListener(object :
                        TabLayout.OnTabSelectedListener {
                        override fun onTabSelected(tab: TabLayout.Tab) {
                            when (tab.position) {
                                0 -> {
                                    if (receivedFiles.isEmpty()) {
                                        binding?.rvReceivedFiles?.visibility = View.GONE
                                        binding?.noItemFound?.visibility = View.VISIBLE
                                        if (binding?.noItemFound!!.isVisible) {
                                            binding?.constraintLoader?.visibility = View.GONE
                                        } else {
                                            binding?.constraintLoader?.visibility = View.VISIBLE
                                        }
                                    } else {
                                        binding?.rvReceivedFiles?.visibility = View.VISIBLE
                                        binding?.noItemFound?.visibility = View.GONE
                                        binding?.constraintLoader?.visibility = View.GONE
                                    }
                                    adapter.updateList(receivedFiles, 0)
                                    return
                                }

                                else -> {
                                    if (sentFiles.isEmpty()) {
                                        binding?.rvReceivedFiles?.visibility = View.GONE
                                        binding?.noItemFound?.visibility = View.VISIBLE
                                        if (binding?.noItemFound!!.isVisible) {
                                            binding?.constraintLoader?.visibility = View.GONE
                                        } else {
                                            binding?.constraintLoader?.visibility = View.VISIBLE
                                        }
                                    } else {
                                        binding?.rvReceivedFiles?.visibility = View.VISIBLE
                                        binding?.noItemFound?.visibility = View.GONE
                                        binding?.constraintLoader?.visibility = View.GONE
                                    }
                                    adapter.updateList(sentFiles, 1)
                                    return
                                }
                            }
                        }

                        override fun onTabUnselected(
                            tab: TabLayout.Tab?
                        ) {

                        }

                        override fun onTabReselected(
                            tab: TabLayout.Tab?
                        ) {

                        }

                    })
                }

            }
        }
    }

    class HistoryModel(val title: String, val size: String, val path: String)

    fun view(context: Context, uri: Uri?, type: String?) {
        try {
            if (Build.VERSION.SDK_INT >= 24) {
                try {
                    val m = StrictMode::class.java.getMethod("disableDeathOnFileUriExposure")
                    m.invoke(null)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            context.startActivity(
                Intent(Intent.ACTION_VIEW).setDataAndType(uri, type)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            )
        } catch (e: ActivityNotFoundException) {
            Constant.showSnackBar(
                requireActivity(),
                "Content not found"
            )
        } catch (e: SecurityException) {
            Constant.showSnackBar(
                requireActivity(),
                "Content not found"
            )
        } catch (e: Exception) {
            Constant.showSnackBar(
                requireActivity(),
                "Content not found"
            )
        }
    }
}