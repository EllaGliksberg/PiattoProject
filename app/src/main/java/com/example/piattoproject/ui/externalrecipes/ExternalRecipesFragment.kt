package com.example.piattoproject.ui.externalrecipes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.piattoproject.databinding.FragmentExternalRecipesBinding

class ExternalRecipesFragment : Fragment() {

    private var _binding: FragmentExternalRecipesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ExternalRecipesViewModel by viewModels()
    private lateinit var adapter: ExternalRecipesAdapter
    private var currentError: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentExternalRecipesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ExternalRecipesAdapter()
        binding.recipesRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.recipesRecyclerView.adapter = adapter

        binding.searchButton.setOnClickListener {
            val query = binding.searchInput.text.toString().trim()
            if (query.isNotEmpty()) {
                viewModel.searchRecipes(query)
            }
        }

        viewModel.recipes.observe(viewLifecycleOwner) { recipes ->
            adapter.submitList(recipes)
            updateMessage(recipes.isEmpty())
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            if (isLoading) {
                binding.messageTextView.visibility = View.GONE
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            currentError = error
            updateMessage(adapter.itemCount == 0)
        }

        if (savedInstanceState == null) {
            binding.searchInput.setText(DEFAULT_QUERY)
            viewModel.searchRecipes(DEFAULT_QUERY)
        }
    }

    private fun updateMessage(isEmpty: Boolean) {
        val error = currentError
        when {
            !error.isNullOrBlank() -> {
                binding.messageTextView.text = error
                binding.messageTextView.visibility = View.VISIBLE
                binding.recipesRecyclerView.visibility = View.GONE
            }
            isEmpty && viewModel.isLoading.value != true -> {
                binding.messageTextView.text = "No recipes found"
                binding.messageTextView.visibility = View.VISIBLE
                binding.recipesRecyclerView.visibility = View.GONE
            }
            else -> {
                binding.messageTextView.visibility = View.GONE
                binding.recipesRecyclerView.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val DEFAULT_QUERY = "chicken"
    }
}
