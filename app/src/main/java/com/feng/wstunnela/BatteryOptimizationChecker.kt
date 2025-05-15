import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import com.feng.wstunnela.MainActivity
import java.util.Locale

object BatteryOptimizationChecker {

    lateinit var ctx: MainActivity
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
    fun openRestrictionSettings() {
        try {
            val intent = Intent().apply {
                action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                data = Uri.parse("package:${ctx.packageName}")
            }
            ctx.vm.batteryOptimizationLauncher.launch(intent)
            //ctx.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // 跳转失败时降级处理
            Log.d("openRestrictionSettings","更改忽略电池优化设置失败")
        }
    }

    fun openAutoStartSettings() {
        val brand = Build.BRAND.lowercase(Locale.ROOT) // 替代 Build.MANUFACTURER
        var success = true

        try {
            when {
                brand.contains("xiaomi") -> {
                    showActivity(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.autostart.AutoStartManagementActivity"
                    )
                }

                brand.contains("oppo") -> {
                    try {
                        showActivity("com.coloros.phonemanager")
                    } catch (e1: java.lang.Exception) {
                        try {
                            showActivity("com.oppo.safe")
                        } catch (e2: java.lang.Exception) {
                            try {
                                showActivity("com.coloros.oppoguardelf")
                            } catch (e3: java.lang.Exception) {
                                showActivity("com.coloros.safecenter")
                            }
                        }
                    }
                }

                brand.contains("vivo") -> {
                    showActivity("com.iqoo.secure")
                }

                brand.contains("huawei") || brand.contains("honor") -> {
                    try {
                        showActivity(
                            "com.huawei.systemmanager",
                            "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                        )
                    } catch (e: Exception) {
                        showActivity(
                            "com.huawei.systemmanager",
                            "com.huawei.systemmanager.optimize.bootstart.BootStartActivity"
                        )
                    }
                }

                brand.contains("samsung") -> {
                    try {
                        showActivity("com.samsung.android.sm_cn")
                    } catch (e: java.lang.Exception) {
                        showActivity("com.samsung.android.sm")
                    }
                }
                brand.contains("meizu") -> {
                    showActivity("com.meizu.safe")
                }

                else -> {

                    showActivity("com.android.settings")
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
            success = false
        }

        if (!success) {
            // 如果跳转失败，提示用户手动前往设置
            Toast.makeText(
                ctx,
                "无法自动跳转，请手动前往设置开启自启动权限",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    /**
     * 跳转到指定应用的首页
     */
    private fun showActivity(packageName: String) {
        val intent = ctx.getPackageManager().getLaunchIntentForPackage(packageName)
        ctx.vm.backgroundExecutionLauncher.launch(intent)
    }
    /**
     * 跳转到指定应用的指定页面
     */
    private fun showActivity(packageName: String, activityDir: String) {
        val intent = Intent().apply{
        component = ComponentName(packageName, activityDir)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        ctx.vm.backgroundExecutionLauncher.launch(intent)
    }
}