package com.mycyclecoach.feature.hello.service;

import com.mycyclecoach.feature.hello.domain.HelloResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class HelloServiceImpl implements HelloService {

    @Override
    public HelloResponse greet() {
        log.info("Serving hello world greeting");
        return new HelloResponse("Hello, World!");
    }
}
