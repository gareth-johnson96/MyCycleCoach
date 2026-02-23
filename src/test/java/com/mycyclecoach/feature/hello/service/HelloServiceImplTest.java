package com.mycyclecoach.feature.hello.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycyclecoach.feature.hello.domain.HelloResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HelloServiceImplTest {

    @InjectMocks
    private HelloServiceImpl helloService;

    @Test
    void shouldReturnHelloWorldResponse() {
        // when
        HelloResponse response = helloService.greet();

        // then
        assertThat(response).isNotNull();
        assertThat(response.message()).isEqualTo("Hello, World!");
    }
}
