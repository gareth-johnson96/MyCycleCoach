package com.mycyclecoach.feature.hello.controller;

import com.mycyclecoach.feature.hello.domain.HelloResponse;
import com.mycyclecoach.feature.hello.service.HelloService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/hello")
@RequiredArgsConstructor
@Tag(name = "Hello", description = "Hello World endpoint")
public class HelloController {

    private final HelloService helloService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Return a Hello World greeting")
    public HelloResponse hello() {
        return helloService.greet();
    }
}
