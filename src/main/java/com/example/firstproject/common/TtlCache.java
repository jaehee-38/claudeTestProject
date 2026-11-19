package com.example.firstproject.common;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 외부 API 호출 횟수 제한을 아끼기 위한 아주 단순한 메모리 캐시.
 * 실패한 호출은 캐시하지 않습니다.
 */
public class TtlCache<K, V> {

    private record Entry<V>(V value, Instant expiresAt) {}

    private final Duration ttl;
    private final ConcurrentHashMap<K, Entry<V>> map = new ConcurrentHashMap<>();

    public TtlCache(Duration ttl) {
        this.ttl = ttl;
    }

    private final ConcurrentHashMap<K, Object> locks = new ConcurrentHashMap<>();

    /**
     * 같은 키를 동시에 요청하면 한 번만 불러오고 나머지는 그 결과를 기다립니다
     * (대시보드 카드들이 같은 시세를 동시에 요청해도 외부 API 는 1번만 호출).
     */
    public V get(K key, Supplier<V> loader) {
        V hit = fresh(key);
        if (hit != null) return hit;
        synchronized (locks.computeIfAbsent(key, k -> new Object())) {
            hit = fresh(key);
            if (hit != null) return hit;
            V value = loader.get();
            map.put(key, new Entry<>(value, Instant.now().plus(ttl)));
            return value;
        }
    }

    private V fresh(K key) {
        Entry<V> e = map.get(key);
        return e != null && Instant.now().isBefore(e.expiresAt()) ? e.value() : null;
    }
}
