import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log

object BatteryOptimizationChecker {

    /**
     * 检测是否受电池优化限制
     * @return true 表示需要引导用户关闭优化
     */

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return  powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }
    /**
     * 使得忽略电池优化此应用
     */
    @SuppressLint("BatteryLife")
    fun openRestrictionSettings(ctx:Context) {
        try {
            val intent = Intent().apply {
                action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                data = Uri.parse("package:${ctx.packageName}")
            }
            ctx.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // 跳转失败时降级处理
            Log.d("openRestrictionSettings","更改忽略电池优化设置失败")
        }
    }
//    fun hasBackgroundRestrictions(ctx: Context): Boolean {
//        // 检测电池优化
//        val isBatteryOptimized = !isIgnoringBatteryOptimizations(ctx)
//        return isBatteryOptimized
//    }
}