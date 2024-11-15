package com.feng.wstunnela

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch
import org.junit.Test

import org.junit.Assert.*

import java.io.File
import okhttp3.*
import okio.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
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

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }
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
            .onEach {
                value->
                val alltar=value?.assets
                val url=alltar?.map{it.browser_download_url}
                url?.forEach { url->
                    println(url)
                }
//                if (alltar != null) {
//                    for (a in alltar) {
//                        if(a.browser_download_url.contains("android_arm64"))
//                        {
//                            println(a.browser_download_url)
//                        }
//                        if(a.browser_download_url.contains("linux_amd64"))
//                        {
//                            println(a.browser_download_url)
//                        }
//                    }
//                }
            }
            .catch(){
                // 处理错误，如记录错误日志

            }
            .onCompletion {
                // 可以在这里添加请求完成后的操作，如隐藏加载动画
                println("成功")
            }
    }
    @Test
    fun test()
    {
        runBlocking{
            launch {
                val test=getGithubRelease().firstOrNull()
                println(test)
//                        value ->
////                    print(value?.assets.toString())
//                    val alltar=value?.assets
//                    if (alltar != null) {
//                        for (a in alltar) {
//                            if(a.browser_download_url.contains("android_arm64"))
//                            {
//                                println(a.browser_download_url)
//                            }
//                            if(a.browser_download_url.contains("linux_amd64"))
//                            {
//                                println(a.browser_download_url)
//                            }
//                        }
//                    }
//                }
            }
        }
    }
//    suspend fun DownbinFromgithub() {
//        val destPath = "/home/qte/Downloads/1.apk" // 本地保存路径，这里需要替换为实际的有效路径
//        RxHttp.get("http://192.168.31.145/app-release.apk")
//            .toDownloadFlow(destPath,true) //传入本地路径
//            .onProgress {
//                val fraction = it.fraction //当前进度 [0.0, 1.0]
//                val progress = it.progress //当前进度 [0, 100]
//                val currentSize = it.currentSize //当前已下载的字节大小
//                val totalSize = it.totalSize     //要下载的总字节大小
//                val speed = it.speed //下载速度 单位: byte/s
//                val time = it.calculateRemainingTime()  //下载剩余时间，单位: s
//                println(fraction)
//            }
//            .catch {
//                //异常回调
//                println("下载失败")
//            }.collect {
//                //成功回调
//                println("下载成功")
//            }
//    }
}
