# Verification Log

ตรวจบน OpenJDK 17.0.20 ด้วย Java source-file launcher ซึ่งคอมไพล์ source และเริ่ม backend จาก `src/threadsync/Main.java` ได้

- หน้าเว็บ, `/theme.css`, `/polish.css`: PASS
- No Sync runtime: PASS
- ReentrantLock runtime: PASS; Counter ตรงตาม Expected
- Per-thread history: PASS สำหรับ CREATED, READ, MODIFY, WRITE, ACQUIRED LOCK, RUNNING, RELEASE LOCK และ COMPLETED
- WAITING FOR LOCK ระบุ logical owner เป็น Thread-X: PASS
- Compare Mode: PASS; input ตรงกัน แต่ state/Counter/Worker แยกชุด
- Pause / Next Step / Resume: PASS; ตรวจว่า Step เพิ่ม event checkpoint โดยยัง paused ก่อน Resume
- Frontend JavaScript syntax (`node --check`): PASS
- `javac --release 17`: ไม่ได้เรียกคำสั่งตรง ๆ เพราะ environment ไม่มี executable `javac`; ใช้ JDK 17 source-file launcher ตรวจการ compile และ HTTP integration แทน

Lost Update ขึ้นกับ interleaving จึงไม่รับประกันจำนวนผลในแต่ละ run; ระบบบันทึกจาก write ที่เกิดจริง
