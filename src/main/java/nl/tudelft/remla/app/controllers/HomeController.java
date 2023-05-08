package nl.tudelft.remla.app.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

	public HomeController(){}

	@GetMapping("/home")
	public String home() {
		return "static/index.html";
	}

}

