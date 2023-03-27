package com.example.doodle.remote.setting

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doodle.R
import com.example.doodle.base.ActivityResultObserver
import com.example.doodle.base.ToolbarActivity
import com.example.doodle.config.AppConfig
import com.example.doodle.event.Event
import com.example.doodle.event.EventLifecycleObserver
import com.example.doodle.event.Observer
import com.example.doodle.remote.setting.SettingData.collectPath
import com.example.doodle.remote.setting.path.PickPathActivity
import com.example.doodle.util.PermissionUtil

class SettingActivity : ToolbarActivity(), SettingAdapter.OnItemClickListener, Observer {
    private lateinit var mAdapter: SettingAdapter

    private val storagePermissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE)

    private val settingResultObserver by lazy {
        ActivityResultObserver("setting", activityResultRegistry) {
            if (PermissionUtil.hasPermissions(*storagePermissions)) {
                startActivity(PickPathActivity::class.java)
            } else {
                finish()
            }
        }
    }

    override val contentLayout: Int
        get() = R.layout.layout_setting

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(EventLifecycleObserver(this))
        lifecycle.addObserver(settingResultObserver)
    }

    override fun initView() {
        setTitle(getStr(R.string.setting))
        val settingItems: MutableList<SettingItem> = ArrayList()
        settingItems.add(collectPathItem)
        mAdapter = SettingAdapter(this, settingItems, false, this)
        val settingRv = findViewById<RecyclerView>(R.id.settings_rv)
        if (settingRv != null) {
            settingRv.layoutManager = LinearLayoutManager(this)
            settingRv.adapter = mAdapter
        }
    }

    private val collectPathItem: SettingItem
        get() {
            var path = collectPath
            if (TextUtils.isEmpty(path)) {
                path = getStr(R.string.not_set)
            }
            return SettingItem(ID_COLLECT_PATH, getStr(R.string.downloaded_path), path)
        }

    override fun onItemClick(id: Int) {
        when (id) {
            ID_COLLECT_PATH -> chooseCollectPath()
            else -> {}
        }
    }

    private fun chooseCollectPath() {
        pickPath()
    }

    private fun pickPath() {
        if (PermissionUtil.hasPermissions(*storagePermissions)) {
            startActivity(PickPathActivity::class.java)
        } else {
            ActivityCompat.requestPermissions(this, storagePermissions, RC_READ_WRITE_STORAGE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RC_READ_WRITE_STORAGE) {
            if (PermissionUtil.allGran(grantResults)) {
                startActivity(PickPathActivity::class.java)
            } else {
                showPermissionRequest()
            }
        }
    }

    private fun showPermissionRequest() {
        AlertDialog.Builder(this)
            .setTitle(R.string.storage_permission_request)
            .setPositiveButton(R.string.to_system_setting) { _, _ ->
                val packageUri = Uri.parse("package:${AppConfig.appContext.packageName}")
                settingResultObserver.launch(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri))
            }.show()
    }

    private fun getItem(id: Int): SettingItem {
        val items = mAdapter.data
        for (item in items) {
            if (item.id == id) {
                return item
            }
        }
        return SettingItem(0, "", "")
    }

    override fun onEvent(event: Int, vararg args: Any?) {
        when (event) {
            Event.CHOSE_PATH -> {
                val item = getItem(ID_COLLECT_PATH)
                item.subtitle = args[0] as String
                val pos = mAdapter.data.indexOf(item)
                mAdapter.notifyItemChanged(pos)
            }
            else -> {}
        }
    }

    override fun listenEvents(): IntArray {
        return intArrayOf(
            Event.CHOSE_PATH
        )
    }

    companion object {
        private const val ID_COLLECT_PATH = 1
        private const val RC_READ_WRITE_STORAGE = 1
    }
}
