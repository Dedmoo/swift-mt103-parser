package com.mehmetserin.swift.web;

import com.mehmetserin.swift.model.Mt103Models.ParseRequest;
import com.mehmetserin.swift.model.Mt103Models.ParsedMt103;
import com.mehmetserin.swift.service.Mt103Parser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/swift/mt103")
public class SwiftController {

    private final Mt103Parser parser;

    public SwiftController(Mt103Parser parser) {
        this.parser = parser;
    }

    @PostMapping("/parse")
    public ResponseEntity<ParsedMt103> parse(@Valid @RequestBody ParseRequest request) {
        return ResponseEntity.ok(parser.parse(request.message()));
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "healthy", "service", "SwiftMt103Parser");
    }
}
