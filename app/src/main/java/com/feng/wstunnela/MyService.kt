package com.feng.wstunnela

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.text.Html
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.Charset


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

        val manager=getSystemService(NOTIFICATION_SERVICE) as NotificationManager
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
    fun wstunnel(cmdstr:String,vm:vm){

        val strs =cmdstr.split("\\s+".toRegex()).toList()
        val textrecycle = Channel<String>()
        CoroutineScope(Dispatchers.IO).async {
            exeshell(strs, textrecycle)
        }
        CoroutineScope(Dispatchers.Main).async {
            var text=""
            for(value in textrecycle) {
                text+= "$value<br>"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

                    vm.runlog_textvie4.value= Html.fromHtml(text,Html.FROM_HTML_MODE_LEGACY)
                } else {
                    vm.runlog_textvie4.value= Html.fromHtml(text)
                }
            }
        }
    }

    fun exit()
    {
        if(this::process.isInitialized) {
            process.destroy()
        }
        if(this::isr.isInitialized)isr.close()

        if(this::bfr.isInitialized)bfr.close()


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