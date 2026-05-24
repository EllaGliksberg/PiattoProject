package com.example.piattoproject.ui.externalrecipes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.piattoproject.R
import com.example.piattoproject.data.remote.externalrecipes.MealDto
import com.example.piattoproject.databinding.ItemExternalRecipeBinding
import com.example.piattoproject.utils.ImageUtils

class ExternalRecipesAdapter : RecyclerView.Adapter<ExternalRecipesAdapter.RecipeViewHolder>() {

    private var recipes: List<MealDto> = emptyList()

    class RecipeViewHolder(val binding: ItemExternalRecipeBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val binding = ItemExternalRecipeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RecipeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        val meal = recipes[position]

        holder.binding.recipeTitleTextView.text = meal.strMeal
        holder.binding.recipeCategoryTextView.text = meal.strCategory.orEmpty()
        holder.binding.recipeCategoryTextView.visibility = if (meal.strCategory.isNullOrBlank()) View.GONE else View.VISIBLE
        holder.binding.recipeAreaTextView.text = meal.strArea.orEmpty()
        holder.binding.recipeAreaTextView.visibility = if (meal.strArea.isNullOrBlank()) View.GONE else View.VISIBLE
        
        ImageUtils.loadImage(holder.binding.recipeImageView, meal.strMealThumb)
    }

    override fun getItemCount() = recipes.size

    fun submitList(newItems: List<MealDto>) {
        recipes = newItems
        notifyDataSetChanged()
    }
}
