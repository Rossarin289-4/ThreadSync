# ThreadSync Testing Results

ผลด้านล่างเป็นผลเดิมจาก Prototype ก่อนเพิ่ม Thread Inspector ซึ่งรันใน Linux container ด้วย OpenJDK 21 และ compile ด้วย `javac --release 17` ผลตัวเลข No Sync เป็นเพียงตัวอย่างจากหนึ่ง run ไม่ใช่ผลที่รับประกันในทุกครั้ง

| Test Case | Input | Expected Result | Actual Result | Status |
|---|---|---|---|---|
| TC-01 Without Synchronization | 3 threads, 20 iterations/thread, 2 ms delay | Run completes; Actual may differ from 60 | Actual = 22, Expected = 60; Race flag = true | PASS |
| TC-02 With Synchronization | 3 threads, 20 iterations/thread, 1 ms delay | Actual = 60 | Actual = 60, Expected = 60; Race flag = false | PASS |
| TC-03 Reset | Reset after experiment | State returns to ready state | API returned `ok=true` | PASS |
| TC-04 Run multiple times | Two sequential runs in TC-01/02 | Each run completes independently | Completed successfully | PASS |
| TC-05 Change number of threads | 3 threads (tested baseline) | Expected = threads × iterations | Expected calculated as 60 | PASS |
| TC-06 Change iterations | 20 iterations (tested baseline) | Expected = threads × iterations | Expected calculated as 60 | PASS |

## Notes
- The Race Condition result is not hard-coded. The observed run produced 22 instead of 60.
- Race Condition is timing/interleaving dependent and therefore should not be claimed to occur on every run.
- The GUI was also opened in headless Chromium to verify that the web page can render; no manual desktop-GUI interaction was performed in the headless environment.


การตรวจ Thread Inspector ที่เพิ่มใหม่: test script ตรวจโครงสร้าง history ราย Thread และ event การรอ/ได้/ปล่อย Lock เมื่อรันบนเครื่องที่ติดตั้ง JDK 17+; environment สำหรับจัดแพ็กครั้งนี้ไม่มี `javac` จึงยังไม่ได้รัน Java integration test ซ้ำหลังการแก้ไข

## Final UI and runtime pass

หลังปรับหน้าเว็บภาษาไทยและ Fair ReentrantLock ได้รัน API integration บน OpenJDK 17.0.20 ด้วย Java source-file launcher:

| Test | Result |
|---|---|
| ReentrantLock 3 × 4, Counter = Expected | PASS |
| History and Lock Owner identity in per-thread events | PASS |
| Compare 3 × 3, same input and independent states | PASS |
| Pause → Next Step while paused → Resume | PASS |
| Static UI and theme assets | PASS |

หมายเหตุ: runtime launcher คอมไพล์ source ให้โดย JDK 17 แต่คำสั่ง `javac --release 17` ยังไม่ได้รันแยกใน environment นี้
