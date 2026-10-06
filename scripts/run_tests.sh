#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
rm -rf build && mkdir -p build/classes
javac --release 17 -d build/classes src/threadsync/Main.java tests/ThreadSyncTest.java
java -cp build/classes ThreadSyncTest
java -cp build/classes threadsync.Main 18081 > build/server.log 2>&1 &
PID=$!
trap 'kill $PID 2>/dev/null || true' EXIT
sleep 1
curl -fsS http://127.0.0.1:18081/api/state > build/state0.json
curl -fsS -X POST http://127.0.0.1:18081/api/start -H 'Content-Type: application/json' -d '{"threads":3,"iterations":20,"delay":10,"mode":"sync"}' > build/start.json
for i in $(seq 1 100); do
  curl -fsS http://127.0.0.1:18081/api/state > build/state.json
  grep -q '"running":false' build/state.json && break
  sleep 0.1
done
python3 - <<'PY'
import json
s=json.load(open('build/state.json'))
assert s['expected']==60, s
assert s['counter']==60, s
assert s['race'] is False, s
assert len(s['events'])>1
assert all(t['completedIterations']==20 and t['writes']==20 and t['successful']==20 for t in s['threads']), s['threads']
assert all({'CREATED','READ','MODIFY','WRITE','ACQUIRED LOCK','RUNNING','RELEASE LOCK','COMPLETED'}.issubset({e['type'] for e in t['history']}) for t in s['threads'])
assert any(e['type']=='WAITING FOR LOCK' and 'Waiting for Thread-' in e['detail'] for t in s['threads'] for e in t['history'])
print('TC-04 Per-thread history and ReentrantLock waits: PASS')
print('TC-02 With Synchronization: PASS')
PY
# Compare mode must expose independent real worker histories for identical input.
curl -fsS -X POST http://127.0.0.1:18081/api/compare/start -H 'Content-Type: application/json' -d '{"threads":3,"iterations":4,"delay":5}' >/dev/null
for i in $(seq 1 100); do
  curl -fsS http://127.0.0.1:18081/api/compare/state > build/state-compare.json
  grep -q '"running":false' build/state-compare.json && grep -q '"sync":{"running":false' build/state-compare.json && break
  sleep 0.1
done
python3 - <<'PY'
import json
s=json.load(open('build/state-compare.json'))
assert s['race']['expected']==12 and s['sync']['expected']==12, s
assert s['sync']['counter']==12, s
assert all(t['history'] for t in s['race']['threads']+s['sync']['threads']), s
print('TC-06 Compare Mode, same input / separate worker state: PASS')
PY
# Compare controls advance and resume both real simulations.
curl -fsS -X POST http://127.0.0.1:18081/api/compare/start -H 'Content-Type: application/json' -d '{"threads":2,"iterations":3,"delay":100}' >/dev/null
curl -fsS -X POST http://127.0.0.1:18081/api/compare/control -H 'Content-Type: application/json' -d '{"action":"pause"}' >/dev/null
python3 - <<'PY'
import json,time,urllib.request
base='http://127.0.0.1:18081'
def state(): return json.load(urllib.request.urlopen(base+'/api/compare/state'))
def control(action):
 req=urllib.request.Request(base+'/api/compare/control',data=json.dumps({'action':action}).encode(),headers={'Content-Type':'application/json'},method='POST')
 urllib.request.urlopen(req).read()
def count(lane): return sum(len(t['history']) for t in lane['threads'])
deadline=time.time()+5
while time.time()<deadline:
 s=state()
 if s['race']['paused'] and s['sync']['paused']: break
 time.sleep(.03)
assert s['race']['paused'] and s['sync']['paused'],s
before=(count(s['race']),count(s['sync']))
control('step')
deadline=time.time()+3
while time.time()<deadline:
 s=state()
 if count(s['race'])>before[0] and count(s['sync'])>before[1]: break
 time.sleep(.03)
assert count(s['race'])>before[0] and count(s['sync'])>before[1],s
assert s['race']['paused'] and s['sync']['paused'],s
control('resume')
deadline=time.time()+10
while time.time()<deadline:
 s=state()
 if not s['race']['running'] and not s['sync']['running']: break
 time.sleep(.05)
assert not s['race']['running'] and not s['sync']['running'] and s['sync']['counter']==6,s
print('TC-07 Compare Pause / Next Step / Resume: PASS')
PY
# Pause/Next Step/Resume smoke test with a real paused-state assertion.
curl -fsS -X POST http://127.0.0.1:18081/api/reset >/dev/null
curl -fsS -X POST http://127.0.0.1:18081/api/start -H 'Content-Type: application/json' -d '{"threads":2,"iterations":2,"delay":200,"mode":"race"}' >/dev/null
curl -fsS -X POST http://127.0.0.1:18081/api/control -H 'Content-Type: application/json' -d '{"action":"pause"}' >/dev/null
python3 - <<'PY'
import json,time,urllib.request
base='http://127.0.0.1:18081'
def state(): return json.load(urllib.request.urlopen(base+'/api/state'))
def control(action):
 req=urllib.request.Request(base+'/api/control',data=json.dumps({'action':action}).encode(),headers={'Content-Type':'application/json'},method='POST')
 urllib.request.urlopen(req).read()
def count(s): return sum(len(t['history']) for t in s['threads'])
deadline=time.time()+5
while time.time()<deadline:
 s=state()
 if s['paused']: break
 time.sleep(.03)
assert s['paused'],s
before=count(s)
control('step')
deadline=time.time()+3
while time.time()<deadline:
 s=state()
 if count(s)>before: break
 time.sleep(.03)
assert count(s)>before and s['paused'],s
print('Next Step advanced one worker checkpoint while paused: PASS')
PY
curl -fsS -X POST http://127.0.0.1:18081/api/control -H 'Content-Type: application/json' -d '{"action":"resume"}' >/dev/null
for i in $(seq 1 100); do
  curl -fsS http://127.0.0.1:18081/api/state > build/state-control.json
  grep -q '"running":false' build/state-control.json && break
  sleep 0.1
done
python3 - <<'PY2'
import json
s=json.load(open('build/state-control.json'))
assert s['completed']==4, s
print('TC-05 Pause / Next Step / Resume: PASS')
PY2
curl -fsS -X POST http://127.0.0.1:18081/api/reset >/dev/null
curl -fsS -X POST http://127.0.0.1:18081/api/start -H 'Content-Type: application/json' -d '{"threads":3,"iterations":20,"delay":2,"mode":"race"}' >/dev/null
for i in $(seq 1 100); do
  curl -fsS http://127.0.0.1:18081/api/state > build/state-race.json
  grep -q '"running":false' build/state-race.json && break
  sleep 0.1
done
python3 - <<'PY'
import json
s=json.load(open('build/state-race.json'))
assert s['expected']==60, s
assert len(s['events'])>1
assert all(t['writes']==20 and t['completedIterations']==20 for t in s['threads']), s['threads']
assert all(any(e['type']=='READ' for e in t['history']) and any(e['type']=='WRITE' for e in t['history']) for t in s['threads'])
print('TC-01 Without Synchronization and per-thread history: PASS (race outcome timing-dependent)')
PY
curl -fsS -X POST http://127.0.0.1:18081/api/reset > build/reset.json
python3 - <<'PY'
import json
s=json.load(open('build/reset.json'))
assert s['ok'] is True
print('TC-03 Reset API: PASS')
PY
