package net.saadbr.saadbrbot.debug;

import net.saadbr.saadbrbot.clients.CustomerClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * @author saade
 **/
@RestController
@RequestMapping("/debug")
public class DebugController {

    private final CustomerClient customerClient;

    public DebugController(CustomerClient customerClient) {
        this.customerClient = customerClient;
    }

    @GetMapping("/customer/{id}")
    public Map<String, Object> getCustomer(@PathVariable long id) {
        return customerClient.getCustomer(id);
    }
}
