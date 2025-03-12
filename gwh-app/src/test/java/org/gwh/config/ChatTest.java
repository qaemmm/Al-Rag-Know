package org.gwh.config;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.gwh.trigger.http.OllamaController;
import org.gwh.trigger.http.service.Ollama;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.core.publisher.Flux;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class ChatTest {

    @Resource
    private OllamaController ollamaController;

    @Test
    public void test() {
        String model = "deepseek-r1:1.5b";
        String ragTag = "";
        String message = "1+1";
        Flux<ChatResponse> chatResponseFlux = ollamaController.generateStreamRag(model, ragTag, message);
        Flux<ChatResponse> chatResponseFlux2 = ollamaController.generateStream(model, message);
        System.out.println(chatResponseFlux2.toString());
    }

//    @Resource
//    private Ollama ollama;

//    @Test
//    public void test2(){
//        System.out.println(ollama.generateStreamRag("deepseek-r1:1.5b", "日常总结", "总结"));
//
//    }

}
