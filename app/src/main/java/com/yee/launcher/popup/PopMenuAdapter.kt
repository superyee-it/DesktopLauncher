package com.yee.launcher.popup

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.yee.launcher.data.model.MenuBean
import com.yee.launcher.databinding.ItemPopupMenuBinding
import com.yee.launcher.ui.adapter.BaseViewHolder
import com.yee.launcher.ui.adapter.OnItemClickListener

class PopMenuAdapter(private var listener: OnItemClickListener<MenuBean>? = null) : RecyclerView.Adapter<BaseViewHolder<ItemPopupMenuBinding>>() {

    var mData: List<MenuBean>? = null
    var hoverListener: OnItemClickListener<MenuBean>? = null

    override fun onCreateViewHolder(
        parent: ViewGroup, viewType: Int
    ): BaseViewHolder<ItemPopupMenuBinding> {
        val viewHolder = BaseViewHolder(ItemPopupMenuBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        viewHolder.itemView.setOnClickListener { v ->
            val position = viewHolder.bindingAdapterPosition
            val item = mData?.get(position)
            item?.let {
                listener?.onItemClick(v, it)
            }
        }
        viewHolder.itemView.setOnHoverListener { v, event ->
            if (event.action == MotionEvent.ACTION_HOVER_ENTER) {
                val position = viewHolder.bindingAdapterPosition
                val item = mData?.get(position)
                item?.let {
                    hoverListener?.onItemClick(v, it)
                }
            }
            return@setOnHoverListener false
        }
        return viewHolder
    }

    override fun getItemCount(): Int {
        return mData?.size ?: 0
    }

    override fun onBindViewHolder(holder: BaseViewHolder<ItemPopupMenuBinding>, position: Int) {
        val item = mData?.get(position)
        holder.binding.textView.text = item?.title
        if (item?.leftIcon == 0) {
            holder.binding.ivIcon.visibility = RecyclerView.GONE
        } else {
            holder.binding.ivIcon.visibility = RecyclerView.VISIBLE
        }
        holder.binding.ivIcon.setImageResource(item?.leftIcon ?: 0)
        holder.binding.textView.setCompoundDrawablesWithIntrinsicBounds(
            0, 0, item?.rightIcon ?: 0, 0
        )
        holder.binding.divider.visibility = if (item?.showDivider == true) View.VISIBLE else View.GONE
    }

}