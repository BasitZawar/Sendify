package com.smartswitch.presentation

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.smartswitch.R
import com.smartswitch.databinding.FragmentErrorSolBinding
import com.smartswitch.utils.extensions.handleBackPressWithAction

class ErrorSolFragment : Fragment() {
    private var _binding: FragmentErrorSolBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentErrorSolBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.headerLayout.setNavigationOnClickListener {
//            findNavController().navigateUp()
            val navController = findNavController()
            navController.navigate(R.id.homeSendifyFragment) {
                popUpTo(0) { inclusive = true }
            }

        }

        requireActivity().handleBackPressWithAction {
//            findNavController().navigateUp()
            val navController = findNavController()
            navController.navigate(R.id.homeSendifyFragment) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

}