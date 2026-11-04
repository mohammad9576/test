package com.kodomo.app.domain.model

/**
 * Where a video is played from. CMS editors paste a normal page link (Aparat / YouTube / Telewebion) or a direct stream,
 * and the app resolves it to an embeddable player – the child never sees the source website itself.
 */
sealed interface VideoSource {
    /** Direct MP4 / HLS / DASH stream or an offline file played by ExoPlayer. */
    data class Direct(val url: String) : VideoSource

    /** Locked web embed (iframe player) for platforms that do not expose raw streams. */
    data class Embed(val platform: Platform, val embedUrl: String) : VideoSource

    enum class Platform(val label: String) { APARAT("آپارات"), YOUTUBE("یوتیوب"), TELEWEBION("تلوبیون") }

    companion object {
        private val aparat = Regex("""aparat\.com/(?:v/|video/video/embed/videohash/)([A-Za-z0-9]+)""")
        private val youtube = Regex("""(?:youtube(?:-nocookie)?\.com/(?:watch\?(?:.*&)?v=|embed/|shorts/|live/)|youtu\.be/)([A-Za-z0-9_-]{11})""")
        private val telewebion = Regex("""telewebion\.com/(?:episode|embed)/(?:0x)?([A-Za-z0-9]+)""")

        fun parse(url: String): VideoSource {
            aparat.find(url)?.let {
                return Embed(Platform.APARAT, "https://www.aparat.com/video/video/embed/videohash/${it.groupValues[1]}/vt/frame")
            }
            youtube.find(url)?.let {
                return Embed(
                    Platform.YOUTUBE,
                    "https://www.youtube-nocookie.com/embed/${it.groupValues[1]}?autoplay=1&rel=0&modestbranding=1&playsinline=1&fs=1&iv_load_policy=3",
                )
            }
            telewebion.find(url)?.let {
                return Embed(Platform.TELEWEBION, "https://telewebion.com/embed/${it.groupValues[1]}")
            }
            return Direct(url)
        }

        /** Hosts an embedded player is allowed to load resources from; every other navigation is blocked. */
        val embedHosts = listOf(
            "aparat.com", "aparat.cloud", "aparatcdn.com", "youtube-nocookie.com", "youtube.com", "ytimg.com",
            "googlevideo.com", "ggpht.com", "telewebion.com", "telewebion.ir",
        )
    }
}
