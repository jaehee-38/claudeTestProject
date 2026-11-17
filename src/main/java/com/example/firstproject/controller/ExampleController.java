package com.example.firstproject.controller;

import com.example.firstproject.dto.ExampleDto;
import com.example.firstproject.service.ExampleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/examples")
@RequiredArgsConstructor
public class ExampleController {

    private final ExampleService exampleService;

    // 전체 조회: GET /api/examples
    @GetMapping
    public List<ExampleDto> findAll() {
        return exampleService.findAll();
    }

    // 단건 조회: GET /api/examples/{id}
    @GetMapping("/{id}")
    public ExampleDto findById(@PathVariable Long id) {
        return exampleService.findById(id);
    }

    // 생성: POST /api/examples  {"name": "..."}
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExampleDto create(@RequestBody ExampleDto request) {
        return exampleService.create(request);
    }

    // 수정: PUT /api/examples/{id}  {"name": "..."}
    @PutMapping("/{id}")
    public ExampleDto update(@PathVariable Long id, @RequestBody ExampleDto request) {
        return exampleService.update(id, request);
    }

    // 삭제: DELETE /api/examples/{id}
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        exampleService.delete(id);
    }
}
