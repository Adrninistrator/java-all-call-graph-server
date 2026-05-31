# 环境依赖

依赖 Python、Node.js 运行环境

# 前置步骤 

在控制台进入当前项目 python 目录

# 安装 Python 依赖

执行 pip install -r requirements.txt

# 启动 MCP client

执行 mcp dev mcp_dev_server.py

或执行 start_mcp_client.bat

第一次执行出现以下提示，需要输入“y”后回车

```
Need to install the following packages:
@modelcontextprotocol/inspector@0.21.2
Ok to proceed? (y) 
```

之后会提示类似如下内容

```
Starting MCP inspector...
⚙️ Proxy server listening on localhost:6277
🔑 Session token: 5de0e86d1c68a20ee46787674efb1e03a15ce974322cc8fc33424118fbcebcc9
   Use this token to authenticate requests or set DANGEROUSLY_OMIT_AUTH=true to disable auth

🚀 MCP Inspector is up and running at:
   http://localhost:6274/?MCP_PROXY_AUTH_TOKEN=5de0e86d1c68a20ee46787674efb1e03a15ce974322cc8fc33424118fbcebcc9

🌐 Opening browser...
```

会通过浏览器打开以上链接

# 连接 MCP server

在打开的浏览器页面中操作

- Transport Type

选择 SSE

- URL

输入 http://localhost:8080/mcp/sse

- Connection Type

选择 Direct

点击“Connect”按钮连接 MCP server

连接成功后，可以点击“List Tools”列出支持的工具并使用
