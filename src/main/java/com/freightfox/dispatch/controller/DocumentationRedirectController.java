package com.freightfox.dispatch.controller;

import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A discoverable entry point: the API's root redirects to its OpenAPI explorer.
 * This does not add an application frontend.
 */
@RestController
public class DocumentationRedirectController {
    @GetMapping("/")
    public ResponseEntity<Void> documentation() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create("/swagger-ui/index.html"))
                .build();
    }
}
