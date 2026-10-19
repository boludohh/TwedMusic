package com.twedmusic.app.core.audio

/**
 * Lista blanca de formatos de audio reproducibles por BASS con los
 * plugins empaquetados en TwedMusic, y legibles por TagLib 2.3.2
 * (KTagLib) para metadatos.
 *
 * Se decide por el par **contenedor** ([General.FORMAT] de MediaInfo)
 * + **códec de audio** ([Audio.FORMAT] de MediaInfo), no por extensión.
 *
 * Códecs cubiertos por los plugins presentes:
 * - BASS core: MPEG Audio (MP3), Vorbis, PCM (Wave/AIFF)
 * - BASSFLAC: FLAC (nativo y Ogg FLAC)
 * - BASS_AAC: AAC (MPEG-4 / ADTS)
 * - BASSALAC: ALAC (MPEG-4)
 * - BASSAPE: Monkey's Audio
 * - BASSOPUS: Opus
 * - BASSWV: WavPack
 * - BASSDSD: DSD (DSF / DSDIFF)
 */
object SupportedAudioFormats {

    /**
     * Indica si el par contenedor + códec está permitido.
     *
     * @param container Valor de MediaInfo [General.FORMAT] (puede ser vacío).
     * @param codec Valor de MediaInfo [Audio.FORMAT] (puede ser vacío).
     * @return true solo si el archivo puede reproducirse con los plugins
     *         actuales y TagLib puede leer sus metadatos de forma fiable.
     */
    fun isAllowed(container: String?, codec: String?): Boolean {
        val c = container?.trim()?.lowercase().orEmpty()
        val a = codec?.trim()?.lowercase().orEmpty()
        if (c.isEmpty() || a.isEmpty()) return false

        return when {
            // MP3 / MP2 / MP1
            c.contains("mpeg audio") && a.contains("mpeg audio") -> true

            // FLAC nativo
            c == "flac" && a.contains("flac") -> true

            // Ogg: FLAC, Vorbis u Opus
            c == "ogg" && (
                a.contains("flac") ||
                    a.contains("vorbis") ||
                    a.contains("opus")
                ) -> true

            // AAC en MPEG-4 / M4A
            c.contains("mpeg-4") && a.contains("aac") -> true

            // ALAC en MPEG-4 / M4A
            c.contains("mpeg-4") && a.contains("alac") -> true

            // AAC en ADTS (archivo .aac suelto)
            (c.contains("adts") || c == "aac") && a.contains("aac") -> true

            // Monkey's Audio
            c.contains("monkey") && a.contains("monkey") -> true

            // WavPack
            c.contains("wavpack") && a.contains("wavpack") -> true

            // DSD
            (c == "dsf" || c.contains("dsdiff") || c == "dff") &&
                a.contains("dsd") -> true

            // PCM en Wave / AIFF (BASS core)
            (c == "wave" || c.contains("aiff")) && a.contains("pcm") -> true

            else -> false
        }
    }
}