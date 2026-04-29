SecureDocAI 📄🔐
AI-Powered Secure Document Scanner for Android
On-device OCR + ML Classification + LLM Summarization — built with privacy-first design
________________________________________
📱 Demo
Scan	Classify	Summarize
Camera/Gallery → OCR text	ML model phân loại tài liệu	Groq LLM trích xuất thông tin
________________________________________
✨ Features
•	 Scan tài liệu — chụp ảnh hoặc chọn từ thư viện
•	 On-device OCR — ML Kit Text Recognition v2, hoạt động offline, ảnh không rời khỏi device
•	 AI Classification — tự động phân loại: Hóa đơn / Hợp đồng / CMND / Khác
•	 LLM Summarization — trích xuất thông tin quan trọng (tổng tiền, bên A/B, số CMND...)
•	Export & Share — xuất kết quả dạng text
________________________________________
🏗️ Architecture
┌─────────────────────────────────────────┐
│         Presentation Layer              │
│   ScannerScreen (Jetpack Compose)       │
│   ScannerViewModel (StateFlow)          │
└────────────────┬────────────────────────┘
                 │
┌────────────────▼────────────────────────┐
│           Domain Layer                  │
│   DocumentRepository                   │
│   LocalDocumentRepository              │
└──────────┬──────────────┬──────────────┘
           │              │
┌──────────▼──────┐  ┌────▼──────────────┐
│   Data Layer    │  │   Data Layer      │
│                 │  │                   │
│  ML Kit OCR     │  │  ClassifierAPI    │
│  (On-device)    │  │  (HF Space)       │
│                 │  │                   │
│  Room Database  │  │  Groq API         │
│  (Local)        │  │  (LLM Summary)    │
└─────────────────┘  └───────────────────┘
Pattern: Clean Architecture + MVVM + Hilt DI
________________________________________
 AI Stack
1. On-device OCR — ML Kit Text Recognition v2
•	Chạy hoàn toàn offline trên device
•	Ảnh tài liệu không bao giờ rời khỏi device — privacy-first
•	Latency < 1s trên mid-range Android device
2. ML Document Classifier — Custom Python Model
•	TF-IDF + Random Forest — train trên dataset tiếng Việt
•	Deploy trên Hugging Face Spaces (serverless)
•	Phân loại 4 nhãn: HÓA ĐƠN / HỢP ĐỒNG / CMND / KHÁC
•	Trả về confidence score cho mỗi prediction
🔗 Document Classifier API
3. LLM Summarization — Groq API (Llama 3.1)
•	Trích xuất thông tin có cấu trúc từ văn bản OCR
•	Prompt engineering theo từng loại tài liệu: 
o	Hóa đơn → tên công ty, MST, tổng tiền, VAT
o	Hợp đồng → bên A/B, thời hạn, giá trị
o	CMND → họ tên, ngày sinh, số CMND
Tại sao thiết kế như vậy?
Quyết định	Lý do
ML Kit cho OCR (không gửi ảnh lên cloud)	Ảnh hóa đơn chứa thông tin tài chính nhạy cảm
Custom ML model thay vì chỉ dùng LLM	Classify nhanh hơn, rẻ hơn, không tốn LLM token
Groq cho summarize (không phải OCR)	Chỉ gửi text đã extract, không gửi ảnh gốc
Offline-first với Room	Xem lại lịch sử không cần mạng
________________________________________
 Tech Stack
Layer	Technology
Language	Kotlin
UI	Jetpack Compose + Material Design 3
Architecture	Clean Architecture + MVVM
DI	Hilt
Camera	CameraX
On-device AI	ML Kit Text Recognition v2
Network	Retrofit + OkHttp
Local DB	Room
Async	Coroutines + StateFlow
AI Backend	Python (Flask) + scikit-learn
AI Deploy	Hugging Face Spaces
LLM	Groq API (Llama 3.1 8B)

 Setup
Prerequisites
•	Android Studio Hedgehog+
•	Android device/emulator API 26+
•	Groq API key (free): https://console.groq.com
1. Clone repo
bash
git clone https://github.com/Az290/SecureDocAI.git
cd SecureDocAI
2. Thêm API keys vào local.properties
properties
GROQ_API_KEY=your_groq_api_key_here
3. Build & Run
bash
./gradlew assembleDebug
________________________________________
 Project Structure
app/src/main/java/com/securedoc/ai/
├── data/
│   ├── local/          # Room DB, DAO, LocalRepository
│   ├── remote/         # Retrofit APIs, GroqApi, ClassifierApi
│   └── di/             # Hilt modules
├── presentation/
│   └── scanner/        # ScannerScreen, ScannerViewModel
└── utils/              # PdfExporter, GalleryLauncher
________________________________________
 AI Concepts Applied
Concept	Ứng dụng trong project
On-device ML	ML Kit OCR chạy trên device, không cần internet
TF-IDF	Vector hóa văn bản cho document classifier
Prompt Engineering	System prompt khác nhau cho từng loại tài liệu
LLM Inference	Groq API gọi Llama 3.1 để summarize
Confidence Score	Hiển thị độ tin cậy của classification


 License
MIT License

