package io.terrax.brewery.beer.inventory.job;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemWriter;

import io.terrax.brewery.beer.Beer;

public class BeerInventoryWriter implements ItemWriter<Beer> {

	private static final Logger log = LoggerFactory.getLogger(BeerInventoryWriter.class);
	
    @Override
    public void write(List<? extends Beer> items) throws Exception {

        items.forEach(i -> log.info("Adding beer to inventory: " + i.getName()));
    }
}