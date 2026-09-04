# SmartFactory-Agent

Industrial intelligent operations and maintenance platform.

## Tech Stack

- Java 26
- Spring Boot
- MyBatis
- MySQL
- Redis
- Spring Security
- JWT
- RabbitMQ
- MongoDB
- Elasticsearch
- MinIO
- Spring AI
- Spring Cloud

## Project Goal

Build an industrial intelligent operations and maintenance platform integrating industrial data collection, device management, alarm management, AI diagnosis and microservices.

## Development Status

- [x] Project initialization
- [ ] User management
- [ ] Device management
- [ ] MyBatis + MySQL
- [ ] Spring Security + JWT
- [ ] Redis
- [ ] Modbus TCP
- [ ] RabbitMQ
- [ ] MongoDB
- [ ] Elasticsearch
- [ ] MinIO
- [ ] Spring AI Agent
- [ ] RAG
- [ ] MCP
- [ ] Spring Cloud

## 整体技术架构
                         用户 / 前端
                              │
                              ▼
                           Nginx
                              │
                              ▼
                    ┌──────────────────┐
                    │   Spring Boot    │
                    │   核心业务系统    │
                    └────────┬─────────┘
                             │
        ┌────────────────────┼────────────────────┐
        │                    │                    │
        ▼                    ▼                    ▼
   Spring MVC            Security             Swagger
        │                 JWT/RBAC              │
        │                    │                  API文档
        ▼                    ▼
     业务层              Redis
        │              实时状态/缓存
        │
        ▼
      MyBatis
        │
        ▼
      MySQL
   核心业务数据库
        │
        ├─────────────────────────────┐
        │                             │
        ▼                             ▼
    RabbitMQ                       MongoDB
    异步消息                       原始设备数据
        │
        │
        ▼
   Elasticsearch
   日志/报警/搜索
        │
        ▼
      MinIO
  PDF/图片/报告/手册


                 工业互联网部分
                      │
                      ▼
              ┌──────────────┐
              │ PLC Simulator │
              └──────┬───────┘
                     │
                 Modbus TCP
                     │
                     ▼
              Java Modbus Master
                     │
                     ▼
                 Spring Boot
                     │
              ┌──────┴──────┐
              ▼             ▼
            Redis        MongoDB
          实时状态       历史原始数据


                    AI部分
                     │
                     ▼
                  Spring AI
                     │
        ┌────────────┼────────────┐
        ▼            ▼            ▼
     ChatClient   Tool Calling   Memory
                     │
                     ▼
                    RAG
                     │
                     ▼
                    MCP
                     │
                     ▼
                 AI Agent


             最后再进入 Spring Cloud
                     │
        ┌────────────┼────────────┐
        ▼            ▼            ▼
      Gateway    User Service   Device Service
                                  │
                            Alarm Service
                                  │
                            WorkOrder Service
                                  │
                              AI Service


设备产生数据 → Java采集 → 数据存储 → 实时监控 → 异常报警 → 消息异步处理 → 历史搜索 → 文件知识库 → AI分析 → Agent自动执行 → 微服务化