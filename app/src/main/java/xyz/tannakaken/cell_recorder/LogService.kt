package xyz.tannakaken.cell_recorder

import android.Manifest
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 *
 * フォアグラウンドサービスにすることによって、アプリが表示されていない場合、別のアプリが表示されている状態、画面スリープ状態、でも処理が続行される。
 *
 * 以下を参考にした
 *
 * https://dev.classmethod.jp/articles/android-use-foreground-service-for-location-background/
 *
 * 以下も参照せよ
 * https://support.google.com/googleplay/android-developer/answer/13392821?hl=ja
 */
class LogService: Service() {

    private lateinit var telephonyManager: TelephonyManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var data: MutableList<CellLogRow> = mutableListOf()
    companion object {
        const val CHANNEL_ID = "777"
    }
    override fun onCreate() {
        Log.d(this::class.java.simpleName, "onCreate")
        super.onCreate()
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(this::class.java.simpleName, "onStartCommand")
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                // 一応権限のチェック
                if (ActivityCompat.checkSelfPermission(
                        this@LogService,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(
                        this@LogService,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    Toast.makeText(this@LogService, "位置情報の取得を許可されていません。", Toast.LENGTH_SHORT).show()
                    return
                }
                val location = locationResult.lastLocation // 最新の情報だけ拾う
                location ?: return
                try {
                    Log.d("LocationSensor","$location")
                    val cellInfoList = telephonyManager.allCellInfo
                    Log.d(this::class.java.simpleName, cellInfoList.toString())
                    data.add(CellLogRow(location, cellInfoList, LocalDateTime.now()))
                } catch (exception: CellInfoException) {
                    Toast.makeText(this@LogService, "基地局情報の取得ができませんでした。", Toast.LENGTH_SHORT).show()
                } catch (exception: UnsupportedOperationException) {
                    Toast.makeText(this@LogService, "基地局情報の取得をサポートしていません。", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val openIntent = Intent(this, MainActivity::class.java).let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_UPDATE_CURRENT)
        }
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("基地局情報取得")
            .setContentText("基地局情報と位置情報を取得しています...")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(openIntent)
            .build()

        startForeground(9999, notification)

        startLocationUpdates()

        return START_STICKY
    }

    override fun onBind(p0: Intent?): IBinder? {
        Log.d(this::class.java.simpleName, "onBind")
        return null
    }

    override fun stopService(name: Intent?): Boolean {
        stopLocationUpdates()
        return super.stopService(name)
    }

    override fun onDestroy() {
        stopLocationUpdates()
        stopSelf()
        super.onDestroy()
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "バックグラウンドでの位置情報の取得を許可されていません。", Toast.LENGTH_SHORT).show()
            return
        }
        val locationRequest = createLocationRequest()
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            null)
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        saveData()
        data = mutableListOf()
    }

    private fun createLocationRequest(): LocationRequest {
        return LocationRequest.Builder(1000)
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY).build()
    }

    private fun saveData() {
        Log.d(this::class.java.simpleName, "saveData")
        val cellLog = CellLog(data)
        // データの保存
        val date = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
        val filename = "celllog_${date}.json"
        val file = File(getExternalFilesDir(""), filename)
        try {
            file.bufferedWriter().use {
                val jsonData = Json.encodeToString(cellLog)
                it.write(jsonData)
                it.flush()
                Toast.makeText(this, "ファイルの保存に成功しました", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "ファイルの保存に失敗しました", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }
}