package com.example.doodle.remote

import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.example.doodle.R
import com.example.doodle.base.BaseActivity
import com.example.doodle.remote.download.DownloadFragment
import com.example.doodle.remote.setting.SettingActivity
import com.example.doodle.remote.setting.SettingData.lastShowingFragment
import io.github.doodle.Doodle
import io.github.doodle.transformations.CircleTransformation

class RemoteImageActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {
    private var mCurrentTag: String? = null
    var mToolbar: Toolbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_remote_image)
        initView(savedInstanceState)
    }

    private fun initView(savedInstanceState: Bundle?) {
        val toolbar = findViewById<Toolbar>(R.id.toolbar_main)
        setSupportActionBar(toolbar)
        mToolbar = toolbar

        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        val toggle = ActionBarDrawerToggle(
            this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawer.addDrawerListener(toggle)
        toggle.syncState()
        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)

        if (savedInstanceState == null) {
            val tag = lastShowingFragment
            toolbar.post {
                updateTitle(tag)
            }
            showFragment(if (TextUtils.isEmpty(tag)) PagerImageFragment.TAG else tag)
        }

        drawer.post {
            findViewById<ImageView?>(R.id.avatar_iv)?.let { avatarIv ->
                Doodle.load(R.drawable.ez)
                    .transform(CircleTransformation())
                    .into(avatarIv)
            }
        }
    }

    private fun updateTitle(tag: String) {
        if (tag == DownloadFragment.TAG) {
            mToolbar?.setTitle(R.string.downloaded)
        } else {
            mToolbar?.setTitle(R.string.remote_images)
        }
    }

    private fun showFragment(tag: String) {
        if (TextUtils.equals(tag, mCurrentTag)) {
            return
        }
        updateTitle(tag)
        lastShowingFragment = tag
        val manager = supportFragmentManager
        val transaction = manager.beginTransaction()
        var fragment = manager.findFragmentByTag(tag)
        if (fragment == null) {
            fragment = if (DownloadFragment.TAG == tag) {
                DownloadFragment()
            } else {
                PagerImageFragment.newInstance()
            }
            transaction.add(R.id.main_fragment_container, fragment, tag)
        } else {
            transaction.show(fragment)
        }
        if (!TextUtils.isEmpty(mCurrentTag)) {
            val f = manager.findFragmentByTag(mCurrentTag)
            if (f != null) {
                transaction.hide(f)
            }
        }
        transaction.commit()
        mCurrentTag = tag
    }

    override fun onBackPressed() {
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.action_settings) {
            startActivity(SettingActivity::class.java)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.nav_download) {
            showFragment(DownloadFragment.TAG)
        } else if (id == R.id.nav_remote_image) {
            showFragment(PagerImageFragment.TAG)
        }
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        drawer.closeDrawer(GravityCompat.START)
        return true
    }
}
