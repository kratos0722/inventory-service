package com.kratos0722.inventory.web;

import com.kratos0722.inventory.domain.Product;
import com.kratos0722.inventory.service.InventoryService;
import com.kratos0722.inventory.web.dto.CreateProductRequest;
import com.kratos0722.inventory.web.dto.PagedResponse;
import com.kratos0722.inventory.web.dto.ProductResponse;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final InventoryService service;

    public ProductController(InventoryService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest request) {
        Product product = service.createProduct(
                request.sku(),
                request.name(),
                request.description(),
                request.unitPrice(),
                request.quantityAvailable());
        return ResponseEntity
                .created(URI.create("/api/v1/products/" + product.getSku()))
                .body(ProductResponse.from(product));
    }

    @GetMapping("/{sku}")
    public ProductResponse get(@PathVariable String sku) {
        return ProductResponse.from(service.getProduct(sku));
    }

    @GetMapping
    public PagedResponse<ProductResponse> list(@PageableDefault(size = 20) Pageable pageable) {
        return PagedResponse.from(service.listProducts(pageable).map(ProductResponse::from));
    }
}
