package net.saadbr.saadbrbot.tools;

import net.saadbr.saadbrbot.clients.BillingClient;
import net.saadbr.saadbrbot.clients.CustomerClient;
import net.saadbr.saadbrbot.clients.InventoryClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author saade
 **/
@Component
public class EcomTools {

    private final CustomerClient customerClient;
    private final InventoryClient inventoryClient;
    private final BillingClient billingClient;

    public EcomTools(CustomerClient customerClient,
                     InventoryClient inventoryClient,
                     BillingClient billingClient) {
        this.customerClient = customerClient;
        this.inventoryClient = inventoryClient;
        this.billingClient = billingClient;
    }

    @Tool(description = "Get customer details by id from CUSTOMER-SERVICE")
    public Map<String, Object> getCustomerById(long id) {
        return customerClient.getCustomer(id);
    }

    @Tool(description = "List customers from CUSTOMER-SERVICE")
    public Map<String, Object> listCustomers() {
        return customerClient.listCustomers();
    }

    @Tool(description = "Get product details by id from INVENTORY-SERVICE")
    public Map<String, Object> getProductById(long id) {
        return inventoryClient.getProduct(id);
    }

    @Tool(description = "List products from INVENTORY-SERVICE")
    public Map<String, Object> listProducts() {
        return inventoryClient.listProducts();
    }

    @Tool(description = "Get bill details by id from BILLING-SERVICE")
    public Map<String, Object> getBillById(long id) {
        return billingClient.getBill(id);
    }
}
