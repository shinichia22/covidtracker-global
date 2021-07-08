package com.abhishek.coronavirustracker.service;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.abhishek.coronavirustracker.model.LocationStats;

@Service
public class CoronaVirusDataService {
	private static String VIRUS_DATA_URL = "https://raw.githubusercontent.com/CSSEGISandData/COVID-19/master/csse_covid_19_data/csse_covid_19_time_series/time_series_covid19_confirmed_global.csv";

	private List<LocationStats> allStats = new ArrayList<>();

	public List<LocationStats> getAllStats() {
		return allStats;
	} // since this is private and to get all records

	@PostConstruct // will execute at end of spring-boot execution
	@Scheduled(cron = "* * 1 * * *") // this will schedule to run every seconds when the application runs
	public void fetchVirusData() throws IOException, InterruptedException {

		List<LocationStats> newStats = new ArrayList<>(); // for resolving concurrency issues

		// creating HTTP request explicitly
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(VIRUS_DATA_URL)).build();

		// send response
		HttpResponse<String> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());

		// to parse the response body from string data to character
		StringReader csvBodyReader = new StringReader(httpResponse.body());

		Iterable<CSVRecord> records = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(csvBodyReader);
		for (CSVRecord record : records) {
			LocationStats locationstat = new LocationStats();
			String state = record.get("Province/State");

			if (state == "" || state == null) {
				state = "All";
			}
			locationstat.setState(state);
			locationstat.setCountry(record.get("Country/Region"));

			int latestCases = Integer.parseInt(record.get(record.size() - 1));
			int preDayCases = Integer.parseInt(record.get(record.size() - 2));

			locationstat.setLatestTotalCases(latestCases);
			locationstat.setDiffFromPrevDay(latestCases - preDayCases);
			newStats.add(locationstat);

		}

		this.allStats = newStats;

	}

}