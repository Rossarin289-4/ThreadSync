# ThreadSync: Thread Synchronization & Race Condition Visualizer

Mini Project สำหรับแสดงการทำงานของ Thread และการเข้าถึง Shared Resource โดยเน้นให้เห็นความแตกต่างระหว่างการทำงานแบบไม่มี Synchronization และการทำงานที่ใช้ Lock

ระบบใช้ Shared Counter เป็นตัวอย่างหลัก และมี Web UI สำหรับกำหนดค่าการทดลองและดูสถานะของแต่ละ Thread ระหว่างการทำงาน

## Technology Stack

* Java 17+
* Java Standard Library
* `com.sun.net.httpserver.HttpServer` สำหรับ Web Server
* `java.util.concurrent` สำหรับ Thread, Executor และ Barrier
* `ReentrantLock` สำหรับ Synchronization Mode
* HTML / CSS / JavaScript สำหรับ Web UI
* ไม่ใช้ External Dependency
* ไม่ใช้ Maven หรือ Gradle

## Requirements

* JDK 17 หรือใหม่กว่า
* Web Browser เช่น Chrome, Safari หรือ Firefox
* macOS / Linux / Windows

Prototype นี้ออกแบบให้สามารถ Compile และ Run ได้ด้วย Java Standard Library โดยไม่ต้องติดตั้ง Library เพิ่ม

## Run

เปิด Terminal แล้วเข้าไปที่โฟลเดอร์ `ThreadSync`

```bash
chmod +x scripts/run.sh
./scripts/run.sh
```

จากนั้นเปิด Browser และเข้า:

```text
http://localhost:8080
```

เมื่อต้องการหยุดโปรแกรม ให้กด:

```text
Ctrl + C
```

## How to Use

1. กำหนดจำนวน Thread ที่ต้องการใช้ (2–8)
2. กำหนดจำนวน Iterations ต่อ Thread (1–1000)
3. กำหนด Delay (0–100 ms)
4. เลือก `Without Synchronization` เพื่อทดลองการเข้าถึง Shared Counter โดยไม่มี Lock
5. เลือก `With Synchronization` เพื่อทดลองการใช้ `ReentrantLock`
6. ดูค่าของ Counter, Expected Result, Actual Result และสถานะของ Thread
7. ดู Lock Holder และ Event Log ระหว่างการทำงาน
8. กด `Reset` เมื่อต้องการเริ่มการทดลองใหม่

## Without Synchronization

ใน Mode นี้ Thread จะทำงานกับ Shared Counter โดยไม่มี Lock ครอบส่วน Read-Modify-Write

เมื่อหลาย Thread อ่านค่า Counter ในช่วงเวลาใกล้กัน อาจเกิดกรณีที่แต่ละ Thread ได้อ่านค่าเดียวกัน แล้วนำค่าที่คำนวณได้กลับมาเขียนทับกัน ทำให้จำนวนครั้งที่เพิ่มจริงน้อยกว่าค่าที่คาดไว้

ผลลัพธ์ของ Mode นี้ขึ้นอยู่กับจังหวะการทำงานของ Thread ดังนั้น Actual Result อาจแตกต่างกันในแต่ละ Run และไม่ได้กำหนดค่าผลลัพธ์ไว้ล่วงหน้า

## With Synchronization

Mode นี้ใช้ `ReentrantLock` เพื่อควบคุมส่วน Critical Section ของการทำงานกับ Shared Counter

Thread ที่ได้รับ Lock จะสามารถทำ Read-Modify-Write ได้ ส่วน Thread อื่นที่ยังไม่ได้รับ Lock จะต้องรอ

หน้า Web UI จะแสดง Thread ที่ถือ Lock ผ่าน `Lock Holder` และแสดงสถานะการรอของ Thread รวมถึง Event ที่เกิดขึ้นระหว่างการทำงาน

## Testing

การทดสอบสามารถรันได้ด้วย:

```bash
./scripts/run_tests.sh
```

ผลการทดสอบที่บันทึกไว้สามารถดูได้ที่:

```text
docs/testing-results.md
```

การทดสอบในเอกสารดังกล่าวเป็นผลจากการ Run Prototype ใน Environment ที่ใช้พัฒนา

## Project Structure

```text
ThreadSync/
├── src/
│   ├── threadsync/
│   │   └── Main.java
│   └── web/
│       ├── index.html
│       ├── app.css
│       └── app.js
├── tests/
│   └── ThreadSyncTest.java
├── scripts/
│   ├── run.sh
│   └── run_tests.sh
├── docs/
│   ├── testing-results.md
│   └── architecture.svg
└── report/
    └── ThreadSync_Progress_Report.docx
```

## Academic References

เอกสารอ้างอิงที่ใช้ประกอบการพัฒนาและจัดทำรายงานมาจาก Oracle Java Documentation และ Java Language Specification

* Oracle — Synchronization
  https://docs.oracle.com/javase/tutorial/essential/concurrency/sync.html

* Oracle — Thread Interference
  https://docs.oracle.com/javase/tutorial/essential/concurrency/interfere.html

* Oracle — Java Language Specification, Java SE 17, Chapter 17: Threads and Locks
  https://docs.oracle.com/javase/specs/jls/se17/html/jls-17.html

* Oracle — ReentrantLock, Java SE 17 API
  https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/locks/ReentrantLock.html
