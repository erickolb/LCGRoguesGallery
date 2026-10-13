const {test} = require('node:test');
const assert = require('node:assert/strict');
const {DOMParser} = require('linkedom');
const fs = require('node:fs');
const vm = require('node:vm');
const script = fs.readFileSync('android/app/src/main/assets/collection.js','utf8');
const row = (id,variant='',image='/images/comics/1-cover.jpg') => `<li><div class="comic-title"><a href="/comic/${id}/title${variant}">Title ${id}</a></div><div class="comic-cover-art"><img data-original="${image}"></div></li>`;
async function run(responses,html='user_id: 42') {
  const calls=[];
  const context={window:{},document:{documentElement:{innerHTML:html}},DOMParser,URL,URLSearchParams,location:{origin:'https://leagueofcomicgeeks.com'},fetch:async url=>{calls.push(url); const response=responses.shift(); if(!response)throw new Error('Unexpected extra request'); return {ok:true,json:async()=>response};}};
  await vm.runInNewContext(script,context);
  return {result:context.window.__roguesResult,calls};
}
test('requests owned issues and preserves distinct variants',async()=>{
  const {result,calls}=await run([{list:row(1)+row(1,'?variant=2')+row(1)+row(3,'','https://cdn.example/cover.jpg')}]);
  assert.equal(result.covers.length,3);
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
  assert.match(result.error,/No cover images/);
});
test('reads the current League markup observed on its live site',async()=>{
  const html=fs.readFileSync('test/fixtures/league-live-issue.html','utf8');
  const {result}=await run([{list:html}]);
  assert.equal(result.covers.length,1);
  assert.equal(result.covers[0].title,'The Amazing Spider-Man #1001');
  assert.equal(result.covers[0].url,'https://s3.amazonaws.com/comicgeeks/comics/covers/medium-3191578.jpg?1791486274');
});
test('reads current lazy-loaded covers before the browser has loaded them',async()=>{
  const html='<li class="issue"><div class="cover"><a href="/comic/1/example"><img src="/placeholder.svg" data-src="https://cdn.example/owned.jpg"></a></div><div class="title"><a href="/comic/1/example">Owned #1</a></div></li>';
  const {result}=await run([{list:html}]);
  assert.equal(result.covers.length,1);
  assert.equal(result.covers[0].url,'https://cdn.example/owned.jpg');
});
