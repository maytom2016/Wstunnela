package com.feng.wstunnela
import android.app.AlertDialog
import android.content.Context
import android.view.View
import android.widget.TextView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onErrorResume
import kotlinx.coroutines.flow.onStart
import okhttp3.ResponseBody
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
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
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption


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
                    400
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
