package dev.ex4.wetbulbweather.api.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.text.SimpleDateFormat
import java.util.*

object DateSerializer : KSerializer<Date> {

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Date", PrimitiveKind.STRING)

    private val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ") // ISO-8601 date format

    override fun deserialize(decoder: Decoder): Date {
        return fmt.parse(decoder.decodeString())
    }

    override fun serialize(encoder: Encoder, value: Date) {
        encoder.encodeString(fmt.format(value))
    }
}
