package com.universe.innertube.models.bodies

import com.universe.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class BrowseBodyWithLocale(
    val context: Context = Context.DefaultWebWithLocale,
    val browseId: String,
    val params: String? = null
)
