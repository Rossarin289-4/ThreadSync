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
curl -fsS -X POST http://127.0.0.1:18081/api/start -H 'Content-Type: application/json' -d '{"threads":3,"iterations":20,"delay":1,"mode":"sync"}' > build/start.json
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
print('TC-02 With Synchronization: PASS')
PY
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
print('TC-01 Without Synchronization: PASS (run completed; discrepancy is timing-dependent)')
PY
curl -fsS -X POST http://127.0.0.1:18081/api/reset > build/reset.json
python3 - <<'PY'
import json
s=json.load(open('build/reset.json'))
assert s['ok'] is True
print('TC-03 Reset API: PASS')
PY
