package com.publicis.recipes.controller;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.publicis.recipes.dto.RecipeDTO;
import com.publicis.recipes.model.Recipe;
import com.publicis.recipes.service.RecipeService;

@WebMvcTest(RecipeController.class)
public class RecipeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RecipeService recipeService;

    private Recipe recipe;
    
    private RecipeDTO recipeDTO;

    @BeforeEach
    void setup() {
        recipe = new Recipe();
        recipe.setId(1L);
        recipe.setName("Pasta");
        recipe.setCuisine("Italian");
        
        recipeDTO = new RecipeDTO(recipe);
    }

    @Test
    void testLoadRecipes() throws Exception {
        Mockito.when(recipeService.loadRecipesFromExternal()).thenReturn("Loaded 50 recipes");

        mockMvc.perform(post("/api/recipes/load"))
                .andExpect(status().isOk())
                .andExpect(content().string("Loaded 50 recipes"));
    }

    @Test
    void testSearchRecipes() throws Exception {
        Mockito.when(recipeService.searchRecipes(anyString())).thenReturn(List.of(recipeDTO));

        mockMvc.perform(get("/api/recipes/search").param("query", "pasta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Pasta"));
    }

    @Test
    void testGetById() throws Exception {
        RecipeDTO dto = new RecipeDTO();
        dto.setId(1L);
        dto.setName("Pizza");
        dto.setCuisine("Italian");

        Mockito.when(recipeService.getById(anyLong())).thenReturn(dto);

        mockMvc.perform(get("/api/recipes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Pizza"))
                .andExpect(jsonPath("$.cuisine").value("Italian"));
    }

    @Test
    void testSearchRecipesHighlight() throws Exception {
        RecipeDTO dto = new RecipeDTO();
        dto.setId(2L);
        dto.setName("Burger");
        dto.setCuisine("American");

        Mockito.when(recipeService.searchRecipesHighlight(anyString())).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/recipes/search-highlight").param("query", "burger"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Burger"));
    }
}