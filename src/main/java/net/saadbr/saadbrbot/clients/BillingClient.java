package net.saadbr.saadbrbot.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "BILLING-SERVICE")
public interface BillingClient {

    @GetMapping("/api/bills/{id}")
    Map<String, Object> getBill(@PathVariable Long id);
}