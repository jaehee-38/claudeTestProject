package com.example.firstproject.service;

import com.example.firstproject.dto.ExampleDto;
import com.example.firstproject.entity.Example;
import com.example.firstproject.repository.ExampleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExampleService {

    private final ExampleRepository exampleRepository;

    public List<ExampleDto> findAll() {
        return exampleRepository.findAll().stream()
                .map(ExampleDto::from)
                .toList();
    }

    public ExampleDto findById(Long id) {
        return ExampleDto.from(getExample(id));
    }

    @Transactional
    public ExampleDto create(ExampleDto request) {
        String name = validateName(request);
        return ExampleDto.from(exampleRepository.save(new Example(name)));
    }

    @Transactional
    public ExampleDto update(Long id, ExampleDto request) {
        String name = validateName(request);
        Example example = getExample(id);
        example.changeName(name);
        return ExampleDto.from(example);
    }

    @Transactional
    public void delete(Long id) {
        exampleRepository.delete(getExample(id));
    }

    private Example getExample(Long id) {
        return exampleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Example not found: " + id));
    }

    private String validateName(ExampleDto request) {
        if (request == null || request.getName() == null || request.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name은 필수입니다.");
        }
        String name = request.getName().trim();
        if (name.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name은 100자 이하여야 합니다.");
        }
        return name;
    }
}
