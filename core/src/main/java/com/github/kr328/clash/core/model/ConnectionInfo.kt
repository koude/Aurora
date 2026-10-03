package com.github.kr328.clash.core.model

import android.os.Parcel
import android.os.Parcelable
import com.github.kr328.clash.core.util.Parcelizer
import kotlinx.serialization.Serializable

@Serializable
data class ConnectionInfo(
    val id: String,
    val host: String = "",
    val process: String = "",
    val network: String = "",
    val destination: String = "",
    val rule: String = "",
    val rulePayload: String = "",
    val chains: List<String> = emptyList(),
    val uploaded: Long = 0,
    val downloaded: Long = 0,
    val startedAt: Long = 0,
) : Parcelable {
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        Parcelizer.encodeToParcel(serializer(), parcel, this)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<ConnectionInfo> {
        override fun createFromParcel(parcel: Parcel): ConnectionInfo =
            Parcelizer.decodeFromParcel(serializer(), parcel)

        override fun newArray(size: Int): Array<ConnectionInfo?> = arrayOfNulls(size)
    }
}
