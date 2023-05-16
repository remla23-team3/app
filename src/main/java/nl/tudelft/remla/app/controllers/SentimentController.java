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

import nl.tudelft.remla.app.models.FeedbackRequest;
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

	private Integer requestsCounter = 0;  // number of successfully made requests
	private Integer requestsPositive = 0; // number of successfully made requests
	private Integer requestsNegative = 0; // number of successfully made requests
	private Integer sentimentFeedback = 0; // variable feedback value
	private Integer negativeFeedback = 0; 	  // number of user negative feedback
	private Integer positiveFeedback = 0;	  // number of user positive feedback

	@GetMapping("/")
	public String showForm(Model model) {
		SentimentRequest sentReq = new SentimentRequest();
		model.addAttribute("sentiment", sentReq);

		return "index";
	}


	@GetMapping("/give-feedback")
	public String showResult(Model model) {
		FeedbackRequest feedbackRequest = new FeedbackRequest();
		feedbackRequest.setFeedback(-1);
		model.addAttribute("feedback", feedbackRequest);
		
		return "feedbackpage";
	}


	@GetMapping(value = "/metrics", produces = MediaType.TEXT_PLAIN_VALUE)
	public ResponseEntity<String> showMetric(Model model) {
		var httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(new MediaType("text", "plain", StandardCharsets.UTF_8));

		Random rand = new Random();
		int random = rand.nextInt(25);
		StringBuilder metrics = new StringBuilder();
		metrics.append("# HELP my_app_random This is just a random 'gauge' for illustration.\n");
		metrics.append("# TYPE my_app_random gauge\n");
		metrics.append("my_app_random ").append(random).append("\n\n");

		metrics.append("# HELP num_sentiment_total_requests The number of all requests that have been made.\n");
		metrics.append("# TYPE num_sentiment_total_requests counter\n");
		metrics.append("num_sentiment_total_requests{method=\"post\",code=\"200\"} ").append(requestsCounter).append("\n\n");

		metrics.append("# HELP num_sentiment_requests_per_type The number of positive sentiments that have been made.\n");
		metrics.append("# TYPE num_sentiment_requests_per_type counter\n");
		metrics.append("num_sentiment_requests_per_type{type=\"positive\"} ").append(requestsPositive).append("\n");
		metrics.append("num_sentiment_requests_per_type{type=\"negative\"} ").append(requestsNegative).append("\n");

		return new ResponseEntity<>(metrics.toString(), httpHeaders, HttpStatus.OK);
	}

	@PostMapping("/sentiment")
	public String submitSentimentForm(@ModelAttribute("sentiment") SentimentRequest sentReq) throws IOException {
		double sentiment = sendSentimentRequest(sentReq);
		requestsCounter++;

		if (sentiment < 0.5) {
			// Do something with the negative sentiment

			// Do not overwrite sentReq with sentReq.setSentiment()
			// because the "):" would be overwritten
			sentReq.setReview("):");
			requestsNegative++;
		} else {
			// Do something with the positive sentiment

			// Do not overwrite sentReq with sentReq.setSentiment()
			// because the "(:" would be overwritten
			sentReq.setReview("(:");
			requestsPositive++;
		}

		return "index";
	}

	@PostMapping("/feedback")
	public String submitFeedback(@ModelAttribute("feedback") FeedbackRequest sentReq) throws IOException {
		double feedback = sentReq.getFeedback();
		requestsCounter++;

		if (feedback < 50) {
			// The user gives negative feedback when the prediction is bad
			// Do not overwrite it with sentReq.setFeedback()
			this.sentimentFeedback = sentReq.getFeedback();
			this.negativeFeedback++;

			System.out.println("Sentiment prediction feedback: " + this.sentimentFeedback);
			System.out.println("Total # negative prediction feedback: " + this.negativeFeedback);

		} else {
			// The user gives positive feedback when the prediction is good
			// Do not overwrite it with sentReq.setFeedback()
			this.sentimentFeedback = sentReq.getFeedback();
			this.positiveFeedback++;

			System.out.println("Sentiment prediction feedback: " + this.sentimentFeedback);
			System.out.println("Total # positive prediction feedback: " + this.positiveFeedback);
		}

		return "feedbackpage";
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
