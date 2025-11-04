package com.publicis.recipes.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;
import org.hibernate.search.engine.search.query.dsl.SearchQuerySelectStep;
import org.hibernate.search.engine.search.query.dsl.SearchQueryWhereStep;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import com.publicis.recipes.dto.RecipeDTO;
import com.publicis.recipes.exception.CustomException;
import com.publicis.recipes.model.Recipe;
import com.publicis.recipes.repository.RecipeRepository;

import jakarta.persistence.EntityManager;

@ExtendWith(MockitoExtension.class)
class RecipeServiceTest {

    @Mock
    private RecipeRepository recipeRepository;

    @Mock
    private EntityManager entityManager;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private RecipeService recipeService;

    private Recipe recipe;
    
    private RecipeDTO recipeDTO;

    @BeforeEach
    void setup() {
        recipe = new Recipe();
        recipe.setId(1L);
        recipe.setName("Pizza");
        recipe.setCuisine("Italian");
        recipe.setImage("pizza.jpg");
        recipe.setPrepTimeMinutes(10);
        recipe.setCookTimeMinutes(15);
        recipe.setCaloriesPerServing(250);
        recipe.setServings(2);
        recipe.setDifficulty("Easy");
        recipe.setRating(4.5);
        recipe.setReviewCount(120);
        recipe.setIngredients(List.of("Flour", "Cheese"));
        recipe.setInstructions(List.of("Mix", "Bake"));
        recipe.setTags(List.of("Italian", "Dinner"));
        recipe.setMealType(List.of("Dinner"));
        
        recipeDTO = new RecipeDTO(recipe);
    }

    @Test
    void testLoadRecipesFromExternal_Success() throws Exception {
    	ReflectionTestUtils.setField(recipeService, "recipesUrl", "https://dummyjson.com/recipes");
    	
    	Map<String, Object> recipeData = Map.ofEntries(
    	        Map.entry("id", 1),
    	        Map.entry("name", "Pizza"),
    	        Map.entry("cuisine", "Italian"),
    	        Map.entry("image", "pizza.png"),
    	        Map.entry("prepTimeMinutes", 10),
    	        Map.entry("cookTimeMinutes", 20),
    	        Map.entry("caloriesPerServing", 300),
    	        Map.entry("servings", 2),
    	        Map.entry("difficulty", "Easy"),
    	        Map.entry("rating", 4.5),
    	        Map.entry("reviewCount", 10),
    	        Map.entry("userId", 1),
    	        Map.entry("ingredients", List.of("Flour")),
    	        Map.entry("instructions", List.of("Bake it")),
    	        Map.entry("tags", List.of("Dinner")),
    	        Map.entry("mealType", List.of("Lunch"))
    	);

    	Map<String, Object> apiResponse = new HashMap<>();
    	apiResponse.put("recipes", List.of(recipeData));

        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(apiResponse);
        when(recipeRepository.saveAll(anyList())).thenReturn(Collections.emptyList());

        // Mock Hibernate Search massIndexer
        SearchSession mockSearchSession = mock(SearchSession.class);
        var mockMassIndexer = mock(org.hibernate.search.mapper.orm.massindexing.MassIndexer.class);

        when(mockSearchSession.massIndexer()).thenReturn(mockMassIndexer);
        doNothing().when(mockMassIndexer).startAndWait();

        try (MockedStatic<Search> mockedSearch = mockStatic(Search.class)) {
            mockedSearch.when(() -> Search.session(entityManager)).thenReturn(mockSearchSession);

            String result = recipeService.loadRecipesFromExternal();

            assertTrue(result.contains("Recipes loaded successfully"));
            verify(recipeRepository, atLeastOnce()).saveAll(anyList());
            verify(mockMassIndexer).startAndWait(); // confirm index rebuilding
        }
    }

    @Test
    void testLoadRecipesFromExternal_Failure() {
        // Inject recipesUrl so URI build doesn't crash first
        ReflectionTestUtils.setField(recipeService, "recipesUrl", "https://dummyjson.com/recipes");

        // Simulate network failure
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenThrow(new RuntimeException("Connection error"));

        CustomException ex = assertThrows(CustomException.class, () -> recipeService.loadRecipesFromExternal());

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatus());
        assertTrue(ex.getMessage().contains("Failed to fetch data"));
    }
    
    @Test
    void testLoadRecipesFallback() {
        String msg = recipeService.loadRecipesFallback(new RuntimeException("Timeout"));
        assertTrue(msg.contains("Failed to load recipes"));
    }
    
    @Test
    void testGetById_Success() throws Exception {
        when(recipeRepository.findById(1L)).thenReturn(Optional.of(recipe));

        RecipeDTO dto = recipeService.getById(1L);

        assertEquals("Pizza", dto.getName());
        assertEquals("Italian", dto.getCuisine());
        assertEquals(4.5, dto.getRating());
    }
    
    @Test
    void testGetById_NotFound() {
        when(recipeRepository.findById(anyLong())).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class, () -> recipeService.getById(99L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertTrue(ex.getMessage().contains("Recipe not found"));
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
    void testSearchRecipes_Success() {
    	// Fake data
        List<Recipe> fakeRecipes = List.of(recipe);

        // Mocks
        SearchSession mockSearchSession = mock(SearchSession.class);
        SearchQuerySelectStep mockSelectStep = mock(SearchQuerySelectStep.class);
        SearchQueryOptionsStep mockFinalStep = mock(SearchQueryOptionsStep.class);

        // Mock the static call
        try (MockedStatic<Search> mockedSearch = mockStatic(Search.class)) {
            mockedSearch.when(() -> Search.session(entityManager)).thenReturn(mockSearchSession);
            when(mockSearchSession.search(Recipe.class)).thenReturn(mockSelectStep);
            when(mockSelectStep.where(any(Function.class))).thenReturn(mockFinalStep);
            when(mockFinalStep.fetchHits(20)).thenReturn(fakeRecipes);

            // Execute
            List<RecipeDTO> results = recipeService.searchRecipes("Pizza");

            // Verify
            assertEquals(1, results.size());
            assertEquals("Pizza", results.get(0).getName());
        }
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
    void testSearchRecipesHighlight_Success() {
    	// Fake data setup
        Recipe recipe = new Recipe();
        recipe.setId(1L);
        recipe.setName("South Indian Masala Dosa");
        recipe.setCuisine("Indian");

        List<String> nameHighlight = List.of("South <em>Indian</em> Masala <em>Dosa</em>");
        List<String> cuisineHighlight = List.of("<em>Indian</em>");
        List<Object> hit = new ArrayList<>();
        hit.add(recipe);
        hit.add(nameHighlight);
        hit.add(cuisineHighlight);
        List<List<?>> fakeResults = List.of(hit);

        // Mock Hibernate Search chain
        SearchSession mockSearchSession = mock(SearchSession.class);
        SearchQuerySelectStep mockSelectStep = mock(SearchQuerySelectStep.class);
        SearchQueryWhereStep mockWhereStep = mock(SearchQueryWhereStep.class);
        SearchQueryOptionsStep mockOptionsStep = mock(SearchQueryOptionsStep.class);

        // Mock static Search.session(entityManager)
        try (MockedStatic<Search> mockedSearch = mockStatic(Search.class)) {
            mockedSearch.when(() -> Search.session(entityManager)).thenReturn(mockSearchSession);
            when(mockSearchSession.search(Recipe.class)).thenReturn(mockSelectStep);

            // Correct generic chain for Hibernate Search 7.x
            when(mockSelectStep.select(any(Function.class))).thenReturn((SearchQueryWhereStep) mockWhereStep);
            when(mockWhereStep.where(any(Function.class))).thenReturn(mockOptionsStep);
            when(mockOptionsStep.highlighter(anyString(), any(Function.class))).thenReturn(mockOptionsStep);
            when(mockOptionsStep.fetchHits(20)).thenReturn(fakeResults);

            // Execute
            List<RecipeDTO> results = recipeService.searchRecipesHighlight("chicken");

            // Verify
            assertEquals(1, results.size());
            RecipeDTO dto = results.get(0);
            assertEquals("South Indian Masala Dosa", dto.getName());
            assertEquals("Indian", dto.getCuisine());
            assertEquals("South <em>Indian</em> Masala <em>Dosa</em>", dto.getHighlightedName());
            assertEquals("<em>Indian</em>", dto.getHighlightedCuisine());
        }
    }
}