package com.feng.wstunnela

import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import android.widget.Toast

// ServiceController.kt
class ServiceController(private val context: Context) {
    private var service: MyService? = null
    var serviceBound = false
    private lateinit var myBinder: MyService.mBinder
    private lateinit var cmdstr: String



    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            myBinder = binder as MyService.mBinder
            service = myBinder.service
            val ctx=context as MainActivity
            service!!.wstunnel(cmdstr, ctx.vm)
            serviceBound = true
            ctx.vm.updateServiceBound(serviceBound)
//            val fragment=ctx.supportFragmentManager.findFragmentById(R.id.FragmentConfig) as FragmentConfig
//            fragment.update(serviceBound)
//            uiUpdateListener.update(serviceBound)

        }
        //服务异常才触发事件，正常解绑定服务无效
        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            serviceBound = false
            val ctx=context as MainActivity
            ctx.vm.updateServiceBound(serviceBound)
        }
    }
    fun bindService(command: String) {
        val ctx=context as MainActivity
//        if (!BatteryOptimizationChecker.isIgnoringBatteryOptimizations(ctx)) {
//            showRestrictionDialog(ctx)
//            return // 暂不启动服务
//        }
        cmdstr=command
        val intent = Intent(context, MyService::class.java)
//            .apply {
//            putExtra("COMMAND", command)
//            val ctx=context  as MainActivity
//        }
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }
    fun unbindService() {
        if (serviceBound) {
            context.unbindService(connection)
            serviceBound = false
        }
        //提示命令错误
        val ctx=context as MainActivity
//        Log.d("runlog",ctx.vm.runlog_textvie4.value.toString())
        if(ctx.vm.runlog_textvie4.value.toString().contains("Usage: wstunnel client") == true)
        {
            val message="${ctx.getString(R.string.illlegalwstunnelcmd)} : ${ctx.vm.runlog_textvie4.value}"
            Toast.makeText(ctx, message, Toast.LENGTH_LONG).show()
        }
    }

    fun checkProcessAlive(): Boolean = service?.checkprocess() ?: false


    private fun showRestrictionDialog(ctx: Context) {
        val dialog = AlertDialog.Builder(ctx)
            .setTitle(ctx.getString(R.string.battery_permission_title))
            .setMessage(ctx.getString(R.string.battery_permission_content))
            .setPositiveButton(ctx.getString(R.string.background_running_set)) { _, _ ->
//                BatteryOptimizationChecker.openRestrictionSettings(ctx)

//                BatteryOptimizationChecker.openAutoStartSettings(ctx)
            }
            .setNegativeButton(ctx.getString(R.string.cancel_button)) { _, _ ->
//                stopSelf() // 用户拒绝则停止服务
            }
            .setCancelable(false)
            .create()
        dialog.show()
    }

}