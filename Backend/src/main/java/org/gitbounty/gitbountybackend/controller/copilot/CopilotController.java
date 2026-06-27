package org.gitbounty.gitbountybackend.controller.copilot;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/copilot")
@CrossOrigin(origins = "*")
public class CopilotController {

    private final ChatClient chatClient;

    @Autowired
    public CopilotController(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem("You are Jemala, the helpful GitBounty AI copilot. " +
                        "Your goal is to help developers navigate their GitHub issues and bounties. " +
                        "Be concise and professional.")
                .build();
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        try {
            String response = chatClient.prompt().user(request.message()).call().content();

            return ResponseEntity.ok(new ChatResponse(response));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ChatResponse("Sorry, Jemala is currently offline. Please try again later."));
        }
    }

    public record ChatRequest(String message) {}
    public record ChatResponse(String response) {}
}