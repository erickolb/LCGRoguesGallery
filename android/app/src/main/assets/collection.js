(async () => {
  window.__roguesResult = null;
  try {
    const html = document.documentElement.innerHTML;
    const idMatch = html.match(/(?:user_id|userId|userID)["']?\s*[:=]\s*["']?(\d+)/)
      || html.match(/data-user-id=["'](\d+)/)
      || html.match(/UserVoice\.push\(\['identify',\s*\{[^}]*?id\s*:\s*['"]?(\d+)/);
    if (!idMatch) throw new Error('Unable to identify this League account. Open your own profile collection and try again.');
    const covers = [], seen = new Set(), pages = new Set();
    for (let page = 1; page <= 500; page++) {
      const query = new URLSearchParams({list:'2',list_option:'issue',user_id:idMatch[1],view:'list',order:'alpha-asc',page:String(page)});
      const response = await fetch('/comic/get_comics?' + query, {credentials:'same-origin'});
      if (!response.ok) throw new Error('League returned HTTP ' + response.status + '. Please sign in again or try later.');
      const data = await response.json();
      if (typeof data.list !== 'string') throw new Error('League changed its collection response. This version needs an update.');
      if (pages.has(data.list)) throw new Error('League did not advance the collection page. No partial gallery was imported.');
      pages.add(data.list);
      const doc = new DOMParser().parseFromString(data.list, 'text/html');
      const rows = doc.querySelectorAll('li');
      for (const row of rows) {
        const anchor = row.querySelector('.comic-title a');
        const img = row.querySelector('.comic-cover-art img');
        if (!anchor || !img) continue;
        const raw = img.getAttribute('data-original') || img.getAttribute('data-src') || img.getAttribute('src');
        if (!raw || raw.includes('no-cover')) continue;
        const url = new URL(raw, location.origin);
        if (url.protocol !== 'https:') continue;
        const key = new URL(anchor.getAttribute('href'), location.origin).href;
        if (!seen.has(key)) { seen.add(key); covers.push({title:anchor.textContent.trim(),url:url.href}); }
      }
      const navigation = new DOMParser().parseFromString(String(data.pagination || ''), 'text/html');
      const next = navigation.querySelector('[rel="next"], .next:not(.disabled) a, a.next:not(.disabled)') || doc.querySelector('a[rel="next"]');
      const totalPages = Number(data.total_pages || data.pages || 0);
      if (!(next || totalPages > page)) break;
      if (page === 500) throw new Error('Collection exceeds the supported page limit. No partial gallery was imported.');
    }
    if (!covers.length) throw new Error('No cover images were found in Have It. Your collection may be empty, or League may have changed its page format.');
    window.__roguesResult = {covers};
  } catch (error) { window.__roguesResult = {error: error.message || 'Collection request failed.'}; }
})();
