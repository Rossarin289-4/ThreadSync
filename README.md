# ThreadSync: Thread Synchronization & Race Condition Visualizer

Mini Project สำหรับเรียนรู้ Thread, Multithreading, Shared Resource, Race Condition และ Synchronization ผ่าน Web Visualization

## Technology Stack
- Java 17+ (โค้ดใช้ Java Standard Library)
- `com.sun.net.httpserver.HttpServer` สำหรับ Web Server ขนาดเล็ก
- `java.util.concurrent` สำหรับ Thread/Executor/Barrier
- `ReentrantLock` สำหรับ Synchronization Mode
- HTML/CSS/JavaScript สำหรับ Visualization
- ไม่มี External Dependency และไม่มี Maven/Gradle ที่ต้องดาวน์โหลดเพิ่ม

## Requirements
- JDK 17 หรือใหม่กว่า
- Browser สมัยใหม่ เช่น Chrome/Safari/Firefox
- macOS/Linux/Windows

> Prototype นี้ตั้งใจให้เหมาะกับ environment ที่อาจไม่มี Internet หรือไม่สามารถติดตั้ง dependency เพิ่มได้

## Run
จากโฟลเดอร์ `ThreadSync`:

```bash
chmod +x scripts/run.sh
./scripts/run.sh
```

จากนั้นเปิด `http://localhost:8080`

หยุดด้วย `Ctrl+C`

## How to Use
1. กำหนด Number of Threads (2–8)
2. กำหนด Iterations ต่อ Thread (1–1000)
3. กำหนด Delay (0–100 ms)
4. เลือก `Without Synchronization` เพื่อสังเกต Read-Modify-Write ที่ไม่ถูกป้องกัน
5. เลือก `With Synchronization` เพื่อสังเกต Lock และ Critical Section
6. ดู Counter, Thread Status, Lock Holder และ Event Log
7. กด Reset เพื่อกลับสู่สถานะพร้อมทดลองใหม่

## Race Condition Mode
Shared counter ถูกอ่าน เก็บค่าไว้คำนวณ และเขียนกลับโดยไม่มี Lock ระหว่าง critical sequence ดังนั้นหลาย Thread อาจอ่านค่าเดียวกันแล้วเขียนทับกัน เกิด Lost Update ได้ ผล Actual Result ต้องมาจากการ Run จริงและอาจไม่ผิดพลาดทุกครั้ง

## Synchronization Mode
ใช้ `ReentrantLock` ครอบ Critical Section ทำให้มี Thread เดียวในช่วง Read-Modify-Write ต่อครั้ง Thread อื่นจะแสดงสถานะ WAITING และมีการแสดง Lock Holder

## Testing
ดู `docs/testing-results.md` และ `scripts/run_tests.sh` ผลในเอกสารจะเป็นผลจากการ Run จริงของ environment ที่สร้าง prototype นี้

## Project Structure
```text
ThreadSync/
├── src/threadsync/Main.java
├── src/web/index.html
├── src/web/app.css
├── src/web/app.js
├── tests/ThreadSyncTest.java
├── scripts/run.sh
├── scripts/run_tests.sh
├── docs/testing-results.md
├── docs/architecture.svg
└── report/ThreadSync_Progress_Report.docx
```

## Academic References
อ้างอิงที่ใช้ในรายงานมาจาก Oracle Java documentation และ Java Language Specification โดยตรวจสอบ URL ก่อนจัดทำรายงาน
