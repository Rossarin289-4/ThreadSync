# ThreadSync: Thread Synchronization & Race Condition Visualizer

Mini Project สำหรับเรียนรู้การทำงานของ Java Thread, Shared Resource, Race Condition และ ReentrantLock ผ่าน Web UI แบบโต้ตอบได้

จุดสำคัญคือทุก Event และผลลัพธ์มาจากการทำงานจริงของ Java Worker Thread ในแต่ละรอบ ไม่ใช่ Animation หรือข้อมูล hard-code ดังนั้นผลของ Race Condition อาจแตกต่างกันได้ในแต่ละการทดลอง

## Features

- ทดลองแบบ `Without Synchronization` เพื่อดู Race Condition และ Lost Update จริง
- ทดลองแบบ `With ReentrantLock` เพื่อดูการป้องกัน Critical Section
- `Compare Mode` เปรียบเทียบ No Sync และ ReentrantLock แบบ side-by-side
- ใช้ค่า input เดียวกัน แต่แต่ละฝั่งมี Simulation State และ Shared Counter แยกกัน
- แสดงสถานะทุก Thread ทั้งภาษาไทยและอังกฤษ เช่น
  - กำลังอ่าน (READ)
  - กำลังคำนวณ (MODIFY)
  - กำลังเขียน (WRITE)
  - กำลังรอ Lock (WAITING FOR LOCK)
  - ถือ Lock อยู่ (LOCK OWNER)
  - เสร็จสิ้น (COMPLETED)
- Thread Inspector: กดการ์ดของ Thread เพื่อดู Timeline และประวัติราย Thread
- บันทึก Event จริง เช่น `CREATED`, `READ`, `MODIFY`, `WRITE`, `REQUEST LOCK`,
  `WAITING FOR LOCK`, `ACQUIRED LOCK`, `RELEASE LOCK`, `COMPLETED`, `LOST UPDATE`
- เมื่อรอ Lock จะแสดงเจ้าของ Lock เช่น  
  `กำลังรอ Thread-2 / Waiting for Thread-2`
- เมื่อเกิด Lost Update จะแสดง
  - ค่าที่ Thread อ่าน (Read Value)
  - ค่าที่ตั้งใจเขียน (Intended Write Value)
  - Shared Counter ก่อนและหลังเขียน
  - Thread ที่เขียนแทรกระหว่าง READ กับ WRITE
- Filter Timeline ตาม Event: All, READ, MODIFY, WRITE, LOCK, WAIT, LOST UPDATE
- เลือกดูประวัติตาม Iteration ได้
- Summary ต่อ Thread: Writes, Successful Updates, Lost Updates, Lock Waits และ Completed Iterations
- ควบคุมการทดลองด้วย Pause, Resume และ Next Step
- Export ผลลัพธ์เป็น JSON และ CSV

## Technology Stack

- Java 17+
- Java Standard Library
- `com.sun.net.httpserver.HttpServer`
- `java.util.concurrent`
- `ReentrantLock`
- HTML, CSS และ JavaScript
- ไม่ใช้ External Dependency
- ไม่ใช้ Maven หรือ Gradle

## Requirements

- JDK 17 หรือใหม่กว่า
- Web Browser เช่น Chrome, Edge, Firefox หรือ Safari
- Windows, macOS หรือ Linux

## Run on Windows

เปิด PowerShell หรือ Command Prompt ในโฟลเดอร์โปรเจกต์ แล้วรัน:

```bat
scripts\run.bat
