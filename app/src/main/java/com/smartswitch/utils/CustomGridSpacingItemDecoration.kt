package com.smartswitch.utils

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class CustomGridSpacingItemDecoration(
    private val spanCount: Int,
    private val spacing: Int,
    private val includeEdge: Boolean
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view) // item position
        val column = position % spanCount // item column

        if (includeEdge) {
            // Add spacing to the left and right
            outRect.left = spacing - column * spacing / spanCount
            outRect.right = (column + 1) * spacing / spanCount

            // Add top spacing for the first row
            if (position < spanCount) {
                outRect.top = spacing
            }
            // Add bottom spacing
            outRect.bottom = spacing
        } else {
            // Add spacing to the left and right
            outRect.left = column * spacing / spanCount
            outRect.right = spacing - (column + 1) * spacing / spanCount

            // Add top spacing for items beyond the first row
            if (position >= spanCount) {
                outRect.top = spacing
            }
        }
    }
}
