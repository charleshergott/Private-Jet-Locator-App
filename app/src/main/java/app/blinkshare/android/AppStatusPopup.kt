package app.blinkshare.android

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.DialogFragment
import app.blinkshare.android.databinding.AppStatusPopupBinding
import app.blinkshare.android.utills.ScreenSizeCompat
import com.astritveliu.boom.Boom


class AppStatusPopup : DialogFragment() {

    private lateinit var binding: AppStatusPopupBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        binding = AppStatusPopupBinding.inflate(layoutInflater, container, false)
        isCancelable = false
        initListeners()
        return binding.root
    }

    private fun initListeners(){
        Boom(binding.tvOk)
        binding.tvOk.setOnClickListener {
            dismiss()
        }
    }

    override fun onResume() {
        isCancelable = false
        val window: Window? = dialog!!.window
        val size = ScreenSizeCompat.getScreenSize(requireContext())
        window?.setLayout((size.width * 0.75).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)
        window?.setGravity(Gravity.CENTER)
        super.onResume()
    }
}