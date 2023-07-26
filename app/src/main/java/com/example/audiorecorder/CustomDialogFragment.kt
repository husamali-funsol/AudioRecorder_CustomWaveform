//package com.example.audiorecorder
//
//import android.app.Dialog
//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import androidx.fragment.app.DialogFragment
//import com.example.audiorecorder.databinding.BottomSheetBinding
//
//class CustomDialogFragment: DialogFragment() {
//
//
//    private lateinit var binding: BottomSheetBinding
//
//    companion object{
//        fun newInstance(): CustomDialogFragment{
//            return CustomDialogFragment()
//        }
//    }
//
//
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View? {
//        binding = BottomSheetBinding.inflate(inflater, container, false)
//        return binding!!.root
//    }
//
//
////    override fun onDestroyView() {
////        super.onDestroyView()
////
////        binding = null
////    }
//
//    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
//        return super.onCreateDialog(savedInstanceState)
//    }
//
//    fun dismissDialog(){
//        dismiss()
//    }
//
//    fun showComponents(filename: String){
//        binding.etFilename.setText(filename)
//
//    }
//
//
//
//
//}