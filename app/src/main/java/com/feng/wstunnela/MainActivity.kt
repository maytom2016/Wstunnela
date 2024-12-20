package com.feng.wstunnela
import android.app.*
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.text.Html
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.lifecycle.lifecycleScope
import com.feng.wstunnela.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import java.io.*
import java.nio.charset.Charset
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull
import java.lang.Thread.sleep
import kotlin.collections.forEach
import kotlin.collections.map

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var service: MyService? = null
    lateinit var myBinder:MyService.mBinder
    lateinit var scope: Job
    lateinit var Crontask:Job
    lateinit var binpath:String
    private var serviceBound = false
    private val connection=object : ServiceConnection {
        override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
            myBinder=p1 as MyService.mBinder
            service = myBinder.service
            service!!.wstunnel(binpath, binding)
            serviceBound=true
            //定时检查后台ws进程是否存在，如果不存在就杀死服务
            Crontask=lifecycleScope.launch {
                repeat(Int.MAX_VALUE){
                    delay(5000L)
                    if (!service!!.checkprocess()) {
                        setdisableservice()
                        Crontask.cancel()
                    }
                }
            }
            //启动wstunnel后立即检查进程情况，若不存在则注销服务。
            scope=lifecycleScope.launch {
                delay(500)
                if (service!!.checkprocess()) {
                    setenableservice()
                    savecmd()
                }
                else
                {
                    setdisableservice()
                    Crontask.cancel()
                }
            }
            Log.d("info","connect")
        }
        override fun onServiceDisconnected(p0: ComponentName?) {
            //服务异常结束才触发，正常时无用
            service =null
            serviceBound=false
            Log.d("info","disconnect")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binpath=this.getString(R.string.binfilepath)

        var tarfilestr="$filesDir/test.tar.gz"
        if(!File(binpath).exists() && !File(tarfilestr).exists()) {
            val dialog = DialogUtil.showDownloadProgress(this@MainActivity, "正在下载...")
            val view = dialog.findViewById<ProgressBar>(R.id.d_progress_bar)
            CoroutineScope(Dispatchers.IO).launch {
                copybinfromgithub(dialog,view,tarfilestr)
            }
        }
        else if(!File(binpath).exists() && File(tarfilestr).exists())
        {
            //解压wstunnel文件到执行目录
            val lastIndex = binpath.lastIndexOf('/')
            val binpath2 = binpath.substring(0,lastIndex + 1)
            if(File(tarfilestr).exists()){
                DownloadManager.extractTarGz(tarfilestr,binpath2)
            }
        }
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        getwstunnelversion()
        getsystemarch()
        readcmd()
        binding.textView4.movementMethod = ScrollingMovementMethod.getInstance()
        binding.button.setOnClickListener{
            if(binding.button.text==getString(R.string.buttonstart)&& chekcklegalwstunnelcmd()) {
                val  intent=Intent(this,MyService::class.java)
                bindService(intent,connection,Context.BIND_AUTO_CREATE)
            }
            else if(binding.button.text==getString(R.string.buttonstop)){
                setdisableservice()
                if(this::Crontask.isInitialized && Crontask.isActive) {
                    Crontask.cancel()
                }
            }
            else
            {
                Toast.makeText(this, R.string.illlegalwstunnelcmd, Toast.LENGTH_SHORT).show()
            }
        }

    }
    fun setenableservice()
    {
        binding.button.text=this.getString(R.string.buttonstop)
        binding.textView31.text=this.getString(R.string.buttonstart)
        binding.textView31.setTextColor(this.getColor(R.color.forestgreen))
        binding.editText.isEnabled=false
    }
    fun setdisableservice()
    {
        if(serviceBound&&this::myBinder.isInitialized)
        {
                unbindService(connection)//解绑Service
        }

        binding.editText.isEnabled=true
        binding.button.text=this.getString(R.string.buttonstart)
        binding.textView31.text=this.getString(R.string.buttonstop)
        binding.textView31.setTextColor(this.getColor(R.color.red))
        binding.textView4.text=getString(R.string.errormessage)
    }
    fun getsystemarch()
    {
        val arch = System.getProperty("os.arch")
        binding.textView21.text=arch

    }
    fun getwstunnelversion()
    {
        val cmd= "$binpath --version"
        val exeFile = File(binpath)
        if (!exeFile.exists()) {
            if(!copybin(binpath)) return
        }
        exeFile.setExecutable(true, true)
        binding.textView1.text = execCmd(cmd.split("\\s+".toRegex()).toTypedArray())
    }
    @Throws(IOException::class)
    private fun copybin(strOutFileName: String):Boolean {

        val dirstr=(".+/".toRegex()).find(binpath)
        val dir=File(dirstr?.value)

        if(dir.exists()) {
            val fis: InputStream?
            val fos = FileOutputStream(strOutFileName)
            //目前仅考虑64位架构，因为绝大多数安卓设备都已经是64位系统
            var arch = System.getProperty("os.arch")?.lowercase()
            if (arch==null)arch="error"
            if (arch.contains("x86_64") or arch.contains("amd64")) {
                fis = this.getAssets().open(getString(R.string.wsamd64))
            } else if (arch.contains("armv8") or arch.contains("arm64") or arch.contains("aarch64")) {
                fis = this.getAssets().open(getString(R.string.wsarm64))
            } else {
                binding.textView1.text = this.getString(R.string.nosupportremind)
                binding.textView1.setTextColor(this.getColor(R.color.red))
                binding.button.isEnabled = false
                fos.close()
                return false
            }

            val b = ByteArray(fis.available())
            val length = fis.read(b)
            if (length > 0) {
                fos.write(b, 0, length)
            }
            fos.flush()
            fis.close()
            fos.close()
        }
            val file = File(strOutFileName)
            return file.exists()
    }
    suspend fun copybinfromgithub( dialog:AlertDialog,view:ProgressBar,tarfilestr:String)
    {
        //程序路径
        var downloadpath= "$filesDir"
        var downloadurl=""
        var filename=""
        //使用api获取最新版本下载地址
        var arch = System.getProperty("os.arch")?.lowercase()
        if (arch==null)arch="error"
        if (arch.contains("x86_64") or arch.contains("amd64")){
            arch="linux_amd64"
        }
        else if(arch.contains("armv8") or arch.contains("arm64") or arch.contains("aarch64")){
            arch="android_arm64"
        }
        val alltar=DownloadManager.getGithubRelease().firstOrNull()?.assets
        if (alltar != null) {
            val urls=alltar.map{it.browser_download_url}
            urls.forEach { url->
                if(url.contains(arch))
                {
                    downloadurl=url
                    val lastIndex = url.lastIndexOf('/')
                    filename = url.substring(lastIndex + 1)
                }
            }
        }
//        println("~~~"+filename)
        filename="test.tar.gz"
//        println("~~~path"+downloadpath)
//        println("~~~downloadurl"+downloadurl)
        //下载tar.gz包

        DownloadManager.download(downloadurl,
            File(downloadpath, filename)
        ).collect {
        //collect有可能报错，解决方法为头部增加import kotlinx.coroutines.flow.collect
            when (it) {
                is DownloadState.InProgress -> {
                    Log.d("~~~", "download in progress: ${it.progress}.")
                        view.progress = it.progress
                }
                is DownloadState.Success -> {
                    Log.d("~~~", "download finished.")
                    dialog.dismiss()
                }
                is DownloadState.Error -> {
                    Log.d("~~~", "download error: ${it.throwable}.")
                    dialog.dismiss()
                }
            }
        }
        //解压文件
        val lastIndex = binpath.lastIndexOf('/')
        val binpath2 = binpath.substring(0,lastIndex + 1)
//        println("~~~$binpath2")
        if(File(tarfilestr).exists()){
            DownloadManager.extractTarGz(tarfilestr,binpath2)
        }
    }
    @Throws(IOException::class)
    private fun execCmd(cmd:Array<String>):String {
        try {
            val process = Runtime.getRuntime().exec(cmd)
            val isr = InputStreamReader(process.inputStream)
            val input = BufferedReader(isr)
            var line: String?
            var stdstr=""

            while (null != input.readLine().also { line = it }) {
                Log.e("##info", line!!)
                stdstr+=line
            }
            process.destroy()
            isr.close()
            input.close()
            return stdstr
        }
        catch (e: IOException) {
            e.printStackTrace()
        }
        return ""
    }
    fun chekcklegalwstunnelcmd():Boolean
    {
        if(binding.editText.text.toString().contains("wstunnel"))return true
        return false
    }
    fun savecmd()
    {
        savecfg("cmd",binding.editText.text.toString())
    }
    fun readcmd()
    {
        val read=readlasttimecfg("cmd")
        if (read!="error") {
            binding.editText.setText(read)
        }
    }
    fun savecfg(FILENAME:String, filestr: String)
    {
        try {
            val fos = this.openFileOutput(FILENAME, MODE_PRIVATE)
            fos.write(filestr.toByteArray())
            fos.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
    fun readlasttimecfg(FILENAME:String):String
    {
        val filestr:String?
        try {
            val fis = this.openFileInput(FILENAME)
            val b = ByteArray(fis.available())
            fis.read(b)
            filestr = String(b)
            fis.close()
            return filestr

        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return "error"
    }
}


class MyService : Service() {
    private val myBinder=mBinder()
    private lateinit var process:Process
    private lateinit var isr: InputStreamReader
    private lateinit var bfr: BufferedReader
    inner class mBinder: Binder(){
        val service: MyService get() = this@MyService
    }
    override fun onBind(intent: Intent): IBinder {
        Log.d("data","onBind")
        return myBinder
    }


    override fun onCreate() {
        super.onCreate()
        Log.d("data","onCreate")

        val manager=getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.O){
            val channel= NotificationChannel("my_service","前台Service通知",NotificationManager.IMPORTANCE_DEFAULT)
            manager.createNotificationChannel(channel)
        }
        //设置通知栏通知打开软件，默认是打开应用详情
        val intent=Intent(this,MainActivity::class.java)
        val pi= PendingIntent.getActivity(this,0,intent, PendingIntent.FLAG_IMMUTABLE)
        //设置通知配置
        val notification= NotificationCompat.Builder(this,"my_service")
            .setContentTitle(this.getString(R.string.Notificationtitle))
            .setContentText(this.getString(R.string.Notificationcontent))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setDefaults(Notification.DEFAULT_SOUND)
            .setContentIntent(pi)
            .build()
        startForeground(1,notification)
    }

    override fun onDestroy() {
        super.onDestroy()

        CoroutineScope(Dispatchers.IO).launch {
            exit()
        }

        Log.d("data","onDestroy")
    }
    fun wstunnel(binpath:String,binding: ActivityMainBinding){
        //val cmdstr=binpath+"  client -L udp://23456:localhost:23456?timeout_sec=0 wss://exam.com:1709"
        val cmdstr=binpath+binding.editText.text.toString().replace("wstunnel","").replace("\"","").replace("\'","")
        val strs =cmdstr.split("\\s+".toRegex()).toList()
        val textrecycle = Channel<String>()
        CoroutineScope(Dispatchers.IO).async {
            exeshell(strs, textrecycle)
        }
        CoroutineScope(Dispatchers.Main).async {
            var text=""
             for(value in textrecycle) {
                 text+=value
                 if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                     binding.textView4.setText(Html.fromHtml(text,Html.FROM_HTML_MODE_LEGACY))
                 } else {
                     binding.textView4.setText(Html.fromHtml(text))
                 }
            }
        }

    }

    fun exit()
    {
        if(this::process.isInitialized) {
            process.destroy()
        }
            isr.close()
            bfr.close()


    }
    suspend fun exeshell(shell: List<String>,channel:Channel<String>)
    {
        val pb: ProcessBuilder = ProcessBuilder(shell).redirectErrorStream(true)
        process = withContext(Dispatchers.IO) {
            pb.start()
        }
        isr = InputStreamReader(process.inputStream, Charset.forName("iso-8859-1"))
        bfr = BufferedReader(isr)
        var line: String?

        while (null != withContext(Dispatchers.IO) {
                bfr.readLine()
            }.also { line = it }) {
            Log.e("info", line!!)
            channel.send(makeoutputstr(line))
        }
        withContext(Dispatchers.IO) {
            process.waitFor()
        }
        channel.close()
        exit()
    }
    fun checkprocess():Boolean{
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O&&this::process.isInitialized) {
            process.isAlive
        }else return false
    }
    fun makeoutputstr(logstr :String?):String
    {
        val map = mapOf("31m" to "red" , "32m" to "green","33m" to "#f5b041","2m" to "blue","0m" to "black")
        //部分实现映射集合ANSI Escape Sequences
        val allansicolor=logstr?.let { Regex("\u001B\\[\\d+m\\s*(\u001B\\[\\d+m)*").findAll(logstr) }
        //ANSI Escape Sequences分割符
        val ret= logstr?.split("\u001B\\[\\d+m\\s*(\u001B\\[\\d+m)*".toRegex())?.toTypedArray()
        //log日志内容
        var finalstr=""
        allansicolor?.forEachIndexed {index,it ->
//            Log.d("esc",it.value)
            val content= ret?.get(index+1)
            for ((key,value)in map)
            {
                if (it.value.contains(key))
                {
                    val color=value
                    finalstr="$finalstr<font color=\'$color\'>$content</font> "
                    break
                }
            }
        }
        return finalstr
    }
}