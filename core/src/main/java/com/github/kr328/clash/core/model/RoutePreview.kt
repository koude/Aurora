package com.github.kr328.clash.core.model

import android.os.Parcel
import android.os.Parcelable
import com.github.kr328.clash.core.util.Parcelizer
import kotlinx.serialization.Serializable

@Serializable
data class RoutePreview(
    val target: String,
    val mode: String,
    val rule: String,
    val policy: String,
    val outbound: String,
    val resolvedIp: String? = null,
    val error: String? = null,
) : Parcelable {
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        Parcelizer.encodeToParcel(serializer(), parcel, this)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<RoutePreview> {
        override fun createFromParcel(parcel: Parcel): RoutePreview =
            Parcelizer.decodeFromParcel(serializer(), parcel)

        override fun newArray(size: Int): Array<RoutePreview?> = arrayOfNulls(size)
    }
}
