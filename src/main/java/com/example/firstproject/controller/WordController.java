package com.example.firstproject.controller;

import com.example.firstproject.dto.LevelSummaryDto;
import com.example.firstproject.dto.WordDto;
import com.example.firstproject.service.WordService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class WordController {

    private final WordService wordService;

    @GetMapping("/levels")
    public List<LevelSummaryDto> getLevels() {
        return wordService.getLevelSummaries();
    }

    @GetMapping("/words")
    public List<WordDto> getWords(@RequestParam int level) {
        return wordService.getWordsByLevel(level);
    }
}
