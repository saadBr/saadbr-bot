package net.saadbr.saadbrbot.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "INVENTORY-SERVICE")
public interface InventoryClient {

    @GetMapping("/products/{id}")
    Map<String, Object> getProduct(@PathVariable Long id);

    @GetMapping("/products")
    Map<String, Object> listProducts();
}