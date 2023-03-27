package com.example.doodle.base

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.doodle.config.AppConfig
import io.github.doodle.Doodle

abstract class BaseFragment : Fragment() {
    protected var name: String = this.javaClass.simpleName ?: "Fragment"

    @JvmField
    protected var mActivity: BaseActivity? = null
    private var mRootView: View? = null
    private var isViewRecycled = false
    private var isShow = false
    private var isActivityCreated = false
    private var isDataLoaded = false
    protected abstract val layoutResource: Int
    protected abstract fun initView()
    protected open fun loadData() {}

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mActivity = context as BaseActivity
    }

    protected fun findViewById(id: Int): View {
        return mRootView!!.findViewById(id)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (AppConfig.DEBUG) {
            Log.d(name, "onCreate")
        }
        if (savedInstanceState != null) {
            val isSupportHidden = savedInstanceState.getBoolean(STATE_SAVE_IS_HIDDEN)
            val ft = parentFragmentManager.beginTransaction()
            if (isSupportHidden) {
                ft.hide(this)
            } else {
                ft.show(this)
            }
            ft.commit()
            if (AppConfig.DEBUG) {
                Log.d(name, "restore instance state")
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_SAVE_IS_HIDDEN, isHidden)
        if (AppConfig.DEBUG) {
            Log.d(name, "onSaveInstanceState")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (AppConfig.DEBUG) {
            Log.d(name, "onCreateView " + (mRootView == null))
        }
        if (mRootView == null) {
            mRootView = inflater.inflate(layoutResource, container, false)
            isViewRecycled = false
        } else {
            isViewRecycled = true
        }
        return mRootView
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (AppConfig.DEBUG) {
            Log.d(name, "onActivityCreated")
        }
        if (isViewRecycled) {
            return
        }
        initView()
        if (AppConfig.DEBUG) {
            Log.d(name, "fragment decor root 3: " + System.identityHashCode(mRootView!!.rootView))
        }
        isActivityCreated = true
        checkLoadData()
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        if (AppConfig.DEBUG) {
            Log.d(name, "setUserVisibleHint $isVisibleToUser")
        }
        if (isVisibleToUser) {
            Doodle.notifyResume(this)
        } else {
            Doodle.notifyPause(this)
        }
        isShow = isVisibleToUser
        checkLoadData()
    }

    override fun onResume() {
        super.onResume()
        checkLoadData()
    }

    private fun checkLoadData() {
        if (isShow && isActivityCreated && !isDataLoaded) {
            if (AppConfig.DEBUG) {
                Log.d(name, "loadData")
            }
            loadData()
            isDataLoaded = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Doodle.notifyDestroy(this)
    }
    companion object {
        private const val STATE_SAVE_IS_HIDDEN = "STATE_SAVE_IS_HIDDEN"
    }
}