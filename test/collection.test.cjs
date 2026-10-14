const {test} = require('node:test');
const assert = require('node:assert/strict');
const {DOMParser} = require('linkedom');
const fs = require('node:fs');
const vm = require('node:vm');
const script = fs.readFileSync('android/app/src/main/assets/collection.js','utf8');
const row = (id,variant='',image='/images/comics/1-cover.jpg') => `<li><div class="comic-title"><a href="/comic/${id}/title${variant}">Title ${id}</a></div><div class="comic-cover-art"><img data-original="${image}"></div></li>`;
test('a slow older import cannot overwrite a newer collection',async()=>{
  let finishFirst,calls=0;
  const context={window:{},document:{documentElement:{innerHTML:'user_id: 42'}},DOMParser,URL,URLSearchParams,location:{origin:'https://leagueofcomicgeeks.com'},fetch:async()=>{
    const call=++calls;
    if(call===1)await new Promise(resolve=>{finishFirst=resolve;});
    return {ok:true,json:async()=>({list:row(call)})};
  }};
  const first=vm.runInNewContext(script,context);
  await vm.runInNewContext(script,context);
  const latest=context.window.__roguesResult;
  finishFirst();await first;
  assert.strictEqual(context.window.__roguesResult,latest);
  assert.equal(latest.covers[0].id,'2:');
});
async function run(responses,html='user_id: 42') {
  const calls=[];
  const context={window:{},document:{documentElement:{innerHTML:html}},DOMParser,URL,URLSearchParams,location:{origin:'https://leagueofcomicgeeks.com'},fetch:async url=>{calls.push(url); const response=responses.shift(); if(!response)throw new Error('Unexpected extra request'); return {ok:true,json:async()=>response,text:async()=>response};}};
  await vm.runInNewContext(script,context);
  return {result:context.window.__roguesResult,calls};
}
test('requests owned issues and preserves distinct variants',async()=>{
  const {result,calls}=await run([{list:row(1)+row(1,'?variant=2')+row(1)+row(3,'','https://cdn.example/cover.jpg')}]);
  assert.equal(result.covers.length,3);
  assert.equal(result.accountId,'42');
  assert.deepEqual(Array.from(result.covers,c=>c.id),['1:','1:2','3:']);
  const query=new URL(calls[0],'https://leagueofcomicgeeks.com').searchParams;
  assert.equal(query.get('list'),'2'); assert.equal(query.get('list_option'),'issue'); assert.equal(query.get('user_id'),'42');
  assert.equal(result.covers[2].url,'https://cdn.example/cover.jpg');
});
test('loads multiple pages and deduplicates overlapping entries',async()=>{
  const {result,calls}=await run([{list:row(1),total_pages:2},{list:row(1)+row(2),total_pages:2}]);
  assert.equal(result.covers.length,2); assert.equal(calls.length,2);
});
test('rejects repeated pages rather than silently showing partial data',async()=>{
  const {result}=await run([{list:row(1),total_pages:2},{list:row(1),total_pages:2}]);
  assert.match(result.error,/did not advance/); assert.equal(result.covers,undefined);
});
test('rejects unknown accounts and changed response formats',async()=>{
  assert.match((await run([], 'login form')).result.error,/identify/);
  assert.match((await run([{unexpected:true}])).result.error,/changed/);
});
test('skips missing covers and rejects insecure images',async()=>{
  const {result}=await run([{list:row(1,'','/no-cover.jpg')+row(2,'','http://cdn.example/cover.jpg')}]);
  assert.match(result.error,/No owned issue covers/);
});
test('reads the current League markup observed on its live site',async()=>{
  const html=fs.readFileSync('test/fixtures/league-live-issue.html','utf8');
  const {result}=await run([{list:html}]);
  assert.equal(result.covers.length,1);
  assert.equal(result.covers[0].title,'The Amazing Spider-Man #1001');
  assert.equal(result.covers[0].url,'https://s3.amazonaws.com/comicgeeks/comics/covers/large-3191578.jpg?1791486274');
  assert.equal(result.covers[0].fallbackUrl,'https://s3.amazonaws.com/comicgeeks/comics/covers/medium-3191578.jpg?1791486274');
});
const seriesRow = id => `<li><div class="cover"><img src="https://s3.amazonaws.com/comicgeeks/comics/covers/medium-${id}.jpg"></div><div class="title"><a class="link-collection-series" href="/profile/test/collection/${id}" data-id="${id}">Series ${id}</a></div></li>`;
test('expands 51 grouped series into 188 individual owned issues',async()=>{
  const grouped=Array.from({length:51},(_,i)=>seriesRow(i+1)).join('');
  let issueId=1000;
  const responses=[{list:grouped},...Array.from({length:51},(_,i)=>({list:Array.from({length:i<35?4:3},()=>row(issueId++)).join('')}))];
  const {result,calls}=await run(responses);
  assert.equal(result.covers.length,188);
  assert.equal(calls.length,52);
  assert.ok(result.covers.every(c=>!c.title.startsWith('Series')));
  for(let i=1;i<calls.length;i++){
    const params=new URL(calls[i],'https://leagueofcomicgeeks.com').searchParams;
    assert.equal(params.get('series_id'),String(i));
    assert.equal(params.get('list'),'2'); assert.equal(params.get('user_id'),'42');
  }
});
test('handles pagination inside a grouped series and retains owned variants',async()=>{
  const {result}=await run([{list:seriesRow(10)},{list:row(1)+row(1,'?variant=7'),total_pages:2},{list:row(2),total_pages:2}]);
  assert.equal(result.covers.length,3);
});
test('falls back to the owned collection page without importing recommended issues',async()=>{
  const {result,calls}=await run([{list:seriesRow(10)},{list:seriesRow(10)},'<ul id="comic-list-issues">'+row(1)+row(2)+'</ul><ul id="more-issues">'+row(3)+'</ul>']);
  assert.equal(result.covers.length,2);
  assert.equal(calls[2],'/profile/test/collection/10');
});
test('rejects an unexpanded series rather than displaying its preview cover',async()=>{
  const {result}=await run([{list:seriesRow(10)},{list:seriesRow(10)},'<ul id="comic-list-titles">'+seriesRow(10)+'</ul>']);
  assert.match(result.error,/did not return owned issues/); assert.equal(result.covers,undefined);
});
test('promotes variant cover filenames to large while preserving their query',async()=>{
  const {result}=await run([{list:row(1,'?variant=99','https://s3.amazonaws.com/comicgeeks/comics/covers/medium-987654.png?t=123')}]);
  assert.equal(result.covers[0].url,'https://s3.amazonaws.com/comicgeeks/comics/covers/large-987654.png?t=123');
  assert.equal(result.covers[0].fallbackUrl,'https://s3.amazonaws.com/comicgeeks/comics/covers/medium-987654.png?t=123');
});
test('does not rewrite third-party image URLs',async()=>{
  const {result}=await run([{list:row(1,'','https://cdn.example/comics/covers/medium-1.jpg')}]);
  assert.equal(result.covers[0].url,'https://cdn.example/comics/covers/medium-1.jpg');
  assert.equal(result.covers[0].fallbackUrl,undefined);
});
test('reads current lazy-loaded covers before the browser has loaded them',async()=>{
  const html='<li class="issue"><div class="cover"><a href="/comic/1/example"><img src="/placeholder.svg" data-src="https://cdn.example/owned.jpg"></a></div><div class="title"><a href="/comic/1/example">Owned #1</a></div></li>';
  const {result}=await run([{list:html}]);
  assert.equal(result.covers.length,1);
  assert.equal(result.covers[0].url,'https://cdn.example/owned.jpg');
});
