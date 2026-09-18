package com.sentinel.persistence;

import java.util.List;

/**
 * Generic data-access interface. Each entity in the application gets its
 * own repository implementation, keeping SQL isolated from the domain and
 * CLI layers entirely.
 *
 * @param <T> the domain type this repository persists
 */
public interface Repository<T> {

    void save(T item);

    List<T> findAll();
}
