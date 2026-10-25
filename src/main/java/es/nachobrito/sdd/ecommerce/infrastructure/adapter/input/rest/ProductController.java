package es.nachobrito.sdd.ecommerce.infrastructure.adapter.input.rest;

import es.nachobrito.sdd.ecommerce.domain.exception.ProductAlreadyExistsException;
import es.nachobrito.sdd.ecommerce.domain.model.Product;
import es.nachobrito.sdd.ecommerce.domain.port.inbound.CreateProductUseCase;
import es.nachobrito.sdd.ecommerce.domain.port.inbound.GetProductUseCase;
import es.nachobrito.sdd.ecommerce.infrastructure.adapter.input.rest.dto.CreateProductRequest;
import es.nachobrito.sdd.ecommerce.infrastructure.adapter.input.rest.dto.ProductResponse;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Error;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Status;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * REST controller implementing catalog Product endpoints per OpenAPI contract.
 */
@Controller("/v1/products")
@ExecuteOn(TaskExecutors.BLOCKING)
public class ProductController {

    private final CreateProductUseCase createProductUseCase;
    private final GetProductUseCase getProductUseCase;

    /**
     * Constructs the controller with explicit constructor injection.
     *
     * @param createProductUseCase Inbound port for creating products
     * @param getProductUseCase Inbound port for retrieving products
     */
    public ProductController(
            final CreateProductUseCase createProductUseCase,
            final GetProductUseCase getProductUseCase
    ) {
        this.createProductUseCase = Objects.requireNonNull(createProductUseCase, "createProductUseCase must not be null");
        this.getProductUseCase = Objects.requireNonNull(getProductUseCase, "getProductUseCase must not be null");
    }

    /**
     * Creates a new product.
     *
     * @param request Validated create product request body
     * @return HTTP 201 Created with ProductResponse
     */
    @Post
    @Status(HttpStatus.CREATED)
    public HttpResponse<ProductResponse> createProduct(@Body @Valid final CreateProductRequest request) {
        final Product product = createProductUseCase.createProduct(
                request.id(),
                request.sku(),
                request.name(),
                request.description(),
                request.priceInCents(),
                request.currency()
        );
        return HttpResponse.created(ProductResponse.fromDomain(product));
    }

    /**
     * Lists all products.
     *
     * @return List of ProductResponse DTOs
     */
    @Get
    public List<ProductResponse> listProducts() {
        return getProductUseCase.listAll().stream()
                .map(ProductResponse::fromDomain)
                .toList();
    }

    /**
     * Retrieves a single product by UUID.
     *
     * @param id Product unique identifier
     * @return HTTP 200 with ProductResponse or HTTP 404 Not Found
     */
    @Get("/{id}")
    public HttpResponse<ProductResponse> getProductById(@PathVariable final UUID id) {
        return getProductUseCase.getById(id)
                .map(ProductResponse::fromDomain)
                .map(HttpResponse::ok)
                .orElseGet(HttpResponse::notFound);
    }

    /**
     * Handles duplicate product conflict when a client attempts to create a product with an existing ID.
     *
     * @param ex Conflict exception thrown from domain/service layer
     * @return HTTP 409 Conflict response
     */
    @Error(exception = ProductAlreadyExistsException.class)
    public HttpResponse<Map<String, Object>> handleProductAlreadyExists(final ProductAlreadyExistsException ex) {
        return HttpResponse.status(HttpStatus.CONFLICT).body(Map.of(
                "message", ex.getMessage(),
                "status", HttpStatus.CONFLICT.getCode()
        ));
    }
}
