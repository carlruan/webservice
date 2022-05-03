package edu.neu.webapp.controller;

import com.timgroup.statsd.StatsDClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Healthz {

    @Autowired
    private StatsDClient statsDClient;

    @GetMapping(path = "/healthz")
    public ResponseEntity getStatus(){
        statsDClient.incrementCounter("endpoint.homepage.http.get.healthz");
        return ResponseEntity.ok().body(null);

    }

}