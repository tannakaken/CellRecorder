package xyz.tannakaken.cell_recorder

import android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import android.os.PersistableBundle
import android.telephony.TelephonyManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import xyz.tannakaken.cell_recorder.databinding.ActivityMainBinding
import java.time.LocalDateTime


class MainActivity : AppCompatActivity() {
    companion object {
        const val LOGGING_STATE_TAG = "LOGGING_STATE_TAG"
    }

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var telephonyManager: TelephonyManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val _requestCodeLocation = 100
    private var logging = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        logging = savedInstanceState?.getBoolean(LOGGING_STATE_TAG) ?: false
        // content_main.xmlで「fragmentをFragmentContainerViewに変えろ」というlintのwarningが出るが、
        // それをすると
        // val navController = findNavController(R.id.nav_host_fragment_content_main)
        // という当初のコードでは
        // java.lang.IllegalStateException: Activity Caused by: java.lang.IllegalStateException: Activity {Activity} does not have a NavController set on  {nav_host_fragmentのid}
        // が出て落ちてしまう。
        // そこで以下のサイトを参考に集成した。
        // https://qiita.com/kkt_yu/items/2a5e0cebae3c36970116
        val navController = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main)!!.findNavController()
        // アプリが綴じていた間もロギングが実行中だった場合、最初のページを変更する。
        // TODO このコードにどれくらい意味があるかは、確認中
        if (logging) {
            val navGraph = navController.navInflater.inflate(R.navigation.nav_graph)
            navGraph.setStartDestination(R.id.nav_host_recording_fragment)
            navController.graph = navGraph
        }
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        requestPermissions()
        createNotificationChannel()

        binding.fab.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("このアプリについて")
                // このアプリを北見工業大学のプロジェクトとして集成する場合、必要があるならこの部分や連絡先を集成してください。
                .setMessage("このアプリは北見工業大学において、基地局情報と位置情報を取得するために作られたものである。\n連絡先：田中健策 <tannakaken@gmail.com>")
                .setNegativeButton("OK", null)
                .show()
        }
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        outState.putBoolean(LOGGING_STATE_TAG, logging)
        super.onSaveInstanceState(outState, outPersistentState)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == _requestCodeLocation) {
            if (grantResults[0] == PERMISSION_GRANTED) {
                Toast.makeText(this, "バックグラウンドでの位置情報の取得が許可されました。", Toast.LENGTH_SHORT).show()
            }
            else {
                Toast.makeText(this, "バックグラウンドでの位置情報の取得が拒否されました。", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> {
                AlertDialog.Builder(this)
                    .setTitle("設定")
                    .setMessage("今のところ設定すべき項目はない。")
                    .setNegativeButton("OK", null)
                    .show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        stopLogging()
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    private fun requestPermissions() {
        Log.d(this::class.java.simpleName, "requestPermissions")
        if (ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION, ACCESS_BACKGROUND_LOCATION), _requestCodeLocation)
        } else {
            Log.d(this::class.java.simpleName, "すでにバックグラウンドでの位置情報の取得を許可されている。")
        }
    }

    private fun createNotificationChannel() {
        Log.d(this::class.java.simpleName, "createNotificationChannel")
        val channel = NotificationChannel(
            LogService.CHANNEL_ID,
            "位置情報ログ収集開始のお知らせ",
            NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "位置情報ログの収集を開始しました。"
        }
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    fun startLogging() {
        Log.d(this::class.java.simpleName, "startLogging")
        logging = true
        val intent = Intent(this, LogService::class.java)
        startForegroundService(intent)
    }

    fun stopLogging() {
        Log.d(this::class.java.simpleName, "stopLogging")
        logging = false
        val intent = Intent(this, LogService::class.java)
        stopService(intent)
    }

    fun getLog(callback: (CellLogRow) -> Unit) {
        Log.d(this::class.java.simpleName, "getLog")
        if (ActivityCompat.checkSelfPermission(
                this,
                ACCESS_FINE_LOCATION
            ) != PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                this,
                ACCESS_COARSE_LOCATION
            ) != PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                this,
                ACCESS_BACKGROUND_LOCATION
            ) != PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "バックグラウンドでの位置情報の取得を許可されていません。", Toast.LENGTH_SHORT).show()
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            try {
                Log.d(this::class.java.simpleName,"$location")
                val cellInfoList = telephonyManager.allCellInfo
                Log.d(this::class.java.simpleName, cellInfoList.toString())
                callback(CellLogRow(location, cellInfoList, LocalDateTime.now()))
            } catch (exception: CellInfoException) {
                Toast.makeText(this, "基地局情報の取得ができませんでした。", Toast.LENGTH_SHORT).show()
            } catch (exception: UnsupportedOperationException) {
                Toast.makeText(this, "基地局情報の取得をサポートしていません。", Toast.LENGTH_SHORT).show()
            }
        }
    }
}