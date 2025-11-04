package com.publicis.recipes.controller;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.publicis.recipes.dto.RecipeDTO;
import com.publicis.recipes.exception.CustomException;
import com.publicis.recipes.model.Recipe;
import com.publicis.recipes.service.RecipeService;

@RestController
@RequestMapping("/api/recipes")
@CrossOrigin(origins = "*")
public class RecipeController {

    private final RecipeService recipeService;
    
    RecipeController(RecipeService recipeService){
    	this.recipeService=recipeService;
    }

    @PostMapping("/load")
    public String loadRecipes() throws CustomException {
        return recipeService.loadRecipesFromExternal();
    }

    @GetMapping("/search")
    public List<RecipeDTO> searchRecipes(@RequestParam String query) {
        return recipeService.searchRecipes(query);
    }
    
    @GetMapping("/search-highlight")
    public List<RecipeDTO> searchRecipesHighlight(@RequestParam String query) {
        return recipeService.searchRecipesHighlight(query);
    }

    @GetMapping("/{id}")
    public RecipeDTO getById(@PathVariable Long id) throws CustomException {
        return recipeService.getById(id);
    }
}