import threadsync.Main;

/**
 * ThreadSyncTest เป็น compile-level smoke test เบื้องต้นของโปรเจกต์
 * ส่วน integration test ที่ยิง HTTP API และตรวจ simulation จริงอยู่ใน scripts/run_tests.sh
 */
public class ThreadSyncTest {
    public static void main(String[] args) {
        // ถ้า class นี้ compile พร้อม Main ได้ แปลว่าโครงสร้าง source หลักพร้อมสำหรับ integration test ขั้นต่อไป
        System.out.println("ThreadSyncTest: compile-level test class loaded successfully.");
    }
}
