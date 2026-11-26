package com.erikraft.soundstickers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SharePayloadValidatorTest {
    @Test
    fun acceptsSupportedVideoAndArchivePayloads() {
        assertNotNull(SharePayloadValidator.validate("AAAA", "video/mp4;codecs=avc1", "clip.mp4"))
        assertNotNull(SharePayloadValidator.validate("AAAA", "video/webm", "clip.webm"))
        assertNotNull(SharePayloadValidator.validate("AAAA", "application/zip", "pack.zip"))
    }

    @Test
    fun rejectsUnsupportedMimeTypesAndMismatchedExtensions() {
        assertNull(SharePayloadValidator.validate("AAAA", "text/html", "payload.html"))
        assertNull(SharePayloadValidator.validate("AAAA", "video/mp4", "clip.webm"))
        assertNull(SharePayloadValidator.validate("AAAA", "application/zip", "pack.mp4"))
    }

    @Test
    fun rejectsPathLikeAndUnsafeFileNames() {
        assertNull(SharePayloadValidator.validate("AAAA", "application/zip", "../pack.zip"))
        assertNull(SharePayloadValidator.validate("AAAA", "application/zip", "folder/pack.zip"))
        assertNull(SharePayloadValidator.validate("AAAA", "application/zip", "pack..zip"))
    }

    @Test
    fun rejectsMalformedBase64AndOversizedPayloadLengths() {
        assertNull(SharePayloadValidator.validate("AAAA!", "video/mp4", "clip.mp4"))
        assertFalse(SharePayloadValidator.isEncodedLengthAllowed(SharePayloadValidator.MAX_SHARE_BYTES * 2L))
        assertFalse(SharePayloadValidator.isEncodedLengthAllowed(0L))
        assertTrue(SharePayloadValidator.isEncodedLengthAllowed(4L))
    }

    @Test
    fun rejectsNonAsciiOrOversizedFileNames() {
        assertNull(SharePayloadValidator.validate("AAAA", "video/mp4", "clípe.mp4"))
        assertNull(SharePayloadValidator.validate("AAAA", "video/mp4", "a".repeat(121) + ".mp4"))
        val clean = SharePayloadValidator.validate("AAAA", "video/mp4", "clip-01.mp4")
        assertEquals("clip-01.mp4", clean?.fileName)
    }
}
