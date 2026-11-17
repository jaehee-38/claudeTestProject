package com.example.firstproject.dto;

import com.example.firstproject.entity.Example;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ExampleDto {

    private Long id;
    private String name;

    public static ExampleDto from(Example example) {
        return new ExampleDto(example.getId(), example.getName());
    }
}
