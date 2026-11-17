package com.example.firstproject.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "example")
@Getter
@NoArgsConstructor
public class Example {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    public Example(String name) {
        this.name = name;
    }

    public void changeName(String name) {
        this.name = name;
    }
}
