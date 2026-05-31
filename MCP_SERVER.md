# 1. Java 调用链 MCP 分析工具

## 1.1. 作用

向大模型 Agent 提供生成 Java 方法调用链的工具，使其能够更好地理解与分析 Java 代码。

## 1.2. 可以解决的问题

### 1.2.1. 复杂业务系统代码难以理解

面对大型 Java 业务系统，代码量庞大、功能模块众多、调用层次深，无论是接手新项目还是维护长期迭代的老项目，都很难理清代码的调用关系和业务流程。逐个文件阅读源码效率低，且容易遗漏关键路径。

所有的项目只有在刚创建时是全新的，从第一次提交后开始，都可能涉及到对存量代码的修改，对存量代码的理解是无法避免的。而理解存量代码的前提，就是理清方法之间的调用关系。

**调用链工具对大模型 Agent 的帮助**：从入口方法（如 Controller）生成向下调用链，即可看到完整的请求处理流程；从核心方法生成向上调用链，即可了解所有触发该方法的业务场景。无需逐文件阅读源码，即可掌握项目的核心调用结构。

### 1.2.2. 影响范围难以评估

无论是修改代码前评估变更影响，还是线上发现问题后确定影响范围，都需要找到所有受波及的上层功能。传统方式下只能靠经验判断或全局搜索，容易遗漏跨模块、跨层的间接调用关系，导致"改一处坏一片"，或问题影响范围评估不完整。

**调用链工具对大模型 Agent 的帮助**：从目标方法生成向上调用链，自动找出所有直接和间接受影响的上层调用方，包括跨模块、跨层的完整调用路径。修改代码前可据此评估变更影响范围，避免遗漏；发现问题后可据此确定受波及的业务入口，为问题定级和修复优先级判断提供依据。

### 1.2.3. 数据库写操作路径难以追踪

排查数据异常时，需要知道某个业务操作到底写了哪些数据库表。手动查找 Mapper 文件和 SQL 语句，在大型项目中耗时且容易遗漏。

**调用链工具对大模型 Agent 的帮助**：生成向上或向下调用链时，自动识别并标注方法涉及的 MyBatis 数据库写操作及对应的数据表名称（目前支持识别使用 MyBatis 时的 Java 代码与 SQL 语句、数据库表之间的关系），调用链中即可直观看到每个方法影响了哪些表。

### 1.2.4. 回归测试范围难以确定

代码变更后，需要确定回归测试的边界。范围太小可能漏测，范围太大浪费资源。尤其是跨模块的间接影响，很难凭经验判断完整。

**调用链工具对大模型 Agent 的帮助**：通过变更影响分析确定受影响的调用链路，据此明确回归测试需要覆盖的入口和路径，既避免漏测，也避免不相关模块的过度测试。

## 1.3. 优势

向大模型 Agent 提供生成 Java 方法调用链工具后，相比大模型 Agent 本身拥有的工具和能力，具有以下优势：

### 1.3.1. 减少大模型调用次数

大模型 Agent 通过 IDE 工具（如 Call Hierarchy）查看调用关系时，每次只能查一个方法，需要手动递归展开。假设一个方法有 3 层调用、每层 3 个分支，递归查询需要 40+ 次工具调用。调用链工具一次执行即可生成指定方法的所有上下层调用链，大模型 Agent 只需调用一次即可获取完整结果。

### 1.3.2. 减少 Token 消耗

大模型 Agent 递归查询调用关系时，每次工具调用的返回结果都进入上下文，层层累积消耗大量 Token。调用链工具将完整调用链一次性写入文件，大模型 Agent 可按需读取关心的部分，避免将大量中间过程信息填入上下文。

### 1.3.3. 减少耗时

大模型 Agent 递归查询调用关系时，每次工具调用都需要等待大模型推理和工具返回，多层递归导致总耗时随调用深度线性增长。调用链工具在服务端一次性完成递归分析，大模型 Agent 只需等待一次执行结果，整体耗时显著降低。

### 1.3.4. 结果更准确

- **多态与泛型实际类型识别**：大模型 Agent 通过文本搜索或 IDE 工具查找调用关系时，接口调用只能看到声明类型，若要识别运行时的实际实现类，需要额外查找实现类并分析继承关系，耗时与 Token 消耗更大。调用链工具在静态分析阶段即解析多态的实际类型，生成的调用链直接包含实际类型。
- **Spring Bean 注入实际类型识别**：大模型 Agent 通过文本搜索查找 Spring Bean 的调用关系时，字段声明类型是接口，若要确定实际注入的实现类，需要额外分析配置文件或注解，耗时与 Token 消耗更大。调用链工具解析 Spring Bean 注入关系，在调用链中直接体现实际注入的类型。
- **跨模块间接调用自动追踪**：大模型 Agent 通过全局搜索方法名查找调用方时，只能找到直接调用，无法自动追踪跨模块、跨层的间接调用链路。调用链工具递归生成完整的调用链，每一层的间接调用都会包含在内（前提：对应模块的代码已指定为需要分析的范围）。
- **方法注解信息展示**：大模型 Agent 需要单独查找方法上的注解信息（如 Spring MVC 请求映射路径、事务注解等）。调用链工具在生成调用链时自动展示方法注解信息，无需额外查找。
- **MyBatis 数据库操作关联**：大模型 Agent 无法直接将方法调用与数据库操作关联起来，需要手动查找 Mapper 文件和 SQL 语句。调用链工具在生成向下调用链时自动标注 MyBatis 写操作及对应的数据表，无需额外查找。

## 1.4. 特点

- **丰富的配置参数**：支持灵活的配置，包括需要解析的代码范围、需要解析的 Java 包/类/方法、生成调用链时需要忽略的类与方法。通过精细化的配置，可以聚焦关心的代码范围，过滤干扰信息，生成更有针对性的调用链结果。
- **通过大模型 Agent 接入，省去人工配置**：大模型 Agent 根据用户的分析需求，自动选择合适的配置参数并执行分析，无需用户手动填写配置文件或理解每个参数的含义。用户只需用自然语言描述想要分析的内容，大模型 Agent 即可完成从配置到结果获取的全过程。

## 1.5. 支持分析的代码格式

以下格式的文件或目录可作为输入，供工具进行静态分析和调用链生成：

```
jar
class
war
jmod
fat jar
Maven/Gradle 构建输出目录（如 target/classes、build/classes/java/main）
```

## 1.6. 使用方式

### 1.6.1. 运行环境

运行 java-all-call-graph-server 需要 JDK8 及以上版本

部分 JDK8 版本在通过 Gradlew 编译时会失败，需要升级。

编译失败信息如下，从 Maven 仓库下载文件时失败：

```
FAILURE: Build failed with an exception.

* What went wrong:
A problem occurred configuring root project 'java-all-call-graph-server'.
> Could not resolve all files for configuration ':classpath'.
   > Could not resolve org.springframework.boot:spring-boot-gradle-plugin:2.7.18.
     Required by:
         project :
      > Could not resolve org.springframework.boot:spring-boot-gradle-plugin:2.7.18.
         > Could not get resource 'https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-gradle-plugin/2.7.18/spring-boot-gradle-plugin-2.7.18.pom'.
            > Could not GET 'https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-gradle-plugin/2.7.18/spring-boot-gradle-plugin-2.7.18.pom'.
               > The server may not support the client's requested TLS protocol versions: (TLSv1.2). You may need to configure the client to allow other protocols to be used. See: https://docs.gradle.org/7.6.6/userguide/build_environment.html#gradle_system_properties
                  > sun.security.validator.ValidatorException: PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target
```

存在问题的版本为 jdk1.8.0_111。

安装更高版本的 JDK 后解决问题，可从 https://adoptium.net/zh-CN/temurin/releases?version=8 下载，例如 jdk1.8.0_462 版本。

### 1.6.2. 代码下载

从以下地址下载 java-all-call-graph-server 源码：

[https://github.com/Adrninistrator/java-all-call-graph-server](https://github.com/Adrninistrator/java-all-call-graph-server)

### 1.6.3. 运行 java-all-call-graph-server MCP 服务

#### 1.6.3.1. 通过 IDE 运行源码

在 IDE 中执行启动类 `com.github.adrninistrator.jacgserver.JacgServerApplication`

#### 1.6.3.2. 构建后运行

- 构建：在项目根目录执行 `gradlew bootJar`，生成可执行 jar 到 `build/libs` 目录
- 运行：在 `build/libs` 目录执行 `start.bat`（Windows）或 `start.sh`（Linux/Mac）

### 1.6.4. 配置 MCP 服务

配置通过 SSE 方式访问的 MCP 服务，将以下配置添加到大模型 Agent 的 MCP 服务配置中，对应的 URL 如下：

```json
    "jacg-server": {
      "url": "http://127.0.0.1:34567/mcp/sse"
    }
```

## 1.7. 技术演进路线

| 时间 | 组件 | 说明 |
|------|------|------|
| 2021.07 | java-callgraph2 | 核心静态分析，生成方法调用关系 |
| 2021.07 | java-all-call-graph | 结构化入库，支持完整调用链与影响分析 |
| 2026.04 | java-all-call-graph-server | 提供 Web 界面，项目管理与可视化操作 |
| 2026.05 | java-all-call-graph-server | 提供 MCP 服务，支持大模型 Agent 接入 |

相关项目地址：

- java-callgraph2（核心静态分析）

https://github.com/adrninistrator/java-callgraph2

- java-all-call-graph（结构化入库与调用链生成）

https://github.com/adrninistrator/java-all-call-graph

- java-all-call-graph-server（Web 界面与 MCP 服务）

https://github.com/Adrninistrator/java-all-call-graph-server
