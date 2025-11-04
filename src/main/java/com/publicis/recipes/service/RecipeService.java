package com.publicis.recipes.service;

import com.publicis.recipes.dto.RecipeDTO;
import com.publicis.recipes.exception.CustomException;
import com.publicis.recipes.model.Recipe;
import com.publicis.recipes.repository.RecipeRepository;

import io.github.resilience4j.retry.annotation.Retry;
import jakarta.persistence.EntityManager;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Service
public class RecipeService {
	private static final Logger logger = LoggerFactory.getLogger(RecipeService.class);
	
	@Value("${app.external.recipes-url}")
	private String recipesUrl;
	
    private final RecipeRepository recipeRepository;
    private final EntityManager entityManager;
    private final RestTemplate restTemplate;
    
    RecipeService(RecipeRepository recipeRepository, RestTemplate restTemplate, EntityManager entityManager) {
        this.recipeRepository = recipeRepository;
        this.restTemplate = restTemplate;
        this.entityManager = entityManager;
    }
    
    @Retry(name = "recipes-api", fallbackMethod = "loadRecipesFallback")
    public String loadRecipesFromExternal() throws CustomException {
        logger.info("Loading recipes from external API...");

        int skip = 0;
        int limit = 30;
        List<Recipe> allRecipes = new ArrayList<>();

        do {
        	String uri = UriComponentsBuilder
        	        .fromUriString(recipesUrl)
        	        .queryParam("limit", limit)
        	        .queryParam("skip", skip)
        	        .toUriString();
        	
        	logger.debug("Calling external API: {}", uri);
        	
        	Map response;
        	try {
        		response = restTemplate.getForObject(uri, Map.class);
        	} catch (Exception ex) {
        	    logger.error("Failed to fetch data from external API at skip={} : {}", skip, ex.getMessage());
        	    throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to fetch data from external API, Error: %s".formatted(ex.getLocalizedMessage()));
        	}


            if (response == null || !response.containsKey("recipes")) {
                logger.warn("No valid response from external API. Stopping.");
                throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to fetch data from external API");
            }

            List<Map<String, Object>> recipes = (List<Map<String, Object>>) response.get("recipes");

            if (recipes == null || recipes.isEmpty()) {
                logger.info("No more recipes found. Ending fetch.");
                break;
            }

            List<Recipe> batch = recipes.stream().map(r -> {
            	Recipe recipe = new Recipe();
                recipe.setId(Long.valueOf(r.get("id").toString()));
                recipe.setName(r.get("name").toString());
                recipe.setCuisine(r.get("cuisine").toString());
                recipe.setImage(r.get("image").toString());
                recipe.setPrepTimeMinutes((Integer) r.get("prepTimeMinutes"));
                recipe.setCookTimeMinutes((Integer) r.get("cookTimeMinutes"));
                recipe.setCaloriesPerServing((Integer) r.get("caloriesPerServing"));
                recipe.setServings((Integer) r.get("servings"));
                recipe.setDifficulty((String) r.get("difficulty"));
                recipe.setRating(Double.valueOf(r.get("rating").toString()));
                recipe.setReviewCount((Integer) r.get("reviewCount"));
                recipe.setUserId(Long.valueOf(r.get("userId").toString()));
                recipe.setIngredients((List<String>) r.get("ingredients"));
                recipe.setInstructions((List<String>) r.get("instructions"));
                recipe.setTags((List<String>) r.get("tags"));
                recipe.setMealType((List<String>) r.get("mealType"));
                return recipe;
            }).toList();

            allRecipes.addAll(batch);
            logger.info("Fetched {} recipes (total so far: {})", batch.size(), allRecipes.size());

            skip += limit;
            
            if (batch.size() < limit) {
                logger.info("Last batch detected ({} < {}), ending fetch.", batch.size(), limit);
                break;
            }

        } while (true);

        recipeRepository.saveAll(allRecipes);
        logger.info("Successfully loaded {} recipes into H2 DB", allRecipes.size());
        
        // Rebuild Lucene index
        SearchSession searchSession = Search.session(entityManager);
        try {
			searchSession.massIndexer().startAndWait();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

        return "Recipes loaded successfully: " + allRecipes.size();
    }
    
    public String loadRecipesFallback(Throwable ex) {
        logger.error("Fallback triggered due to error: {}", ex.getMessage());
        return "Failed to load recipes. Please try again later.";
    }

    public List<RecipeDTO> searchRecipes(String query) {
        logger.info("Performing full-text search for query: {}", query);

        SearchSession searchSession = Search.session(entityManager);
        
        List<Recipe> recipes = searchSession.search(Recipe.class)
                .where(f -> f.bool()
                        .should(f.match().field("name").matching(query).fuzzy(2))
                        .should(f.match().field("cuisine").matching(query).fuzzy(2))
                )
                .fetchHits(20);
        
        List<RecipeDTO> responseList = new ArrayList<>();
        for(Recipe recipe: recipes) {
        	RecipeDTO recipeDTO = new RecipeDTO(recipe);
	        responseList.add(recipeDTO);
        }
        
        return responseList;
    }

    public RecipeDTO getById(Long id) throws CustomException {
    	Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Recipe not found with id: " + id));
    	
    	RecipeDTO recipeDTO = new RecipeDTO(recipe);
    	return recipeDTO;
    }

	public List<RecipeDTO> searchRecipesHighlight(String query) {
		logger.info("Performing full-text search with highlighting for query: {}", query);

	    SearchSession searchSession = Search.session(entityManager);

	    var result = searchSession.search(Recipe.class)
	            .select(f -> f.composite(
	                    f.entity(),
	                    f.highlight("name"),
	                    f.highlight("cuisine")
	            ))
	            .where(f -> f.bool()
	                    .should(f.match().field("name").matching(query).fuzzy(2))
	                    .should(f.match().field("cuisine").matching(query).fuzzy(2))
	            )
	            .highlighter("html", f -> f.unified())
	            .fetchHits(20);
	    
	    List<RecipeDTO> responseList = new ArrayList<>();

	    for (List<?> hit : result) {
	        Recipe recipe = (Recipe) hit.get(0);
	        List<String> nameHighlights = (List<String>) hit.get(1);
	        List<String> cuisineHighlights = (List<String>) hit.get(2);

	        RecipeDTO recipeDTO = new RecipeDTO(recipe);

	        // Include highlights (if found)
	        if (nameHighlights != null && !nameHighlights.isEmpty()) {
	        	recipeDTO.setHighlightedName(nameHighlights.get(0));
	        }
	        if (cuisineHighlights != null && !cuisineHighlights.isEmpty()) {
	        	recipeDTO.setHighlightedCuisine(cuisineHighlights.get(0));
	        }

	        responseList.add(recipeDTO);
	    }

	    logger.info("Found {} results for query '{}'", responseList.size(), query);
	    return responseList;
	}
}