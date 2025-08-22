package com.smartswitch.utils.extensions

import android.annotation.SuppressLint
import android.util.Log
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.RecyclerView
import com.smartswitch.domain.model.MediaInfoModel
import com.smartswitch.utils.SelectedListManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("NotifyDataSetChanged")
fun <T : RecyclerView.ViewHolder> RecyclerView.Adapter<T>.selectAllMedia(
    flag: Boolean, list: List<MediaInfoModel>, lifecycleCoroutineScope: LifecycleCoroutineScope, listener :() -> Unit ={}
) {
    lifecycleCoroutineScope.launch(Dispatchers.Default) {
        if (flag) {
            if (SelectedListManager.getSelectedMediaList().containsAll(list)) {
                Log.i("selectAllMedia", "selectAllMedia:already contains ${list.size} ")
                return@launch
            } else {
                Log.i("selectAllMedia", "selectAllMedia:adding all ${list.size} ")
                SelectedListManager.addAllSelectedMedia(list)
            }
        } else {
            Log.i("selectAllMedia", "selectAllMedia:removing all ${list.size} ")
            SelectedListManager.removeAllSelectedMedia(list)
        }
        withContext(Dispatchers.Main) {
            notifyDataSetChanged()
            listener.invoke()
        }
    }
}

@SuppressLint("NotifyDataSetChanged")
fun <T : RecyclerView.ViewHolder> RecyclerView.Adapter<T>.selectAllContacts(
    flag: Boolean, list: List<MediaInfoModel>, lifecycleCoroutineScope: LifecycleCoroutineScope, listener :() -> Unit ={}
) {
    lifecycleCoroutineScope.launch(Dispatchers.Default) {
        if (flag) {
            if (SelectedListManager.getSelectedContactsList().containsAll(list)) {
                return@launch
            } else {
                SelectedListManager.addAllSelectedContacts(list)
            }
        } else {
            SelectedListManager.removeAllSelectedContacts(list)
        }
        withContext(Dispatchers.Main) {
            notifyDataSetChanged()
            listener.invoke()
        }
    }
}