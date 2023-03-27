package com.example.doodle.remote.setting.path

import android.os.Build
import android.os.Environment
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doodle.event.Event
import com.example.doodle.event.EventManager.notify
import com.example.doodle.base.ToolbarActivity
import com.example.doodle.util.ToastUtil
import com.example.doodle.R
import com.example.doodle.remote.media.MediaUtil
import com.example.doodle.remote.setting.SettingData
import com.example.doodle.remote.setting.SettingData.collectPath
import com.example.doodle.remote.setting.SettingData.showHidden
import com.example.doodle.util.LogUtil
import com.example.doodle.util.onClick
import java.io.File
import java.io.FileFilter
import java.util.*


class PickPathActivity : ToolbarActivity() {
    private var mPathRv: RecyclerView? = null
    private var mPathAdapter: PathAdapter? = null
    private var mCurrentDir: File? = null
    private var mPathFiler: PathFilter? = null

    override val activityLayout: Int
        get() = R.layout.activity_toolbar_relative
    override val contentLayout: Int
        get() = R.layout.layout_pick_path


    override fun initView() {
        setTitle(getStr(R.string.select_path))
        val root = getRootFile()
        mCurrentDir = root

        val pathFiler = PathFilter()
        pathFiler.setShowHidden(showHidden)
        mPathFiler = pathFiler

        val data = getFiles(root, pathFiler)
        val pathAdapter = PathAdapter(this, data, true, pathFiler)
        mPathAdapter = pathAdapter

        val pathRv = findViewById<RecyclerView>(R.id.path_rv)
        pathRv.layoutManager = LinearLayoutManager(this)
        pathRv.adapter = pathAdapter
        mPathRv = pathRv

        val pathTv = findViewById<TextView>(R.id.path_tv)
        pathTv.text = root.path

        val fileStack = Stack<File>()
        val pathStack = Stack<List<File>>()
        pathAdapter.setOnItemClickListener(object : PathAdapter.OnItemClickListener {
            override fun onItemClick(file: File) {
                val subFiles = getFiles(file, pathFiler)
                fileStack.push(mCurrentDir)
                pathStack.push(ArrayList(pathAdapter.data))
                pathTv.text = file.path
                pathAdapter.data = subFiles
                mCurrentDir = file
            }
        })
        val backTv = findViewById<TextView>(R.id.back_tv)
        backTv.onClick {
            if (fileStack.isNotEmpty()) {
                val file = fileStack.pop()
                val subFiles = pathStack.pop()
                pathTv.text = file.path
                pathAdapter.data = subFiles
                pathRv.scrollToPosition(0)
                mCurrentDir = file
            }
        }

        val selectBtn = findViewById<Button>(R.id.select_btn)
        selectBtn.onClick {
            val checkFile = pathAdapter.checkFile
            if (checkFile != null) {
                collectPath = checkFile.path
                notify(Event.CHOSE_PATH, checkFile.path)
                finish()
            } else {
                val currentPath = mCurrentDir?.path
                if (currentPath != null) {
                    collectPath = currentPath
                    notify(Event.CHOSE_PATH, currentPath)
                    finish()
                } else {
                    ToastUtil.showTips(R.string.current_path_is_null)
                }
            }
        }
    }

    private fun getRootFile(): File {
        LogUtil.d("MyTag", "SDK version:" + Build.VERSION.SDK_INT)
        try {
            val root = Environment.getExternalStorageDirectory()
            LogUtil.d("MyTag", "root:" + root.path +" canRead:"+ root.canRead())
            if (root != null && root.canRead() && !root.list().isNullOrEmpty()) {
                return root
            }
        } catch (ignore: Throwable) {
        }
        return File(MediaUtil.picturePath)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.pick_path, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val itemShowHidden = menu.findItem(R.id.action_show_hidden)
        if (!showHidden) {
            itemShowHidden.setTitle(R.string.show_hidden_files)
        } else {
            itemShowHidden.setTitle(R.string.hidden_files)
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        when (id) {
            R.id.action_show_hidden -> {
                toggleShowHidden()
                return true
            }
            else -> {}
        }
        return super.onOptionsItemSelected(item)
    }

    private fun toggleShowHidden() {
        val showHidden = showHidden
        SettingData.showHidden = !showHidden

        val currentPath = mCurrentDir
        val pathFilter = mPathFiler
        val pathAdapter = mPathAdapter
        val pathRv = mPathRv
        if (currentPath != null && pathFilter != null && pathAdapter != null && pathRv != null) {
            pathFilter.setShowHidden(!showHidden)
            val subFiles = getFiles(currentPath, pathFilter)
            pathAdapter.data = subFiles
            pathRv.scrollToPosition(0)
            pathRv.postDelayed({ invalidateOptionsMenu() }, 100L)
        }
    }

    private fun getFiles(root: File, filter: FileFilter): List<File> {
        val files: MutableList<File> = ArrayList()
        val fileArray = root.listFiles(filter)
        if (fileArray != null && fileArray.isNotEmpty()) {
            files.addAll(listOf(*fileArray))
        }
        files.sortWith { o1, o2 -> o1.path.compareTo(o2.path) }
        return files
    }
}