package com.kodomo.app.ui.kids.games.pose

import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.abs

/**
 * Jump detection from the hip centre. While standing, a slow moving baseline follows the hips; a rise of more than
 * 22 % of the torso length above that baseline counts as one jump, re-armed when the child lands again.
 */
class PoseJumpDetector {
    private var baseline: Float? = null
    private var airborne = false

    fun update(pose: BodyPose): Boolean {
        val torso = pose.torso ?: return false
        val lh = pose.visible(PoseLandmark.LEFT_HIP) ?: return false
        val rh = pose.visible(PoseLandmark.RIGHT_HIP) ?: return false
        val hipY = (lh.y + rh.y) / 2f * pose.imageHeight
        val base = baseline ?: hipY.also { baseline = it }
        val rise = base - hipY
        if (!airborne && rise > torso * 0.22f) {
            airborne = true
            return true
        }
        if (airborne && rise < torso * 0.08f) airborne = false
        if (!airborne) baseline = base * 0.92f + hipY * 0.08f
        return false
    }

    fun reset() {
        baseline = null
        airborne = false
    }
}

/** Average landmark displacement between two poses, relative to torso length (≈0 when frozen, >0.06 when dancing). */
fun movement(previous: BodyPose?, current: BodyPose?): Float {
    if (previous == null || current == null) return 0f
    val torso = current.torso ?: return 0f
    val tracked = listOf(
        PoseLandmark.NOSE, PoseLandmark.LEFT_WRIST, PoseLandmark.RIGHT_WRIST, PoseLandmark.LEFT_ELBOW, PoseLandmark.RIGHT_ELBOW,
        PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER, PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP,
        PoseLandmark.LEFT_KNEE, PoseLandmark.RIGHT_KNEE,
    )
    var sum = 0f
    var n = 0
    for (t in tracked) {
        val a = previous.visible(t) ?: continue
        val b = current.visible(t) ?: continue
        sum += current.distance(a, b)
        n++
    }
    return if (n == 0) 0f else sum / n / torso
}

enum class TargetPose(val prompt: String) {
    BOTH_UP("هر دو دست را بالا ببر!"),
    AIRPLANE("دست‌ها را مثل هواپیما باز کن!"),
    RIGHT_UP("دست راستت را بالا ببر!"),
    LEFT_UP("دست چپت را بالا ببر!"),
    HANDS_ON_HEAD("دست‌ها را روی سرت بگذار!"),
    ;

    fun matches(pose: BodyPose): Boolean {
        val torso = pose.torso ?: return false
        val nose = pose.visible(PoseLandmark.NOSE) ?: return false
        val lw = pose.visible(PoseLandmark.LEFT_WRIST, 0.4f)
        val rw = pose.visible(PoseLandmark.RIGHT_WRIST, 0.4f)
        val ls = pose.visible(PoseLandmark.LEFT_SHOULDER) ?: return false
        val rs = pose.visible(PoseLandmark.RIGHT_SHOULDER) ?: return false
        val h = pose.imageHeight
        fun above(p: PosePoint?, ref: PosePoint) = p != null && (ref.y - p.y) * h > torso * 0.1f
        fun below(p: PosePoint?, ref: PosePoint) = p == null || (p.y - ref.y) * h > -torso * 0.05f
        return when (this) {
            BOTH_UP -> above(lw, nose) && above(rw, nose)
            RIGHT_UP -> above(rw, nose) && below(lw, ls)
            LEFT_UP -> above(lw, nose) && below(rw, rs)
            AIRPLANE -> lw != null && rw != null &&
                abs(lw.y - ls.y) * h < torso * 0.35f && abs(rw.y - rs.y) * h < torso * 0.35f &&
                pose.distance(lw, rw) > pose.distance(ls, rs) * 2.3f
            HANDS_ON_HEAD -> lw != null && rw != null && pose.distance(lw, nose) < torso * 0.7f && pose.distance(rw, nose) < torso * 0.7f &&
                lw.y < ls.y && rw.y < rs.y
        }
    }
}

/** Counts jumping jacks: arms up + feet apart («open») followed by arms down + feet together («closed»). */
class JumpingJackCounter {
    private var open = false
    var count = 0
        private set

    fun update(pose: BodyPose): Boolean {
        val torso = pose.torso ?: return false
        val nose = pose.visible(PoseLandmark.NOSE) ?: return false
        val lw = pose.visible(PoseLandmark.LEFT_WRIST, 0.4f) ?: return false
        val rw = pose.visible(PoseLandmark.RIGHT_WRIST, 0.4f) ?: return false
        val ls = pose.visible(PoseLandmark.LEFT_SHOULDER) ?: return false
        val rs = pose.visible(PoseLandmark.RIGHT_SHOULDER) ?: return false
        val la = pose.visible(PoseLandmark.LEFT_ANKLE, 0.3f)
        val ra = pose.visible(PoseLandmark.RIGHT_ANKLE, 0.3f)
        val lh = pose.visible(PoseLandmark.LEFT_HIP) ?: return false
        val rh = pose.visible(PoseLandmark.RIGHT_HIP) ?: return false
        val hipWidth = pose.distance(lh, rh)
        val feetApart = if (la != null && ra != null) pose.distance(la, ra) > hipWidth * 1.6f else true
        val feetTogether = if (la != null && ra != null) pose.distance(la, ra) < hipWidth * 1.25f else true
        val armsUp = lw.y < nose.y && rw.y < nose.y
        val armsDown = (lw.y - ls.y) * pose.imageHeight > torso * 0.3f && (rw.y - rs.y) * pose.imageHeight > torso * 0.3f
        if (!open && armsUp && feetApart) {
            open = true
        } else if (open && armsDown && feetTogether) {
            open = false
            count++
            return true
        }
        return false
    }
}
