package io.terrax.brewery.beer.inventory.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;

import io.terrax.brewery.beer.Beer;

public class BeerProcessor implements ItemProcessor<Beer, Beer> {

	private static final Logger log = LoggerFactory.getLogger(BeerProcessor.class);

	@Override
	public Beer process(Beer item) throws Exception {

		log.info("Processing beer information: {}", item.getName());
		
		// you could do some work on the Beer item here

		return item;
	}
}
