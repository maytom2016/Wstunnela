package com.feng.wstunnela
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.DOWNLOAD_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import retrofit2.Call
import okhttp3.ResponseBody
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url
import java.io.File
import java.io.IOException
import java.util.jar.Manifest

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */

@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.example.wstunnela", appContext.packageName)
    }

    //                File("/storage/emulated/0/", "text.exe")
    //测试目录getFilesDir(), getCacheDir(), getExternalFilesDir(null), getExternalCacheDir()
    //            context.getString(R.string.binfilepath)
    @Test
    fun test() {
//        runBlocking {
//            val context = InstrumentationRegistry.getInstrumentation().targetContext
//            Log.d("~~~", context.getFilesDir().toString())
//            DownloadManager.download(
//                "https://vscode.download.prss.microsoft.com/dbazure/download/stable/e8653663e8840adaf45af01eab5c627a5af81807/VSCodeUserSetup-x64-1.95.2.exe",
//                File(context.filesDir, "text.ext")
//            ).collect {
//                when (it) {
//                    is DownloadState.InProgress -> {
//                        Log.d("~~~", "download in progress: ${it.progress}.")
//                    }
//
//                    is DownloadState.Success -> {
//                        Log.d("~~~", "download finished.")
//                    }
//
//                    is DownloadState.Error -> {
//                        Log.d("~~~", "download error: ${it.throwable}.")
//                    }
//                }
//            }
//        }
    }
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
        //从url分割出BaseUrl
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
}
