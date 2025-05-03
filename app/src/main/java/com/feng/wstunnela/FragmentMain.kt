package com.feng.wstunnela


import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import kotlin.getValue
import kotlin.math.abs

class FragmentMain : Fragment() {
    private val vm: vm by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO: Use the ViewModel
    }

    @OptIn(ExperimentalFoundationApi::class)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val ctx=context as MainActivity

        if(vm.rules.value.isEmpty()) {
            val rules = ctx.readcmd()
            vm.updateRules(rules)
        }

//        return inflater.inflate(R.layout.fragment_main, container, false)
        return ComposeView(requireContext()).apply {
            setContent {
                val rules = vm.rules.collectAsState().value
                val onEditClick: (Rule) -> Unit = { rule ->
                    // 这里可以编写编辑逻辑，例如弹出编辑对话框并更新规则
                    val ctx=context as MainActivity
                    ctx.navigate_config()
                    vm.updateRule(rule)
                    vm.selectedRuleId.value=rule.id
//                    vm.selectedRuleId.value?.let { Log.d("select",it) }
                    vm.allowedittextinput=false
                }
                val onDeleteClick: (Rule) -> Unit = { rule ->
                    // 这里可以编写删除逻辑，例如从列表中移除规则并更新
                    vm.removeRule(rule)
                    ctx.savecmd_whendelete()
                }

                val onSelectedChange: (Rule) -> Unit = { rule ->
                    // 这里可以编写选中逻辑
                    vm.selectedRuleId.value=rule.id
                    vm.updateRule(rule)
                    vm.selectedRuleId.value?.let { Log.d("select",it) }
                    vm.allowedittextinput=false

                }

                RuleList(
                    rules = rules,
                    onEditClick = onEditClick,
                    onDeleteClick = onDeleteClick,
                    selectedRuleIdIn=vm.selectedRuleId.value,
                    onSelectedChange=onSelectedChange,
                    modifier = Modifier.fillMaxSize()

                )
        }
    }

}

    @Composable
    fun RuleList(
        rules: List<Rule>,
        onEditClick: (Rule) -> Unit,
        onDeleteClick: (Rule) -> Unit,
        selectedRuleIdIn: String?,
        onSelectedChange: (Rule) -> Unit,
        modifier: Modifier = Modifier
    ) {
//        var selectedRuleId by remember { mutableStateOf<String?>(null) }
//        if (selectedRuleId == null) {
//            selectedRuleId = selectedRuleIdIn
//        }
        var selectedRuleId = selectedRuleIdIn
        val isEnabled= !(vm.fabstate.value)
        LazyColumn(
            modifier = modifier
                .alpha(if (isEnabled) 1f else 0.6f), // 禁用时降低透明度
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                count = rules.size,
                key = { index: Int -> rules[index].id }
            ) { index ->
                val rule = rules[index]
                var isSelected = rule.id == selectedRuleId
                Log.d("Performance", "RuleItem recomposed for ${rule.id}")
                RuleItem(
                    rule = rule,
                    onEditClick = { if (isEnabled) onEditClick(rule) },
                    onDeleteClick = { if (isEnabled) onDeleteClick(rule) },
                    isSelected = isSelected,
                    onItemClick = {
                        if (isEnabled) {
                            selectedRuleId = rule.id
                            onSelectedChange(rule)
                        }
                    },
                    isEnabled = isEnabled, // 传递给子组件
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem(
                            fadeInSpec = tween(durationMillis = 300),
                            fadeOutSpec = tween(durationMillis = 300),
                        )
                        .padding(horizontal = 8.dp)
                )
            }
        }
    }
    @Composable
    fun RuleItem(
        rule: Rule,
        onEditClick: (Rule) -> Unit,
        onDeleteClick: (Rule) -> Unit,
        isSelected: Boolean,
        isEnabled : Boolean, // 传递给子组件
        onItemClick: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        //rule项重组检测
        SideEffect {
            Log.d("Performance", "RuleItem recomposed for ${rule.id}")
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .fillMaxWidth()
                .clickable {
                    if(true)onItemClick()
                }
                .background(
                    if (isSelected) Color.LightGray.copy(alpha = 0.3f)
                    else Color.Transparent
                )
                .padding(vertical = 8.dp)
        ) {
            // 选中状态指示器（左侧竖条）
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(24.dp)
                    .background(
                        if (isSelected) Color.Blue
                        else Color.Transparent
                    )
            )

            Spacer(Modifier.width(10.dp))

            // 原有内容保持不变
            RuleIcon(rule)
            ListItemText(rule)
            val EmptyEvent: (Rule) -> Unit = {  }
            IconButton(Icons.Filled.Edit, "编辑规则", Color.Black,  onClick = { if(isEnabled) onEditClick(rule) else EmptyEvent })
            Spacer(Modifier.width(10.dp))
            IconButton(Icons.Filled.Delete, "删除规则", Color.Black, onClick = { if(isEnabled) onDeleteClick(rule) else EmptyEvent})
        }
    }
@Composable
fun IconButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color = MaterialTheme.colors.onSurface,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current, // 使用系统默认的Indication
                onClick = onClick
            )
            .background(
                color = if (isPressed)
                    MaterialTheme.colors.primary.copy(alpha = 0.12f)
                else
                    Color.Transparent,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
    }
}
@Composable
fun ListItemText(rule: Rule) {
    val configuration = LocalConfiguration.current
    val itemWidth = remember(configuration) {
        // 根据屏幕宽度动态计算（示例：屏幕宽度的70%）
        (configuration.screenWidthDp * 0.5).dp
    }
    Column(
        modifier = Modifier
            .widthIn(min = itemWidth)
            .padding(horizontal = 12.dp, vertical = 8.dp) // 增加内边距
    ) {
        Text(
            text = rule.name,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium, // 中粗体
            color = MaterialTheme.colors.primary, // 主题主色
            maxLines = 1, // 限制为单行
            overflow = TextOverflow.Ellipsis, // 超出部分显示为...
            modifier = Modifier.padding(bottom = 4.dp)
                .width(itemWidth), // 需要指定宽度约束// 下间距
        )
        Text(
            text = rule.content,
            fontSize = 14.sp,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f), // 半透明文字
            lineHeight = 14.sp, // 适当行高
            maxLines = 1, // 限制为单行
            overflow = TextOverflow.Ellipsis, // 超出部分显示为...
            modifier = Modifier.width(itemWidth) // 需要指定宽度约束
        )
    }
}
@Composable
fun RuleIcon(rule: Rule)
{
    val iconColor = remember(rule.name) {
        getColorByText(rule.name)
    }
// 首字符圆形图标
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(
                color = iconColor,
                shape = CircleShape
            )
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.2f), // 极细边框
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = rule.name.take(1), // 取首字符
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
private fun getColorByText(text: String): Color {
    val colors = listOf(
        Color(0xFFF44336), // Red
        Color(0xFFE91E63), // Pink
        Color(0xFF9C27B0), // Purple
        Color(0xFF673AB7), // Deep Purple
        Color(0xFF3F51B5), // Indigo
        Color(0xFF2196F3), // Blue
        Color(0xFF03A9F4), // Light Blue
        Color(0xFF00BCD4), // Cyan
        Color(0xFF009688), // Teal
        Color(0xFF4CAF50), // Green
        Color(0xFF8BC34A), // Light Green
        Color(0xFFCDDC39), // Lime
        Color(0xFFFFC107), // Amber
        Color(0xFFFF9800), // Orange
        Color(0xFFFF5722), // Deep Orange
        Color(0xFF795548)  // Brown
    )
    // 通过字符串哈希值确保相同文字返回相同颜色
    return colors[abs(text.hashCode()) % colors.size]
}
@Preview(showBackground = true)
@Composable
fun RuleItemPreview() {
    val rules = listOf(
        Rule("规则1", "所有用户必须年满13周岁才能注册账号。"),
        Rule("规则2", "禁止在平台上发布包含恶意链接的内容。"),
    )
    val onEditClick: (Rule) -> Unit = { /* 这里可以添加编辑点击的实际逻辑 */ }
    val onDeleteClick: (Rule) -> Unit = { /* 这里可以添加删除点击的实际逻辑 */ }
    val onSelectedChange: (String?) -> Unit = { /* 这里可以添加删除点击的实际逻辑 */ }

//    ruleList(
//        rules = rules,
//        onEditClick = onEditClick,
//        onDeleteClick = onDeleteClick,
//        onSelectedChange = onSelectedChange,
//    )
}

}

