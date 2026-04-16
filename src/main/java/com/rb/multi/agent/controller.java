package com.rb.multi.agent;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/agent")
public class controller {

    private final TechAgent techAgent;

    public controller(TechAgent techAgent) {
        this.techAgent = techAgent;
    }

    @GetMapping("/tech")
    public String perguntar(@RequestParam String q) {
        return techAgent.responder(q);
    }
}
