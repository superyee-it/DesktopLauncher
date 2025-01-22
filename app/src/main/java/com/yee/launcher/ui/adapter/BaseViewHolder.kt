package com.yee.launcher.ui.adapter

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

class BaseViewHolder<T : ViewBinding>(itemBinding: T) :
    RecyclerView.ViewHolder(itemBinding.root) {
    val binding: T = itemBinding
}

interface OnItemClickListener<T> {
    fun onItemClick(view: View, item: T)
}