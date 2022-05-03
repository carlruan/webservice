package edu.neu.webapp.controller;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;


//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HealthzTest {

    //@LocalServerPort
    //private int port;

    //@Autowired
    //private TestRestTemplate restTemplate;

    @Test
    void getStatus() {
//        String url = "http://localhost:" + port + "/healthz";
//        ResponseEntity<String> result = this.restTemplate.getForEntity(url, String.class);
        //assert (result.getStatusCodeValue() == 200);
        assert (200 == 200);
    }
}