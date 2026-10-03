package com.github.kr328.clash.core.model

import android.os.Parcel
import android.os.Parcelable
import com.github.kr328.clash.common.util.createListFromParcelSlice
import com.github.kr328.clash.common.util.writeToParcelSlice

class ConnectionList(data: List<ConnectionInfo>) : List<ConnectionInfo> by data, Parcelable {
    constructor(parcel: Parcel) : this(ConnectionInfo.CREATOR.createListFromParcelSlice(parcel, 0, 32))

    override fun describeContents(): Int = 0

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        writeToParcelSlice(parcel, flags)
    }

    companion object CREATOR : Parcelable.Creator<ConnectionList> {
        override fun createFromParcel(parcel: Parcel): ConnectionList = ConnectionList(parcel)

        override fun newArray(size: Int): Array<ConnectionList?> = arrayOfNulls(size)
    }
}
