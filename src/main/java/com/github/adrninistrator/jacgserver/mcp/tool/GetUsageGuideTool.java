package com.github.adrninistrator.jacgserver.mcp.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.adrninistrator.jacgserver.enums.McpToolEnum;
import com.github.adrninistrator.jacgserver.mcp.McpToolDefinition;
import com.github.adrninistrator.jacgserver.mcp.McpToolHandler;
import com.github.adrninistrator.jacgserver.mcp.McpToolHelper;
import org.springframework.stereotype.Component;

/**
 * 获取当前项目使用说明的 MCP Tool
 *
 * AI 可通过此工具获取当前项目的使用说明，包括所有 MCP 服务的功能说明及推荐使用流程。
 * 该接口仅提供MCP服务方式调用，不提供HTTP接口。
 *
 * @author adrninistrator
 * @since 1.0.0
 */
@Component
public class GetUsageGuideTool implements McpToolHandler {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public McpToolDefinition getDefinition() {
        return new McpToolDefinition(McpToolEnum.GET_USAGE_GUIDE,
                "获取当前项目的使用说明，包括所有 MCP 服务的功能说明及推荐使用流程。")
                .addProperty("language", "string", "返回语言，可选值：zh（中文，默认）、en（英文）", false);
    }

    @Override
    public JsonNode handle(JsonNode arguments) {
        String language = arguments.has("language") ? arguments.get("language").asText() : "zh";
        if (!"zh".equalsIgnoreCase(language) && !"en".equalsIgnoreCase(language)) {
            return McpToolHelper.createErrorResult("不支持的language参数值: " + language + "，可选值：zh（中文）、en（英文）");
        }
        boolean isEn = "en".equalsIgnoreCase(language);

        try {
            ObjectNode data = objectMapper.createObjectNode();

            // 项目说明
            data.put("projectName", "java-all-call-graph-server");
            data.put("projectDesc", isEn
                    ? "Java call graph analysis service, supports static analysis of Java projects and generates method call chains"
                    : "Java调用链分析服务，支持对Java项目进行静态分析并生成方法调用链");

            // MCP 服务列表
            ArrayNode services = objectMapper.createArrayNode();
            for (McpToolEnum tool : McpToolEnum.values()) {
                ObjectNode service = objectMapper.createObjectNode();
                service.put("name", tool.getName());
                service.put("desc", isEn ? tool.getDescEn() : tool.getDesc());
                services.add(service);
            }
            data.set("services", services);

            // 推荐使用流程
            ArrayNode workflow = objectMapper.createArrayNode();
            if (isEn) {
                workflow.add("1. Call " + McpToolEnum.CHECK_PROJECT_DESCRIPTION.getName() + " to check if project description is available");
                workflow.add("2. Call " + McpToolEnum.CREATE_PROJECT.getName() + " to create a project (specify description, jar paths, project root directory)");
                workflow.add("3. Call " + McpToolEnum.EXECUTE_ANALYSIS.getName() + " to execute static analysis on the project");
                workflow.add("4. Call " + McpToolEnum.QUERY_ANALYSIS_STATUS.getName() + " to check if static analysis is complete");
                workflow.add("5. Call " + McpToolEnum.QUERY_DEFAULT_TEMPLATE_INFO.getName() + " to check if default templates exist");
                workflow.add("6. If default templates do not exist, call " + McpToolEnum.CREATE_DEFAULT_TEMPLATE.getName() + " (direction=callee/caller) to create them");
                workflow.add("7. Call " + McpToolEnum.EXECUTE_DEFAULT_TEMPLATE.getName() + " (direction=callee/caller, specify entry methods) to generate call chains (async execution, returns exec ID)");
                workflow.add("8. Call " + McpToolEnum.QUERY_CALL_GRAPH_STATUS.getName() + " (direction=callee/caller, pass exec_id) to check call chain generation status and results (including call graph file path map)");
                workflow.add("9. To modify configuration parameters, first call " + McpToolEnum.GET_CONFIG_GUIDE.getName() + " to understand the config structure, then call " + McpToolEnum.QUERY_CONFIG.getName() + " (with project_id or template_id) to check current values, then call " + McpToolEnum.SAVE_PROJECT_CONFIG.getName() + " or " + McpToolEnum.SAVE_TEMPLATE_CONFIG.getName());
                workflow.add("10. For EL expression type parameters, use " + McpToolEnum.QUERY_CONFIG.getName() + " to check the allowedVariables field (available variables, types, descriptions, examples), and use " + McpToolEnum.GET_EL_USAGE.getName() + " (type=common for general guide, type=javacg2 for JavaCG2 guide, type=jacg for JACG guide) to get expression usage examples before modifying");
                workflow.add("11. " + McpToolEnum.QUERY_CONFIG.getName() + " returns complete parameter descriptions, including enum options, numeric ranges, and allowed variables for EL expression parameters");
            } else {
                workflow.add("1. 调用 " + McpToolEnum.CHECK_PROJECT_DESCRIPTION.getName() + " 检查项目描述是否可用");
                workflow.add("2. 调用 " + McpToolEnum.CREATE_PROJECT.getName() + " 创建项目（需要指定项目描述、jar路径、项目根目录）");
                workflow.add("3. 调用 " + McpToolEnum.EXECUTE_ANALYSIS.getName() + " 对项目执行静态分析");
                workflow.add("4. 调用 " + McpToolEnum.QUERY_ANALYSIS_STATUS.getName() + " 查询静态分析是否完成");
                workflow.add("5. 调用 " + McpToolEnum.QUERY_DEFAULT_TEMPLATE_INFO.getName() + " 查询项目是否已有默认模板");
                workflow.add("6. 若默认模板不存在，调用 " + McpToolEnum.CREATE_DEFAULT_TEMPLATE.getName() + "（direction=callee/caller）创建默认模板");
                workflow.add("7. 调用 " + McpToolEnum.EXECUTE_DEFAULT_TEMPLATE.getName() + "（direction=callee/caller，指定入口类/方法）执行调用链生成（异步执行，返回执行ID）");
                workflow.add("8. 调用 " + McpToolEnum.QUERY_CALL_GRAPH_STATUS.getName() + "（direction=callee/caller，传入exec_id）查询调用链生成状态及结果（包含调用链文件路径Map）");
                workflow.add("9. 如需修改配置参数，先调用 " + McpToolEnum.GET_CONFIG_GUIDE.getName() + " 了解配置参数结构，再调用 " + McpToolEnum.QUERY_CONFIG.getName() + "（传入project_id或template_id）查询配置参数定义及当前值，最后调用 " + McpToolEnum.SAVE_PROJECT_CONFIG.getName() + " 或 " + McpToolEnum.SAVE_TEMPLATE_CONFIG.getName() + " 修改");
                workflow.add("10. 对于表达式类型的配置参数，修改前需要通过 " + McpToolEnum.QUERY_CONFIG.getName() + " 查看返回的 allowedVariables 字段（可使用的变量、类型、描述、示例），并通过 " + McpToolEnum.GET_EL_USAGE.getName() + "（type=common获取通用说明，type=javacg2获取JavaCG2表达式说明，type=jacg获取JACG表达式说明）获取表达式使用说明示例后再修改");
                workflow.add("11. " + McpToolEnum.QUERY_CONFIG.getName() + " 返回完整的参数说明，包括枚举选项、数值范围，以及表达式参数允许使用的变量信息");
            }
            data.set("workflow", workflow);

            // 重要说明
            ArrayNode notes = objectMapper.createArrayNode();
            if (isEn) {
                notes.add("Default template call chain generation is async; returns exec ID, use " + McpToolEnum.QUERY_CALL_GRAPH_STATUS.getName() + " (direction=callee/caller, pass exec_id) to check results");
                notes.add("Project static analysis and template call chain generation MCP services return the corresponding log file path (logFilePath), which can be used to analyze execution details");
                notes.add("Project description must be globally unique; recommend calling " + McpToolEnum.CHECK_PROJECT_DESCRIPTION.getName() + " before creating a project");
                notes.add("Project root directory represents the root directory of the project being analyzed, must be globally unique");
                notes.add("When performing static analysis on Java project compiled class files, you can specify the build output directory generated by build tools such as Maven (e.g. target/classes), Gradle (e.g. build/classes/java/main) as the jar file path or directory path when creating a project");
                notes.add("After executing static analysis, wait for completion before generating call chains");
                notes.add("Each project can only have one callee default template and one caller default template");
                notes.add("Entry methods support class name (e.g. com.example.Controller) or full method (e.g. com.example.Service:processOrder)");
                notes.add("Default template entry method config uses placeholders; actual entry methods are specified by MCP tools at execution time");
                notes.add("Default template Set config OCFUSE_METHOD_CLASS_4CALLEE/OCFUSE_METHOD_CLASS_4CALLER cannot be modified (frontend and MCP interface both forbidden)");
                notes.add(McpToolEnum.GET_SYSTEM_INFO.getName() + " returns only app name and MCP protocol version by default; pass include_path=true for output root directory and app root directory path info");
                notes.add("Configuration parameters are updated using merge: only modified parameters are updated, unmodified parameters remain unchanged");
                notes.add("To use configuration parameters: first call " + McpToolEnum.GET_CONFIG_GUIDE.getName() + " to understand the config parameter structure and formats, then call " + McpToolEnum.QUERY_CONFIG.getName() + " (with project_id or template_id) to query config parameter definitions and current values");
                notes.add("When modifying EL expression type configuration parameters, use " + McpToolEnum.QUERY_CONFIG.getName() + " to check the allowedVariables field for available variables and their usage, and use " + McpToolEnum.GET_EL_USAGE.getName() + " (type=common for general guide, type=javacg2 for JavaCG2 guide, type=jacg for JACG guide) to get EL expression usage examples");
                notes.add("Use " + McpToolEnum.GET_EL_USAGE.getName() + " with type parameter: common (EL expression general guide), javacg2 (JavaCG2 expression guide), jacg (JACG expression guide)");
                notes.add("When deciding whether to execute or re-execute static analysis on a project: " +
                        "if the current project has no successful static analysis execution record, static analysis needs to be executed; " +
                        "otherwise, compare the project's most recent compilation time with the most recent successful static analysis execution record. " +
                        "To determine the project's most recent compilation time: " +
                        "jar files: use the file modification time; " +
                        "class files: Gradle project reads the modification time of build/tmp/compileJava/previous-compilation-data.bin file; " +
                        "Maven project scans the modification time of the latest .class file under target/classes directory; " +
                        "if neither exists, scan the modification time of the latest .class file under build/classes/java/main, out/production, bin directories in order; " +
                        "other directories: get the most recent modification time of jar and class files in the directory; " +
                        "do NOT use the directory's own modification time (incremental compilation overwriting files does not update directory mtime). " +
                        "If the most recent compilation time is later than the most recent successful static analysis execution time, static analysis needs to be re-executed");
            } else {
                notes.add("默认模板调用链生成为异步执行，返回执行ID，需要调用 " + McpToolEnum.QUERY_CALL_GRAPH_STATUS.getName() + "（direction=callee/caller，传入exec_id）查询执行结果");
                notes.add("项目执行静态分析和模板生成调用链的MCP服务会返回对应的日志文件路径（logFilePath），可用于查看执行过程详情");
                notes.add("项目描述在全局范围内唯一，创建项目前建议先调用 " + McpToolEnum.CHECK_PROJECT_DESCRIPTION.getName() + " 检查");
                notes.add("项目根目录代表被解析的代码对应项目的根目录，在全局范围内唯一");
                notes.add("当需要对Java项目编译的class文件进行静态分析时，可以在创建项目时指定Maven（如target/classes）、Gradle（如build/classes/java/main）等构建工具生成的编译输出目录作为分析的jar文件路径或目录路径");
                notes.add("执行静态分析后需要等待分析完成，才能执行调用链生成");
                notes.add("每个项目只能有一个向上的默认模板和一个向下的默认模板");
                notes.add("执行默认模板时，入口类/方法支持类名（如 com.example.Controller）或完整方法（如 com.example.Service:processOrder）");
                notes.add("默认模板的入口类/方法配置使用占位符，执行时由MCP工具指定实际的入口类/方法");
                notes.add("默认模板的Set配置 OCFUSE_METHOD_CLASS_4CALLEE/OCFUSE_METHOD_CLASS_4CALLER 不允许修改（前端页面和MCP服务接口都禁止修改）");
                notes.add(McpToolEnum.GET_SYSTEM_INFO.getName() + " 默认只返回应用名称和MCP协议版本；传入 include_path=true 可获取输出根目录和应用根目录等路径信息");
                notes.add("配置参数修改使用合并方式更新：仅修改传入的配置项，未传入的配置项保持不变");
                notes.add("配置参数使用方式：首先调用 " + McpToolEnum.GET_CONFIG_GUIDE.getName() + " 获取配置参数使用说明（了解参数结构、格式等），再调用 " + McpToolEnum.QUERY_CONFIG.getName() + "（传入project_id或template_id）查询项目或模板的配置参数定义及当前值");
                notes.add("修改表达式类型的配置参数时，需要通过 " + McpToolEnum.QUERY_CONFIG.getName() + " 查看返回的 allowedVariables 字段了解可使用的变量及用法，再通过 " + McpToolEnum.GET_EL_USAGE.getName() + "（type=common获取通用说明，type=javacg2获取JavaCG2表达式说明，type=jacg获取JACG表达式说明）获取表达式使用说明和示例");
                notes.add("通过 " + McpToolEnum.GET_EL_USAGE.getName() + " 的type参数获取不同类型的EL表达式说明：common（EL表达式通用说明）、javacg2（JavaCG2表达式说明）、jacg（JACG表达式说明）");
                notes.add("判断是否需要对当前项目执行静态分析时：若当前项目没有静态分析执行成功的记录，则需要执行静态分析；" +
                        "否则需要比较项目最近编译时间与最近一次成功的静态分析执行记录。" +
                        "获取项目最近编译时间的方法：" +
                        "指定分析jar文件：通过文件修改时间获得最近编译时间；" +
                        "指定分析项目编译目录：Gradle项目读取build/tmp/compileJava/previous-compilation-data.bin文件的修改时间；" +
                        "Maven项目扫描target/classes目录下最新.class文件的修改时间；" +
                        "二者皆不存在时依次扫描build/classes/java/main、out/production、bin等目录下最新.class文件的修改时间；" +
                        "指定分析其他目录：获取目录中jar、class文件的最近修改时间；" +
                        "切勿使用目录本身的修改时间（增量编译覆盖文件不会更新目录mtime）。" +
                        "若项目最近编译时间晚于最近一次成功的静态分析执行时间，则需要重新执行静态分析");
            }
            data.set("notes", notes);

            ObjectNode response = objectMapper.createObjectNode();
            response.put("success", true);
            response.set("data", data);

            return McpToolHelper.createJsonResult(response);
        } catch (Exception e) {
            return McpToolHelper.createErrorResult(e.getMessage());
        }
    }
}
