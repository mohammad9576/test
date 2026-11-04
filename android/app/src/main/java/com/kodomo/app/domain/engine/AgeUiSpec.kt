package com.kodomo.app.domain.engine

import com.kodomo.app.domain.model.AgeGroup

/** Pure description of how the kids space should look & behave for an age tier. Rendered by the UI theme layer. */
data class AgeUiSpec(
    val group: AgeGroup,
    val gridColumns: Int,
    val cardHeightDp: Int,
    val iconSizeSp: Int,
    val titleScale: Float,
    val showCardLabels: Boolean,
    val showDescriptions: Boolean,
    val voiceGuidance: Boolean,
    val gamified: Boolean,
    val darkThemeAllowed: Boolean,
    val cornerRadiusDp: Int,
) {
    companion object {
        fun forGroup(group: AgeGroup): AgeUiSpec = when (group) {
            AgeGroup.TODDLER -> AgeUiSpec(
                group, gridColumns = 2, cardHeightDp = 170, iconSizeSp = 64, titleScale = 1.25f,
                showCardLabels = true, showDescriptions = false, voiceGuidance = true, gamified = false,
                darkThemeAllowed = false, cornerRadiusDp = 36,
            )
            AgeGroup.PRIMARY_KID -> AgeUiSpec(
                group, gridColumns = 3, cardHeightDp = 128, iconSizeSp = 44, titleScale = 1.05f,
                showCardLabels = true, showDescriptions = false, voiceGuidance = false, gamified = true,
                darkThemeAllowed = false, cornerRadiusDp = 28,
            )
            AgeGroup.PRE_TEEN -> AgeUiSpec(
                group, gridColumns = 2, cardHeightDp = 108, iconSizeSp = 32, titleScale = 1f,
                showCardLabels = true, showDescriptions = true, voiceGuidance = false, gamified = false,
                darkThemeAllowed = true, cornerRadiusDp = 20,
            )
        }
    }
}
