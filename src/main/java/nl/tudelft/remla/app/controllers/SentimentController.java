package nl.tudelft.remla.app.controllers;
import nl.tudelft.remla.app.models.SentimentRequest;
import nl.tudelft.remla.app.models.SentimentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

@Controller
public class SentimentController {

	@Value("${modelServiceUrl}")
	private String modelServiceUrl;

	@PostMapping(value = "/sentiment", consumes = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public SentimentResponse getSentiment(@RequestBody SentimentRequest request) {
		// Set the headers for the request
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		// Set the request entity
		HttpEntity<SentimentRequest> entity = new HttpEntity<>(request, headers);

		// Send the POST request to the model-service and get the response
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<SentimentResponse> response = restTemplate.exchange(modelServiceUrl, HttpMethod.POST, entity, SentimentResponse.class);

		// Return the sentiment response back to the frontend
		return response.getBody();
	}
}

