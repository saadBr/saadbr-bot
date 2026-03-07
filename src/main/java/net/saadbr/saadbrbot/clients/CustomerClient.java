package net.saadbr.saadbrbot.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * @author saade
 **/
@FeignClient(name = "CUSTOMER-SERVICE")
public interface CustomerClient {

    @GetMapping("/customers/{id}")
    Map<String, Object> getCustomer(@PathVariable Long id);

    @GetMapping("/customers")
    Map<String, Object> listCustomers();
}
