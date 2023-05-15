package nl.tudelft.remla.app.controllers;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import nl.tudelft.remla.app.models.SentimentRequest;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Random;

@Controller
public class SentimentController {

	@Value("${modelServiceUrl}")
	private String modelServiceUrl;

	private Integer requestsCounter = 0; // number of successfully made requests

	@GetMapping("/")
	public String showForm(Model model) {
		SentimentRequest sentReq = new SentimentRequest();
		model.addAttribute("sentiment", sentReq);
		return "index";
	}


	@GetMapping(value = "/metrics", produces = MediaType.TEXT_PLAIN_VALUE)
	public ResponseEntity<String> showMetric(Model model) {
		var httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(new MediaType("text", "plain", StandardCharsets.UTF_8));

		Random rand = new Random();
		int random = rand.nextInt(25);

		String numRequestMetric = "# HELP my_app_random This is just a random 'gauge' for illustration. \n"
		 + "# TYPE my_app_random gauge \n" +
			"my_app_random " +random+"\n\n" + "# HELP num_sentiment_requests The number of requests that have been served, by page.\n"
		+ "# TYPE num_sentiment_requests counter\n" + "num_sentiment_requests{method=\"post\",code=\"200\"} " + requestsCounter+"\n";

		return new ResponseEntity<>(numRequestMetric, httpHeaders, HttpStatus.OK);
	}

	@PostMapping("/sentiment")
	public String submitSentimentForm(@ModelAttribute("sentiment") SentimentRequest sentReq) throws IOException {
		double sentiment = sendSentimentRequest(sentReq);
		requestsCounter++;

		if (sentiment < 0.5) {
			sentReq.setSentiment("):");
		} else {
			sentReq.setSentiment("(:");
		}

		return "result";
	}

	/**
	 * This method sends the review to the model-service, and returns a double indicating the
	 * sentiment of the review.
	 * @param sentReq the original sentiment request
	 * @return double containing the sentiment
	 * @throws IOException in case of problems sending the HTTP requests to model-service
	 */
	public double sendSentimentRequest(SentimentRequest sentReq) throws IOException {
		URL url;
		try {
			url = new URL(modelServiceUrl);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}

		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod("POST");
		con.setRequestProperty("Content-Type", "application/json");
		con.setRequestProperty("Accept", "/");
		con.setDoOutput(true);
		String jsonInputString = "{\"content\": \"" + sentReq.getSentiment() + "\"}";

		try (OutputStream os = con.getOutputStream()) {
			byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
			os.write(input, 0, input.length);
		}
		StringBuilder response = new StringBuilder();

		try (BufferedReader br = new BufferedReader(
			new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
			String responseLine;
			while ((responseLine = br.readLine()) != null) {
				response.append(responseLine.trim());
			}
			System.out.println(response);
		}

		JSONObject json = new JSONObject(response.toString());
		return json.getDouble("sentiment");
	}
}
