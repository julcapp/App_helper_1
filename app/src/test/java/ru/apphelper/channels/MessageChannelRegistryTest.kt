package ru.apphelper.channels

import org.junit.Assert.assertEquals
import org.junit.Test

class MessageChannelRegistryTest {
    @Test fun identifiesMax() {
        assertEquals(MessageChannel.MAX, MessageChannelRegistry.detect("ru.oneme.app"))
    }

    @Test fun identifiesTelegram() {
        assertEquals(MessageChannel.TELEGRAM, MessageChannelRegistry.detect("org.telegram.messenger"))
    }

    @Test fun identifiesWhatsAppAndBusinessVariant() {
        assertEquals(MessageChannel.WHATSAPP, MessageChannelRegistry.detect("com.whatsapp"))
        assertEquals(MessageChannel.WHATSAPP, MessageChannelRegistry.detect("com.whatsapp.w4b"))
    }

    @Test fun unrecognizedPackagesStayUnknown() {
        assertEquals(MessageChannel.OTHER, MessageChannelRegistry.detect("com.example.unknown"))
        assertEquals(MessageChannel.OTHER, MessageChannelRegistry.detect(""))
    }
}
