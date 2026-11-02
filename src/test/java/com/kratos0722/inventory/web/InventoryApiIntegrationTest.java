package com.kratos0722.inventory.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/** End-to-end tests of the REST API through the full Spring context. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InventoryApiIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(String body) {
        return json.readValue(body, Map.class);
    }

    private int intField(Map<String, Object> map, String key) {
        return ((Number) map.get(key)).intValue();
    }

    private void createProduct(String sku, int qty) throws Exception {
        String body = """
            {"sku":"%s","name":"Widget","description":"d","unitPrice":9.99,"quantityAvailable":%d}
            """.formatted(sku, qty);
        mvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void createProduct_thenFetch_roundTrips() throws Exception {
        createProduct("API-1", 10);

        String res = mvc.perform(get("/api/v1/products/API-1"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Map<String, Object> node = asMap(res);
        assertEquals("API-1", node.get("sku"));
        assertEquals(10, intField(node, "quantityAvailable"));
    }

    @Test
    void reserve_returns201_andDecrementsAvailable() throws Exception {
        createProduct("API-2", 10);

        String body = """
            {"idempotencyKey":"api-key-1","sku":"API-2","quantity":4}
            """;
        String res = mvc.perform(post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        assertEquals("RESERVED", asMap(res).get("status"));

        String prod = mvc.perform(get("/api/v1/products/API-2"))
                .andReturn().getResponse().getContentAsString();
        assertEquals(6, intField(asMap(prod), "quantityAvailable"));
    }

    @Test
    void reserve_insufficientStock_returns409() throws Exception {
        createProduct("API-3", 2);

        String body = """
            {"idempotencyKey":"api-key-2","sku":"API-3","quantity":5}
            """;
        mvc.perform(post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void getUnknownProduct_returns404() throws Exception {
        mvc.perform(get("/api/v1/products/DOES-NOT-EXIST"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createProduct_invalidBody_returns400() throws Exception {
        String body = """
            {"sku":"","name":"","unitPrice":-1,"quantityAvailable":-5}
            """;
        mvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void duplicateSku_returns409() throws Exception {
        createProduct("API-4", 1);
        String body = """
            {"sku":"API-4","name":"Widget","unitPrice":1.00,"quantityAvailable":1}
            """;
        mvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }
}
