package com.feng.wstunnela

import BatteryOptimizationChecker
import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI
import com.feng.wstunnela.MainActivity
import com.feng.wstunnela.databinding.FragmentBatteryBinding


class FragmentBattery : Fragment() {
    private var _binding: FragmentBatteryBinding? = null
    private val binding get() = _binding!!
    private val vm: vm by activityViewModels()
    lateinit var ctx: MainActivity
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentBatteryBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }


        ctx=context as MainActivity
        val composeView = binding.composeView
        composeView.setContent {
            val  onGotoBatterySettingsClick: (Boolean) -> Unit = { t ->
                // 这里可以编写允许忽略电池优化事件响应
                if(!t) {
                    BatteryOptimizationChecker.openRestrictionSettings()
                }
                else{
                    Toast.makeText(ctx, R.string.setting_is_ok, Toast.LENGTH_SHORT).show()
                }
            }
            val  onGotoBackgroundSettingsClick: (Boolean) -> Unit = { t->
                // 这里可以编写前往后台应用运行设置事件响应
                if(!t) {
                    BatteryOptimizationChecker.openAutoStartSettings()
                }
            }
            BatteryOptimizationAndBackgroundExecutionScreen(
                context=LocalContext.current,
                onGotoBatterySettingsClick=onGotoBatterySettingsClick,
                onGotoBackgroundSettingsClick=onGotoBackgroundSettingsClick
            )
        }
    }

    @Composable
    fun BatteryOptimizationAndBackgroundExecutionScreen(
        context: Context,
        onGotoBatterySettingsClick: (Boolean) -> Unit,
        onGotoBackgroundSettingsClick: (Boolean) -> Unit
    ) {
        var isBatteryOptimizationIgnored by remember { mutableStateOf(checkBatteryOptimization(context)) }
        var isBackgroundExecutionAllowed by remember { mutableStateOf(checkBackgroundExecution(context)) }
        var isPowerSaveModeEnabled by remember { mutableStateOf( checkPowerSaveModeEnabled(context)) }


        val batteryOptimizationLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) {
            isBatteryOptimizationIgnored = checkBatteryOptimization(context)
        }

        val backgroundExecutionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) {
            isBackgroundExecutionAllowed = checkBackgroundExecution(context)
            isPowerSaveModeEnabled=checkPowerSaveModeEnabled(context)
        }

        vm.backgroundExecutionLauncher=backgroundExecutionLauncher
        vm.batteryOptimizationLauncher=batteryOptimizationLauncher
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = ctx.getString(R.string.battery_permission_content),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(24.dp))

            StatusRow(
                label = context.getString(R.string.ignore_battery_optimization),
                isAllowed = isBatteryOptimizationIgnored
            )

            StatusRow(
                label = context.getString(R.string.android_backgroundRestricted),
                isAllowed = !isBackgroundExecutionAllowed
            )
            StatusRow(
                label = context.getString(R.string.not_save_power_mode),
                isAllowed = !isPowerSaveModeEnabled
            )
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { onGotoBatterySettingsClick(isBatteryOptimizationIgnored) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = ctx.getString(R.string.ignore_battery_optimization))
            }

            Button(
                onClick = {onGotoBackgroundSettingsClick(isBackgroundExecutionAllowed)},
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = ctx.getString(R.string.background_running_set))
            }
        }
    }
    @Composable
    fun StatusRow(
        label: String,
        isAllowed: Boolean,
        modifier: Modifier = Modifier
    ) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label)
            Icon(
                imageVector = if (isAllowed) Icons.Filled.Check else Icons.Filled.Close,
                contentDescription = if (isAllowed) "Allowed" else "Not Allowed",
                tint = if (isAllowed) Color.Green else Color.Red,
                modifier = Modifier.size(24.dp)
            )
        }
    }
    private fun checkBatteryOptimization(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
        return powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
    }

    private fun checkBackgroundExecution(context: Context): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return when {Build.VERSION.SDK_INT>= Build.VERSION_CODES.P ->
            {
                activityManager.isBackgroundRestricted()
            } else-> false
        }
    }

    private fun checkPowerSaveModeEnabled(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return powerManager?.isPowerSaveMode ?: false
    }



//@Preview(showBackground = true)
//@Composable
//fun showfragment()
//{
//    BatteryOptimizationAndBackgroundExecutionScreen(LocalContext.current)
//}

}
