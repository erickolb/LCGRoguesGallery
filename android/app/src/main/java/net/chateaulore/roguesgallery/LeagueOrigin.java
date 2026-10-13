package net.chateaulore.roguesgallery;
import java.net.URI;
final class LeagueOrigin {
    static boolean accepts(String url) {
        if (url == null) return false;
        try { URI uri = new URI(url); return "https".equals(uri.getScheme()) && "leagueofcomicgeeks.com".equals(uri.getHost()) && uri.getUserInfo() == null && (uri.getPort()==-1 || uri.getPort()==443); }
        catch(Exception e) { return false; }
    }
}
