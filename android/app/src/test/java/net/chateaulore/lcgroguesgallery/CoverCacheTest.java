package net.chateaulore.lcgroguesgallery;

import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class CoverCacheTest {
    @Rule public TemporaryFolder temporary=new TemporaryFolder();
    @Test public void hitsAvoidNetworkAndAccountsAreSeparate() throws Exception{
        CoverCache cache=new CoverCache(temporary.newFolder());AtomicInteger fetches=new AtomicInteger();
        CoverCache.Fetcher fetch=()->new byte[]{(byte)fetches.incrementAndGet()};
        assertArrayEquals(new byte[]{1},cache.get("alice","1","https://cover",fetch));
        assertArrayEquals(new byte[]{1},cache.get("alice","1","https://cover",fetch));
        assertArrayEquals(new byte[]{2},cache.get("bob","1","https://cover",fetch));
        assertEquals(2,fetches.get());
        cache.clear();cache.get("alice","1","https://cover",fetch);assertEquals(3,fetches.get());
    }
    @Test public void leastRecentlyUsedEntriesAreEvicted() throws Exception{
        File dir=temporary.newFolder();AtomicLong clock=new AtomicLong(10000);
        CoverCache cache=new CoverCache(dir,100,2,()->clock.addAndGet(1000));AtomicInteger misses=new AtomicInteger();
        CoverCache.Fetcher fetch=()->{misses.incrementAndGet();return new byte[]{1};};
        cache.get("a","1","url",fetch);cache.get("a","2","url",fetch);cache.get("a","1","url",fetch);cache.get("a","3","url",fetch);
        assertEquals(2,dir.listFiles().length);cache.get("a","1","url",fetch);assertEquals(3,misses.get());
        cache.get("a","2","url",fetch);assertEquals(4,misses.get());
    }
    @Test public void byteBudgetIsEnforcedAndOversizedEntriesAreNotCached() throws Exception{
        File dir=temporary.newFolder();CoverCache cache=new CoverCache(dir,3,100,System::currentTimeMillis);
        cache.get("a","1","url",()->new byte[]{1,2});cache.get("a","2","url",()->new byte[]{1,2});
        assertEquals(1,dir.listFiles().length);
        assertEquals(4,cache.get("a","large","url",()->new byte[4]).length);assertEquals(1,dir.listFiles().length);
    }
}
