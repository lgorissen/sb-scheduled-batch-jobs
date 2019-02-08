package io.terrax.brewery.beer.inventory;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import io.terrax.brewery.beer.Beer;
import io.terrax.brewery.beer.inventory.job.BeerCatalogReader;
import io.terrax.brewery.beer.inventory.job.BeerInventoryWriter;
import io.terrax.brewery.beer.inventory.job.BeerProcessor;

@Component
public class ScheduledTasks {

	private static final Logger log = LoggerFactory.getLogger(ScheduledTasks.class);
	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

	@Autowired
	JobLauncher jobLauncher;

	@Autowired
	private JobBuilderFactory jobs;

	@Autowired
	private StepBuilderFactory steps;

	@Autowired
	JobCompletionNotificationListener listener;
	
	@Autowired
	Environment environment;

	@Scheduled(fixedDelay = 5000)
	public void updateBeerInventory() {

		log.info("Updating beer inventory for Terra10 {}", dateFormat.format(new Date()));

		try {
			
			RestTemplate restTemplate = new RestTemplate();
			ItemReader<Beer> restBeerReaderLocal = new BeerCatalogReader(environment.getRequiredProperty("rest.api.to.beer.catalog.url"), restTemplate);
			
			ItemProcessor<Beer, Beer> restBeerProcessorLocal = new BeerProcessor();

			ItemWriter<Beer> restBeerWriterLocal = new BeerInventoryWriter();
			

			
			Step restBeerStepLocal = steps.get("restBeerStepLocal")
					                      .allowStartIfComplete(true)
					                      .<Beer, Beer>chunk(1)
					                      .reader(restBeerReaderLocal)
					                      .processor(restBeerProcessorLocal)
					                      .writer(restBeerWriterLocal)
					                      .build();
			
			Job restBeerJobLocal = jobs.get("restBeerJobLocal")
					                   .incrementer(new RunIdIncrementer())
					                   .listener(listener)
					                   .flow(restBeerStepLocal)
					                   .end()
					                   .build();
			
			jobLauncher.run(restBeerJobLocal, new JobParameters());
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}

}
