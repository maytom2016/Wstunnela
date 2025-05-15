package com.feng.wstunnela

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.NavOptions
import androidx.navigation.Navigation
import androidx.navigation.fragment.NavHostFragment
import com.feng.wstunnela.databinding.FragmentConfigBinding
import androidx.navigation.findNavController

class FragmentConfig : Fragment(){
    private var _binding: FragmentConfigBinding? = null
    private val binding get() = _binding!!
    private val vm: vm by activityViewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentConfigBinding.inflate(inflater, container, false)
        setupViews()
        //1、为按钮绑定服务
        return binding.root
    }

    private fun setupViews() {
        val ctx=context as MainActivity
        if(vm.wsver_textView1.isEmpty())
        {
            ctx.getwstunnelversion()
        }
        binding.textView1.text=vm.wsver_textView1
        binding.textView21.text=vm.arch_textView21

//        updateUI(vm.serviceBound.value == true,ctx)
        binding.button.setOnClickListener {
//            val check=chekcklegalwstunnelcmd()
//            if (binding.button.text == getString(R.string.buttonstart)&& check) {
//
//                vm.cmdstr=vm.binpath+binding.EditTextRuleContent.text.toString().replace("wstunnel","").replace("\"","").replace("\'","")
//                ctx.startService()
////                updateUI(true,ctx)
//            }
//            else if(binding.button.text==getString(R.string.buttonstart)&&!check ){
//                Toast.makeText(ctx, R.string.illlegalwstunnelcmd, Toast.LENGTH_SHORT).show()
//            }
//            else {
//
//                ctx.stopService()
////                updateUI(false,ctx)
//            }
//            val navHostFragment = ctx.supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            val navController= view?.let { it.findNavController() }
            navController?.navigate(R.id.action_navigation_config_to_navigation_battery)
        }
        vm.serviceBound.observe(viewLifecycleOwner) { serviceBound ->
            updateUI(serviceBound  == true,ctx)
        }

        //rule同步规则到ui上
        vm.rule.observe(viewLifecycleOwner, Observer { rule ->
            rule?.let {
                // 在这里处理rule的更新，例如更新UI显示
                if(it.name!=binding.EditTextRuleName.text.toString()) {
                    binding.EditTextRuleName.setText(it.name)
                }
                if(it.content!=binding.EditTextRuleContent.text.toString()) {
                    binding.EditTextRuleContent.setText(it.content)
                }
            }
        })

//        binding.editText.onFocusChangeListener
        binding.EditTextRuleContent.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
//                var beforerule=vm.rule.value?:Rule()
//                var rule=Rule(,beforerule.content,beforerule.id)
//                vm.updateRule(rule)
                if(vm.allowedittextinput ) {
                    vm.updateContent(s.toString())
                    Log.d("rule_change", s.toString())
                    Log.d("rule_change", vm.rule.value?.content.toString())
                }
                vm.allowedittextinput=true
//                return
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        binding.EditTextRuleName.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if(vm.allowedittextinput ) {
                    vm.updateName(s.toString())
                    Log.d("rule_change", s.toString())
                    Log.d("rule_change", vm.rule.value?.name.toString())
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })



    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(false)
            setDisplayShowHomeEnabled(false)
        }
    }

    private fun updateUI(started: Boolean, con: Context) {
        binding.apply {
//            button.text = if (!started) getString(R.string.buttonstart) else getString(R.string.buttonstop)
            textView31.text=if (!started) getString(R.string.buttonstop) else getString(R.string.buttonstart)
            val font_color=if(started)ContextCompat.getColor(con,R.color.forestgreen)else ContextCompat.getColor(con,R.color.red)
            binding.textView31.setTextColor(font_color)
            EditTextRuleContent.isEnabled = !started
            EditTextRuleName.isEnabled= !started
        }
    }
    fun chekcklegalwstunnelcmd():Boolean
    {
        if(binding.EditTextRuleContent.text.contains("wstunnel"))return true
        return false
    }

}
