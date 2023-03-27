package com.example.doodle.event

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class EventLifecycleObserver(private val observer: Observer) : DefaultLifecycleObserver {
    override fun onCreate(owner: LifecycleOwner) {
        EventManager.register(observer)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        EventManager.unregister(observer)
    }
}
