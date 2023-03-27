package com.example.doodle.base

import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import com.example.doodle.R

abstract class ToolbarActivity : BaseActivity() {
    private var mToolbar: Toolbar? = null
    protected abstract val contentLayout: Int
    protected abstract fun initView()

    protected open val activityLayout: Int
         get() = R.layout.activity_toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val inflater = layoutInflater
        val contentView = inflater.inflate(activityLayout, null) as ViewGroup
        inflater.inflate(contentLayout, contentView, true)
        val toolbar = contentView.findViewById<Toolbar>(R.id.toolbar)
        mToolbar = toolbar
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setContentView(contentView)
        initView()
    }

    protected fun setTitle(title: String?) {
        mToolbar?.title = title
    }

    protected fun hideTitle() {
        supportActionBar?.setDisplayShowTitleEnabled(false)
    }
}