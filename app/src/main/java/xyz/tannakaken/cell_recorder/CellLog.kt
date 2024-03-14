package xyz.tannakaken.cell_recorder

import android.location.Location
import android.telephony.CellIdentity
import android.telephony.CellIdentityNr
import android.telephony.CellInfo
import android.telephony.CellInfoCdma
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellInfoTdscdma
import android.telephony.CellInfoWcdma
import android.telephony.CellSignalStrength
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.Exception

/**
 * https://developer.android.com/reference/android/location/Location.html
 *
 * TODO もっと細かい情報を拾える
 */
@Serializable
data class SerializableLocation(private val altitude: Double, private val longitude: Double) {
    constructor(location: Location): this(location.altitude, location.longitude)
}

/**
 * https://developer.android.com/reference/android/telephony/CellIdentity?_gl=1*1ot98te*_up*MQ..*_ga*MzgyOTQ4NDEyLjE3MTAyMjIxOTQ.*_ga_6HH9YJMN9M*MTcxMDIyMjE5My4xLjAuMTcxMDIyMjE5NC4wLjAuMA..
 *
 * TODO まだ細かい基地局データは取れていない。手元の環境だと「KDDI」しか情報が取れていない。もっと細かい情報を得るためには、CellIdentityの具体的なクラスの仕様に踏み込む必要がある。
 */
@Serializable
data class SerializableCellIdentity(private val operatorAlphaLong: String, private val operatorAlphaShort: String) {
    constructor(cellIdentity: CellIdentity): this(cellIdentity.operatorAlphaLong.toString(), cellIdentity.operatorAlphaShort.toString())
}

/**
 * https://developer.android.com/reference/android/telephony/CellSignalStrength?_gl=1*1aljt1n*_up*MQ..*_ga*MzgyOTQ4NDEyLjE3MTAyMjIxOTQ.*_ga_6HH9YJMN9M*MTcxMDIyMjE5My4xLjAuMTcxMDIyMjE5NC4wLjAuMA..
 */
@Serializable
data class SerializableCellSignalStrength(private val asuLevel: Int, private val dbm: Int, private val level: Int) {
    constructor(cellSignalStrength: CellSignalStrength): this(cellSignalStrength.asuLevel, cellSignalStrength.dbm, cellSignalStrength.level)
}

class CellInfoException(message: String): Exception(message)

/**
 * minimum API Levelが30以上ならCellInfoから直接CellIdentityが取れるのだが、手元の端末が29なので、無理やり行う。
 *
 * https://developer.android.com/reference/android/telephony/CellInfo
 *
 * と
 *
 * https://developer.android.com/reference/android/telephony/CellInfo#getCellIdentity()
 * を参照
 *
 */
fun getCellIdentity(cellInfo: CellInfo): CellIdentity {
    return when (cellInfo) {
        is CellInfoCdma -> cellInfo.cellIdentity
        is CellInfoGsm -> cellInfo.cellIdentity
        is CellInfoLte -> cellInfo.cellIdentity
        is CellInfoNr -> cellInfo.cellIdentity
        is CellInfoTdscdma -> cellInfo.cellIdentity
        is CellInfoWcdma -> cellInfo.cellIdentity
        else -> throw CellInfoException("CellIdentityが取得できません。")
    }
}

/**
 * minimum API Levelが30以上ならCellInfoから直接CellSignalIdetityが取れるのだが、手元の端末が29なので、無理やり行う。
 *
 * https://developer.android.com/reference/android/telephony/CellInfo
 *
 * と
 *
 * https://developer.android.com/reference/android/telephony/CellInfo#getCellSignalIdentity()
 * を参照
 */
fun getCellSignalStrength(cellInfo: CellInfo): CellSignalStrength {
    return when (cellInfo) {
        is CellInfoCdma -> cellInfo.cellSignalStrength
        is CellInfoGsm -> cellInfo.cellSignalStrength
        is CellInfoLte -> cellInfo.cellSignalStrength
        is CellInfoNr -> cellInfo.cellSignalStrength
        is CellInfoTdscdma -> cellInfo.cellSignalStrength
        is CellInfoWcdma -> cellInfo.cellSignalStrength
        else -> throw CellInfoException("CellSignalStrengthが取得できません。")
    }
}

/**
 * https://developer.android.com/reference/android/telephony/CellInfo
 */
@Serializable
data class SerializableCellInfo(private val connectionStatus: Int, private val cellIdentity: SerializableCellIdentity, private val cellSignalStrength: SerializableCellSignalStrength) {
    constructor(cellInfo: CellInfo): this(cellInfo.cellConnectionStatus, SerializableCellIdentity(
        getCellIdentity(cellInfo)), SerializableCellSignalStrength(getCellSignalStrength(cellInfo)))
}

fun toSerializableCellInfoList(cellInfoList: List<CellInfo>): List<SerializableCellInfo> {
    return cellInfoList.map { cellInfo: CellInfo ->  SerializableCellInfo(cellInfo) }
}

@Serializer(forClass = LocalDateTime::class)
@kotlinx.serialization.ExperimentalSerializationApi
object DateTimeSerializer : KSerializer<LocalDateTime> {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        encoder.encodeString(value.format(formatter))
    }

    override fun deserialize(decoder: Decoder): LocalDateTime {
        return LocalDateTime.parse(decoder.decodeString(), formatter)
    }
}

@Serializable
data class CellLogRow @OptIn(ExperimentalSerializationApi::class) constructor(
    private val location: SerializableLocation,
    private val cellInfoList: List<SerializableCellInfo>,
    @Serializable(with = DateTimeSerializer::class)
    private val datetime: LocalDateTime) {
    constructor(location: Location, cellInfoList: List<CellInfo>, dateTime: LocalDateTime): this(SerializableLocation(location), toSerializableCellInfoList(cellInfoList), dateTime)
}

@Serializable
data class CellLog(private val logs: List<CellLogRow>)
