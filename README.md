# 🍅 Tomato Disease Detection Front End

## 📌 Overview
Front end for this [repository](https://github.com/MinorvaFalk/tomato-disease-ml)

Front End created using Android Studio with `Kotlin` languange

Application only support device with `API 21+` (Android 5.0+)

`Camera` is required to run this app

Application is using `MVVM Architecture` with easily decoupled service and repository.

## 🚧 Todo List :
- [x] Added README.md
- [x] Create project structure
- [x] Added documentation 
- [ ] Fix socket connection closed for `LiveDetection` feature
- [ ] Added error handler
- [ ] Implement fixed API or Dynamic API

## 📌 Getting Started

#### ⚠️**BEFORE RUNNING THE APP**⚠️  
Open `api/API`, then change the `baseUrl` without http/https\
Then you can run the app safely.

## 📌 Project Structure
```bash
├───analyzer
├───api
├───di
├───domain
│   ├───model
│   ├───repository
│   └───service
├───ui
│   ├───adapter
│   ├───canvas
│   └───viewmodels
└───utils
```

📂 **Analyzer**
---
This directory contain all analyzer for `CameraX`\
Available analyzer: `Luminosity`, `Objects`

📂 **API**
---
This directory contain API endpoints for `Retrofit API Client`\
Also configure the `baseUrl` here

📂 **DI (Dependency Injection)**
---
This directory serves as DI Module.

📂 **Domain**
---
This directory contain sub-directory :
- 📂 **Model**\
  Contain all model for `DTO` (Data Transfer Object) and `Data Classes`

- 📂 **Repository**\
  Process data from `services` or `API` into data classes.

- 📂 **Service**\
  Layer that `connected directly` to `API Endpoint` and convert responses into data classes 

📂 **UI**
---
This directory contain User Interface

📂 **Utils**
---
This directory serves as helper function or objects