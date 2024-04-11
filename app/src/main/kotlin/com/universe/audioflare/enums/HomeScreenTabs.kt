package com.universe.audioflare.enums

enum class HomeScreenTabs {
    Default,
    QuickPics,
    Discovery,
    Songs,
    Artists,
    Albums,
    Library;



    val index: Int
        get() = when (this) {
            Default -> 100
            QuickPics -> 0
            Discovery -> 1
            Songs -> 2
            Artists -> 3
            Albums -> 4
            Library -> 5
            

        }

}