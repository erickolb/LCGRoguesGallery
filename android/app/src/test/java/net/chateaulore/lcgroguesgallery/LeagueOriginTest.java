package net.chateaulore.lcgroguesgallery;
import org.junit.Test;
import static org.junit.Assert.*;
public class LeagueOriginTest {
    @Test public void acceptsLeagueHttps() { assertTrue(LeagueOrigin.accepts("https://leagueofcomicgeeks.com/login")); }
    @Test public void rejectsCredentialExfiltrationOrigins() {
        for(String url:new String[]{"http://leagueofcomicgeeks.com/login","https://leagueofcomicgeeks.com.evil.example/","https://leagueofcomicgeeks.com@evil.example/","https://evil.example@leagueofcomicgeeks.com/","https://leagueofcomicgeeks.com:444/","file:///etc/passwd","javascript:alert(1)"}) assertFalse(url,LeagueOrigin.accepts(url));
        assertFalse(LeagueOrigin.accepts(null));
    }
}
