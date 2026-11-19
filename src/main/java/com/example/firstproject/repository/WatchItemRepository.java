package com.example.firstproject.repository;

import com.example.firstproject.entity.WatchItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WatchItemRepository extends JpaRepository<WatchItem, Long> {

    List<WatchItem> findAllByOrderByIdAsc();

    Optional<WatchItem> findByCode(String code);
}
