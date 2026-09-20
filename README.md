# SapaChat 💬

SapaChat adalah aplikasi pesan *real-time* modern yang dibangun khusus untuk Android menggunakan **Kotlin** dan **Jetpack Compose**. Didesain dengan antarmuka yang bersih dan minimalis, SapaChat menawarkan pengalaman komunikasi interaktif, mulai dari pesan teks privat, berbagi media, hingga *voice note*.

## ✨ Fitur Utama

* **Pesan Real-Time:** Sinkronisasi *chat* super cepat yang ditenagai oleh Firebase Firestore.
* **Berbagi Media:** Kirim gambar secara instan dari galeri atau ambil langsung dari kamera.
* **Voice Notes:** Rekam dan putar pesan suara langsung di dalam gelembung obrolan.
* **Swipe-to-Reply:** Fitur geser (*swipe*) intuitif untuk membalas pesan tertentu, layaknya aplikasi *chat* komersial populer.
* **Reaksi Interaktif:** Ketuk dua kali atau klik ikon hati untuk menyukai pesan, lengkap dengan animasi hati mengambang yang memuaskan.
* **Indikator Mengetik (Typing):** Ketahui kapan temanmu sedang mengetik balasan secara *live*.
* **Tarik Pesan (Unsend):** Salah ketik? Tekan lama pesanmu untuk menghapusnya bagi semua orang di ruang obrolan.
* **Manajemen Profil:** Profil pengguna yang dapat disesuaikan sepenuhnya dengan nama tampilan dan foto profil.

## 🛠️ Teknologi & Arsitektur

* **UI Framework:** [Jetpack Compose](https://developer.android.com/jetpack/compose) untuk antarmuka pengguna yang sepenuhnya deklaratif dan reaktif.
* **Bahasa Pemrograman:** 100% Kotlin.
* **Backend & Database:** [Firebase](https://firebase.google.com/) (Authentication & Cloud Firestore) untuk login yang aman dan sinkronisasi data *real-time*.
* **Penyimpanan Media:** API [Cloudinary](https://cloudinary.com/) untuk memproses unggahan gambar/audio dengan cepat dan optimal.
* **Image Loading:** [Coil](https://coil-kt.github.io/coil/) untuk memuat gambar secara asinkron dengan mulus.
* **Navigasi:** Jetpack Navigation Compose untuk perpindahan antarlayar yang lancar.

## 🚀 Cara Memulai

Untuk membangun dan menjalankan proyek ini di perangkatmu:
1. *Clone* repositori ini.
2. Buka proyek di **Android Studio**.
3. Hubungkan proyek dengan Firebase:
    * Buat proyek baru di Firebase Console.
    * Tambahkan file `google-services.json` milikmu ke dalam direktori `app/`.
    * Aktifkan **Authentication** (Email/Password) dan **Firestore Database**.
4. Atur konfigurasi Cloudinary di dalam aplikasi untuk keperluan unggah media.
5. *Build* dan jalankan aplikasi pada emulator atau perangkat fisik (HP Android).

---
*Dibuat dengan ❤️ menggunakan Android Studio dan Jetpack Compose.*