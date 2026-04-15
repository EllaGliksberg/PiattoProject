package com.example.piattoproject.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.piattoproject.R
import com.example.piattoproject.databinding.FragmentProfileBinding

class ProfileFragment : Fragment() {
    private var binding: FragmentProfileBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindMockProfileData()
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private fun bindMockProfileData() {
        val viewBinding = binding ?: return
        viewBinding.displayNameTextView.text = getString(R.string.mock_display_name)
        viewBinding.usernameTextView.text = getString(R.string.mock_username)
        viewBinding.bioTextView.text = getString(R.string.mock_bio)
    }
}
