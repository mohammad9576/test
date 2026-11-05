package com.kodomo.app.ui.kids.video

import androidx.compose.runtime.Composable
import com.kodomo.app.domain.model.ActivityCategory
import com.kodomo.app.ui.kids.TrackActivity

/** The live «پویا» kids channel on Telewebion. The WebView is pinned to this one page: no links, ads or other sites. */
const val POOYA_LIVE_URL = "https://telewebion.ir/live/pooya"

@Composable
fun LiveTvScreen(onBack: () -> Unit) {
    TrackActivity(ActivityCategory.VIDEO, "شبکه پویا (زنده)", "telewebion_pooya_live")
    EmbeddedVideoScreen(title = "شبکه پویا • زنده", platformLabel = "تلوبیون", onBack = onBack, onFinished = null) { modifier ->
        LockedWebPlayer(WebLock.Pinned(POOYA_LIVE_URL), modifier)
    }
}
