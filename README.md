# 🧵 ThreadSync
### Thread Synchronization & Race Condition Visualizer

> เว็บจำลองการทำงานของ Java Thread เพื่อให้เห็นความแตกต่างระหว่าง  
> **Without Synchronization** และ **With ReentrantLock** อย่างเข้าใจง่าย

![Java](https://img.shields.io/badge/Java-17%2B-orange?logo=openjdk)
![Platform](https://img.shields.io/badge/Platform-Windows%20%7C%20macOS%20%7C%20Linux-blue)
![Dependencies](https://img.shields.io/badge/Dependencies-None-success)

---

## ✨ จุดเด่นของโปรเจกต์

- 🧪 ทดลอง Race Condition จากการทำงานจริงของ Java Worker Thread
- 🔒 เปรียบเทียบ `Without Synchronization` กับ `With ReentrantLock`
- ⚖️ Compare Mode แสดงผลทั้งสองฝั่งแบบ side-by-side
- 🔍 **Thread Inspector** — กดการ์ด Thread เพื่อดูประวัติการทำงานราย Thread
- 🕒 Timeline ของ Event เช่น `READ`, `MODIFY`, `WRITE`, `WAIT`, `LOCK`
- ⚠️ ตรวจจับและอธิบาย **Lost Update** จากข้อมูลจริง
- 👤 แสดงว่า Thread ใดกำลังถือ Lock และ Thread ใดกำลังรอ
- ⏯️ ควบคุมการทดลองด้วย Pause, Resume และ Next Step
- 📥 Export ผลลัพธ์เป็น JSON และ CSV
- 🇹🇭 ทุกสถานะรองรับภาษาไทย + English

---

## 🎯 สิ่งที่เรียนรู้ได้

| Without Synchronization | With ReentrantLock |
|---|---|
| หลาย Thread อ่านและเขียนค่าเดียวกันพร้อมกันได้ | เข้า Critical Section ได้ทีละ Thread |
| อาจเกิด Race Condition | ป้องกัน Race Condition |
| อาจเกิด Lost Update | ค่าผลลัพธ์ถูกต้องตาม Expected |
| Shared Counter อาจน้อยกว่าที่ควรเป็น | Shared Counter ควรเท่ากับ Expected |

**สูตรผลลัพธ์ที่ควรได้**

```text
Expected Counter = จำนวน Thread × จำนวน Iterations
```

หาก `Shared Counter < Expected Counter` ใน No Sync Mode  
แสดงว่าเกิด **Lost Update** จาก Race Condition

> ผลแต่ละรอบอาจต่างกันได้ เพราะขึ้นอยู่กับจังหวะการทำงานจริงของ Thread ใน Java

---

## 🚀 วิธีเปิดใช้งาน

### สิ่งที่ต้องมี

- ติดตั้ง **JDK 17 หรือใหม่กว่า**
- Browser เช่น Chrome, Edge หรือ Firefox

ตรวจสอบ Java:

```powershell
java -version
javac -version
```

### Windows

เปิด PowerShell หรือ Command Prompt ในโฟลเดอร์โปรเจกต์ แล้วรัน:

```powershell
.\scripts\run.bat
```

จากนั้นเปิด Browser ที่:

```text
http://localhost:8080
```

หยุดโปรแกรมด้วย:

```text
Ctrl + C
```

### macOS / Linux

```bash
chmod +x scripts/run.sh
./scripts/run.sh
```

แล้วเปิด:

```text
http://localhost:8080
```

---

## 🧭 วิธีทดลอง

1. เลือกจำนวน **Threads** และ **Iterations**
2. กำหนด **Delay** เพื่อทำให้เห็นการแย่งใช้ข้อมูลชัดขึ้น
3. เลือก Mode ที่ต้องการ
   - `Without Synchronization`
   - `With ReentrantLock`
   - `Compare Mode`
4. กด Start
5. ดูค่า `Expected Counter` เทียบกับ `Shared Counter`
6. กดการ์ด Thread เพื่อเปิด **Thread Inspector**
7. ใช้ Filter เพื่อดู Event ที่สนใจ เช่น READ, WRITE, WAIT หรือ LOST UPDATE

---

## 🔍 Thread Inspector

แต่ละ Thread สามารถเปิดดูประวัติจริงได้ เช่น:

```text
CREATED
→ READ
→ MODIFY
→ WRITE
→ COMPLETED
```

เมื่อใช้ ReentrantLock จะเห็นเพิ่ม:

```text
REQUEST LOCK
→ WAITING FOR LOCK
→ ACQUIRED LOCK
→ RUNNING
→ RELEASE LOCK
```

หาก Thread รอ Lock ระบบจะแสดงตัวอย่างเช่น:

```text
กำลังรอ Thread-2 / Waiting for Thread-2
```

หากเกิด Lost Update จะแสดง:

- ค่าที่ Thread อ่าน
- ค่าที่ตั้งใจเขียน
- Shared Counter ก่อน/หลังเขียน
- Thread ที่เขียนแทรกระหว่าง READ และ WRITE

---

## 🧪 การทดสอบ

```bash
./scripts/run_tests.sh
```

ผลทดสอบดูได้ที่:

```text
docs/testing-results.md
```

---

## 🗂️ โครงสร้างโปรเจกต์

```text
ThreadSync/
├── src/
│   ├── threadsync/Main.java
│   └── web/
│       ├── index.html
│       ├── app.js
│       ├── app.css
│       ├── theme.css
│       └── polish.css
├── scripts/
│   ├── run.bat
│   ├── run.ps1
│   ├── run.sh
│   └── run_tests.sh
├── tests/
│   └── ThreadSyncTest.java
└── docs/
    ├── architecture.svg
    ├── testing-results.md
    └── verification.md
```

---

<p align="center">
  Built with Java 17 • Designed for learning Thread Synchronization
</p>
