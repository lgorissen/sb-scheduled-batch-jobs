# sb-scheduled-batch-jobs

There are plenty of Spring guides that get you started on lots of topics: https://spring.io/guides. But every now and then, you need to combine topics to achieve your goal. This is such an example, where we want to:
- invoke a REST Service that returns a number of records 
- process these records one-by-one (=batch processing)
- ... and do that on a scheduled interval (=task scheduling)

## Overview 

As always, this example is situated in the Terra 10 beer brewery:

![Overview](overview.png)

In the above figure:

**Beer catalog**

An external REST API that returns a list of beer records:

```bash
developer@developer-VirtualBox:~$ curl https://virtserver.swaggerhub.com/TerraX_Brewery/Beercatalog/1.0.0/beers
[ {
  "id" : "d290f1ee-6c54-4b01-90e6-d701748f0851",
  "name" : "TerraX Tripel",
  "releaseDate" : "2019-01-28T09:12:33.001Z",
  "manufacturer" : {
    "name" : "TerraX Brewery",
    "homePage" : "https://terra10.io",
    "phone" : "123-555-666"
  }
}, {
  "id" : "d290f1ee-6c54-4b01-90e6-d701748f0852",
  "name" : "TerraX Golden Dragon",
.....
```
It is implemented as an mock service in SwaggerHub. Note that all sources can be found on GitHub: https://github.com/lgorissen/sb-scheduled-batch-jobs.git. The Swagger definition can be found there as file `TerraX_Brewery-Beercatalog-1.0.0-swagger.yaml`. 

**Scheduled Jobs**

This is the core part of this blog: a Spring Boot service that schedules a job every 10 seconds, that:
1. Queries the *Beer catalog* service for a list of beer records
2. Processes each beer record, i.e. one-by-one

The goal is to achieve the above functionality with as little code as possible, i.e. maximal leveraging of the Spring Boot features.

## Code highlights

The table below shows the java files:

|   File   |  Description |
|----------|--------------|
| io/terrax/brewery/beer/Beer.java | Defines the records from the Beer catalog API - Beer |
| io/terrax/brewery/beer/Manufacturer.java | Defines the records from the Beer catalog API - Manufacturer  |
| io/terrax/brewery/beer/inventory/Application.java | Schedules Jobs - the Spring Boot application |
| io/terrax/brewery/beer/inventory/JobCompletionNotificationListener.java | Logic that is executed when job is completed | 
| io/terrax/brewery/beer/inventory/ScheduledTasks.java | Job definition and scheduling  | 
| io/terrax/brewery/beer/inventory/job/BeerCatalogReader.java | Job step - for reading from Beer catalog API | 
| io/terrax/brewery/beer/inventory/job/BeerInventoryWriter.java | Job step - for updating the Beer inventory TerraX | 
| io/terrax/brewery/beer/inventory/job/BeerProcessor.java | Job step - for internal transformation of Beer records | 


### Beer.java and Manufacturer.java

Both `Beer.java` and `Manufacturer.java` are business classes that represent the beer records that are received from the Beer catalog API. What more is there to say ...


### Application.java

The `Application.java` starts the Spring Boot application. Note that it enables both Scheduling and BatchProcessing:

```java
...
@SpringBootApplication
@EnableScheduling              // enable scheduling 
@EnableBatchProcessing         // enable batch processing
public class Application {

    public static void main(String[] args) throws Exception {
        SpringApplication.run(Application.class, args);
    }
}

```

### ScheduledTasks.java

By far the most interesting part: the job scheduling happens here. The most interesting parts:

- ScheduledTasks is a SpringBoot Component, thus registering this class as a Bean
- A JobLauncher, JobBuilderFactory and StepBuilderFactory are autowired as they are needed for handling jobs 
- A JobCompletionNotificationListener is needed for executing logic upon Job completion 
- @Scheduled annotation schedules a void method for execution. In this example, the method is executed 5000msec after the previous execution has finished
 

```java
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
			
			RestTemplate restTemplate = new RestTemplate();  // for REST call to Beer catalog API
                        // A Job consists of
                        // - an ItemReader
                        // - an ItemProcessor
                        // - an ItemWriter

			ItemReader<Beer> restBeerReaderLocal = new BeerCatalogReader(environment.getRequiredProperty("rest.api.to.beer.catalog.url"), restTemplate);
			
			ItemProcessor<Beer, Beer> restBeerProcessorLocal = new BeerProcessor();

			ItemWriter<Beer> restBeerWriterLocal = new BeerInventoryWriter();
			

		        // Define a Job Step 	
			Step restBeerStepLocal = steps.get("restBeerStepLocal")          // get new StepBuilder
                                                      .allowStartIfComplete(true)        // allow job to be re-started
                                                      .<Beer, Beer>chunk(1)              // step processes chunks of size 1
                                                      .reader(restBeerReaderLocal)       // add the reader
			                              .processor(restBeerProcessorLocal) // add the processer
                                                      .writer(restBeerWriterLocal)       // add the writer
                                                      .build();                          // build the step
		        // Define a Job	
			Job restBeerJobLocal = jobs.get("restBeerJobLocal")              // get new JobBuilder
                                                   .incrementer(new RunIdIncrementer())  // increment Run.id parameter 
                                                   .listener(listener)                   // at job listener to JobBuilder
                                                   .flow(restBeerStepLocal)              // add step to JobBuilder
                                                   .end()                                // let JobFlowBuilder build
                                                   .build();                             // inject into parent builder
			
			jobLauncher.run(restBeerJobLocal, new JobParameters());
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
```

### BeerCatalogReader.java

The BeerCatalogReader is the ItemReader of the Job. It reads the records from the Beer catalog API.

```java
package io.terrax.brewery.beer.inventory.job;

...

public class BeerCatalogReader implements ItemReader<Beer> {
	 
	 
    public BeerCatalogReader(String apiUrl, RestTemplate restTemplate) {
        this.apiUrl = apiUrl;                       // api endpoint needed for doing the REST call
        this.restTemplate = restTemplate;           // restTeample needed for doing the REST call
        nextBeerIndex = 0;
    }
 
    @Override
    public Beer read() throws Exception {           // ItemReader needs a read() method
        if (beerDataIsNotInitialized()) {
            beerData = fetchBeerDataFromAPI();
        }
 
        Beer nextBeer = null;
 
        if (nextBeerIndex < beerData.size()) {
            nextBeer = beerData.get(nextBeerIndex);
            nextBeerIndex++;
        }
 
        return nextBeer;                            // the read() method returns the beers one-by-one
    }

...
 
}

```



[highlight code aspects]

[show that it works / test it]

[explain where to find it]



