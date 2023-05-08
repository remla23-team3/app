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
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class SentimentController {

	@Value("${modelServiceUrl}")
	private String modelServiceUrl;

	@PostMapping(value = "/sentiment", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
//	@ResponseBody
	public RedirectView handleSentimentRequest(@ModelAttribute SentimentRequest request, RedirectAttributes redirectAttributes, Model model) {
		System.out.println(request);

		// Call the service to get the sentiment analysis result
		String sentiment = "sad";

		// Add the sentimentRequest and sentiment attributes to the model
		model.addAttribute("sentimentRequest", request);
		model.addAttribute("sentiment", sentiment);
		redirectAttributes.addFlashAttribute("sentimentRequest", request);
		redirectAttributes.addFlashAttribute("sentiment", sentiment);
		// Return the name of the view
		return new RedirectView("/showResult");
	}

	@GetMapping("/showResult")
	public String showResult(@ModelAttribute("sentimentRequest") SentimentRequest sentimentRequest,
							 @ModelAttribute("sentiment") String sentiment, Model model) {
		model.addAttribute("sentimentRequest", new SentimentRequest());
		model.addAttribute("sentiment", "sad");
		return "result";
	}
//		HttpHeaders headers = new HttpHeaders();
//		headers.setContentType(MediaType.APPLICATION_JSON);
//
//		// Set the request entity
//		HttpEntity<SentimentRequest> entity = new HttpEntity<>(request, headers);
//
//		// Send the POST request to the model-service and get the response
//		RestTemplate restTemplate = new RestTemplate();
//		ResponseEntity<SentimentResponse> response = restTemplate.exchange(modelServiceUrl, HttpMethod.POST, entity, SentimentResponse.class);
//		return "sad";

		// Return the sentiment response back to the frontend
//		return response.getBody();

}

