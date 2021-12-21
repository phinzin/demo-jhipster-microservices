package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.Category;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Spring Data SQL reactive repository for the Category entity.
 */
@SuppressWarnings("unused")
@Repository
public interface CategoryRepository extends R2dbcRepository<Category, Long>, CategoryRepositoryInternal {
    // just to avoid having unambigous methods
    @Override
    Flux<Category> findAll();

    @Override
    Mono<Category> findById(Long id);

    @Override
    <S extends Category> Mono<S> save(S entity);
}

interface CategoryRepositoryInternal {
    <S extends Category> Mono<S> insert(S entity);
    <S extends Category> Mono<S> save(S entity);
    Mono<Integer> update(Category entity);

    Flux<Category> findAll();
    Mono<Category> findById(Long id);
    Flux<Category> findAllBy(Pageable pageable);
    Flux<Category> findAllBy(Pageable pageable, Criteria criteria);
}
