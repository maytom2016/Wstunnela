import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.feng.wstunnela.MainActivity
import com.feng.wstunnela.MyService

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
    }

    fun checkProcessAlive(): Boolean = service?.checkprocess() ?: false

}