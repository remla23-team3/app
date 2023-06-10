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
	private Integer requestsPositive = 0;  //number of successfully made positive requests
	private Integer requestsNegative = 0;  // number of successfully made negative requests
	private Integer negativeFeedback = 0; 	  // number of user negative feedback
	private Integer positiveFeedback = 0;	  // number of user positive feedback
	private Integer submittedReviews = 0;
	private Integer submittedFeedback = 0;

	@GetMapping("/")
	public String showForm(Model model) {
		SentimentRequest sentReq = new SentimentRequest();
		model.addAttribute("sentiment", sentReq);

		return "index";
	}


	@GetMapping("/give-feedback")
	public String showResult(Model model) {
		FeedbackRequest feedbackRequest = new FeedbackRequest();
		feedbackRequest.setFeedback(null);
		model.addAttribute("feedback", feedbackRequest);
		
		return "feedbackpage";
	}

	/**
	 * Metrics endpoint that will be used by Prometheus.
	 * @return plain text with definitions of the metrics for Prometheus
	 */

	@GetMapping(value = "/metrics", produces = MediaType.TEXT_PLAIN_VALUE)
	public ResponseEntity<String> showMetric() {
		var httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(new MediaType("text", "plain", StandardCharsets.UTF_8));

		StringBuilder metrics = new StringBuilder();

		metrics.append("# HELP remla23_team3:num_sentiment_total_requests The number of all requests that have been made.\n");
		metrics.append("# TYPE remla23_team3:num_sentiment_total_requests counter\n");
		metrics.append("remla23_team3:num_sentiment_total_requests{method=\"post\",code=\"200\"} ").append(requestsCounter).append("\n\n");

		metrics.append("# HELP remla23_team3:num_sentiment_requests_per_type The number of sentiments per type based on the model prediction.\n");
		metrics.append("# TYPE remla23_team3:num_sentiment_requests_per_type counter\n");
		metrics.append("remla23_team3:num_sentiment_requests_per_type{type=\"positive\"} ").append(requestsPositive).append("\n");
		metrics.append("remla23_team3:num_sentiment_requests_per_type{type=\"negative\"} ").append(requestsNegative).append("\n\n");

		metrics.append("# HELP remla23_team3:feedback_per_type The number of sentiments per type based on the feedback.\n");
		metrics.append("# TYPE remla23_team3:feedback_per_type counter\n");
		metrics.append("remla23_team3:feedback_per_type{type=\"positive\"} ").append(positiveFeedback).append("\n");
		metrics.append("remla23_team3:feedback_per_type{type=\"negative\"} ").append(negativeFeedback).append("\n\n");

		metrics.append("# HELP accuracy The accuracy based on the feedback.\n");
		metrics.append("# TYPE accuracy gauge\n");
		metrics.append("remla23_team3:accuracy ").append((double) positiveFeedback/ (double) Math.max(1, requestsCounter)).append("\n\n");

		metrics.append("# HELP remla23_team3:feedback_percentage How many people that submitted a review also submitted feedback.\n");
		metrics.append("# TYPE feedback percentage\n");
		metrics.append("remla23_team3:feedback_percentage ").append((double) submittedFeedback / (double) Math.max(1, submittedReviews)).append("\n\n");

		return new ResponseEntity<>(metrics.toString(), httpHeaders, HttpStatus.OK);
	}

	@PostMapping("/sentiment")
	public String submitSentimentForm(@ModelAttribute("sentiment") SentimentRequest sentReq) throws IOException {
		double sentiment = sendSentimentRequest(sentReq);
		requestsCounter++;
		submittedReviews++;
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
	public String submitFeedback(@ModelAttribute("feedback") FeedbackRequest sentReq) {
		String feedback = sentReq.getFeedback();
		requestsCounter++;
		submittedFeedback++;
		if (feedback.equals("bad")) {
			// The user gives negative feedback when the prediction is bad
			// Do not overwrite it with sentReq.setFeedback()
			this.negativeFeedback++;

			System.out.println("Total # negative prediction feedback: " + this.negativeFeedback);

		} else {
			// The user gives positive feedback when the prediction is good
			// Do not overwrite it with sentReq.setFeedback()
			this.positiveFeedback++;

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
