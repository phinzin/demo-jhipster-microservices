package com.mycompany.myapp.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

import com.mycompany.myapp.IntegrationTest;
import com.mycompany.myapp.domain.Todo;
import com.mycompany.myapp.repository.TodoRepository;
import com.mycompany.myapp.repository.search.TodoSearchRepository;
import com.mycompany.myapp.service.EntityManager;
import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Integration tests for the {@link TodoResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureWebTestClient
@WithMockUser
class TodoResourceIT {

    private static final String DEFAULT_TASK = "AAAAAAAAAA";
    private static final String UPDATED_TASK = "BBBBBBBBBB";

    private static final String DEFAULT_DESCRIPTION = "AAAAAAAAAA";
    private static final String UPDATED_DESCRIPTION = "BBBBBBBBBB";

    private static final Boolean DEFAULT_COMPLETED = false;
    private static final Boolean UPDATED_COMPLETED = true;

    private static final String ENTITY_API_URL = "/api/todos";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";
    private static final String ENTITY_SEARCH_API_URL = "/api/_search/todos";

    private static Random random = new Random();
    private static AtomicLong count = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private TodoRepository todoRepository;

    /**
     * This repository is mocked in the com.mycompany.myapp.repository.search test package.
     *
     * @see com.mycompany.myapp.repository.search.TodoSearchRepositoryMockConfiguration
     */
    @Autowired
    private TodoSearchRepository mockTodoSearchRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private WebTestClient webTestClient;

    private Todo todo;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Todo createEntity(EntityManager em) {
        Todo todo = new Todo().task(DEFAULT_TASK).description(DEFAULT_DESCRIPTION).completed(DEFAULT_COMPLETED);
        return todo;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Todo createUpdatedEntity(EntityManager em) {
        Todo todo = new Todo().task(UPDATED_TASK).description(UPDATED_DESCRIPTION).completed(UPDATED_COMPLETED);
        return todo;
    }

    public static void deleteEntities(EntityManager em) {
        try {
            em.deleteAll(Todo.class).block();
        } catch (Exception e) {
            // It can fail, if other entities are still referring this - it will be removed later.
        }
    }

    @AfterEach
    public void cleanup() {
        deleteEntities(em);
    }

    @BeforeEach
    public void setupCsrf() {
        webTestClient = webTestClient.mutateWith(csrf());
    }

    @BeforeEach
    public void initTest() {
        deleteEntities(em);
        todo = createEntity(em);
    }

    @Test
    void createTodo() throws Exception {
        int databaseSizeBeforeCreate = todoRepository.findAll().collectList().block().size();
        // Configure the mock search repository
        when(mockTodoSearchRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        // Create the Todo
        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(TestUtil.convertObjectToJsonBytes(todo))
            .exchange()
            .expectStatus()
            .isCreated();

        // Validate the Todo in the database
        List<Todo> todoList = todoRepository.findAll().collectList().block();
        assertThat(todoList).hasSize(databaseSizeBeforeCreate + 1);
        Todo testTodo = todoList.get(todoList.size() - 1);
        assertThat(testTodo.getTask()).isEqualTo(DEFAULT_TASK);
        assertThat(testTodo.getDescription()).isEqualTo(DEFAULT_DESCRIPTION);
        assertThat(testTodo.getCompleted()).isEqualTo(DEFAULT_COMPLETED);

        // Validate the Todo in Elasticsearch
        verify(mockTodoSearchRepository, times(1)).save(testTodo);
    }

    @Test
    void createTodoWithExistingId() throws Exception {
        // Create the Todo with an existing ID
        todo.setId(1L);

        int databaseSizeBeforeCreate = todoRepository.findAll().collectList().block().size();

        // An entity with an existing ID cannot be created, so this API call must fail
        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(TestUtil.convertObjectToJsonBytes(todo))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Todo in the database
        List<Todo> todoList = todoRepository.findAll().collectList().block();
        assertThat(todoList).hasSize(databaseSizeBeforeCreate);

        // Validate the Todo in Elasticsearch
        verify(mockTodoSearchRepository, times(0)).save(todo);
    }

    @Test
    void getAllTodosAsStream() {
        // Initialize the database
        todoRepository.save(todo).block();

        List<Todo> todoList = webTestClient
            .get()
            .uri(ENTITY_API_URL)
            .accept(MediaType.APPLICATION_NDJSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.APPLICATION_NDJSON)
            .returnResult(Todo.class)
            .getResponseBody()
            .filter(todo::equals)
            .collectList()
            .block(Duration.ofSeconds(5));

        assertThat(todoList).isNotNull();
        assertThat(todoList).hasSize(1);
        Todo testTodo = todoList.get(0);
        assertThat(testTodo.getTask()).isEqualTo(DEFAULT_TASK);
        assertThat(testTodo.getDescription()).isEqualTo(DEFAULT_DESCRIPTION);
        assertThat(testTodo.getCompleted()).isEqualTo(DEFAULT_COMPLETED);
    }

    @Test
    void getAllTodos() {
        // Initialize the database
        todoRepository.save(todo).block();

        // Get all the todoList
        webTestClient
            .get()
            .uri(ENTITY_API_URL + "?sort=id,desc")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.[*].id")
            .value(hasItem(todo.getId().intValue()))
            .jsonPath("$.[*].task")
            .value(hasItem(DEFAULT_TASK))
            .jsonPath("$.[*].description")
            .value(hasItem(DEFAULT_DESCRIPTION))
            .jsonPath("$.[*].completed")
            .value(hasItem(DEFAULT_COMPLETED.booleanValue()));
    }

    @Test
    void getTodo() {
        // Initialize the database
        todoRepository.save(todo).block();

        // Get the todo
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, todo.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.id")
            .value(is(todo.getId().intValue()))
            .jsonPath("$.task")
            .value(is(DEFAULT_TASK))
            .jsonPath("$.description")
            .value(is(DEFAULT_DESCRIPTION))
            .jsonPath("$.completed")
            .value(is(DEFAULT_COMPLETED.booleanValue()));
    }

    @Test
    void getNonExistingTodo() {
        // Get the todo
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, Long.MAX_VALUE)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNotFound();
    }

    @Test
    void putNewTodo() throws Exception {
        // Configure the mock search repository
        when(mockTodoSearchRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        // Initialize the database
        todoRepository.save(todo).block();

        int databaseSizeBeforeUpdate = todoRepository.findAll().collectList().block().size();

        // Update the todo
        Todo updatedTodo = todoRepository.findById(todo.getId()).block();
        updatedTodo.task(UPDATED_TASK).description(UPDATED_DESCRIPTION).completed(UPDATED_COMPLETED);

        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, updatedTodo.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(TestUtil.convertObjectToJsonBytes(updatedTodo))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Todo in the database
        List<Todo> todoList = todoRepository.findAll().collectList().block();
        assertThat(todoList).hasSize(databaseSizeBeforeUpdate);
        Todo testTodo = todoList.get(todoList.size() - 1);
        assertThat(testTodo.getTask()).isEqualTo(UPDATED_TASK);
        assertThat(testTodo.getDescription()).isEqualTo(UPDATED_DESCRIPTION);
        assertThat(testTodo.getCompleted()).isEqualTo(UPDATED_COMPLETED);

        // Validate the Todo in Elasticsearch
        verify(mockTodoSearchRepository).save(testTodo);
    }

    @Test
    void putNonExistingTodo() throws Exception {
        int databaseSizeBeforeUpdate = todoRepository.findAll().collectList().block().size();
        todo.setId(count.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, todo.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(TestUtil.convertObjectToJsonBytes(todo))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Todo in the database
        List<Todo> todoList = todoRepository.findAll().collectList().block();
        assertThat(todoList).hasSize(databaseSizeBeforeUpdate);

        // Validate the Todo in Elasticsearch
        verify(mockTodoSearchRepository, times(0)).save(todo);
    }

    @Test
    void putWithIdMismatchTodo() throws Exception {
        int databaseSizeBeforeUpdate = todoRepository.findAll().collectList().block().size();
        todo.setId(count.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, count.incrementAndGet())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(TestUtil.convertObjectToJsonBytes(todo))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Todo in the database
        List<Todo> todoList = todoRepository.findAll().collectList().block();
        assertThat(todoList).hasSize(databaseSizeBeforeUpdate);

        // Validate the Todo in Elasticsearch
        verify(mockTodoSearchRepository, times(0)).save(todo);
    }

    @Test
    void putWithMissingIdPathParamTodo() throws Exception {
        int databaseSizeBeforeUpdate = todoRepository.findAll().collectList().block().size();
        todo.setId(count.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(TestUtil.convertObjectToJsonBytes(todo))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the Todo in the database
        List<Todo> todoList = todoRepository.findAll().collectList().block();
        assertThat(todoList).hasSize(databaseSizeBeforeUpdate);

        // Validate the Todo in Elasticsearch
        verify(mockTodoSearchRepository, times(0)).save(todo);
    }

    @Test
    void partialUpdateTodoWithPatch() throws Exception {
        // Initialize the database
        todoRepository.save(todo).block();

        int databaseSizeBeforeUpdate = todoRepository.findAll().collectList().block().size();

        // Update the todo using partial update
        Todo partialUpdatedTodo = new Todo();
        partialUpdatedTodo.setId(todo.getId());

        partialUpdatedTodo.task(UPDATED_TASK).description(UPDATED_DESCRIPTION);

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedTodo.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(TestUtil.convertObjectToJsonBytes(partialUpdatedTodo))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Todo in the database
        List<Todo> todoList = todoRepository.findAll().collectList().block();
        assertThat(todoList).hasSize(databaseSizeBeforeUpdate);
        Todo testTodo = todoList.get(todoList.size() - 1);
        assertThat(testTodo.getTask()).isEqualTo(UPDATED_TASK);
        assertThat(testTodo.getDescription()).isEqualTo(UPDATED_DESCRIPTION);
        assertThat(testTodo.getCompleted()).isEqualTo(DEFAULT_COMPLETED);
    }

    @Test
    void fullUpdateTodoWithPatch() throws Exception {
        // Initialize the database
        todoRepository.save(todo).block();

        int databaseSizeBeforeUpdate = todoRepository.findAll().collectList().block().size();

        // Update the todo using partial update
        Todo partialUpdatedTodo = new Todo();
        partialUpdatedTodo.setId(todo.getId());

        partialUpdatedTodo.task(UPDATED_TASK).description(UPDATED_DESCRIPTION).completed(UPDATED_COMPLETED);

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedTodo.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(TestUtil.convertObjectToJsonBytes(partialUpdatedTodo))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Todo in the database
        List<Todo> todoList = todoRepository.findAll().collectList().block();
        assertThat(todoList).hasSize(databaseSizeBeforeUpdate);
        Todo testTodo = todoList.get(todoList.size() - 1);
        assertThat(testTodo.getTask()).isEqualTo(UPDATED_TASK);
        assertThat(testTodo.getDescription()).isEqualTo(UPDATED_DESCRIPTION);
        assertThat(testTodo.getCompleted()).isEqualTo(UPDATED_COMPLETED);
    }

    @Test
    void patchNonExistingTodo() throws Exception {
        int databaseSizeBeforeUpdate = todoRepository.findAll().collectList().block().size();
        todo.setId(count.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, todo.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(TestUtil.convertObjectToJsonBytes(todo))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Todo in the database
        List<Todo> todoList = todoRepository.findAll().collectList().block();
        assertThat(todoList).hasSize(databaseSizeBeforeUpdate);

        // Validate the Todo in Elasticsearch
        verify(mockTodoSearchRepository, times(0)).save(todo);
    }

    @Test
    void patchWithIdMismatchTodo() throws Exception {
        int databaseSizeBeforeUpdate = todoRepository.findAll().collectList().block().size();
        todo.setId(count.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, count.incrementAndGet())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(TestUtil.convertObjectToJsonBytes(todo))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Todo in the database
        List<Todo> todoList = todoRepository.findAll().collectList().block();
        assertThat(todoList).hasSize(databaseSizeBeforeUpdate);

        // Validate the Todo in Elasticsearch
        verify(mockTodoSearchRepository, times(0)).save(todo);
    }

    @Test
    void patchWithMissingIdPathParamTodo() throws Exception {
        int databaseSizeBeforeUpdate = todoRepository.findAll().collectList().block().size();
        todo.setId(count.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(TestUtil.convertObjectToJsonBytes(todo))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the Todo in the database
        List<Todo> todoList = todoRepository.findAll().collectList().block();
        assertThat(todoList).hasSize(databaseSizeBeforeUpdate);

        // Validate the Todo in Elasticsearch
        verify(mockTodoSearchRepository, times(0)).save(todo);
    }

    @Test
    void deleteTodo() {
        // Configure the mock search repository
        when(mockTodoSearchRepository.deleteById(anyLong())).thenReturn(Mono.empty());
        // Initialize the database
        todoRepository.save(todo).block();

        int databaseSizeBeforeDelete = todoRepository.findAll().collectList().block().size();

        // Delete the todo
        webTestClient
            .delete()
            .uri(ENTITY_API_URL_ID, todo.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent();

        // Validate the database contains one less item
        List<Todo> todoList = todoRepository.findAll().collectList().block();
        assertThat(todoList).hasSize(databaseSizeBeforeDelete - 1);

        // Validate the Todo in Elasticsearch
        verify(mockTodoSearchRepository, times(1)).deleteById(todo.getId());
    }

    @Test
    void searchTodo() {
        // Configure the mock search repository
        when(mockTodoSearchRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        // Initialize the database
        todoRepository.save(todo).block();
        when(mockTodoSearchRepository.search("id:" + todo.getId())).thenReturn(Flux.just(todo));

        // Search the todo
        webTestClient
            .get()
            .uri(ENTITY_SEARCH_API_URL + "?query=id:" + todo.getId())
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.[*].id")
            .value(hasItem(todo.getId().intValue()))
            .jsonPath("$.[*].task")
            .value(hasItem(DEFAULT_TASK))
            .jsonPath("$.[*].description")
            .value(hasItem(DEFAULT_DESCRIPTION))
            .jsonPath("$.[*].completed")
            .value(hasItem(DEFAULT_COMPLETED.booleanValue()));
    }
}
