package com.example.firstproject.service;

import com.example.firstproject.dto.LevelSummaryDto;
import com.example.firstproject.dto.WordDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class WordService {

    private static final Map<Integer, String> LEVEL_LABELS = Map.of(
            1, "Level 1 · 기초",
            2, "Level 2 · 중급",
            3, "Level 3 · 고급"
    );

    private Map<Integer, List<WordDto>> wordsByLevel;

    @PostConstruct
    void loadWords() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream in = new ClassPathResource("data/words.json").getInputStream()) {
            List<WordDto> all = mapper.readValue(in, mapper.getTypeFactory()
                    .constructCollectionType(List.class, WordDto.class));
            wordsByLevel = new TreeMap<>(all.stream()
                    .collect(Collectors.groupingBy(WordDto::level)));
        }
    }

    public List<LevelSummaryDto> getLevelSummaries() {
        return wordsByLevel.entrySet().stream()
                .map(e -> new LevelSummaryDto(
                        e.getKey(),
                        LEVEL_LABELS.getOrDefault(e.getKey(), "Level " + e.getKey()),
                        e.getValue().size()))
                .toList();
    }

    public List<WordDto> getWordsByLevel(int level) {
        return wordsByLevel.getOrDefault(level, List.of());
    }
}
