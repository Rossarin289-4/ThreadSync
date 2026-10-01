# ThreadSync Testing Results

ผลด้านล่างเป็นผลจากการรันจริงใน environment ที่สร้าง Prototype นี้ (Linux container, OpenJDK 21) โดย compile ด้วย `javac --release 17` เพื่อรักษาความเข้ากันได้กับ JDK 17

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
