package com.example.doodle.remote.channel

import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager

class ItemDragHelperCallback : ItemTouchHelper.Callback() {
    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        val dragFlags: Int
        val manager = recyclerView.layoutManager
        dragFlags = if (manager is GridLayoutManager || manager is StaggeredGridLayoutManager) {
            ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        } else {
            ItemTouchHelper.UP or ItemTouchHelper.DOWN
        }
        val swipeFlags = 0
        return makeMovementFlags(dragFlags, swipeFlags)
    }

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        if (viewHolder.itemViewType != target.itemViewType) {
            return false
        }
        if (recyclerView.adapter is OnItemMoveListener) {
            val listener = recyclerView.adapter as OnItemMoveListener?
            listener!!.onItemMove(viewHolder.adapterPosition, target.adapterPosition)
        }
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
            if (viewHolder is OnDragVHListener) {
                val itemViewHolder = viewHolder as OnDragVHListener
                itemViewHolder.onItemSelected()
            }
        }
        super.onSelectedChanged(viewHolder, actionState)
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        if (viewHolder is OnDragVHListener) {
            val itemViewHolder = viewHolder as OnDragVHListener
            itemViewHolder.onItemFinish()
        }
        super.clearView(recyclerView, viewHolder)
    }

    override fun isLongPressDragEnabled(): Boolean {
        return false
    }

    override fun isItemViewSwipeEnabled(): Boolean {
        return false
    }
}
