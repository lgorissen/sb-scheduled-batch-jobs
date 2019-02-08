package io.terrax.brewery.beer.inventory.job;

import java.util.Arrays;
import java.util.List;

import org.springframework.batch.item.ItemReader;
import org.springframework.web.client.RestTemplate;

import io.terrax.brewery.beer.Beer;

import org.springframework.http.ResponseEntity;

public class BeerCatalogReader implements ItemReader<Beer> {
	 
	 
    private final String apiUrl;
    private final RestTemplate restTemplate;
 
    private int nextBeerIndex;
    private List<Beer> beerData;
 
    public BeerCatalogReader(String apiUrl, RestTemplate restTemplate) {
        this.apiUrl = apiUrl;
        this.restTemplate = restTemplate;
        nextBeerIndex = 0;
    }
 
    @Override
    public Beer read() throws Exception {
        if (beerDataIsNotInitialized()) {
            beerData = fetchBeerDataFromAPI();
        }
 
        Beer nextStudent = null;
 
        if (nextBeerIndex < beerData.size()) {
            nextStudent = beerData.get(nextBeerIndex);
            nextBeerIndex++;
        }
 
        return nextStudent;
    }
 
    private boolean beerDataIsNotInitialized() {
        return this.beerData == null;
    }
 
    private List<Beer> fetchBeerDataFromAPI() {
        ResponseEntity<Beer[]> response = restTemplate.getForEntity(
            apiUrl, 
            Beer[].class
        );
        Beer[] beerData = response.getBody();
        return Arrays.asList(beerData);
    }
}
