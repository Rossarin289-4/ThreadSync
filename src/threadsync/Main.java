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

public class Main {
    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        SimulationController controller = new SimulationController();
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", ex -> serveFile(ex, "index.html", "text/html; charset=utf-8"));
        server.createContext("/app.css", ex -> serveFile(ex, "app.css", "text/css; charset=utf-8"));
        server.createContext("/app.js", ex -> serveFile(ex, "app.js", "application/javascript; charset=utf-8"));
        server.createContext("/api/state", ex -> json(ex, controller.snapshot()));
        server.createContext("/api/start", ex -> {
            if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) { methodNotAllowed(ex); return; }
            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String,String> p = parseJson(body);
            try {
                int threads = clamp(Integer.parseInt(p.getOrDefault("threads", "3")), 2, 8);
                int iterations = clamp(Integer.parseInt(p.getOrDefault("iterations", "100")), 1, 1000);
                int delay = clamp(Integer.parseInt(p.getOrDefault("delay", "5")), 0, 100);
                String mode = p.getOrDefault("mode", "race");
                controller.start(threads, iterations, delay, mode);
                json(ex, "{\"ok\":true}");
            } catch (Exception e) { json(ex, "{\"ok\":false,\"error\":" + quote(e.getMessage()) + "}"); }
        });
        server.createContext("/api/reset", ex -> {
            controller.reset(); json(ex, "{\"ok\":true}");
        });
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("ThreadSync running at http://localhost:" + port);
        System.out.println("Press Ctrl+C to stop.");
    }

    static int clamp(int n, int min, int max) { return Math.max(min, Math.min(max, n)); }
    static Map<String,String> parseJson(String s) {
        Map<String,String> m = new HashMap<>();
        String x = s.trim().replaceAll("[{}\\\"]", "");
        for (String part : x.split(",")) { String[] kv = part.split(":",2); if (kv.length==2) m.put(kv[0].trim(), kv[1].trim()); }
        return m;
    }
    static String quote(String s) { if (s==null) s=""; return "\""+s.replace("\\","\\\\").replace("\"","\\\"").replace("\n"," ")+"\""; }
    static void json(HttpExchange ex, String body) throws IOException {
        byte[] b=body.getBytes(StandardCharsets.UTF_8); ex.getResponseHeaders().set("Content-Type","application/json; charset=utf-8"); ex.sendResponseHeaders(200,b.length); try(OutputStream o=ex.getResponseBody()){o.write(b);} }
    static void methodNotAllowed(HttpExchange ex)throws IOException{ex.sendResponseHeaders(405,-1);ex.close();}
    static void serveFile(HttpExchange ex,String name,String type)throws IOException{
        Path p=Paths.get("src","web",name); byte[] b=Files.readAllBytes(p); ex.getResponseHeaders().set("Content-Type",type); ex.sendResponseHeaders(200,b.length); try(OutputStream o=ex.getResponseBody()){o.write(b);} }

    static final class SimulationController {
        private final Object stateLock = new Object();
        private SimulationState state = SimulationState.idle();
        private final AtomicBoolean running = new AtomicBoolean(false);
        void start(int threads, int iterations, int delay, String mode) {
            if (!running.compareAndSet(false,true)) throw new IllegalStateException("A simulation is already running");
            String normalized = "sync".equalsIgnoreCase(mode) ? "sync" : "race";
            SimulationState s = new SimulationState(threads, iterations, delay, normalized);
            synchronized(stateLock){state=s;}
            Thread t = new Thread(() -> { try { new Simulation(s).run(); } finally { running.set(false); synchronized(stateLock){s.running=false;} } }, "ThreadSync-Simulation"); t.start();
        }
        void reset(){ if(running.get()) return; synchronized(stateLock){state=SimulationState.idle();} }
        String snapshot(){ synchronized(stateLock){ return state.toJson(); } }
    }

    static final class SimulationState {
        final int threads, iterations, delay; final String mode;
        volatile int counter=0; volatile boolean running=true, race=false; volatile String holder="-";
        final Map<String,String> statuses=new LinkedHashMap<>(); final List<String> events=Collections.synchronizedList(new ArrayList<>());
        long start=System.nanoTime(); int completed=0; int expected;
        SimulationState(int t,int i,int d,String m){threads=t;iterations=i;delay=d;mode=m;expected=t*i;for(int n=1;n<=t;n++)statuses.put("Thread-"+n,"READY"); add("System","Simulation started ("+("race".equals(m)?"Without Synchronization":"With Synchronization")+")");}
        static SimulationState idle(){SimulationState s=new SimulationState(3,100,5,"race");s.running=false;s.events.clear();s.events.add("[00:00] System : Ready for a new experiment");return s;}
        void add(String who,String msg){long ms=(System.nanoTime()-start)/1_000_000;events.add(String.format("[%02d:%02d] %s : %s",ms/1000,(ms/10)%100,who,msg));}
        String esc(String s){return s.replace("\\","\\\\").replace("\"","\\\"").replace("\n"," ");}
        String toJson(){
            StringBuilder b=new StringBuilder("{");
            b.append("\"running\":").append(running)
             .append(",\"mode\":\"").append(mode).append("\",")
             .append("\"counter\":").append(counter)
             .append(",\"expected\":").append(expected)
             .append(",\"race\":").append(race)
             .append(",\"holder\":\"").append(esc(holder)).append("\",")
             .append("\"completed\":").append(completed)
             .append(",\"total\":").append(expected)
             .append(",\"threads\":[");
            int i=0;
            for(var e:statuses.entrySet()){
                if(i++>0)b.append(',');
                b.append("{\"name\":\"").append(e.getKey()).append("\",\"status\":\"").append(esc(e.getValue())).append("\"}");
            }
            b.append("],\"events\":[");
            synchronized(events){
                for(i=0;i<events.size();i++){
                    if(i>0)b.append(',');
                    b.append("\"").append(esc(events.get(i))).append("\"");
                }
            }
            b.append("]}");
            return b.toString();
        }
    }

    static final class Simulation {
        final SimulationState s; final ReentrantLock lock=new ReentrantLock(true); final CyclicBarrier startBarrier;
        Simulation(SimulationState s){this.s=s;startBarrier=new CyclicBarrier(s.threads);}
        void run(){
            ExecutorService pool=Executors.newFixedThreadPool(s.threads); List<Future<?>> fs=new ArrayList<>();
            for(int n=1;n<=s.threads;n++){final int id=n;fs.add(pool.submit(()->worker(id)));}
            for(Future<?> f:fs)try{f.get();}catch(Exception e){s.add("System","Worker error: "+e.getMessage());}
            pool.shutdown(); s.running=false; s.holder="-"; for(String k:s.statuses.keySet())s.statuses.put(k,"COMPLETED");
            s.race=s.counter!=s.expected; s.add("System", "Finished. Expected="+s.expected+", Actual="+s.counter+(s.race?" -> Race Condition detected":" -> No discrepancy detected"));
        }
        void worker(int id){String name="Thread-"+id;try{startBarrier.await();for(int i=1;i<=s.iterations;i++){if("sync".equals(s.mode))syncStep(name,i);else raceStep(name,i);synchronized(s){s.completed++;}}}catch(Exception e){s.add(name,"Stopped: "+e.getMessage());}finally{s.statuses.put(name,"COMPLETED");}}
        void pause(int ms){if(ms<=0)return;try{Thread.sleep(ms);}catch(InterruptedException e){Thread.currentThread().interrupt();}}
        void raceStep(String name,int i){s.statuses.put(name,"RUNNING");s.add(name,"Reading counter = "+s.counter);int local=s.counter;pause(s.delay);s.add(name,"Calculating "+local+" + 1");pause(s.delay);s.counter=local+1;s.add(name,"Writing counter = "+s.counter);if(i<s.iterations)pause(Math.max(1,s.delay/2));}
        void syncStep(String name,int i){s.statuses.put(name,"WAITING");s.add(name,"Waiting for Lock");lock.lock();try{s.statuses.put(name,"LOCKED");s.holder=name;s.add(name,"Acquired Lock");s.add(name,"Critical Section: Reading counter = "+s.counter);int local=s.counter;pause(s.delay);s.add(name,"Writing counter = "+(local+1));s.counter=local+1;}finally{s.add(name,"Released Lock");s.holder="-";s.statuses.put(name,"RUNNING");lock.unlock();}if(i<s.iterations)pause(Math.max(1,s.delay/2));}
    }
}
