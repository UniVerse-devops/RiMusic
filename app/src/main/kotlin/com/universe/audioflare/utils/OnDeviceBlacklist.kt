package com.universe.audioflare.utils

import android.content.Context
import com.universe.audioflare.models.OnDeviceBlacklistPath
import java.io.File


class OnDeviceBlacklist(context: Context) {
    var paths: List<OnDeviceBlacklistPath> = emptyList()

    init {
        val file = File(context.filesDir, "Blacklisted_paths.txt")
        paths = if (file.exists()) {
            file.readLines().map { OnDeviceBlacklistPath(path = it) }
        } else {
            emptyList()
        }
    }

    fun contains(path: String): Boolean {
        return paths.any { it.test(path) }
    }
}