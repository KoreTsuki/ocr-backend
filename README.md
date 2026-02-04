# OCR 项目 :sparkles:

## 📚 项目简介 :sunrise:

本项目是我的毕业设计，一个结合了DDD（领域驱动设计）架构理念的Java-Python跨界之作，旨在提供精准高效的OCR服务。

利用Python的[Paddle OCR](https://github.com/PaddlePaddle/PaddleOCR)库进行文字识别，该库以其高准确率和对复杂场景的适应性著称，能够轻松识别包括180°翻转在内的多种文本形式。

与此同时，Java部分采用了DDD架构，不仅加深了业务理解，也提升了代码的可维护性和扩展性。

前端则选用了Ant Design Pro，为用户提供流畅的交互体验。

- **前端项目地址**：[[ocr-frontend](https://github.com/KoreTsuki/ocr-frontend)] 使用Ant Design Pro构建，为用户带来极致的Web体验。

## 🔄 调用流程与功能说明 :gear:

### 💻 单体版本

- **Python OCR引擎**：图片信息转发至Python后端，调用Paddle OCR进行文字识别。
- **Java处理逻辑**：识别结果被Java后端反序列化，提取关键信息并暂存于Redis（时效5分钟），利用线程池与Redission实现并发控制与流量限制。

## 🎨 功能展示 :framed_picture:

- **截图预览**：

  ![eg](eg.png)

## 🛠️ 软件架构 :wrench:

遵循DDD原则，项目分为四层：

- **触发层**：http、消息队列、定时任务等
- **应用层**：协调业务逻辑
- **领域层**：核心业务规则与实体
- **基础设施层**：数据库、缓存等

## 🔧 技术栈亮点 :key:

### Java技术栈

| 类别     | 技术/框架   | 用途                            |
| -------- | ----------- | ------------------------------- |
| 基础框架 | Spring Boot | 应用基础框架                    |
| Web框架  | Spring MVC  | 提供HTTP接口                    |
| 数据访问 | MyBatis     | ORM框架，操作数据库             |
| 数据库   | MySQL       | 持久化存储                      |
| 缓存     | Redis       | 临时存储OCR结果、限流，消息队列 |
| 工具库   | OkHttp      | HTTP客户端，调用Python OCR服务  |
| 工具库   | Jackson     | JSON序列化/反序列化             |
| 工具库   | Lombok      | 简化Java代码                    |
| API文档  | Knife4j     | 生成API文档                     |
| 分布式锁 | Redission   | 实现并发控制                    |
| 对象存储 | MinIO       | 存储图片等文件                  |

### Python技术栈

| 类别    | 技术/框架 | 用途                       |
| ------- | --------- | -------------------------- |
| Web框架 | Flask     | 轻量级Web框架，提供OCR接口 |
| OCR库   | PaddleOCR | 核心OCR引擎，实现文字识别  |
| 工具库  | NumPy     | 处理数组等数据结构         |
| 工具库  | JSON      | 处理JSON数据               |

### 前端技术

- **React + Ant Design Pro**：构建现代Web应用

## 📖 安装部署教程 :book:

欢迎来到本项目的安装和运行教程！本教程将引导您完成项目的安装、配置和运行过程。请确保您已满足以下前提条件：

- 确保您的计算机上已安装 Python 3.8以上
- 确保您已安装 Git
- 确保您的 jdk 为1.8
- 确保您有idea、vscode等开发环境

### 1. 克隆项目

1. 打开命令行工具（如 Terminal 或 Command Prompt）。
2. 输入以下命令以克隆本项目到本地仓库：

```
git clone https://github.com/KoreTsuki/ocr-backend.git
```

### 2. 安装依赖

#### Python部分

1. 进入项目目录：

```
cd python
```

1. 使用pycharm等开发环境打开 ocr.py
2. 安装paddlepaddle[安装 - PaddleOCR 文档](https://www.paddleocr.ai/main/version3.x/installation.html)

```
# CPU 版本
python -m pip install paddlepaddle==3.2.0 -i https://www.paddlepaddle.org.cn/packages/stable/cpu/

# GPU 版本，需显卡驱动程序版本 ≥450.80.02（Linux）或 ≥452.39（Windows）
python -m pip install paddlepaddle-gpu==3.2.0 -i https://www.paddlepaddle.org.cn/packages/stable/cu118/

# GPU 版本，需显卡驱动程序版本 ≥550.54.14（Linux）或 ≥550.54.14（Windows）
 python -m pip install paddlepaddle-gpu==3.2.0 -i https://www.paddlepaddle.org.cn/packages/stable/cu126/
```

3. 安装paddleocr

```
python -m pip install paddleocr
```

4. 安装其他依赖

```
pip install flask numpy pdf2image requests opencv-contrib-python
```



#### Java部分

1. 使用idea打开项目，maven进行执行生命周期 install, 等待

2. 找到 **ocr-app/src/main/resources/application.yml**，更换为自己的配置，

   不懂minio推荐 [一小时实践入门MinIO—分布式对象存储服务器（一） - 知乎 (zhihu.com)](https://zhuanlan.zhihu.com/p/654273720)

   ocr.url 默认为 **http://127.0.0.1:8888/ocr/**

## 3. 运行项目

#### Python部分

1. 用命令行中运行主程序：

```
python ocr.py
```

默认不更改的话，直接请求的接口即为 http://127.0.0.1:8888/ocr

请求格式为

```
{
    "imgUrl":"网络图片url或本机图片路径"
}
```

响应示例为

```
{
  "code": 200,
  "data": [
    [
      [
        [
          [
            296.0,
            299.0
          ],
          [
            331.0,
            298.0
          ],
          [
            346.0,
            849.0
          ],
          [
            311.0,
            850.0
          ]
        ],
        [
          "浙A D0885",
          0.9767106175422668
        ]
      ]
  ],
  "message": "调用接口成功"
}
```



#### Java部分

1. 找到启动类**ocr-app/src/main/java/com/lrc/ocr/OcrApplication.java**，并启动
2. 进入 knife4j ，查看接口 [接口文档](http://localhost:8500/api/doc.html) （该版本暂不支持文件上传测试接口）
3. Post上传文件请求测试 http://localhost:8500/api/ocr/getToal
