# Allure OpenFeign Decoder 🔍

[![Allure](https://img.shields.io/badge/Allure%20Report-2.29%2B-brightgreen)](https://docs.qameta.io/allure/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17%2B-orange)](https://www.oracle.com/java/)
[![OpenFeign](https://img.shields.io/badge/OpenFeign-13.6+-brightgreen)](https://github.com/OpenFeign/feign)

Automatically captures Feign client HTTP traffic as Allure attachments for comprehensive API test reporting.

## ✨ Features
- **Automatic HTTP logging** of requests/responses
- **Seamless Allure integration** with templated attachments
- **Full traffic capture**: Headers, bodies, status codes

## 📦 Installation
```xml
<dependency>
    <groupId>io.github.sbushmelev</groupId>
    <artifactId>allure-open-feign</artifactId>
    <version>1.0</version>
    <scope>test</scope>
</dependency>
```

## 🚀 Usage (Example with GsonDecoder)
```java
       // Configure your Feign client
       MyClient myClient = Feign.builder()
                .decoder(new AllureResponseDecoder(new GsonDecoder()))
                .target(MyApi.class, "https://test.url");
```

## 📜 License
This project is licensed under the **Apache License 2.0**.  
See [LICENSE](LICENSE) for the full text.

Third-party library notices are in [NOTICE](NOTICE).

## 📊 Sample Report

![General_view](docs/images/general_view.png)