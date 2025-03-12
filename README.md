# 🌟 AI Based Smart Light System

## 🏠 Overview
Welcome to the AI-powered **Smart Lighting Management System**! This system intelligently controls lighting based on user behavior and music preferences, ensuring a seamless and adaptive experience. It consists of four main components:

1. **📱 Application** – Intuitive UI for manual and voice-controlled lighting.
2. **🖥️ Central Control System** – AI-driven control system interpreting behavior and music.
3. **☁️ Cloud Server** – Handles authentication, computation, and AI model fine-tuning.
4. **💡 Light System** – Provides lighting adjustments tailored to user activities.
   
## 🏗️ System Architecture
![smartlightSystem](https://github.com/user-attachments/assets/4aef5308-0c3b-4830-8f2f-8d68ec5cf3b2)

> *The diagram above represents the system's overall architecture, illustrating the interactions between the Application, Central Control System, Cloud Server, and Light System.*

## 🔗 System Components

### 1️⃣ Application (📱)
The application runs on a user's mobile device and provides the following features:
- Manual lighting control via UI.
- Voice-controlled lighting("Hey LUX").
- Feedback collection for AI fine-tuning.

#### **Voice Commands**
- "전체 조명 꺼줘/켜줘" (Turn all lights off/on)
- "조명 n번 꺼줘/켜줘" (Turn light number `n` off/on)
- "음악모드로 바꿔줘" (Switch to Music Mode)
- "일반모드로 바꿔줘" (Switch to Normal Mode)

When in **Music Mode**, lighting adjusts based on the music genre.

### 2️⃣ Central Control System (🖥️)
- Hardware : Raspberry Pi 4B
- Acts as an intermediary between the Application and Light System.
- Uses AI to analyze behavior (camera) and music (microphone) to generate lighting control signals.
- Facilitates communication between Application, Cloud Server, and Light System.

### 3. Cloud Server
- Deployed on **Google Cloud Platform (GCP)** as an **IaaS** solution.
- Provides user authentication and personalized AI models using fine-tuning.
- Stores and manages:
  - **Fine-Tuning Data**: User feedback for improved AI models.
  - **User Data**: Authentication and preference storage.

#### **Fine-Tuning Process**
1. User feedback is collected and stored.
2. Once sufficient data is gathered, AI models are retrained.
3. The updated model is deployed to the Central Control System.

### 4. Light System
- Operates based on received control signals.
- Adjusts lighting to match user behavior and environmental conditions.
- Provides ambient lighting enhancements based on user preferences.
