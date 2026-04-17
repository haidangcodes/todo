package com.todo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WelcomeController {

    @GetMapping("/app")
    public String app() {
        return "forward:/app/index.html";
    }

    @GetMapping("/auth/login")
    public String login() {
        return "forward:/auth/index.html";
    }

    @GetMapping("/")
    public String index() {
        return "forward:/index.html";
    }
}
