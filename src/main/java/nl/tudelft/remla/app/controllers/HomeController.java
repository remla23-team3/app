package nl.tudelft.remla.app.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

	public HomeController(){}

	@GetMapping("/home")
	public String home() {
		System.out.println("test");
		return "index";
	}
}
