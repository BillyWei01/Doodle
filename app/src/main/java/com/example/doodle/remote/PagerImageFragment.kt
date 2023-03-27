package com.example.doodle.remote

import android.text.TextUtils
import android.widget.ImageView
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.example.doodle.base.BaseFragment
import com.example.doodle.R
import com.example.doodle.remote.channel.Channel
import com.example.doodle.remote.channel.ChannelDialog
import com.example.doodle.remote.channel.ChannelManager
import com.example.doodle.util.onClick

class PagerImageFragment : BaseFragment() {
    companion object {
        const val TAG = "RemoteImageFragment"
        @JvmStatic
        fun newInstance(): PagerImageFragment {
            return PagerImageFragment()
        }
    }

    private val myChannels = ChannelManager.myChannels
    private val otherChannels= ChannelManager.otherChannels

    private val _fragments: MutableList<ChannelFragment> = ArrayList()
    private val fragments: List<ChannelFragment>
        get() {
            if (_fragments.isEmpty()) {
                val channels = myChannels
                for (channel in channels) {
                    _fragments.add(ChannelFragment.newInstance(channel))
                }
            }
            return _fragments
        }

    override val layoutResource: Int
        get() = R.layout.fragment_pages

    override fun initView() {
        val pagerAdapter = PageFragmentAdapter(childFragmentManager)
        pagerAdapter.setData(fragments)
        val viewPager = findViewById(R.id.view_pager) as ViewPager
        viewPager.adapter = pagerAdapter
        val tabLayout = findViewById(R.id.tab_layout) as TabLayout
        tabLayout.tabMode = TabLayout.MODE_SCROLLABLE
        tabLayout.setupWithViewPager(viewPager)
        tabLayout.setOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                viewPager.currentItem = tab.position
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        val context = activity ?: return
        val editChannelIv = findViewById(R.id.edit_channel_iv) as ImageView
        editChannelIv.onClick {
            val dialog = ChannelDialog(context, myChannels, otherChannels)
            dialog.setOnDismissListener {
                updateFragments()
                pagerAdapter.notifyDataSetChanged()
            }
            dialog.show()
        }
    }

    // BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
    private inner class PageFragmentAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {
        private lateinit var fragments: List<ChannelFragment>
        fun setData(fragments: List<ChannelFragment>) {
            this.fragments = fragments
        }

        override fun getItem(position: Int): ChannelFragment {
            return fragments[position]
        }

        override fun getCount(): Int {
            return fragments.size
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return fragments[position].title
        }

        override fun getItemId(position: Int): Long {
            return getItem(position).fragmentID
        }

        override fun getItemPosition(item: Any): Int {
            if (item is BaseFragment) {
                val pos = fragments.indexOf(item)
                return if (pos >= 0) pos else POSITION_NONE
            }
            return POSITION_UNCHANGED
        }
    }

    private fun updateFragments() {
        ChannelManager.saveChannels()

        val channels = myChannels
        val newFragments: MutableList<ChannelFragment> = ArrayList(channels.size)
        for (channel in channels) {
            var fragment = pickFragment(channel)
            if (fragment == null) {
                fragment = ChannelFragment.newInstance(channel)
            }
            newFragments.add(fragment)
        }

        _fragments.clear()
        _fragments.addAll(newFragments)
    }

    private fun pickFragment(channel: Channel): ChannelFragment? {
        val iterator = _fragments.iterator()
        while (iterator.hasNext()) {
            val fragment = iterator.next()
            if (TextUtils.equals(fragment.channelID, channel.id)) {
                iterator.remove()
                return fragment
            }
        }
        return null
    }
}
