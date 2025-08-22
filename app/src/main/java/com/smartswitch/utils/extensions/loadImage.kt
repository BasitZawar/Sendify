 package com.smartswitch.utils.extensions

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.imageview.ShapeableImageView


fun ShapeableImageView.loadImage(context: Context,uri: Any?){
    val requestOptions = RequestOptions()
//        .placeholder(R.drawable.ic_placeholder) // Placeholder image
//        .error(R.drawable.ic_error) // Error image
        .centerCrop() // Crop the image to fill the ImageView
//        .override(150, 150) // Resize the image to match ImageView size
        .diskCacheStrategy(DiskCacheStrategy.ALL)

    Glide.with(context)
        .load(uri)
        .apply(requestOptions)
        .into(this)
}


 fun ShapeableImageView.loadVideoThumbnail(context: Context, videoUri: Uri) {
     val retriever = MediaMetadataRetriever()
     try {
         retriever.setDataSource(context, videoUri)
         val bitmap = retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC) // Get the first frame as a thumbnail
         this.setImageBitmap(bitmap)
     } catch (e: Exception) {
         e.printStackTrace()
//         this.setImageResource(R.drawable.ic_error) // Set error image if loading fails
     } finally {
         retriever.release()
     }
 }



fun ShapeableImageView.loadImage30by30(context: Context,uri: Any?){
    val requestOptions = RequestOptions()
//        .placeholder(R.drawable.ic_placeholder) // Placeholder image
//        .error(R.drawable.ic_error) // Error image
        .centerCrop() // Crop the image to fill the ImageView
        .override(context.dpToPx(30).toInt(), context.dpToPx(30).toInt()) // Resize the image to match ImageView size
        .diskCacheStrategy(DiskCacheStrategy.ALL)

    Glide.with(context)
        .load(uri)
        .apply(requestOptions)
        .into(this)
}



fun Context.dpToPx(dp: Int): Float {
    return dp * resources.displayMetrics.density
}

