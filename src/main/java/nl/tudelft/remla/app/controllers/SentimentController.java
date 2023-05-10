package nl.tudelft.remla.app.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import nl.tudelft.remla.app.models.SentimentRequest;

@Controller
public class SentimentController {

	@GetMapping("/")
	public String showForm(Model model) {
		SentimentRequest sentReq = new SentimentRequest();
		model.addAttribute("sentiment", sentReq);
		return "index";
	}

	@PostMapping("/sentiment")
	public String submitSentimentForm(@ModelAttribute("sentiment") SentimentRequest sentReq) {
		return "result";
	}

}
