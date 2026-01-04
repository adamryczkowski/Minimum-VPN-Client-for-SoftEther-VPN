package kittoku.mvc.statistics

import androidx.room.TypeConverter

/**
 * Room TypeConverter for DisconnectReason enum.
 */
class DisconnectReasonConverter {
    @TypeConverter
    fun fromDisconnectReason(reason: DisconnectReason): String {
        return reason.name
    }

    @TypeConverter
    fun toDisconnectReason(value: String): DisconnectReason {
        return try {
            DisconnectReason.valueOf(value)
        } catch (e: IllegalArgumentException) {
            DisconnectReason.UNKNOWN
        }
    }
}
