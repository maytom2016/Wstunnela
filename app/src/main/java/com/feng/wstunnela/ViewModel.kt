package com.feng.wstunnela

import android.app.AlertDialog
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.text.Spanned
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import net.peanuuutz.tomlkt.Toml
import okhttp3.ResponseBody
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.io.FileUtils
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.UUID
import java.util.zip.GZIPInputStream
import kotlin.collections.map
import kotlin.random.Random

class vm : ViewModel() {
    //可执行二进制文件所在目录
    var binpath:String=""
    //下载解压目录
    var filesDir:String=""
    //日志信息
    var runlog_textvie4= MutableLiveData<Spanned>()
    //wstunnel版本信息
    var wsver_textView1:String=""
    //arch架构信息
    var arch_textView21:String=""
    //界面显示的wstunnel启动命令
    var uicmd:String=""
    // wstunnel是否准备好了
    var iswsready=false
    //当前正在运行的wstunnel启动命令
    var cmdstr=""
    //服务启动状态
    var _serviceBound=MutableLiveData<Boolean>(false)
    val serviceBound: LiveData<Boolean> =_serviceBound
    var fabstate=mutableStateOf(false)
    //wstunnel规则列表
    private val _rules = MutableStateFlow(listOf<Rule>())
    val rules: StateFlow<List<Rule>> = _rules
    //当前选中的规则
    private val _rule = MutableLiveData<Rule?>()
    val rule: LiveData<Rule?> = _rule
    //当前选中的id
    var selectedRuleId=mutableStateOf("")
    //是否允许edittext输出到rule，暂无实际作用，备用
    var allowedittextinput=true
    //电池优化事件回调，用来更新ui
    lateinit var batteryOptimizationLauncher:ManagedActivityResultLauncher<Intent, ActivityResult>
    //后台运行权限设置事件回调，用来更新ui
    lateinit var backgroundExecutionLauncher:ManagedActivityResultLauncher<Intent, ActivityResult>

    fun updateServiceBound(Bool: Boolean)
    {
        _serviceBound.value=Bool
    }
    fun updateRules(newRules: List<Rule>) {
        _rules.value = newRules
    }
    // 函数用于删除特定规则
    fun removeRule(ruleToRemove: Rule) {
        val currentRules = _rules.value
        val newRules = currentRules.filter { it!= ruleToRemove }
        updateRules(newRules)
    }
    // 生成随机英文名称的候选词（可根据需要扩展）
    private val adjectives = listOf("Red", "Blue", "Quick", "Lazy", "Smart")
    private val nouns = listOf("Cat", "Dog", "Rule", "Fox", "Bear")

    fun addOrUpdateRule(rule: Rule) {
        val currentList = _rules.value
        //更新选中id函数
        val updateselectid =
            { rule:Rule ->
                if(selectedRuleId.value!=rule.id)
                {
                    selectedRuleId.value=rule.id
                }
            }
        //检查到空名，则新增项
        if (rule.name.isEmpty()){
            val newName = generateUniqueName(currentList)
            var newrule=rule.copy(name = newName,id=UUID.randomUUID().toString())
            var newList=currentList.toMutableList().apply {
                add(newrule) // 新增条目
            }
            //同步规则
            updateRule(newrule)
            //同步列表选中id
            updateselectid(newrule)
            //更新列表
            updateRules(newList)
            return
        }
        // 查找重复项（基于 name）
        val existingIndex = currentList.indexOfFirst {
            it.name == rule.name
        }
        //只要出现了新规则名，就是新规则
        if (existingIndex == -1) {
            val newrule=rule.copy(id=UUID.randomUUID().toString())
            val newList = currentList.toMutableList().apply{
                add(newrule)
            } // 新增条目
            updateRule(newrule)
            updateselectid(newrule)
            updateRules(newList)
            return
        }
        if(existingIndex != -1 && currentList[existingIndex] == rule)
        {
            //内容完全一样，没必要更新，直接退出
            return
        }
        if(existingIndex != -1 && currentList[existingIndex] != rule) {
            //名称一样，内容不一样，更新原有规则
            val newList = currentList.toMutableList().apply {
                set(existingIndex, rule) // 替换重复项
            }
            updateRules(newList)
        }

    }

    fun generateUniqueName(existingRules: List<Rule>): String {
        val existingNames = existingRules.map { it.name }.toSet()
        val number = String.format("%05d", Random.nextInt(1, 10000))
        while (true) {
            val randomName = "${adjectives.random()}${nouns.random()}${number}"
            if (randomName !in existingNames) {
                return randomName
            }
        }
    }
    fun updateRule(newRule: Rule) {
        _rule.value = newRule
    }
    // 仅更新 name 字段
    fun updateName(newName: String) {
        _rule.value = _rule.value?.copy(name = newName)
            ?: Rule(name = newName) // 原值为 null 时创建新对象
    }

    // 仅更新 content 字段
    fun updateContent(newContent: String) {
        _rule.value = _rule.value?.copy(content = newContent)
            ?: Rule(content = newContent) // 原值为 null 时创建新对象
    }
}
@Serializable
data class Rule(
    val name: String="",
    val content: String="",
    val id: String = UUID.randomUUID().toString()

)

object DialogUtil {
      //下载进度条弹窗
    fun showDownloadProgress(
        context: Context,
        title: String? = null
    ): AlertDialog = context.let {
        AlertDialog.Builder(it).create().apply {
            // 设置点击dialog的外部能否取消弹窗
            setCanceledOnTouchOutside(false)
            // 设置能不能返回键取消弹窗
            setCancelable(false)
            show()
            window?.run {
                setLayout(
                    700,
                    600
                )
            }
            setContentView(
                View.inflate(it, R.layout.alert_dialog_download_progress, null).apply {
                    // 设置成顶层视图
                    bringToFront()
                    title?.let { text ->
                        findViewById<TextView>(R.id.d_title).text = text
                    }

                }

            )
        }
    }
}
data class GithubRelease(
    val tag_name: String,
    val assets: List<Asset>

)
data class Asset(
    val name: String,
    val size: Long,
    val browser_download_url :String
)
interface GithubApi {
    @GET("repos/erebe/wstunnel/releases/latest")
    suspend fun getLatestRelease(): GithubRelease
}

//下载相关代码来源https://juejin.cn/post/7090570934125330468
interface DownloadService {

    @Streaming
    @GET
    fun download(@Url url: String): Call<ResponseBody>

}
sealed class DownloadState {
    data class InProgress(val progress: Int) : DownloadState()
    data class Success(val file: File) : DownloadState()
    data class Error(val throwable: Throwable) : DownloadState()
}

object DownloadManager {

    fun getGithubRelease(): Flow<GithubRelease?> {
        return flow {
            val retrofit = Retrofit.Builder()
                .baseUrl("https://api.github.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            val api = retrofit.create(GithubApi::class.java)
            val release = api.getLatestRelease()
            emit(release)
        }.flowOn(Dispatchers.IO)
            .onStart {
                // 可以在这里添加开始请求时的操作，如显示加载动画
            }
            .catch(){
                // 处理错误，如记录错误日志
            }
            .onCompletion {
                // 可以在这里添加请求完成后的操作，如隐藏加载动画
            }
    }
    fun extractTarGz(inputFile: String, outputDirectory: String ){
        val outPath = outputDirectory
        val ios = FileInputStream(inputFile)
        val gis = GzipCompressorInputStream(ios)
        TarArchiveInputStream(gis).use { tgis ->
            var nextEntry: ArchiveEntry?
            while (tgis.nextEntry.also { nextEntry = it }!= null) {
                val name = nextEntry!!.getName()
                val file = File(outPath, name)
                if (nextEntry.isDirectory) {
                    file.mkdir()
                } else {
                    FileUtils.copyToFile(tgis, file)
                    file.setLastModified(nextEntry.lastModifiedDate.time)
                }
            }
        }
    }
    fun CheckTarGzFile(inputFile: String): Boolean {
        return try {
            // 检查文件是否存在
            val file = File(inputFile)
            if (!file.exists() || !file.isFile) {
                return false
            }
            // 尝试读取GZIP头部
            BufferedInputStream(FileInputStream(file)).use { bis ->
                GZIPInputStream(bis).use { gzipIn ->
                    // 尝试读取TAR内容
                    TarArchiveInputStream(gzipIn).use { tarIn ->
                        // 尝试遍历TAR条目，如果出现异常则文件不完整
                        while (tarIn.nextTarEntry != null) {
                            // 只是读取元数据，不实际解压内容
                        }
                    }
                }
            }
            true
        } catch (e: IOException) {
            // 捕获任何IO异常（如损坏的GZIP或TAR格式）
            false
        } catch (e: Exception) {
            // 捕获其他可能的异常
            false
        }
    }
    fun download(url: String, file: File): Flow<DownloadState> {
        return flow {
            val retrofit = Retrofit.Builder()
                .baseUrl(UrlUtils.getBaseUrl(url))
                .build()
            val response = retrofit.create(DownloadService::class.java).download(url).execute()
            if (response.isSuccessful) {
                saveToFile(response.body()!!, file) {
                    emit(DownloadState.InProgress(it))
                }
                emit(DownloadState.Success(file))
            } else {
                emit(DownloadState.Error(IOException(response.toString())))
            }
        }.catch {
            emit(DownloadState.Error(it))
        }.flowOn(Dispatchers.IO)
    }

    private inline fun saveToFile(responseBody: ResponseBody, file: File, progressListener: (Int) -> Unit) {
        val total = responseBody.contentLength()
        var bytesCopied = 0
        var emittedProgress = 0
        file.outputStream().use { output ->
            val input = responseBody.byteStream()
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var bytes = input.read(buffer)
            while (bytes >= 0) {
                output.write(buffer, 0, bytes)
                bytesCopied += bytes
                bytes = input.read(buffer)
                val progress = (bytesCopied * 100 / total).toInt()
                if (progress - emittedProgress > 0) {
                    progressListener(progress)
                    emittedProgress = progress
                }
            }
        }
    }
}
object UrlUtils {
    fun getBaseUrl(url: String): String {
        var mutableUrl = url
        var head = ""
        var index = mutableUrl.indexOf("://")
        if (index != -1) {
            head = mutableUrl.substring(0, index + 3)
            mutableUrl = mutableUrl.substring(index + 3)
        }
        index = mutableUrl.indexOf("/")
        if (index != -1) {
            mutableUrl = mutableUrl.substring(0, index + 1)
        }
        return head + mutableUrl
    }
}
object FileManage {
    lateinit var vm: vm
    fun copybin(con: Context,strOutFileName: String):Boolean {
        val binpath=vm.binpath
        val dirstr=(".+/".toRegex()).find(binpath)
        val dir=File(dirstr!!.value)
        if(dir.exists()) {
            val fis: InputStream?
            val fos = FileOutputStream(strOutFileName)
            //目前仅考虑64位架构，因为绝大多数安卓设备都已经是64位系统
            var arch = System.getProperty("os.arch")?.lowercase()
            if (arch==null)arch="error"
            if (arch.contains("x86_64") or arch.contains("amd64")) {
                fis = con.assets.open(con.getString(R.string.wsamd64))
            } else if (arch.contains("armv8") or arch.contains("arm64") or arch.contains("aarch64")) {
                fis = con.assets.open(con.getString(R.string.wsarm64))
            } else {
                vm.iswsready=false
//                binding.textView1.text = this.getString(R.string.nosupportremind)
//                binding.textView1.setTextColor(this.getColor(R.color.red))
//                binding.button.isEnabled = false
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
        val binpath=vm.binpath
        //程序路径
        var downloadpath= vm.filesDir
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
    fun execCmd(cmd:Array<String>):String {
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

    @Deprecated("此为单行命令配置存储，会在后期移除")
    fun savecfg(con: Context, FILENAME: String, filestr: String) {
        try {
            val fos = con.openFileOutput(FILENAME, MODE_PRIVATE)
            fos.write(filestr.toByteArray())
            fos.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
    @Deprecated("此为单行命令配置读取，会在后期移除")
    fun readlasttimecfg(con: Context, FILENAME: String): String {
        val filestr: String?
        try {
            val fis = con.openFileInput(FILENAME)
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

@Serializable
data class RulesConfig(
    var rules: List<Rule> = emptyList(),
    var selectedRuleId:String=""
)


object TomlConfigManager {
    private fun getConfigFile(context: Context, fileName: String): File {
        return File(context.filesDir, fileName)
    }

    fun loadRules(context: Context, fileName: String): RulesConfig{
        val file = getConfigFile(context, fileName)
        if (!file.exists()) return RulesConfig()
        try {

            val rulesconfig=Toml.decodeFromString<RulesConfig>(file.readText())
            println("读取配置: ${rulesconfig.rules}")
            return rulesconfig

        } catch (e: Exception) {
            println("读取配置失败: ${e.message}")
            return RulesConfig()
        }

    }

    fun saveRules(context: Context, fileName: String, rules: List<Rule>): Boolean {

        return try {
            val ctx =context as MainActivity
            val file = getConfigFile(context, fileName)
            val config = RulesConfig(rules,ctx.vm.selectedRuleId.value)
            val tomlString = Toml.encodeToString(config)
//            println(tomlString)
            file.writeText(tomlString)
            true
        } catch (e: Exception) {
            println("保存配置失败: ${e.message}")
            false
        }
    }

}