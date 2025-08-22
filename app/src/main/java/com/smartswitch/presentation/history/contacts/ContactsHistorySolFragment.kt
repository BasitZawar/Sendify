package com.smartswitch.presentation.history.contacts

import android.os.Bundle
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
import com.smartswitch.databinding.FragmentContactsHistorySolBinding
import com.smartswitch.domain.model.HistoryViewModel
import com.smartswitch.domain.model.MediaInfoModel
import com.smartswitch.utils.SelectedListManagerForDeletion
import com.smartswitch.utils.callback.OnMediaItemClickCallbackForSelectAll
import com.smartswitch.utils.extensions.gone
import com.smartswitch.utils.extensions.visible
import kotlinx.coroutines.launch

class ContactsHistorySolFragment : Fragment(), OnMediaItemClickCallbackForSelectAll {
    private var _binding : FragmentContactsHistorySolBinding? =null
    private val binding get() = _binding!!

    private val contactHistoryViewModel: HistoryViewModel by activityViewModels()
    private lateinit var adapter: ContactsHistoryAdapter
    private var isSent = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentContactsHistorySolBinding.inflate(inflater,container,false)
        return  binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeSentAndReceivedHistory()
        initListeners()

        binding.progressBar.visible()
        binding.tvNoData.gone()
        binding.headerLayout.visible()
        binding.rvHistoryContacts.gone()
    }

    private fun setupRecyclerView() {
        adapter = ContactsHistoryAdapter(this)
        binding.rvHistoryContacts.layoutManager = LinearLayoutManager(context)
        binding.rvHistoryContacts.adapter = adapter
    }

    private fun observeSentAndReceivedHistory() {
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    contactHistoryViewModel.filteredContectssHistory.collect { sentHistoryList ->
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
                val currentContactList = contactHistoryViewModel.filteredContectssHistory.value
                if (isChecked) {
                    adapter.selectAllMedia(true, currentContactList, lifecycleScope)
                } else {
                    adapter.selectAllMedia(false, currentContactList, lifecycleScope)
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
            binding.rvHistoryContacts.gone()
            binding.tvNoData.visible()
            binding.headerLayout.gone()
        } else {
            binding.rvHistoryContacts.visible()
            binding.tvNoData.gone()
            binding.headerLayout.visible()
            adapter.updateData(historyList)
        }
    }

    override fun onMediaItemClickedForSelectAll() {
        lifecycleScope.launch {
            val currentContactList = contactHistoryViewModel.filteredContectssHistory.value
            val allSelected = currentContactList.all { SelectedListManagerForDeletion.isItemSelected(it) }

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