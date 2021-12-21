package com.mycompany.myapp.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

import com.mycompany.myapp.IntegrationTest;
import com.mycompany.myapp.domain.Category;
import com.mycompany.myapp.repository.CategoryRepository;
import com.mycompany.myapp.repository.search.CategorySearchRepository;
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
 * Integration tests for the {@link CategoryResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureWebTestClient
@WithMockUser
class CategoryResourceIT {

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    private static final String DEFAULT_DESCRIPTION = "AAAAAAAAAA";
    private static final String UPDATED_DESCRIPTION = "BBBBBBBBBB";

    private static final Boolean DEFAULT_ACTIVE = false;
    private static final Boolean UPDATED_ACTIVE = true;

    private static final String ENTITY_API_URL = "/api/categories";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";
    private static final String ENTITY_SEARCH_API_URL = "/api/_search/categories";

    private static Random random = new Random();
    private static AtomicLong count = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private CategoryRepository categoryRepository;

    /**
     * This repository is mocked in the com.mycompany.myapp.repository.search test package.
     *
     * @see com.mycompany.myapp.repository.search.CategorySearchRepositoryMockConfiguration
     */
    @Autowired
    private CategorySearchRepository mockCategorySearchRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private WebTestClient webTestClient;

    private Category category;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Category createEntity(EntityManager em) {
        Category category = new Category().name(DEFAULT_NAME).description(DEFAULT_DESCRIPTION).active(DEFAULT_ACTIVE);
        return category;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Category createUpdatedEntity(EntityManager em) {
        Category category = new Category().name(UPDATED_NAME).description(UPDATED_DESCRIPTION).active(UPDATED_ACTIVE);
        return category;
    }

    public static void deleteEntities(EntityManager em) {
        try {
            em.deleteAll(Category.class).block();
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
        category = createEntity(em);
    }

    @Test
    void createCategory() throws Exception {
        int databaseSizeBeforeCreate = categoryRepository.findAll().collectList().block().size();
        // Configure the mock search repository
        when(mockCategorySearchRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        // Create the Category
        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(TestUtil.convertObjectToJsonBytes(category))
            .exchange()
            .expectStatus()
            .isCreated();

        // Validate the Category in the database
        List<Category> categoryList = categoryRepository.findAll().collectList().block();
        assertThat(categoryList).hasSize(databaseSizeBeforeCreate + 1);
        Category testCategory = categoryList.get(categoryList.size() - 1);
        assertThat(testCategory.getName()).isEqualTo(DEFAULT_NAME);
        assertThat(testCategory.getDescription()).isEqualTo(DEFAULT_DESCRIPTION);
        assertThat(testCategory.getActive()).isEqualTo(DEFAULT_ACTIVE);

        // Validate the Category in Elasticsearch
        verify(mockCategorySearchRepository, times(1)).save(testCategory);
    }

    @Test
    void createCategoryWithExistingId() throws Exception {
        // Create the Category with an existing ID
        category.setId(1L);

        int databaseSizeBeforeCreate = categoryRepository.findAll().collectList().block().size();

        // An entity with an existing ID cannot be created, so this API call must fail
        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(TestUtil.convertObjectToJsonBytes(category))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Category in the database
        List<Category> categoryList = categoryRepository.findAll().collectList().block();
        assertThat(categoryList).hasSize(databaseSizeBeforeCreate);

        // Validate the Category in Elasticsearch
        verify(mockCategorySearchRepository, times(0)).save(category);
    }

    @Test
    void getAllCategoriesAsStream() {
        // Initialize the database
        categoryRepository.save(category).block();

        List<Category> categoryList = webTestClient
            .get()
            .uri(ENTITY_API_URL)
            .accept(MediaType.APPLICATION_NDJSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.APPLICATION_NDJSON)
            .returnResult(Category.class)
            .getResponseBody()
            .filter(category::equals)
            .collectList()
            .block(Duration.ofSeconds(5));

        assertThat(categoryList).isNotNull();
        assertThat(categoryList).hasSize(1);
        Category testCategory = categoryList.get(0);
        assertThat(testCategory.getName()).isEqualTo(DEFAULT_NAME);
        assertThat(testCategory.getDescription()).isEqualTo(DEFAULT_DESCRIPTION);
        assertThat(testCategory.getActive()).isEqualTo(DEFAULT_ACTIVE);
    }

    @Test
    void getAllCategories() {
        // Initialize the database
        categoryRepository.save(category).block();

        // Get all the categoryList
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
            .value(hasItem(category.getId().intValue()))
            .jsonPath("$.[*].name")
            .value(hasItem(DEFAULT_NAME))
            .jsonPath("$.[*].description")
            .value(hasItem(DEFAULT_DESCRIPTION))
            .jsonPath("$.[*].active")
            .value(hasItem(DEFAULT_ACTIVE.booleanValue()));
    }

    @Test
    void getCategory() {
        // Initialize the database
        categoryRepository.save(category).block();

        // Get the category
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, category.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.id")
            .value(is(category.getId().intValue()))
            .jsonPath("$.name")
            .value(is(DEFAULT_NAME))
            .jsonPath("$.description")
            .value(is(DEFAULT_DESCRIPTION))
            .jsonPath("$.active")
            .value(is(DEFAULT_ACTIVE.booleanValue()));
    }

    @Test
    void getNonExistingCategory() {
        // Get the category
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, Long.MAX_VALUE)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNotFound();
    }

    @Test
    void putNewCategory() throws Exception {
        // Configure the mock search repository
        when(mockCategorySearchRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        // Initialize the database
        categoryRepository.save(category).block();

        int databaseSizeBeforeUpdate = categoryRepository.findAll().collectList().block().size();

        // Update the category
        Category updatedCategory = categoryRepository.findById(category.getId()).block();
        updatedCategory.name(UPDATED_NAME).description(UPDATED_DESCRIPTION).active(UPDATED_ACTIVE);

        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, updatedCategory.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(TestUtil.convertObjectToJsonBytes(updatedCategory))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Category in the database
        List<Category> categoryList = categoryRepository.findAll().collectList().block();
        assertThat(categoryList).hasSize(databaseSizeBeforeUpdate);
        Category testCategory = categoryList.get(categoryList.size() - 1);
        assertThat(testCategory.getName()).isEqualTo(UPDATED_NAME);
        assertThat(testCategory.getDescription()).isEqualTo(UPDATED_DESCRIPTION);
        assertThat(testCategory.getActive()).isEqualTo(UPDATED_ACTIVE);

        // Validate the Category in Elasticsearch
        verify(mockCategorySearchRepository).save(testCategory);
    }

    @Test
    void putNonExistingCategory() throws Exception {
        int databaseSizeBeforeUpdate = categoryRepository.findAll().collectList().block().size();
        category.setId(count.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, category.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(TestUtil.convertObjectToJsonBytes(category))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Category in the database
        List<Category> categoryList = categoryRepository.findAll().collectList().block();
        assertThat(categoryList).hasSize(databaseSizeBeforeUpdate);

        // Validate the Category in Elasticsearch
        verify(mockCategorySearchRepository, times(0)).save(category);
    }

    @Test
    void putWithIdMismatchCategory() throws Exception {
        int databaseSizeBeforeUpdate = categoryRepository.findAll().collectList().block().size();
        category.setId(count.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, count.incrementAndGet())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(TestUtil.convertObjectToJsonBytes(category))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Category in the database
        List<Category> categoryList = categoryRepository.findAll().collectList().block();
        assertThat(categoryList).hasSize(databaseSizeBeforeUpdate);

        // Validate the Category in Elasticsearch
        verify(mockCategorySearchRepository, times(0)).save(category);
    }

    @Test
    void putWithMissingIdPathParamCategory() throws Exception {
        int databaseSizeBeforeUpdate = categoryRepository.findAll().collectList().block().size();
        category.setId(count.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(TestUtil.convertObjectToJsonBytes(category))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the Category in the database
        List<Category> categoryList = categoryRepository.findAll().collectList().block();
        assertThat(categoryList).hasSize(databaseSizeBeforeUpdate);

        // Validate the Category in Elasticsearch
        verify(mockCategorySearchRepository, times(0)).save(category);
    }

    @Test
    void partialUpdateCategoryWithPatch() throws Exception {
        // Initialize the database
        categoryRepository.save(category).block();

        int databaseSizeBeforeUpdate = categoryRepository.findAll().collectList().block().size();

        // Update the category using partial update
        Category partialUpdatedCategory = new Category();
        partialUpdatedCategory.setId(category.getId());

        partialUpdatedCategory.name(UPDATED_NAME);

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedCategory.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(TestUtil.convertObjectToJsonBytes(partialUpdatedCategory))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Category in the database
        List<Category> categoryList = categoryRepository.findAll().collectList().block();
        assertThat(categoryList).hasSize(databaseSizeBeforeUpdate);
        Category testCategory = categoryList.get(categoryList.size() - 1);
        assertThat(testCategory.getName()).isEqualTo(UPDATED_NAME);
        assertThat(testCategory.getDescription()).isEqualTo(DEFAULT_DESCRIPTION);
        assertThat(testCategory.getActive()).isEqualTo(DEFAULT_ACTIVE);
    }

    @Test
    void fullUpdateCategoryWithPatch() throws Exception {
        // Initialize the database
        categoryRepository.save(category).block();

        int databaseSizeBeforeUpdate = categoryRepository.findAll().collectList().block().size();

        // Update the category using partial update
        Category partialUpdatedCategory = new Category();
        partialUpdatedCategory.setId(category.getId());

        partialUpdatedCategory.name(UPDATED_NAME).description(UPDATED_DESCRIPTION).active(UPDATED_ACTIVE);

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedCategory.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(TestUtil.convertObjectToJsonBytes(partialUpdatedCategory))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Category in the database
        List<Category> categoryList = categoryRepository.findAll().collectList().block();
        assertThat(categoryList).hasSize(databaseSizeBeforeUpdate);
        Category testCategory = categoryList.get(categoryList.size() - 1);
        assertThat(testCategory.getName()).isEqualTo(UPDATED_NAME);
        assertThat(testCategory.getDescription()).isEqualTo(UPDATED_DESCRIPTION);
        assertThat(testCategory.getActive()).isEqualTo(UPDATED_ACTIVE);
    }

    @Test
    void patchNonExistingCategory() throws Exception {
        int databaseSizeBeforeUpdate = categoryRepository.findAll().collectList().block().size();
        category.setId(count.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, category.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(TestUtil.convertObjectToJsonBytes(category))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Category in the database
        List<Category> categoryList = categoryRepository.findAll().collectList().block();
        assertThat(categoryList).hasSize(databaseSizeBeforeUpdate);

        // Validate the Category in Elasticsearch
        verify(mockCategorySearchRepository, times(0)).save(category);
    }

    @Test
    void patchWithIdMismatchCategory() throws Exception {
        int databaseSizeBeforeUpdate = categoryRepository.findAll().collectList().block().size();
        category.setId(count.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, count.incrementAndGet())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(TestUtil.convertObjectToJsonBytes(category))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Category in the database
        List<Category> categoryList = categoryRepository.findAll().collectList().block();
        assertThat(categoryList).hasSize(databaseSizeBeforeUpdate);

        // Validate the Category in Elasticsearch
        verify(mockCategorySearchRepository, times(0)).save(category);
    }

    @Test
    void patchWithMissingIdPathParamCategory() throws Exception {
        int databaseSizeBeforeUpdate = categoryRepository.findAll().collectList().block().size();
        category.setId(count.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(TestUtil.convertObjectToJsonBytes(category))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the Category in the database
        List<Category> categoryList = categoryRepository.findAll().collectList().block();
        assertThat(categoryList).hasSize(databaseSizeBeforeUpdate);

        // Validate the Category in Elasticsearch
        verify(mockCategorySearchRepository, times(0)).save(category);
    }

    @Test
    void deleteCategory() {
        // Configure the mock search repository
        when(mockCategorySearchRepository.deleteById(anyLong())).thenReturn(Mono.empty());
        // Initialize the database
        categoryRepository.save(category).block();

        int databaseSizeBeforeDelete = categoryRepository.findAll().collectList().block().size();

        // Delete the category
        webTestClient
            .delete()
            .uri(ENTITY_API_URL_ID, category.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent();

        // Validate the database contains one less item
        List<Category> categoryList = categoryRepository.findAll().collectList().block();
        assertThat(categoryList).hasSize(databaseSizeBeforeDelete - 1);

        // Validate the Category in Elasticsearch
        verify(mockCategorySearchRepository, times(1)).deleteById(category.getId());
    }

    @Test
    void searchCategory() {
        // Configure the mock search repository
        when(mockCategorySearchRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        // Initialize the database
        categoryRepository.save(category).block();
        when(mockCategorySearchRepository.search("id:" + category.getId())).thenReturn(Flux.just(category));

        // Search the category
        webTestClient
            .get()
            .uri(ENTITY_SEARCH_API_URL + "?query=id:" + category.getId())
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.[*].id")
            .value(hasItem(category.getId().intValue()))
            .jsonPath("$.[*].name")
            .value(hasItem(DEFAULT_NAME))
            .jsonPath("$.[*].description")
            .value(hasItem(DEFAULT_DESCRIPTION))
            .jsonPath("$.[*].active")
            .value(hasItem(DEFAULT_ACTIVE.booleanValue()));
    }
}
