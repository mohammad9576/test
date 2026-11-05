package com.kodomo.app.ui.fx

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * Tiny synthesizer for game audio (no audio files): layered sound effects and looping background music per theme.
 * Everything is rendered once to PCM and cached.
 */
internal object Synth {
    const val RATE = 22_050

    fun note(midi: Int): Float = 440f * 2f.pow((midi - 69) / 12f)

    enum class Wave { SINE, TRIANGLE, SOFT_SQUARE, PLUCK, NOISE }

    private fun osc(wave: Wave, phase: Double): Float = when (wave) {
        Wave.SINE -> sin(2 * PI * phase).toFloat()
        Wave.TRIANGLE -> (2 * abs(2 * (phase - kotlin.math.floor(phase + 0.5))) - 1).toFloat()
        Wave.SOFT_SQUARE -> (kotlin.math.tanh(sin(2 * PI * phase) * 3.0) * 0.7).toFloat()
        Wave.PLUCK -> (sin(2 * PI * phase) * 0.7 + sin(4 * PI * phase) * 0.2 + sin(6 * PI * phase) * 0.1).toFloat()
        Wave.NOISE -> Random.nextFloat() * 2f - 1f
    }

    /** Adds one note into [buf] at [startSec] with attack/decay envelope and an optional pitch slide. */
    fun add(
        buf: FloatArray, startSec: Float, durSec: Float, freq: Float, wave: Wave, volume: Float,
        attack: Float = 0.005f, decay: Float = 4f, slideTo: Float? = null,
    ) {
        val start = (startSec * RATE).toInt()
        val n = (durSec * RATE).toInt()
        var phase = 0.0
        for (k in 0 until n) {
            val i = start + k
            if (i !in buf.indices) break
            val t = k.toFloat() / RATE
            val f = if (slideTo != null) freq + (slideTo - freq) * (k.toFloat() / n) else freq
            phase += f / RATE
            val env = (if (t < attack) t / attack else 1f) * exp(-decay * t / durSec.coerceAtLeast(0.01f))
            buf[i] += osc(wave, phase) * env * volume
        }
    }

    fun toPcm(buf: FloatArray, gain: Float = 1f): ShortArray {
        val peak = buf.maxOfOrNull { abs(it) }?.coerceAtLeast(1f) ?: 1f
        return ShortArray(buf.size) { (buf[it] / peak * gain * Short.MAX_VALUE * 0.9f).toInt().toShort() }
    }
}

/** Sound effects for games. All are short and child-friendly (no harsh buzzers). */
object GameSfx {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val cache = mutableMapOf<String, ShortArray>()
    @Volatile var enabled = true

    private fun render(name: String, seconds: Float, gain: Float = 0.8f, block: (FloatArray) -> Unit): ShortArray =
        synchronized(cache) {
            cache.getOrPut(name) {
                val buf = FloatArray((seconds * Synth.RATE).toInt())
                block(buf)
                Synth.toPcm(buf, gain)
            }
        }

    private fun fire(data: ShortArray) {
        if (!enabled) return
        scope.launch {
            runCatching {
                val track = AudioTrack.Builder()
                    .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                    .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(Synth.RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .setBufferSizeInBytes(data.size * 2)
                    .build()
                track.write(data, 0, data.size)
                track.play()
                delay(data.size * 1000L / Synth.RATE + 60)
                track.release()
            }
        }
    }

    fun coin() = fire(render("coin", 0.35f) { b ->
        Synth.add(b, 0f, 0.09f, Synth.note(88), Synth.Wave.PLUCK, 0.6f)
        Synth.add(b, 0.07f, 0.25f, Synth.note(93), Synth.Wave.PLUCK, 0.6f)
    })

    fun sparkle() = fire(render("sparkle", 0.5f, 0.6f) { b ->
        listOf(84, 88, 91, 96).forEachIndexed { i, m -> Synth.add(b, i * 0.06f, 0.3f, Synth.note(m), Synth.Wave.SINE, 0.5f) }
    })

    fun whoosh() = fire(render("whoosh", 0.4f, 0.55f) { b ->
        Synth.add(b, 0f, 0.38f, 300f, Synth.Wave.NOISE, 0.5f, attack = 0.12f, decay = 3f)
        Synth.add(b, 0f, 0.38f, 220f, Synth.Wave.SINE, 0.3f, attack = 0.1f, slideTo = 520f)
    })

    fun hit() = fire(render("hit", 0.3f, 0.9f) { b ->
        Synth.add(b, 0f, 0.25f, 160f, Synth.Wave.SINE, 1f, decay = 6f, slideTo = 60f)
        Synth.add(b, 0f, 0.08f, 1000f, Synth.Wave.NOISE, 0.5f, decay = 8f)
    })

    fun oops() = fire(render("oops", 0.45f, 0.6f) { b ->
        Synth.add(b, 0f, 0.18f, Synth.note(67), Synth.Wave.TRIANGLE, 0.6f)
        Synth.add(b, 0.16f, 0.26f, Synth.note(62), Synth.Wave.TRIANGLE, 0.6f, slideTo = Synth.note(58))
    })

    fun powerUp() = fire(render("power", 0.7f, 0.7f) { b ->
        Synth.add(b, 0f, 0.6f, 220f, Synth.Wave.SOFT_SQUARE, 0.35f, attack = 0.05f, decay = 1.5f, slideTo = 880f)
        listOf(72, 76, 79, 84).forEachIndexed { i, m -> Synth.add(b, 0.1f + i * 0.1f, 0.4f, Synth.note(m), Synth.Wave.PLUCK, 0.4f) }
    })

    fun levelUp() = fire(render("levelup", 1.3f, 0.8f) { b ->
        val melody = listOf(72, 76, 79, 84, 79, 84, 88)
        melody.forEachIndexed { i, m -> Synth.add(b, i * 0.11f, 0.45f, Synth.note(m), Synth.Wave.PLUCK, 0.5f) }
        Synth.add(b, 0.77f, 0.5f, Synth.note(60), Synth.Wave.TRIANGLE, 0.5f)
        Synth.add(b, 0.77f, 0.5f, Synth.note(64), Synth.Wave.TRIANGLE, 0.4f)
        Synth.add(b, 0.77f, 0.5f, Synth.note(67), Synth.Wave.TRIANGLE, 0.4f)
    })

    fun magic() = fire(render("magic", 0.9f, 0.55f) { b ->
        repeat(10) { i -> Synth.add(b, i * 0.05f, 0.35f, Synth.note(84 + (i * 5) % 12), Synth.Wave.SINE, 0.35f) }
        Synth.add(b, 0f, 0.8f, 600f, Synth.Wave.NOISE, 0.08f, attack = 0.2f, decay = 2f)
    })

    fun boss() = fire(render("boss", 0.9f, 0.85f) { b ->
        Synth.add(b, 0f, 0.85f, 90f, Synth.Wave.SOFT_SQUARE, 0.7f, attack = 0.05f, decay = 2f, slideTo = 55f)
        Synth.add(b, 0f, 0.85f, 400f, Synth.Wave.NOISE, 0.25f, attack = 0.1f, decay = 2f)
    })

    fun beep(high: Boolean = false) = fire(render(if (high) "beepHi" else "beep", 0.2f, 0.6f) { b ->
        Synth.add(b, 0f, 0.18f, if (high) Synth.note(84) else Synth.note(72), Synth.Wave.SINE, 0.7f, decay = 2f)
    })

    fun cheer() = fire(render("cheer", 1.6f, 0.7f) { b ->
        Synth.add(b, 0f, 1.5f, 1200f, Synth.Wave.NOISE, 0.25f, attack = 0.15f, decay = 2.5f)
        listOf(67, 72, 76, 79, 84).forEachIndexed { i, m -> Synth.add(b, i * 0.09f, 0.5f, Synth.note(m), Synth.Wave.PLUCK, 0.45f) }
    })
}

/**
 * Looping background music, synthesized per theme (8 bars of melody, bass and soft drums). Quiet by design so voice
 * instructions stay understandable.
 */
object GameMusic {
    enum class Theme(val bpm: Int, val scale: List<Int>, val lead: Synth.Wave, val root: Int) {
        FOREST(96, listOf(0, 2, 4, 7, 9), Synth.Wave.PLUCK, 60),
        HERO(126, listOf(0, 2, 4, 5, 7, 9, 11), Synth.Wave.SOFT_SQUARE, 62),
        SPACE(84, listOf(0, 3, 5, 7, 10), Synth.Wave.SINE, 57),
        OCEAN(80, listOf(0, 2, 4, 7, 9), Synth.Wave.TRIANGLE, 65),
        PARTY(132, listOf(0, 2, 4, 5, 7, 9, 11), Synth.Wave.PLUCK, 64),
        SPORT(120, listOf(0, 2, 4, 7, 9), Synth.Wave.SOFT_SQUARE, 60),
        CALM(72, listOf(0, 2, 4, 7, 9), Synth.Wave.SINE, 67),
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val cache = mutableMapOf<Theme, ShortArray>()
    private var track: AudioTrack? = null
    private var current: Theme? = null
    @Volatile var enabled = true
        set(value) {
            field = value
            if (!value) stop() else current?.let { val t = it; current = null; play(t) }
        }

    private fun compose(theme: Theme): ShortArray {
        val beat = 60f / theme.bpm
        val bars = 8
        val seconds = bars * 4 * beat
        val buf = FloatArray((seconds * Synth.RATE).toInt())
        val rnd = Random(theme.ordinal * 31 + 7)
        val chords = listOf(0, 5, 3, 4, 0, 5, 3, 4) // I vi IV V progression (scale steps)
        fun pitch(step: Int, octave: Int = 0): Int {
            val s = theme.scale
            val i = ((step % s.size) + s.size) % s.size
            return theme.root + s[i] + 12 * (octave + Math.floorDiv(step, s.size))
        }
        var melodyStep = 2
        for (bar in 0 until bars) {
            val chord = chords[bar]
            val barStart = bar * 4 * beat
            // bass: root on beats 1 and 3
            listOf(0f, 2f).forEach { b -> Synth.add(buf, barStart + b * beat, beat * 1.6f, Synth.note(pitch(chord, -2)), Synth.Wave.TRIANGLE, 0.55f, decay = 3f) }
            // pad chord
            listOf(0, 2, 4).forEach { d -> Synth.add(buf, barStart, beat * 4f, Synth.note(pitch(chord + d, -1)), Synth.Wave.SINE, 0.12f, attack = 0.3f, decay = 1.2f) }
            // melody: 8 eighth notes with a gentle random walk, resting sometimes
            for (e in 0 until 8) {
                if (rnd.nextFloat() < 0.22f) continue
                melodyStep = (melodyStep + rnd.nextInt(-2, 3)).coerceIn(0, 9)
                Synth.add(buf, barStart + e * beat / 2f, beat * 0.55f, Synth.note(pitch(melodyStep, 0)), theme.lead, 0.32f, decay = 3.5f)
            }
            // soft drums: kick on 1/3, hat on off-beats
            listOf(0f, 2f).forEach { b -> Synth.add(buf, barStart + b * beat, 0.18f, 120f, Synth.Wave.SINE, 0.5f, decay = 7f, slideTo = 50f) }
            for (h in 0 until 4) Synth.add(buf, barStart + (h + 0.5f) * beat, 0.05f, 8000f, Synth.Wave.NOISE, 0.07f, decay = 9f)
        }
        return Synth.toPcm(buf, 0.85f)
    }

    fun play(theme: Theme) {
        if (current == theme && track != null) return
        stop()
        current = theme
        if (!enabled) return
        scope.launch {
            val data = synchronized(cache) { cache.getOrPut(theme) { compose(theme) } }
            withContext(Dispatchers.Main) {
                if (current != theme || !enabled) return@withContext
                runCatching {
                    val t = AudioTrack.Builder()
                        .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                        .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(Synth.RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                        .setTransferMode(AudioTrack.MODE_STATIC)
                        .setBufferSizeInBytes(data.size * 2)
                        .build()
                    t.write(data, 0, data.size)
                    t.setLoopPoints(0, data.size, -1)
                    t.setVolume(0.28f)
                    t.play()
                    track = t
                }
            }
        }
    }

    fun stop() {
        track?.let { runCatching { it.stop() }; it.release() }
        track = null
    }

    /** Forget the theme too (leaving the game). */
    fun release() {
        stop()
        current = null
    }
}
