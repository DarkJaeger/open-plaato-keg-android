<a href="https://www.buymeacoffee.com/LocutusOFB"><img src="https://cdn.buymeacoffee.com/buttons/v2/default-yellow.png" alt="Buy Me A Coffee" height="41" width="174"></a>

# Open PLAATO Keg Android

Android application for interacting with a self-hosted Open PLAATO Keg server.

This app provides a mobile interface to monitor and manage your PLAATO Keg data locally, without relying on the discontinued PLAATO cloud services.

---

## 📱 Features

* View real-time keg data (volume, weight, temperature)
* Monitor pour activity and consumption
* Connect to a local Open PLAATO Keg server
* Simple and lightweight mobile interface
* Works fully offline within your local network

---

## 🚀 Background

PLAATO devices originally relied on a cloud-based backend, which is being discontinued.
Projects like open-plaato-keg allow you to:

* Run everything locally
* Keep full control of your data
* Extend functionality with APIs, MQTT, and integrations ([GitHub][1])

This Android app is designed to act as a companion client for that ecosystem.

---

## 🛠️ Requirements

* Android device (Android 8.0+ recommended)
* Running instance of Open PLAATO Keg server

  * Typically hosted on:

    * Raspberry Pi
    * Docker server
    * Home server / NAS

---

## ⚙️ Setup

### 1. Start the Open PLAATO Keg Server

Example (Docker):

```bash
docker run -p 1234:1234 -p 8085:8085 ghcr.io/sklopivo/open-plaato-keg:latest
```

Make note of your server IP address.

---

### 2. Configure Your PLAATO Keg

* Connect to `PLAATO-XXXXX` WiFi network
* Open `http://192.168.4.1`
* Enter:

  * WiFi credentials (2.4GHz required)
  * Auth token
  * Server IP + port (default: 1234)

---

### 3. Build & Install the App

Clone the repository:

```bash
git clone https://github.com/DarkJaeger/open-plaato-keg-android.git
cd open-plaato-keg-android
```

Open in Android Studio and:

* Sync Gradle
* Build the project
* Install on your device or emulator

---

## 🔌 Configuration

Inside the app:

* Enter your Open PLAATO Keg server IP
* Set the correct port (default: `8085` or API endpoint)
* Connect and verify data is being received

---

## 📡 API Notes

The app communicates with the Open PLAATO Keg server via:

* HTTP endpoints
* WebSocket (if implemented)
* Optional MQTT integrations

The server exposes endpoints such as:

```
/api/kegs
/api/kegs/:id
```

---

## 🧪 Development

### Tech Stack

* Kotlin / Java (Android)
* REST API integration
* (Optional) WebSocket support

### Suggested Improvements

* Push notifications for low keg levels
* Graphing / historical data
* Multi-keg dashboard
* Home Assistant integration

---

## 🤝 Contributing

Contributions are welcome!

* Fork the repo
* Create a feature branch
* Submit a pull request

---

## ⚠️ Disclaimer

This project is **not affiliated with PLAATO Technologies**.

It is a community-driven effort to extend the usability of PLAATO hardware after official cloud services are discontinued.

---

## 📄 License

MIT

---

## 🙌 Acknowledgements

* open-plaato-keg community
* PLAATO hardware developers
* Homebrewing community

---

[1]: https://github.com/sklopivo/open-plaato-keg?utm_source=chatgpt.com "sklopivo/open-plaato-keg"
