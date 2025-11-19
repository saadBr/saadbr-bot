package net.saadbr.mcpserver.tools;

import org.springaicommunity.mcp.annotation.McpArg;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author saade
 **/
@Component
public class McpTools {
    @McpTool(name = "getEmployee", description = "Get Information about a given employee")
    public Employee getEmployee(@McpArg(description = "The employee name") String name) {
        return new Employee(name,11500, 3);
    }
    @McpTool(name = "getEmployees", description = "List all employees")
    public List<Employee> getEmployees(){
        return List.of(
                new Employee("Saad", 13500, 3),
                new Employee("Rhita", 11500, 3),
                new Employee("Nour", 8500, 1)
        );
    }
}
record Employee(String name, double salary, int seniority) {}

