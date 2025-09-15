package com.smartswitch.interfaces

interface OnFileClick {
    fun onFileClick(pos:Int)
    fun onFileCheckUncheck(position:Int, selectedOrNot: Boolean)
    fun onHistoryClick(pos: Int,type:Int)
}