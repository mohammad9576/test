package com.kodomo.app.ui.kids.music

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.kodomo.app.ui.fx.Synth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** One note of a melody: MIDI pitch (or null for a rest) and length in beats. */
data class Tone(val midi: Int?, val beats: Float)

/** Built-in songs: well-known public-domain melodies, rendered on the phone (works offline). */
enum class BoxSong(val title: String, val emoji: String, val bpm: Int, val lullaby: Boolean, private val score: String) {
    TWINKLE("چشمک بزن ستاره‌ی کوچک", "⭐", 96, true,
        "C4 C4 G4 G4 A4 A4 G4/2 F4 F4 E4 E4 D4 D4 C4/2 G4 G4 F4 F4 E4 E4 D4/2 G4 G4 F4 F4 E4 E4 D4/2 C4 C4 G4 G4 A4 A4 G4/2 F4 F4 E4 E4 D4 D4 C4/2"),
    BRAHMS("لالایی برامس", "🌙", 72, true,
        "E4/0.5 E4/0.5 G4/2 E4/0.5 E4/0.5 G4/2 E4/0.5 G4/0.5 C5 B4/1.5 A4/0.5 A4 G4 D4/0.5 E4/0.5 F4 D4 D4/0.5 E4/0.5 F4/2 D4/0.5 F4/0.5 B4/0.5 A4/0.5 G4 B4 C5/3"),
    ODE("سرود شادی (بتهوون)", "🎻", 104, false,
        "E4 E4 F4 G4 G4 F4 E4 D4 C4 C4 D4 E4 E4/1.5 D4/0.5 D4/2 E4 E4 F4 G4 G4 F4 E4 D4 C4 C4 D4 E4 D4/1.5 C4/0.5 C4/2"),
    MARY("بره‌ی کوچولوی مری", "🐑", 108, false,
        "E4 D4 C4 D4 E4 E4 E4/2 D4 D4 D4/2 E4 G4 G4/2 E4 D4 C4 D4 E4 E4 E4 E4 D4 D4 E4 D4 C4/4"),
    JACQUES("برادر ژاک", "🔔", 112, false,
        "C4 D4 E4 C4 C4 D4 E4 C4 E4 F4 G4/2 E4 F4 G4/2 G4/0.5 A4/0.5 G4/0.5 F4/0.5 E4 C4 G4/0.5 A4/0.5 G4/0.5 F4/0.5 E4 C4 C4 G3 C4/2 C4 G3 C4/2"),
    ROW("پارو بزن قایقت را", "🚣", 100, true,
        "C4/1.5 C4/1.5 C4 D4/0.5 E4/1.5 E4 D4/0.5 E4 F4/0.5 G4/3 C5/0.5 C5/0.5 C5/0.5 G4/0.5 G4/0.5 G4/0.5 E4/0.5 E4/0.5 E4/0.5 C4/0.5 C4/0.5 C4/0.5 G4 F4/0.5 E4 D4/0.5 C4/3"),
    FARM("مزرعه‌ی پدربزرگ", "🐄", 116, false,
        "G4 G4 G4 D4 E4 E4 D4/2 B4 B4 A4 A4 G4/3 D4 G4 G4 G4 D4 E4 E4 D4/2 B4 B4 A4 A4 G4/3"),
    LONDON("پل لندن", "🌉", 112, false,
        "G4/1.5 A4/0.5 G4 F4 E4 F4 G4/2 D4 E4 F4/2 E4 F4 G4/2 G4/1.5 A4/0.5 G4 F4 E4 F4 G4/2 D4/2 G4/2 E4 C4/3"),
    ELISE("برای الیز (بتهوون)", "🎹", 84, true,
        "E5/0.5 D#5/0.5 E5/0.5 D#5/0.5 E5/0.5 B4/0.5 D5/0.5 C5/0.5 A4/1.5 C4/0.5 E4/0.5 A4/0.5 B4/1.5 E4/0.5 G#4/0.5 B4/0.5 C5/1.5 E4/0.5 E5/0.5 D#5/0.5 E5/0.5 D#5/0.5 E5/0.5 B4/0.5 D5/0.5 C5/0.5 A4/1.5 C4/0.5 E4/0.5 A4/0.5 B4/1.5 E4/0.5 C5/0.5 B4/0.5 A4/3"),
    ;

    val tones: List<Tone> by lazy { parse(score) }

    companion object {
        private val names = mapOf('C' to 0, 'D' to 2, 'E' to 4, 'F' to 5, 'G' to 7, 'A' to 9, 'B' to 11)

        fun parse(score: String): List<Tone> = score.trim().split(Regex("\\s+")).map { tok ->
            val (n, d) = tok.split('/').let { it[0] to (it.getOrNull(1)?.toFloatOrNull() ?: 1f) }
            if (n == "-") Tone(null, d) else {
                val sharp = n.contains('#')
                val octave = n.last().digitToInt()
                Tone(12 * (octave + 1) + names.getValue(n[0]) + if (sharp) 1 else 0, d)
            }
        }
    }
}

enum class BoxSound(val label: String, val wave: Synth.Wave) { MUSIC_BOX("جعبه موسیقی", Synth.Wave.PLUCK), FLUTE("فلوت", Synth.Wave.SINE), XYLO("زایلوفون", Synth.Wave.TRIANGLE) }

/** Plays [BoxSong]s through a small synth, with an optional sleep timer that fades the music out. */
object MusicBox {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null
    private var track: AudioTrack? = null
    private val _playing = MutableStateFlow<BoxSong?>(null)
    val playing: StateFlow<BoxSong?> = _playing

    fun render(song: BoxSong, sound: BoxSound): ShortArray {
        val beat = 60f / song.bpm
        val total = song.tones.sumOf { it.beats.toDouble() }.toFloat() * beat + 1.5f
        val buf = FloatArray((total * Synth.RATE).toInt())
        var t = 0.3f
        var beatPos = 0f
        song.tones.forEach { tone ->
            val dur = tone.beats * beat
            tone.midi?.let { m ->
                Synth.add(buf, t, dur + 0.25f, Synth.note(m), sound.wave, 0.55f, attack = if (sound == BoxSound.FLUTE) 0.04f else 0.004f, decay = if (sound == BoxSound.FLUTE) 1.5f else 3f)
                // soft accompaniment: a low note on every second beat
                if (beatPos % 2f < 0.01f) Synth.add(buf, t, beat * 2f, Synth.note(m - 24), Synth.Wave.SINE, 0.22f, attack = 0.03f, decay = 2f)
            }
            t += dur
            beatPos += tone.beats
        }
        return Synth.toPcm(buf, 0.8f)
    }

    /** Plays [songs] one after another (repeat), stopping after [sleepMinutes] with a fade (0 = no timer). */
    fun play(songs: List<BoxSong>, sound: BoxSound, sleepMinutes: Int = 0) {
        stop()
        if (songs.isEmpty()) return
        job = scope.launch {
            val end = if (sleepMinutes > 0) System.currentTimeMillis() + sleepMinutes * 60_000L else Long.MAX_VALUE
            var i = 0
            while (isActive && System.currentTimeMillis() < end) {
                val song = songs[i % songs.size]
                _playing.value = song
                val data = render(song, sound)
                val t = runCatching {
                    AudioTrack.Builder()
                        .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                        .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(Synth.RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                        .setTransferMode(AudioTrack.MODE_STATIC)
                        .setBufferSizeInBytes(data.size * 2)
                        .build().also { it.write(data, 0, data.size) }
                }.getOrNull() ?: break
                track = t
                val left = end - System.currentTimeMillis()
                t.setVolume(if (left < 60_000) (left / 60_000f).coerceIn(0.15f, 1f) else 1f)
                t.play()
                delay(data.size * 1000L / Synth.RATE)
                runCatching { t.stop() }; t.release()
                if (track === t) track = null
                i++
            }
            _playing.value = null
        }
    }

    fun stop() {
        job?.cancel(); job = null
        track?.let { runCatching { it.stop() }; runCatching { it.release() } }
        track = null
        _playing.value = null
    }

    /** One xylophone bar. */
    fun tap(midi: Int) = com.kodomo.app.ui.fx.GameSfx.note(midi)
}
