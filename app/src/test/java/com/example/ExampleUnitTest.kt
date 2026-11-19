package com.example

import com.example.model.CallTone
import com.example.voice.PhoneCallLauncher
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun sanitizePhoneNumber_stripsFormattingAndLetters() {
        assertEquals("+18005550192", PhoneCallLauncher.sanitizePhoneNumber(" +1 (800) 555-0192 "))
        assertEquals("5550144", PhoneCallLauncher.sanitizePhoneNumber("tel: 555-0144"))
    }

    @Test
    fun callTone_fromRaw_parsesCaseInsensitivelyWithFallback() {
        assertEquals(CallTone.FIRM, CallTone.fromRaw("firm"))
        assertEquals(CallTone.POLITE, CallTone.fromRaw("POLITE"))
        assertEquals(CallTone.PROFESSIONAL, CallTone.fromRaw("unknown"))
    }
}
