package com.kodomo.app.ui.kids

import androidx.lifecycle.SavedStateHandle
import com.kodomo.app.ui.navigation.Routes

val SavedStateHandle.childId: Long get() = get<Long>(Routes.ARG_CHILD) ?: 0L
