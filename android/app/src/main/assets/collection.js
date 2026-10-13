(async () => {
  window.__roguesResult = null;
  window.__roguesProgress = 'Loading owned issues…';
  try {
    const html = document.documentElement.innerHTML;
    const idMatch = html.match(/(?:user_id|userId|userID)["']?\s*[:=]\s*["']?(\d+)/)
      || html.match(/data-user-id=["'](\d+)/)
      || html.match(/UserVoice\.push\(\['identify',\s*\{[^}]*?id\s*:\s*['"]?(\d+)/);
    if (!idMatch) throw new Error('Unable to identify this League account. Open your own profile collection and try again.');
    const covers = [], seenIssues = new Set(), series = new Map();
    let finishedSeries = 0;

    function parseList(markup, expandingSeries) {
      const doc = new DOMParser().parseFromString(markup, 'text/html');
      let issueCount = 0;
      for (const row of doc.querySelectorAll('li')) {
        const anchor = row.querySelector('.comic-title a, .title a, a.link-collection-series');
        if (!anchor) continue;
        const link = new URL(anchor.getAttribute('href'), location.origin);
        if (link.origin !== location.origin) continue;
        const seriesMatch = link.pathname.match(/^\/profile\/[^/]+\/collection\/(\d+)\/?$/)
          || link.pathname.match(/^\/comics\/series\/(\d+)(?:\/|$)/);
        if (seriesMatch) {
          if (!expandingSeries && !series.has(seriesMatch[1])) {
            series.set(seriesMatch[1], {id:seriesMatch[1],title:anchor.textContent.trim(),path:link.pathname});
          }
          continue; // A representative series cover is never an owned issue.
        }
        const issueMatch = link.pathname.match(/^\/comic\/(\d+)(?:\/|$)/);
        if (!issueMatch) continue;
        issueCount++;
        const img = row.querySelector('.comic-cover-art img, .cover img');
        if (!img) continue;
        const raw = img.getAttribute('data-original') || img.getAttribute('data-src') || img.getAttribute('src');
        if (!raw || raw.includes('no-cover')) continue;
        const url = new URL(raw, location.origin);
        if (url.protocol !== 'https:') continue;
        const fallbackUrl = url.href;
        // Keep the exact cover/variant filename and cache-busting query.
        if ((url.hostname === 's3.amazonaws.com' && url.pathname.startsWith('/comicgeeks/'))
            || url.hostname === 'leagueofcomicgeeks.com' || url.hostname === 'cdn.leagueofcomicgeeks.com'
            || /^comicgeeks\.s3(?:\.[a-z0-9-]+)?\.amazonaws\.com$/.test(url.hostname)) {
          url.pathname = url.pathname.replace(/(\/comics\/covers\/)(?:small|medium)-(?=\d)/, '$1large-');
        }
        const key = issueMatch[1] + ':' + (link.searchParams.get('variant') || '');
        if (!seenIssues.has(key)) {
          seenIssues.add(key);
          const item = {id:key,title:anchor.textContent.trim(),url:url.href};
          if (url.href !== fallbackUrl) item.fallbackUrl = fallbackUrl;
          covers.push(item);
        }
      }
      return {doc,issueCount};
    }

    async function readCollection(selectedSeries) {
      const pages = new Set();
      let issues = 0;
      for (let page = 1; page <= 500; page++) {
        const query = new URLSearchParams({list:'2',list_option:'issue',user_id:idMatch[1],view:'list',order:'alpha-asc',page:String(page)});
        if (selectedSeries) query.set('series_id',selectedSeries.id);
        const response = await fetch('/comic/get_comics?' + query, {credentials:'same-origin'});
        if (!response.ok) throw new Error('League returned HTTP ' + response.status + '. Please sign in again or try later.');
        const data = await response.json();
        if (typeof data.list !== 'string') throw new Error('League changed its collection response. This version needs an update.');
        if (pages.has(data.list)) throw new Error('League did not advance the collection page. No partial gallery was imported.');
        pages.add(data.list);
        const parsed = parseList(data.list, !!selectedSeries);
        issues += parsed.issueCount;
        const navigation = new DOMParser().parseFromString(String(data.pagination || ''), 'text/html');
        const next = navigation.querySelector('[rel="next"], .next:not(.disabled) a, a.next:not(.disabled)') || parsed.doc.querySelector('a[rel="next"]');
        const totalPages = Number(data.total_pages || data.pages || 0);
        if (!(next || totalPages > page)) return issues;
        if (page === 500) throw new Error('Collection exceeds the supported page limit. No partial gallery was imported.');
      }
      return issues;
    }

    await readCollection(null);
    for (const entry of series.values()) {
      window.__roguesProgress = 'Loading series ' + (finishedSeries + 1) + ' / ' + series.size + ' · ' + covers.length + ' issue covers';
      const issues = await readCollection(entry);
      if (!issues) {
        // Use only the profile's owned-collection route, never a general series page.
        if (!/^\/profile\/[^/]+\/collection\/\d+\/?$/.test(entry.path)) {
          throw new Error('League did not return owned issues for ' + entry.title + '. No partial gallery was imported.');
        }
        const response = await fetch(entry.path, {credentials:'same-origin'});
        if (!response.ok) throw new Error('Could not load owned issues for ' + entry.title + '. Please try again.');
        const page = new DOMParser().parseFromString(await response.text(), 'text/html');
        const list = page.querySelector('#comic-list-issues');
        if (!list || !parseList(list.outerHTML, true).issueCount) {
          throw new Error('League did not return owned issues for ' + entry.title + '. No partial gallery was imported.');
        }
      }
      finishedSeries++;
    }
    if (!covers.length) throw new Error('No owned issue covers were returned. Open your collection and try again.');
    window.__roguesResult = {accountId:idMatch[1],covers};
  } catch (error) { window.__roguesResult = {error: error.message || 'Collection request failed.'}; }
})();
