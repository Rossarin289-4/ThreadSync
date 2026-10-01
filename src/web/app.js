let timer=null,lastEventCount=0;
const $=id=>document.getElementById(id);
async function post(url,data){return fetch(url,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(data||{})}).then(r=>r.json())}
function render(s){
 $('counter').textContent=s.counter;$('expected').textContent=s.expected;$('holder').textContent=s.holder;$('mode').textContent='Mode: '+(s.mode==='race'?'Without Synchronization':'With Synchronization');
 $('progressText').textContent=`${s.completed} / ${s.total} operations`;$('live').textContent=s.running?'RUNNING':'Idle';
 $('result').textContent=s.running?'RUNNING':(s.race?'RACE CONDITION':'COMPLETED');$('result').className=s.race?'RACE':'';
 $('resultDetail').textContent=s.running?'Experiment in progress':`Expected ${s.expected}, Actual ${s.counter}`;
 const tv=$('threads');tv.innerHTML='';s.threads.forEach(t=>{const d=document.createElement('div');d.className='thread';d.innerHTML=`<b>${t.name}</b><br><span class="status ${t.status}">${t.status}</span><div class="bar"><i style="width:${Math.min(100,(s.completed/s.total)*100)}%"></i></div>`;tv.appendChild(d)});
 if(s.events.length!==lastEventCount){$('events').innerHTML=s.events.map(e=>`<div class="event">${escapeHtml(e)}</div>`).join('');$('events').scrollTop=$('events').scrollHeight;lastEventCount=s.events.length}
 if(!s.running&&timer){clearInterval(timer);timer=null}
}
function escapeHtml(x){return x.replaceAll('&','&amp;').replaceAll('<','&lt;').replaceAll('>','&gt;')}
async function refresh(){try{render(await fetch('/api/state').then(r=>r.json()))}catch(e){$('live').textContent='Server unavailable'}}
async function run(mode){if(timer)return;lastEventCount=0;const data={threads:+$('threads').value,iterations:+$('iterations').value,delay:+$('delay').value,mode};const r=await post('/api/start',data);if(!r.ok){alert(r.error||'Cannot start');return}timer=setInterval(refresh,100);refresh()}
$('race').onclick=()=>run('race');$('sync').onclick=()=>run('sync');$('reset').onclick=async()=>{if(timer)return;await post('/api/reset');lastEventCount=0;refresh()};refresh();
