package com.feng.wstunnela


import ServiceController
import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.toSpanned
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.feng.wstunnela.TomlConfigManager.loadRules
import com.feng.wstunnela.TomlConfigManager.saveRules
import com.feng.wstunnela.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt

lateinit var main: MainActivity

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var crontask: Job
    private lateinit var serviceController: ServiceController
    lateinit var binpath:String
//    var bottomNavPx: Int=0
    val vm: vm by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        main=this
//        savecmd()
//        vm.selectedRuleId.value="7ba65000-d7ab-45d0-bd2a-fbd52e7577b9"
        vm.filesDir=filesDir.toString()
        //同步servicebound和fab的ui
        vm.serviceBound.observe(this) { serviceBound ->
            vm.fabstate.value=serviceBound
        }
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // 按钮状态（默认禁用）
//        var isEnabled :mutableStateOf(false) }
//        isEnabled=vm.serviceBound.value==true
        val composeView = findViewById<ComposeView>(R.id.compose_view)
        composeView.setContent {
            // 这里编写Compose函数内容
            val onFabClick: (Boolean) -> Unit = { isEnabled ->
                // 这里可以编写fab点击事件
                val checkcmd= vm.rule.value?.content?.contains("wstunnel") == true
                //未启动且命令有包含wstunnel时
//                vm.fabstate.value=!(vm.fabstate.value)
                if(!isEnabled && checkcmd){
                    vm.cmdstr=vm.binpath+vm.rule.value?.content.toString().replace("wstunnel","").replace("\"","").replace("\'","")
                    startService()
                    savecmd()
                }
                else if(!isEnabled && !checkcmd)
                {
                    Toast.makeText(this, R.string.illlegalwstunnelcmd, Toast.LENGTH_SHORT).show()
                    Log.d("fab_1",vm.fabstate.value.toString())
                }
                else{
                    stopService()
                }
            }

            Fab(onFabClick,vm)
        }


        // 获取 NavController
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
//        val appBarConfiguration = AppBarConfiguration(
//            setOf(
//                R.id.navigation_main, R.id.navigation_config, R.id.navigation_log
//            )
//        )
        navController = navHostFragment.navController
//        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_nav)

        // 设置底部导航与 NavController 的绑定
        binding.bottomNav.setupWithNavController(navController)

//        NavigationUI.setupWithNavController(bottomNavigationView,navController )
//        navController.setGraph(R.navigation.nav_graph)
//        setupActionBarWithNavController(navController, appBarConfiguration)

        //1、加载环境变量
        vm.binpath=this.getString(R.string.binfilepath)
        binpath=vm.binpath
        vm.filesDir="$filesDir"
        FileManage.vm= vm

        //2、检查wstunnel可执行文件状态
        check_wsexec_file()
        //3、获取配置、版本信息
        getwstunnelversion()
        getsystemarch()
        //4、读取配置
//        readcmd()


//        //获取bottomnav高度，以便限制fab活动区域
//        val view = findViewById<View>(R.id.bottom_nav)
//        view.post {
//            bottomNavPx=  view.height
//        }

    }

    override fun onCreateView(name: String, context: Context, attrs: AttributeSet): View? {

//       binding.bottomNav.height
        return super.onCreateView(name, context, attrs)
    }
    override fun onCreateView(
        parent: View?,
        name: String,
        context: Context,
        attrs: AttributeSet
    ): View? {
        return super.onCreateView(parent, name, context, attrs)
    }

    override fun onDestroy() {
        super.onDestroy()
        crontask.cancel()
    }

    fun check_wsexec_file()
    {
        var tarfilestr="$filesDir/test.tar.gz"
        if(!File(binpath).exists() && !File(tarfilestr).exists()) {
            val dialog = DialogUtil.showDownloadProgress(this,this.getString(R.string.download_remind))
            val view = dialog.findViewById<ProgressBar>(R.id.d_progress_bar)
            CoroutineScope(Dispatchers.IO).launch {
                FileManage.copybinfromgithub(dialog,view,tarfilestr)
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
    }
    fun getsystemarch()
    {
        val arch = System.getProperty("os.arch")
        if (arch != null) {
            vm.arch_textView21=arch
        }

    }
    fun getwstunnelversion()
    {
        val cmd= "$binpath --version"
        val exeFile = File(binpath)
        //检测文件并不可靠，有时下载最新的wstunnel中断时也会有残存文件，但是无法正常运行
//        if (!exeFile.exists()) {
//            if(!FileManage.copybin(this,binpath)) return
//        }
        exeFile.setExecutable(true, true)
        vm.wsver_textView1 = FileManage.execCmd(cmd.split("\\s+".toRegex()).toTypedArray())
        if(vm.wsver_textView1.isEmpty())
        {
            FileManage.copybin(this,binpath)
        }
        vm.wsver_textView1 = FileManage.execCmd(cmd.split("\\s+".toRegex()).toTypedArray())
        Log.d("wsver",vm.wsver_textView1)
    }
    fun startService() {
        serviceController = ServiceController(this)
        serviceController.bindService(vm.cmdstr)
        startProcessMonitoring()
    }

    fun stopService() {
        if(this::crontask.isInitialized && crontask.isActive) {
            crontask.cancel()
        }
        if(this::serviceController.isInitialized) {
            serviceController.unbindService()
        }
        vm.runlog_textvie4.value=this.getString(R.string.errormessage).toSpanned()
//        vm.serviceBound.value=false
        vm.updateServiceBound(false)
    }
    private fun startProcessMonitoring() {
        crontask = lifecycleScope.launch {
            repeat(Int.MAX_VALUE) {
                delay(5000L)
                if (!serviceController.checkProcessAlive()) {
                    stopService()
                    return@launch // 退出协程
                }
            }
        }
    }
    fun readcmd():List<Rule>
    {
        var loadedRules = loadRules(this, "rule.toml")
        //如果为空则成生模板
        if (loadedRules.rules.isEmpty())
        {
            loadedRules.rules = listOf(
                Rule(getString(R.string.wstunnel_client_temp_s), getString(R.string.wstunnel_client_temp),"7ba65000-d7ab-45d0-bd2a-fbd52e7577b9"),
                Rule(getString(R.string.wstunnel_server_temp_s), getString(R.string.wstunnel_server_temp),"211350d3-eedd-4f1f-8183-35e026c91ab9"),
            )
        }
        vm.selectedRuleId.value=loadedRules.selectedRuleId
        if (loadedRules.selectedRuleId?.isNotEmpty() == true) {
            val targetRule = loadedRules.rules.find { it.id == loadedRules.selectedRuleId }
            targetRule?.let { rule ->
                vm.updateRule(rule) // 确保传递要更新的规则对象
            }
        }
        return loadedRules.rules
//        loadedRules.forEach {
//            Log.d("Config", it.name)
//        }
    }
    fun savecmd()
    {
//      FileManage.savecfg(this, "cmd", vm.cmdstr)
        crontask = lifecycleScope.launch {
            delay(5000L)
            if (serviceController.checkProcessAlive()) {
                //若5秒后进程还在，则认为是有效配置，准备保存.
                val rule=vm.rule.value as Rule
                vm.addOrUpdateRule(rule)
                saveRules(this@MainActivity, "rule.toml", vm.rules.value)

            }
        }
    }
    fun savecmd_whendelete()
    {
        saveRules(this@MainActivity, "rule.toml", vm.rules.value)
    }
    fun navigate_config()
    {
        binding.bottomNav.selectedItemId=R.id.navigation_config
    }


}
@Composable
fun Fab(onFabClick:(Boolean) -> Unit,vm:vm) {
//    val uiState by vm._serviceBound.collectAsState()
//    var isEnabled by remember {
//        vm.fabstate
//    }
    val isEnabled= vm.fabstate.value
    //重组性能统计
    var count by remember { mutableStateOf(0) }
    SideEffect {
        count++
        Log.d("fab_a",count.toString())
    }


//    var isEnabled by remember {mutableStateOf(false)}
//    val isEnabled= vm.serviceBound.value==true
//    val isEnabled by vm.serviceBound.collectAsState()
        // 动画参数
    Log.d("fab",isEnabled.toString())
    val buttonColor by animateColorAsState(
        targetValue = if (isEnabled) MaterialTheme.colors.primary else Color.Gray,
        animationSpec = tween(durationMillis = 300)
    )
    val iconTint by animateColorAsState(
        targetValue = if (isEnabled) Color.White else Color.LightGray,
        animationSpec = tween(durationMillis = 300)
    )

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    // 尺寸转换
    val buttonSize = 60.dp
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val buttonSizePx = with(density) { buttonSize.toPx() }
    var offset by remember { mutableStateOf(Offset(screenWidthPx -buttonSizePx, screenHeightPx/10*7)) }
//        var bottomNavPx = insets.getBottom(density)
    Box(modifier = Modifier.fillMaxSize()) {

        // 可拖动悬浮按钮
        Box(
            modifier = Modifier
                .size(60.dp)
                .offset {
                    IntOffset(
                        offset.x.roundToInt(),
                        offset.y.roundToInt()
                    )
                }
                .background(
                    color = buttonColor,
                    shape = CircleShape
                )
                .shadow(
                    elevation = 60.dp,
                    shape = CircleShape,
                    ambientColor = if (isEnabled) MaterialTheme.colors.primary else Color.Gray,
                    spotColor = if (isEnabled) MaterialTheme.colors.primaryVariant else Color.DarkGray
                )
                .pointerInput(Unit) {
                    detectDragGesturesAfterLongPress(
                        onDrag = { _, dragAmount ->
                            offset =Offset(
                                (offset.x + dragAmount.x).coerceIn(0f, screenWidthPx - buttonSizePx),
                                (offset.y + dragAmount.y).coerceIn(0f, screenHeightPx - buttonSizePx -screenHeightPx/6)
                            )
                        }
                    )
                }
                .clickable {
                    //点击事件
                    onFabClick(isEnabled)
//                    isEnabled = !isEnabled
//                    vm.serviceBound.value= isEnabled
//                    Log.d("fab_isEnable",isEnabled.toString())

//                    Log.d("fab_0",vm.serviceBound.value.toString())
//                    Log.d("fab_00",vm._serviceBound.value.toString())
//
//                    Log.d("fab_click",uiState.toString())

                },
            contentAlignment = Alignment.Center
        ) {
            Crossfade(
                targetState = isEnabled,
                animationSpec = tween(durationMillis = 300)
            ) { enabled ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(8.dp)
                ) {
                    Icon(
                        imageVector = if (enabled) Icons.Default.Block else Icons.Default.PlayArrow,
                        contentDescription = if (enabled) main.getString(R.string.fab_stop) else main.getString(R.string.fab_play),
                        tint = iconTint,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = if (enabled) main.getString(R.string.fab_stop) else main.getString(R.string.fab_play),
                        color = iconTint,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
