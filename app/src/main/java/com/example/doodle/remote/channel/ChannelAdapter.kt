package com.example.doodle.remote.channel

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.MotionEventCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.doodle.R

class ChannelAdapter(
    context: Context?, helper: ItemTouchHelper,
    myChannelItems: MutableList<Channel>,
    otherChannelItems: MutableList<Channel>,
    listener: FinishEditListener?
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), OnItemMoveListener {

    companion object {
        const val TYPE_MY_CHANNEL_HEADER = 0

        const val TYPE_MY = 1

        const val TYPE_OTHER_CHANNEL_HEADER = 2

        const val TYPE_OTHER = 3

        private const val COUNT_PRE_MY_HEADER = 1

        private const val COUNT_PRE_OTHER_HEADER = COUNT_PRE_MY_HEADER + 1
        private const val ANIM_TIME = 360L

        private const val SPACE_TIME: Long = 100
    }

    private var startTime: Long = 0
    private val mInflater: LayoutInflater
    private val mItemTouchHelper: ItemTouchHelper
    private val mMyChannelItems: MutableList<Channel>
    private val mOtherChannelItems: MutableList<Channel>
    private val mFinishEditListener: FinishEditListener?

    init {
        mInflater = LayoutInflater.from(context)
        mItemTouchHelper = helper
        mMyChannelItems = myChannelItems
        mOtherChannelItems = otherChannelItems
        mFinishEditListener = listener
    }

    interface FinishEditListener {
        fun onFinishEdit()
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) {
            TYPE_MY_CHANNEL_HEADER
        } else if (position == mMyChannelItems.size + 1) {
            TYPE_OTHER_CHANNEL_HEADER
        } else if (position > 0 && position < mMyChannelItems.size + 1) {
            TYPE_MY
        } else {
            TYPE_OTHER
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view: View
        when (viewType) {
            TYPE_MY_CHANNEL_HEADER -> {
                view = mInflater.inflate(R.layout.item_my_channel_header, parent, false)
                return MyChannelHeaderViewHolder(view)
            }
            TYPE_MY -> {
                view = mInflater.inflate(R.layout.item_my, parent, false)
                val myHolder = MyViewHolder(view)
                myHolder.textView.setOnClickListener {
                    val position = myHolder.adapterPosition
                    val recyclerView = parent as RecyclerView
                    val targetView = recyclerView.layoutManager!!.findViewByPosition(mMyChannelItems.size + COUNT_PRE_OTHER_HEADER)
                    val currentView = recyclerView.layoutManager!!.findViewByPosition(position)
                    if (recyclerView.indexOfChild(targetView) >= 0) {
                        val targetX: Int
                        val targetY: Int
                        val manager = recyclerView.layoutManager
                        val spanCount = (manager as GridLayoutManager?)!!.spanCount

                        if ((mMyChannelItems.size - COUNT_PRE_MY_HEADER) % spanCount == 0) {
                            val preTargetView = recyclerView.layoutManager!!.findViewByPosition(mMyChannelItems.size + COUNT_PRE_OTHER_HEADER - 1)
                            targetX = preTargetView!!.left
                            targetY = preTargetView.top
                        } else {
                            targetX = targetView!!.left
                            targetY = targetView.top
                        }
                        moveMyToOther(myHolder)
                        startAnimation(recyclerView, currentView, targetX.toFloat(), targetY.toFloat())
                    } else {
                        moveMyToOther(myHolder)
                    }
                }
                myHolder.textView.setOnTouchListener { v, event ->
                    when (MotionEventCompat.getActionMasked(event)) {
                        MotionEvent.ACTION_DOWN -> startTime = System.currentTimeMillis()
                        MotionEvent.ACTION_MOVE -> if (System.currentTimeMillis() - startTime > SPACE_TIME) {
                            mItemTouchHelper.startDrag(myHolder)
                        }
                        MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> startTime = 0
                    }
                    false
                }
                return myHolder
            }
            TYPE_OTHER_CHANNEL_HEADER -> {
                view = mInflater.inflate(R.layout.item_other_channel_header, parent, false)
                return object : RecyclerView.ViewHolder(view) {}
            }
            TYPE_OTHER -> {
                view = mInflater.inflate(R.layout.item_other, parent, false)
                val otherHolder = OtherViewHolder(view)
                otherHolder.textView.setOnClickListener {
                    val recyclerView = parent as RecyclerView
                    val manager = recyclerView.layoutManager
                    val currentPosition = otherHolder.adapterPosition
                    val currentView = manager!!.findViewByPosition(currentPosition)
                    val preTargetView = manager.findViewByPosition(mMyChannelItems.size - 1 + COUNT_PRE_MY_HEADER)

                    if (recyclerView.indexOfChild(preTargetView) >= 0) {
                        var targetX = preTargetView!!.left
                        var targetY = preTargetView.top
                        val targetPosition = mMyChannelItems.size - 1 + COUNT_PRE_OTHER_HEADER
                        val gridLayoutManager = manager as GridLayoutManager?
                        val spanCount = gridLayoutManager!!.spanCount
                        if ((targetPosition - COUNT_PRE_MY_HEADER) % spanCount == 0) {
                            val targetView = manager.findViewByPosition(targetPosition)
                            targetX = targetView!!.left
                            targetY = targetView.top
                        } else {
                            targetX += preTargetView.width

                            if (gridLayoutManager.findLastVisibleItemPosition() == itemCount - 1) {
                                if ((itemCount - 1 - mMyChannelItems.size - COUNT_PRE_OTHER_HEADER) % spanCount == 0) {
                                    val firstVisiblePosition = gridLayoutManager.findFirstVisibleItemPosition()
                                    if (firstVisiblePosition == 0) {
                                        if (gridLayoutManager.findFirstCompletelyVisibleItemPosition() != 0) {
                                            val offset = -recyclerView.getChildAt(0).top - recyclerView.paddingTop
                                            targetY += offset
                                        }
                                    } else {
                                        targetY += preTargetView.height
                                    }
                                }
                            }
                        }

                        if (currentPosition == gridLayoutManager.findLastVisibleItemPosition() &&
                            (currentPosition - mMyChannelItems.size - COUNT_PRE_OTHER_HEADER) % spanCount != 0 &&
                            (targetPosition - COUNT_PRE_MY_HEADER) % spanCount != 0) {
                            moveOtherToMyWithDelay(otherHolder)
                        } else {
                            moveOtherToMy(otherHolder)
                        }
                        startAnimation(recyclerView, currentView, targetX.toFloat(), targetY.toFloat())
                    } else {
                        moveOtherToMy(otherHolder)
                    }
                }
                return otherHolder
            }
        }
        throw IllegalArgumentException("wrong viewType:$viewType")
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is MyViewHolder) {
            holder.textView.text = mMyChannelItems[position - COUNT_PRE_MY_HEADER].name
        } else if (holder is OtherViewHolder) {
            val name = mOtherChannelItems[position - mMyChannelItems.size - COUNT_PRE_OTHER_HEADER].name
            holder.textView.text = name
        }
    }

    override fun getItemCount(): Int {
        return mMyChannelItems.size + mOtherChannelItems.size + COUNT_PRE_OTHER_HEADER
    }

    private fun startAnimation(recyclerView: RecyclerView, currentView: View?, targetX: Float, targetY: Float) {
        val viewGroup = recyclerView.parent as ViewGroup
        val mirrorView = addMirrorView(viewGroup, recyclerView, currentView)
        val animation: Animation = getTranslateAnimator(
            targetX - currentView!!.left, targetY - currentView.top)
        currentView.visibility = View.INVISIBLE
        mirrorView.startAnimation(animation)
        animation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {}
            override fun onAnimationEnd(animation: Animation) {
                viewGroup.removeView(mirrorView)
                if (currentView.visibility == View.INVISIBLE) {
                    currentView.visibility = View.VISIBLE
                }
            }

            override fun onAnimationRepeat(animation: Animation) {}
        })
    }

    private fun moveMyToOther(myHolder: MyViewHolder) {
        val position = myHolder.adapterPosition
        val startPosition = position - COUNT_PRE_MY_HEADER
        if (startPosition > mMyChannelItems.size - 1) {
            return
        }
        val item = mMyChannelItems[startPosition]
        mMyChannelItems.removeAt(startPosition)
        mOtherChannelItems.add(0, item)
        notifyItemMoved(position, mMyChannelItems.size + COUNT_PRE_OTHER_HEADER)
    }

    private fun moveOtherToMy(otherHolder: OtherViewHolder) {
        val position = processItemRemoveAdd(otherHolder)
        if (position == -1) {
            return
        }
        notifyItemMoved(position, mMyChannelItems.size - 1 + COUNT_PRE_MY_HEADER)
    }

    private fun moveOtherToMyWithDelay(otherHolder: OtherViewHolder) {
        val position = processItemRemoveAdd(otherHolder)
        if (position == -1) {
            return
        }
        delayHandler.postDelayed({ notifyItemMoved(position, mMyChannelItems.size - 1 + COUNT_PRE_MY_HEADER) }, ANIM_TIME)
    }

    private val delayHandler = Handler()
    private fun processItemRemoveAdd(otherHolder: OtherViewHolder): Int {
        val position = otherHolder.adapterPosition
        val startPosition = position - mMyChannelItems.size - COUNT_PRE_OTHER_HEADER
        if (startPosition > mOtherChannelItems.size - 1) {
            return -1
        }
        val item = mOtherChannelItems[startPosition]
        mOtherChannelItems.removeAt(startPosition)
        mMyChannelItems.add(item)
        return position
    }


    private fun addMirrorView(parent: ViewGroup, recyclerView: RecyclerView, view: View?): ImageView {
        view!!.destroyDrawingCache()
        view.isDrawingCacheEnabled = true
        val mirrorView = ImageView(recyclerView.context)
        val bitmap = Bitmap.createBitmap(view.drawingCache)
        mirrorView.setImageBitmap(bitmap)
        view.isDrawingCacheEnabled = false
        val locations = IntArray(2)
        view.getLocationOnScreen(locations)
        val parenLocations = IntArray(2)
        recyclerView.getLocationOnScreen(parenLocations)
        val params = FrameLayout.LayoutParams(bitmap.width, bitmap.height)
        params.setMargins(locations[0], locations[1] - parenLocations[1], 0, 0)
        parent.addView(mirrorView, params)
        return mirrorView
    }

    override fun onItemMove(fromPosition: Int, toPosition: Int) {
        val item = mMyChannelItems[fromPosition - COUNT_PRE_MY_HEADER]
        mMyChannelItems.removeAt(fromPosition - COUNT_PRE_MY_HEADER)
        mMyChannelItems.add(toPosition - COUNT_PRE_MY_HEADER, item)
        notifyItemMoved(fromPosition, toPosition)
    }

    private fun getTranslateAnimator(targetX: Float, targetY: Float): TranslateAnimation {
        val translateAnimation = TranslateAnimation(
            Animation.RELATIVE_TO_SELF, 0f,
            Animation.ABSOLUTE, targetX,
            Animation.RELATIVE_TO_SELF, 0f,
            Animation.ABSOLUTE, targetY)
        translateAnimation.duration = ANIM_TIME
        translateAnimation.fillAfter = true
        return translateAnimation
    }

    internal class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), OnDragVHListener {
        val textView: TextView
        override fun onItemSelected() {
            textView.setBackgroundResource(R.drawable.bg_channel_p)
        }

        override fun onItemFinish() {
            textView.setBackgroundResource(R.drawable.bg_channel)
        }

        init {
            textView = itemView.findViewById<View>(R.id.my_channel_tv) as TextView
        }
    }

    internal inner class OtherViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView

        init {
            textView = itemView.findViewById<View>(R.id.my_channel_tv) as TextView
        }
    }

    internal inner class MyChannelHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val finishTV: TextView

        init {
            finishTV = itemView.findViewById<View>(R.id.finish_tv) as TextView
            finishTV.setOnClickListener { mFinishEditListener?.onFinishEdit() }
        }
    }
}
