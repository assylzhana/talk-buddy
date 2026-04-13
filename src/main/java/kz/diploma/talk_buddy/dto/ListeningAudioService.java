package kz.diploma.talk_buddy.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListeningAudioService {

    private final OpenAiClientService openAiClientService;

    public String generateAudioForScript(String script, String fileName) {
        return "/audio/" + fileName + ".mp3";
    }
}