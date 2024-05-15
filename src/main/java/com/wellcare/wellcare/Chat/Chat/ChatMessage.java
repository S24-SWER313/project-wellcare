package com.wellcare.wellcare.Chat.Chat;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ChatMessage {
    
    private MessageType type;
    private String content;
    private String sender;
}
