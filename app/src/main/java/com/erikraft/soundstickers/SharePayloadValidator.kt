package com.erikraft.soundstickers

internal data class ValidatedSharePayload(
    val base64Content: String,
    val mimeType: String,
    val fileName: String
)

internal object SharePayloadValidator {
    const val MAX_SHARE_BYTES = 80L * 1024L * 1024L
    private const val MAX_NAME_LENGTH = 120
    private val base64Pattern = Regex("^[A-Za-z0-9+/]+={0,2}$")
    private val safeNamePattern = Regex("^[A-Za-z0-9][A-Za-z0-9._-]{0,119}$")

    fun isEncodedLengthAllowed(length: Long): Boolean {
        val maxEncodedCharacters = ((MAX_SHARE_BYTES + 2L) / 3L) * 4L
        return length in 4L..maxEncodedCharacters && length % 4L == 0L
    }

    fun validate(
        encodedContent: String?,
        contentType: String?,
        fileName: String?
    ): ValidatedSharePayload? {
        val encoded = encodedContent ?: return null
        if (!isEncodedLengthAllowed(encoded.length.toLong()) ||
            !base64Pattern.matches(encoded)
        ) return null

        val rawName = fileName?.trim() ?: return null
        if (rawName.length > MAX_NAME_LENGTH ||
            !safeNamePattern.matches(rawName) || ".." in rawName
        ) return null

        val mime = contentType?.lowercase()?.substringBefore(';')?.trim() ?: return null
        val extension = rawName.substringAfterLast('.', "").lowercase()
        val supported = when (mime) {
            "application/zip" -> extension == "zip"
            "video/mp4" -> extension == "mp4"
            "video/webm" -> extension == "webm"
            else -> false
        }
        if (!supported) return null

        return ValidatedSharePayload(encoded, mime, rawName)
    }
}
