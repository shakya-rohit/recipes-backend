package com.publicis.recipes.dto;

import java.util.List;

public class RecipeDTO {
    private Long id;
    private String name;
    private String highlightedName;
    private String cuisine;
    private String highlightedCuisine;
    private String image;
    private Integer prepTimeMinutes;
    private Integer cookTimeMinutes;
    private Integer caloriesPerServing;
    private Integer servings;
    private String difficulty;
    private Double rating;
    private Integer reviewCount;
    private List<String> ingredients;
    private List<String> instructions;
    private List<String> tags;
    private List<String> mealType;
    
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getHighlightedName() {
		return highlightedName;
	}
	public void setHighlightedName(String highlightedName) {
		this.highlightedName = highlightedName;
	}
	public String getCuisine() {
		return cuisine;
	}
	public void setCuisine(String cuisine) {
		this.cuisine = cuisine;
	}
	public String getHighlightedCuisine() {
		return highlightedCuisine;
	}
	public void setHighlightedCuisine(String highlightedCuisine) {
		this.highlightedCuisine = highlightedCuisine;
	}
	public String getImage() {
		return image;
	}
	public void setImage(String image) {
		this.image = image;
	}
	public Integer getPrepTimeMinutes() {
		return prepTimeMinutes;
	}
	public void setPrepTimeMinutes(Integer prepTimeMinutes) {
		this.prepTimeMinutes = prepTimeMinutes;
	}
	public Integer getCookTimeMinutes() {
		return cookTimeMinutes;
	}
	public void setCookTimeMinutes(Integer cookTimeMinutes) {
		this.cookTimeMinutes = cookTimeMinutes;
	}
	public Integer getCaloriesPerServing() {
		return caloriesPerServing;
	}
	public void setCaloriesPerServing(Integer caloriesPerServing) {
		this.caloriesPerServing = caloriesPerServing;
	}
	public Integer getServings() {
		return servings;
	}
	public void setServings(Integer servings) {
		this.servings = servings;
	}
	public String getDifficulty() {
		return difficulty;
	}
	public void setDifficulty(String difficulty) {
		this.difficulty = difficulty;
	}
	public Double getRating() {
		return rating;
	}
	public void setRating(Double rating) {
		this.rating = rating;
	}
	public Integer getReviewCount() {
		return reviewCount;
	}
	public void setReviewCount(Integer reviewCount) {
		this.reviewCount = reviewCount;
	}
	public List<String> getIngredients() {
		return ingredients;
	}
	public void setIngredients(List<String> ingredients) {
		this.ingredients = ingredients;
	}
	public List<String> getInstructions() {
		return instructions;
	}
	public void setInstructions(List<String> instructions) {
		this.instructions = instructions;
	}
	public List<String> getTags() {
		return tags;
	}
	public void setTags(List<String> tags) {
		this.tags = tags;
	}
	public List<String> getMealType() {
		return mealType;
	}
	public void setMealType(List<String> mealType) {
		this.mealType = mealType;
	}
}