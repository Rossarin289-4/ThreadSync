package threadsync;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Main เป็นจุดเริ่มต้นของ ThreadSync และเป็น HTTP server ขนาดเล็กของโปรเจกต์
 * หน้าที่หลักคือเปิด Web UI และเชื่อม request จาก Browser เข้ากับ SimulationController
 */
public class Main {
    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        SimulationController controller = new SimulationController();
        CompareController compareController = new CompareController();
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // Static files ของหน้าเว็บ
        server.createContext("/", ex -> serveFile(ex, "index.html", "text/html; charset=utf-8"));
        server.createContext("/app.css", ex -> serveFile(ex, "app.css", "text/css; charset=utf-8"));
        server.createContext("/theme.css", ex -> serveFile(ex, "theme.css", "text/css; charset=utf-8"));
        server.createContext("/polish.css", ex -> serveFile(ex, "polish.css", "text/css; charset=utf-8"));
        server.createContext("/app.js", ex -> serveFile(ex, "app.js", "application/javascript; charset=utf-8"));

        // API สำหรับอ่านสถานะปัจจุบันของการทดลอง
        server.createContext("/api/state", ex -> json(ex, 200, controller.snapshot()));

        // API สำหรับเริ่มการทดลอง โดย validate ค่าแทนการ clamp แบบเงียบ ๆ
        server.createContext("/api/start", ex -> {
            if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) { methodNotAllowed(ex); return; }
            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String,String> p = parseJson(body);
            try {
                int threads = requiredInt(p, "threads", 2, 8);
                int iterations = requiredInt(p, "iterations", 1, 1000);
                int delay = requiredInt(p, "delay", 0, 2000);
                String mode = p.getOrDefault("mode", "race");
                if (!mode.equals("race") && !mode.equals("sync")) throw new IllegalArgumentException("Mode must be race or sync");
                controller.start(threads, iterations, delay, mode);
                json(ex, 200, "{\"ok\":true}");
            } catch (Exception e) {
                json(ex, 400, "{\"ok\":false,\"error\":" + quote(e.getMessage()) + "}");
            }
        });

        server.createContext("/api/control", ex -> {
            if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) { methodNotAllowed(ex); return; }
            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String,String> p = parseJson(body);
            String action = p.getOrDefault("action", "");
            if ("pause".equals(action)) controller.pause();
            else if ("resume".equals(action)) controller.resume();
            else if ("step".equals(action)) controller.step();
            else { json(ex,400,"{\"ok\":false,\"error\":\"Unknown control action\"}"); return; }
            json(ex,200,"{\"ok\":true}");
        });

        // Compare API: รัน No Sync และ ReentrantLock พร้อมกันด้วย input ชุดเดียวกัน
        server.createContext("/api/compare/start", ex -> {
            if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) { methodNotAllowed(ex); return; }
            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String,String> p = parseJson(body);
            try {
                int threads = requiredInt(p, "threads", 2, 8);
                int iterations = requiredInt(p, "iterations", 1, 1000);
                int delay = requiredInt(p, "delay", 0, 2000);
                compareController.start(threads, iterations, delay);
                json(ex, 200, "{\"ok\":true}");
            } catch (Exception e) { json(ex,400,"{\"ok\":false,\"error\":"+quote(e.getMessage())+"}"); }
        });
        server.createContext("/api/compare/state", ex -> json(ex,200,compareController.snapshot()));
        server.createContext("/api/compare/control", ex -> {
            if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) { methodNotAllowed(ex); return; }
            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String action = parseJson(body).getOrDefault("action","");
            compareController.control(action);
            json(ex,200,"{\"ok\":true}");
        });

        server.createContext("/api/reset", ex -> {
            if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) { methodNotAllowed(ex); return; }
            if (!controller.reset()) {
                json(ex, 409, "{\"ok\":false,\"error\":\"Cannot reset while a simulation is running\"}");
                return;
            }
            json(ex, 200, "{\"ok\":true}");
        });

        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("ThreadSync running at http://localhost:" + port);
        System.out.println("Press Ctrl+C to stop.");
    }

    // แปลงและตรวจ input จากหน้าเว็บให้ตรงกับขอบเขตของโปรเจกต์
    static int requiredInt(Map<String,String> p, String key, int min, int max) {
        int n;
        try { n = Integer.parseInt(p.getOrDefault(key, "")); }
        catch (NumberFormatException e) { throw new IllegalArgumentException(key + " must be a number"); }
        if (n < min || n > max) throw new IllegalArgumentException(key + " must be between " + min + " and " + max);
        return n;
    }

    // Parser นี้เพียงพอสำหรับ JSON แบบง่ายที่ UI ของโปรเจกต์ส่งมา (key/value ไม่มี object ซ้อน)
    static Map<String,String> parseJson(String s) {
        Map<String,String> m = new HashMap<>();
        String x = s.trim().replaceAll("[{}\\\"]", "");
        for (String part : x.split(",")) {
            String[] kv = part.split(":",2);
            if (kv.length==2) m.put(kv[0].trim(), kv[1].trim());
        }
        return m;
    }

    static String quote(String s) {
        if (s==null) s="";
        return "\""+s.replace("\\","\\\\").replace("\"","\\\"").replace("\n"," ")+"\"";
    }

    static void json(HttpExchange ex, int status, String body) throws IOException {
        byte[] b=body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type","application/json; charset=utf-8");
        ex.sendResponseHeaders(status,b.length);
        try(OutputStream o=ex.getResponseBody()){o.write(b);}
    }

    static void methodNotAllowed(HttpExchange ex)throws IOException { ex.sendResponseHeaders(405,-1); ex.close(); }

    static void serveFile(HttpExchange ex,String name,String type)throws IOException {
        Path p=Paths.get("src","web",name);
        byte[] b=Files.readAllBytes(p);
        ex.getResponseHeaders().set("Content-Type",type);
        ex.sendResponseHeaders(200,b.length);
        try(OutputStream o=ex.getResponseBody()){o.write(b);}
    }

    /**
     * SimulationController ควบคุม lifecycle ของการทดลอง
     * ป้องกันไม่ให้ Start ซ้อนกัน และเป็นตัวกลางระหว่าง HTTP API กับ Simulation จริง
     */
    static final class SimulationController {
        private final Object stateLock = new Object();
        private SimulationState state = SimulationState.idle();
        private final AtomicBoolean running = new AtomicBoolean(false);
        private Simulation activeSimulation;

        void start(int threads, int iterations, int delay, String mode) {
            if (!running.compareAndSet(false,true)) throw new IllegalStateException("A simulation is already running");
            SimulationState s = new SimulationState(threads, iterations, delay, mode);
            synchronized(stateLock){ state=s; }

            // แยก simulation ออกจาก HTTP thread เพื่อให้ Browser ยังอ่านสถานะสดได้ระหว่างรัน
            Thread t = new Thread(() -> {
                try { activeSimulation = new Simulation(s); activeSimulation.run(); }
                finally { running.set(false); s.running=false; }
            }, "ThreadSync-Simulation");
            t.start();
        }

        boolean reset() {
            if (running.get()) return false;
            synchronized(stateLock){ state=SimulationState.idle(); }
            return true;
        }

        boolean isRunning(){ return running.get(); }
        void pause(){ if(activeSimulation!=null) activeSimulation.pauseExecution(); }
        void resume(){ if(activeSimulation!=null) activeSimulation.resumeExecution(); }
        void step(){ if(activeSimulation!=null) activeSimulation.stepExecution(); }
        String snapshot() { synchronized(stateLock){ return state.toJson(); } }
    }

    /** CompareController แยก Simulation สองชุด เพื่อให้หน้า Compare แสดงสถานะสดพร้อมกันได้ */
    static final class CompareController {
        final SimulationController race = new SimulationController();
        final SimulationController sync = new SimulationController();
        void start(int t,int i,int d) {
            if (race.isRunning() || sync.isRunning()) throw new IllegalStateException("Comparison is already running");
            race.start(t,i,d,"race"); sync.start(t,i,d,"sync");
        }
        void control(String a) {
            if("pause".equals(a)){race.pause();sync.pause();}
            else if("resume".equals(a)){race.resume();sync.resume();}
            else if("step".equals(a)){race.step();sync.step();}
        }
        String snapshot(){ return "{\"race\":"+race.snapshot()+",\"sync\":"+sync.snapshot()+"}"; }
    }

    /**
     * ThreadState เก็บสถานะของ Worker แต่ละตัวแยกกัน
     * ทำให้ UI แสดงได้ว่า Thread นั้นกำลัง READ / MODIFY / WRITE / WAITING / LOCKED และทำไปกี่รอบแล้ว
     */
    static final class ThreadState {
        final String name;
        volatile String status = "READY";
        volatile String phase = "READY";
        volatile int iteration = 0;
        volatile int localValue = 0;
        volatile int readValue = 0;
        volatile int writeValue = 0;
        volatile int sharedBefore = 0;
        volatile String operation = "Waiting to start";
        volatile String waitingFor = "-";
        final List<Map<String,Object>> history = Collections.synchronizedList(new ArrayList<>());
        volatile int writes=0, successful=0, lost=0, lockWaits=0, completedIterations=0;
        volatile long readSequence=0;
        volatile Thread workerThread;

        ThreadState(String name) { this.name = name; }
    }

    /**
     * SimulationState เป็นข้อมูลกลางที่ Browser อ่านผ่าน /api/state
     * counter คือ Shared Resource จริง ส่วน lostUpdates คำนวณจากจำนวน write ที่ควรเกิดเทียบกับค่าปัจจุบัน
     */
    static final class SimulationState {
        final int threads, iterations, delay, expected;
        final String mode;
        volatile int counter=0;
        volatile boolean running=true, race=false;
        volatile String holder="-";
        volatile int completed=0;
        volatile int lostUpdates=0;
        volatile String insight="พร้อมเริ่มการทดลอง (Ready for the first operation)";
        volatile boolean paused=false;
        final Map<String,ThreadState> threadStates=new LinkedHashMap<>();
        final List<String> events=Collections.synchronizedList(new ArrayList<>());
        final List<String> highlights=Collections.synchronizedList(new ArrayList<>());
        final List<String> lostEvents=Collections.synchronizedList(new ArrayList<>());
        final Map<Integer, Set<String>> readersByValue = new HashMap<>();
        final Object analysisLock = new Object();
        final List<Map<String,Object>> writeLedger = new ArrayList<>();
        final long start=System.nanoTime();
        static final int MAX_EVENTS=600;

        SimulationState(int t,int i,int d,String m) {
            threads=t; iterations=i; delay=d; mode=m; expected=t*i;
            for(int n=1;n<=t;n++) threadStates.put("Thread-"+n,new ThreadState("Thread-"+n));
            add("System","Simulation started ("+("race".equals(m)?"Without Synchronization":"With ReentrantLock")+")");
        }

        static SimulationState idle() {
            SimulationState s=new SimulationState(3,20,10,"race");
            s.running=false; s.events.clear(); s.highlights.clear();
            s.events.add("[00:00.00] System : Ready for a new experiment");
            s.insight="ตั้งค่าการทดลองแล้วกดเริ่มได้เลย (Choose a mode and start the simulation)";
            return s;
        }

        void add(String who,String msg) {
            long ms=(System.nanoTime()-start)/1_000_000;
            synchronized(events) {
                events.add(String.format("[%02d:%02d.%02d] %s : %s",ms/60000,(ms/1000)%60,(ms/10)%100,who,msg));
                if(events.size()>MAX_EVENTS) events.remove(0); // จำกัด log เพื่อไม่ให้ UI หนักเมื่อ iteration สูง
            }
        }

        /** Stores an event emitted by an actual worker operation with its observed values. */
        void record(ThreadState t,String type,String phase,int iteration,int read,int local,int before,int after,String detail) {
            Map<String,Object> e=new LinkedHashMap<>();e.put("ms",(System.nanoTime()-start)/1_000_000);e.put("type",type);e.put("phase",phase);e.put("iteration",iteration);e.put("readValue",read);e.put("localValue",local);e.put("sharedBefore",before);e.put("sharedAfter",after);e.put("detail",detail);
            synchronized(t.history){t.history.add(e);}
        }

        void highlight(String msg) {
            insight=msg;
            synchronized(highlights) {
                highlights.add(msg);
                if(highlights.size()>8) highlights.remove(0);
            }
        }

        // บันทึกว่า Thread ใดอ่านค่าเดียวกัน เพื่ออธิบาย race ให้คนดูเห็นต้นเหตุ
        void recordRead(String thread, int value) {
            synchronized(analysisLock) {
                Set<String> readers=readersByValue.computeIfAbsent(value,k->new LinkedHashSet<>());
                readers.add(thread);
                if("race".equals(mode) && readers.size()>1) {
                    highlight("⚠ " + String.join(" และ ", readers) + " อ่านค่า Counter = " + value + " เหมือนกัน (same value) การเขียนอาจทับกันได้ (writes may overwrite each other)");
                }
            }
        }

        void afterWrite(ThreadState t,int written) {
            t.writes++;
            synchronized(analysisLock) {
                int before=counter;
                counter=written;
                t.sharedBefore=before;
                t.writeValue=written;
                add(t.name,"WRITE shared counter = "+written+" (before="+before+")");
                List<String> intervening=new ArrayList<>();
                for(Map<String,Object> w:writeLedger) if((long)w.get("seq")>t.readSequence) intervening.add((String)w.get("thread"));
                boolean lost="race".equals(mode)&&written<=before;
                writeLedger.add(Map.of("seq",writeLedger.size()+1L,"thread",t.name,"value",written));
                if(lost)t.lost++;else t.successful++;
                String detail=t.name+" อ่าน (read) "+t.readValue+", ตั้งใจเขียน (intended) "+written+", Shared ก่อนเขียน (before) "+before+", หลังเขียน (after) "+written+(lost?" — ข้อมูลอัปเดตหาย (LOST UPDATE); Thread ที่เขียนแทรก (intervening writer): "+(intervening.isEmpty()?"มีการเขียนพร้อมกัน (concurrent write)":String.join(", ",new LinkedHashSet<>(intervening))):" — อัปเดตสำเร็จ (update advanced shared value)");
                record(t,"WRITE","WRITE",t.iteration,t.readValue,t.localValue,before,written,detail);
                if(lost) record(t,"LOST UPDATE","WRITE",t.iteration,t.readValue,t.localValue,before,written,detail);
                if(lost){lostUpdates++;lostEvents.add(detail);highlight("💥 LOST UPDATE: "+detail);}
            }
        }

        String esc(String s){return s.replace("\\","\\\\").replace("\"","\\\"").replace("\n"," ").replace("\r"," ").replace("\t"," ");}

        String toJson() {
            StringBuilder b=new StringBuilder("{");
            b.append("\"running\":").append(running)
             .append(",\"mode\":\"").append(mode).append("\",")
             .append("\"threadsConfigured\":").append(threads)
             .append(",\"iterations\":").append(iterations)
             .append(",\"delay\":").append(delay)
             .append(",\"counter\":").append(counter)
             .append(",\"expected\":").append(expected)
             .append(",\"lostUpdates\":").append(lostUpdates)
             .append(",\"paused\":").append(paused)
             .append(",\"race\":").append(race)
             .append(",\"holder\":\"").append(esc(holder)).append("\",")
             .append("\"completed\":").append(completed)
             .append(",\"total\":").append(expected)
             .append(",\"insight\":\"").append(esc(insight)).append("\",")
             .append("\"threads\":[");
            int i=0;
            for(ThreadState t:threadStates.values()) {
                if(i++>0)b.append(',');
                b.append("{\"name\":\"").append(t.name)
                 .append("\",\"status\":\"").append(esc(t.status))
                 .append("\",\"phase\":\"").append(esc(t.phase))
                 .append("\",\"iteration\":").append(t.iteration)
                 .append(",\"localValue\":").append(t.localValue)
                 .append(",\"readValue\":").append(t.readValue)
                 .append(",\"writeValue\":").append(t.writeValue)
                 .append(",\"sharedBefore\":").append(t.sharedBefore)
                 .append(",\"operation\":\"").append(esc(t.operation))
                 .append("\",\"waitingFor\":\"").append(esc(t.waitingFor)).append("\"")
                 .append(",\"writes\":").append(t.writes).append(",\"successful\":").append(t.successful).append(",\"lost\":").append(t.lost).append(",\"lockWaits\":").append(t.lockWaits).append(",\"completedIterations\":").append(t.completedIterations).append(",\"history\":[");
                synchronized(t.history){for(int j=0;j<t.history.size();j++){if(j>0)b.append(',');Map<String,Object> e=t.history.get(j);b.append("{\"ms\":").append(e.get("ms")).append(",\"type\":\"").append(esc((String)e.get("type"))).append("\",\"phase\":\"").append(esc((String)e.get("phase"))).append("\",\"iteration\":").append(e.get("iteration")).append(",\"readValue\":").append(e.get("readValue")).append(",\"localValue\":").append(e.get("localValue")).append(",\"sharedBefore\":").append(e.get("sharedBefore")).append(",\"sharedAfter\":").append(e.get("sharedAfter")).append(",\"detail\":\"").append(esc((String)e.get("detail"))).append("\"}");}}
                b.append("]}");
            }
            b.append("],\"highlights\":[");
            synchronized(highlights){for(i=0;i<highlights.size();i++){if(i>0)b.append(',');b.append("\"").append(esc(highlights.get(i))).append("\"");}}
            b.append("],\"lostEvents\":[");
            synchronized(lostEvents){for(i=0;i<lostEvents.size();i++){if(i>0)b.append(',');b.append("\"").append(esc(lostEvents.get(i))).append("\"");}}
            b.append("],\"events\":[");
            synchronized(events){for(i=0;i<events.size();i++){if(i>0)b.append(',');b.append("\"").append(esc(events.get(i))).append("\"");}}
            b.append("]}");
            return b.toString();
        }
    }

    /**
     * Simulation คือส่วนที่สร้าง Java Worker Threads จริงด้วย ExecutorService
     * raceStep จงใจไม่ใช้ lock ส่วน syncStep ใช้ ReentrantLock ครอบ Critical Section เพื่อให้เปรียบเทียบกันได้
     */
    static final class Simulation {
        final SimulationState s;
        /** Fair lock: workers already queued are favored in the order they requested entry. */
        final FairReentrantLock lock=new FairReentrantLock();
        final CyclicBarrier startBarrier;
        final Object controlLock = new Object();
        volatile boolean paused = false;
        int stepPermits = 0;

        Simulation(SimulationState s){ this.s=s; startBarrier=new CyclicBarrier(s.threads); }

        void pauseExecution(){ synchronized(controlLock){ paused=true; s.paused=true; s.highlight("⏸ หยุดการทดลองชั่วคราว (PAUSED) กดทำทีละขั้นหรือทำต่อได้"); } }
        void resumeExecution(){ synchronized(controlLock){ paused=false; s.paused=false; stepPermits=0; controlLock.notifyAll(); s.highlight("▶ ทำการทดลองต่อ (RESUMED)"); } }
        void stepExecution(){ synchronized(controlLock){ paused=true; s.paused=true; stepPermits++; controlLock.notifyAll(); } }
        void checkpoint() {
            synchronized(controlLock) {
                while(paused && stepPermits==0) try { controlLock.wait(); } catch(InterruptedException e){ Thread.currentThread().interrupt(); return; }
                if(paused && stepPermits>0) stepPermits--;
            }
        }

        void run() {
            ExecutorService pool=Executors.newFixedThreadPool(s.threads);
            List<Future<?>> fs=new ArrayList<>();
            for(int n=1;n<=s.threads;n++){ final int id=n; fs.add(pool.submit(()->worker(id))); }
            for(Future<?> f:fs) try{ f.get(); } catch(Exception e){ s.add("System","Worker error: "+e.getMessage()); }
            pool.shutdown();
            s.running=false; s.holder="-";
            for(ThreadState t:s.threadStates.values()){t.status="COMPLETED";t.phase="DONE";}
            s.race=s.counter!=s.expected;
            s.lostUpdates=Math.max(s.lostUpdates,Math.max(0,s.expected-s.counter));
            if(s.race) s.highlight("ผลการทดลอง (RESULT): ค่าที่ควรได้ " + s.expected + " แต่ได้จริง " + s.counter + " • ตรวจพบค่าที่หาย " + s.lostUpdates + " (LOST UPDATE)");
            else s.highlight("ผลการทดลอง (RESULT): ไม่พบค่าที่หาย (NO LOST UPDATE) • " + ("sync".equals(s.mode)?"ReentrantLock ป้องกันส่วนสำคัญแล้ว (protected Critical Section)":"รอบนี้จังหวะทำงานไม่ทำให้ค่าแตกต่าง (this run had no discrepancy)"));
            s.add("System","Finished. Expected="+s.expected+", Actual="+s.counter+(s.race?" -> Race Condition detected":" -> No discrepancy detected"));
        }

        void worker(int id) {
            String name="Thread-"+id;
            ThreadState ts=s.threadStates.get(name);
            ts.workerThread=Thread.currentThread();
            try {
                s.record(ts,"CREATED","READY",0,0,0,s.counter,s.counter,"สร้าง Worker Thread และรอเริ่มพร้อมกัน (Created worker; waiting at start barrier)");
                ts.status="READY"; ts.phase="BARRIER";
                startBarrier.await(); // ทำให้ worker เริ่มใกล้กันเพื่อให้เห็น concurrency ชัดขึ้น
                for(int i=1;i<=s.iterations;i++) {
                    ts.iteration=i;
                    if("sync".equals(s.mode)) syncStep(ts); else raceStep(ts);
                    synchronized(s){ s.completed++; }
                    ts.completedIterations++;
                }
            } catch(Exception e) { s.add(name,"Stopped: "+e.getMessage()); }
            finally { ts.status="COMPLETED"; ts.phase="DONE"; s.record(ts,"COMPLETED","DONE",ts.iteration,ts.readValue,ts.localValue,s.counter,s.counter,"ทำงานครบ "+ts.completedIterations+" รอบ (Worker completed "+ts.completedIterations+" iteration(s))"); }
        }

        void pause(int ms){ if(ms<=0)return; try{Thread.sleep(ms);}catch(InterruptedException e){Thread.currentThread().interrupt();} }

        private String logicalOwnerName() {
            Thread owner=lock.currentOwner();
            if(owner==null)return null;
            for(ThreadState t:s.threadStates.values())if(t.workerThread==owner)return t.name;
            return "Lock Owner";
        }

        // ไม่มี Lock: READ และ WRITE ของหลาย Thread จึงสามารถแทรกสลับกันและเกิด Lost Update ได้
        void raceStep(ThreadState ts) {
            checkpoint();
            ts.status="RUNNING"; ts.phase="READ"; ts.waitingFor="-";
            s.record(ts,"ITERATION","RUNNING",ts.iteration,ts.readValue,ts.localValue,s.counter,s.counter,"เริ่มรอบที่ "+ts.iteration+" (Iteration started)");
            int local;
            synchronized(s.analysisLock){local=s.counter;ts.readSequence=s.writeLedger.size();}
            ts.readValue=local; ts.sharedBefore=local; ts.operation="READ: Shared Counter " + local + " → Local Value";
            ts.writeValue=local+1;
            ts.localValue=local;
            s.add(ts.name,"READ shared counter = "+local);
            s.record(ts,"READ","READ",ts.iteration,local,local,s.counter,s.counter,"อ่าน Shared Counter มาเก็บเป็น Local Value (Read shared counter into local value)");
            s.recordRead(ts.name,local);
            pause(s.delay);

            checkpoint();
            ts.phase="MODIFY";
            int oldLocal=local; local=local+1; ts.operation="MODIFY: " + oldLocal + " + 1 = " + local;
            ts.localValue=local;
            s.record(ts,"MODIFY","MODIFY",ts.iteration,ts.readValue,local,s.counter,s.counter,"บวก Local Value ขึ้น 1 (Local value incremented by 1)");
            s.add(ts.name,"MODIFY local value -> "+local);
            pause(s.delay);

            checkpoint();
            ts.phase="WRITE";
            ts.writeValue=local; ts.operation="WRITE: Local Value " + local + " → Shared Counter";
            s.afterWrite(ts,local);
            pause(Math.max(1,s.delay/2));
        }

        // Fair lock serves queued workers in request order; the first worker depends on runtime scheduling.
        void syncStep(ThreadState ts) {
            checkpoint();
            ts.status="WAITING"; ts.phase="WAIT LOCK"; ts.waitingFor="-";
            ts.operation="WAIT: waiting for " + ts.waitingFor + " to release ReentrantLock";
            s.record(ts,"REQUEST LOCK","WAIT LOCK",ts.iteration,ts.readValue,ts.localValue,s.counter,s.counter,"ขอเข้า Lock (Requested ReentrantLock)");
            String firstOwner=logicalOwnerName();
            if(firstOwner!=null) {
                ts.lockWaits++;
                ts.waitingFor=firstOwner; ts.operation="WAIT: "+firstOwner+" is inside the Critical Section";
                s.record(ts,"WAITING FOR LOCK","WAIT LOCK",ts.iteration,ts.readValue,ts.localValue,s.counter,s.counter,"กำลังรอ "+firstOwner+" / Waiting for "+firstOwner+" (Lock Owner)");
            }
            lock.lock(); // Fair ReentrantLock queues contending workers; no busy-spin or barging tryLock.
            try {
                s.holder=ts.name;
                checkpoint();
                ts.status="LOCKED"; ts.phase="READ"; ts.waitingFor="-";
                s.record(ts,"ACQUIRED LOCK","READ",ts.iteration,ts.readValue,ts.localValue,s.counter,s.counter,"ได้ Lock แล้ว (ACQUIRED)");
                ts.status="RUNNING";
                s.record(ts,"RUNNING","READ",ts.iteration,ts.readValue,ts.localValue,s.counter,s.counter,"เริ่มทำงานใน Critical Section (RUNNING after ACQUIRED)");
                s.add(ts.name,"ACQUIRED LOCK -> entered Critical Section");
                int local=s.counter; ts.readValue=local; ts.sharedBefore=s.counter; ts.operation="READ: Shared Counter " + local + " → Local Value";
                ts.writeValue=local+1;
                synchronized(s.analysisLock){ts.readSequence=s.writeLedger.size();}
                ts.localValue=local;
                s.add(ts.name,"READ shared counter = "+local);
                s.record(ts,"READ","READ",ts.iteration,local,local,s.counter,s.counter,"อ่าน Shared Counter ภายในส่วนสำคัญ (Read inside Critical Section)");
                pause(s.delay);

                checkpoint();
                ts.phase="MODIFY";
                int oldLocal=local; local++;
                ts.localValue=local; ts.operation="MODIFY: " + oldLocal + " + 1 = " + local;
                s.record(ts,"MODIFY","MODIFY",ts.iteration,ts.readValue,local,s.counter,s.counter,"บวก Local Value ขึ้น 1 (Local value incremented by 1)");
                s.add(ts.name,"MODIFY local value -> "+local);
                pause(s.delay);

                checkpoint();
                ts.phase="WRITE"; ts.writeValue=local; ts.operation="WRITE: Local Value " + local + " → Shared Counter";
                s.afterWrite(ts,local);
            } finally {
                s.add(ts.name,"RELEASED LOCK -> left Critical Section");
                s.record(ts,"RELEASE LOCK","AFTER WRITE",ts.iteration,ts.readValue,ts.localValue,s.counter,s.counter,"ปล่อย ReentrantLock ให้คิวถัดไป (Released lock)");
                s.holder="-"; ts.status="RUNNING"; ts.phase="AFTER WRITE";
                lock.unlock();
            }
            pause(Math.max(1,s.delay/2));
        }
    }

    /** Exposes the current owner name for an honest WAITING explanation in the UI. */
    static final class FairReentrantLock extends ReentrantLock {
        FairReentrantLock() { super(true); }
        Thread currentOwner() { return getOwner(); }
    }
}
