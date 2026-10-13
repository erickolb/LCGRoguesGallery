package net.chateaulore.lcgroguesgallery;

import android.app.*;
import android.os.*;
import android.graphics.*;
import android.net.Uri;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.net.URL;
import java.net.HttpURLConnection;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService images = Executors.newSingleThreadExecutor();
    private WebView league;
    private LinearLayout root, controls;
    private ImageView cover;
    private TextView caption, status;
    private JSONArray collection = new JSONArray();
    private int index, polls;
    private volatile int generation;
    private boolean playing, importing;
    private Bitmap currentBitmap;
    private final Runnable slideshow = new Runnable() { public void run() { if (playing) { advance(1); handler.postDelayed(this,15000); } } };
    @Override public void onCreate(Bundle state) { super.onCreate(state); createWebView(); showLogin(); if(Build.VERSION.SDK_INT>=33) ModernBack.register(this,this::handleBack); }
    @android.annotation.TargetApi(33)
    private static class ModernBack {
        static void register(Activity activity, Runnable action) {
            activity.getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,action::run);
        }
    }
    private void createWebView() {
        league = new WebView(this);
        league.getSettings().setJavaScriptEnabled(true);
        league.getSettings().setDomStorageEnabled(true);
        league.getSettings().setAllowFileAccess(false);
        league.getSettings().setAllowContentAccess(false);
        league.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        CookieManager.getInstance().setAcceptCookie(true);
        league.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                return request.isForMainFrame() && !LeagueOrigin.accepts(uri.toString());
            }
            @Override public void onPageFinished(WebView view, String url) { CookieManager.getInstance().flush(); }
            @Override public void onReceivedSslError(WebView view, android.webkit.SslErrorHandler handler, android.net.http.SslError error) { handler.cancel(); }
        });
    }
    private static byte[] readBytes(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(); byte[] buffer = new byte[8192]; int n;
        while ((n = in.read(buffer)) != -1) out.write(buffer,0,n);
        return out.toByteArray();
    }
    private void base() {
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(Color.rgb(24,17,25));
        root.setOnApplyWindowInsetsListener((v,insets)->{v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets;}); setContentView(root);
    }
    private Button button(String label, Runnable action) { Button b = new Button(this); b.setText(label); b.setOnClickListener(v -> action.run()); return b; }
    private void text(String value) { TextView t = new TextView(this); t.setText(value); t.setTextColor(Color.WHITE); t.setPadding(20,12,20,12); root.addView(t); }
    private void showLogin() {
        stopPlayback(); generation++; base();
        text("LCG Rogues Gallery · Sign in on League, then open your own profile collection and tap Load Have It. Passwords go directly to League.");
        LinearLayout row = new LinearLayout(this);
        row.addView(button("Load Have It", this::importCollection)); row.addView(button("Credits", this::credits));
        root.addView(row);
        status = new TextView(this); status.setTextColor(Color.WHITE); status.setPadding(20,0,20,8); root.addView(status);
        if (league.getParent() != null) ((android.view.ViewGroup)league.getParent()).removeView(league);
        root.addView(league, new LinearLayout.LayoutParams(-1,0,1));
        if (league.getUrl() == null) league.loadUrl("https://leagueofcomicgeeks.com/login");
    }
    private void importCollection() {
        if (importing) return;
        if (!LeagueOrigin.accepts(league.getUrl())) { status.setText("Open League before loading your collection."); return; }
        try {
            String script;
            try (InputStream in = getAssets().open("collection.js")) { script = new String(readBytes(in), java.nio.charset.StandardCharsets.UTF_8); }
            importing = true; polls=0; status.setText("Loading your Have It covers…");
            league.evaluateJavascript(script, ignored -> handler.postDelayed(this::pollImport,1000));
        } catch(Exception e) { status.setText("Could not start import."); }
    }
    private void pollImport() {
        if (!importing) return;
        league.evaluateJavascript("JSON.stringify(window.__roguesResult || null)", value -> {
            try {
                Object decoded = new JSONTokener(value).nextValue();
                if (!(decoded instanceof String) || "null".equals(decoded)) {
                    if (++polls < 120) handler.postDelayed(this::pollImport,1000);
                    else { importing=false; status.setText("League did not respond within two minutes. Try again."); }
                    return;
                }
                JSONObject result = new JSONObject((String)decoded); importing=false;
                if (result.has("error")) { status.setText(result.getString("error")); return; }
                collection=result.getJSONArray("covers"); index=0; showGallery();
            } catch(Exception e) { importing=false; status.setText("Unable to read League's response. Try signing in again."); }
        });
    }
    private void showGallery() {
        base(); getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(5894);
        cover = new ImageView(this); cover.setScaleType(ImageView.ScaleType.FIT_CENTER);
        cover.setContentDescription("Comic cover"); root.addView(cover,new LinearLayout.LayoutParams(-1,0,1));
        caption = new TextView(this); caption.setTextColor(Color.WHITE); caption.setGravity(Gravity.CENTER); root.addView(caption);
        controls = new LinearLayout(this); controls.setGravity(Gravity.CENTER);
        controls.addView(button("‹",()->advance(-1)));
        controls.addView(button("Play / Pause",()->{playing=!playing;handler.removeCallbacks(slideshow);if(playing)handler.postDelayed(slideshow,15000);}));
        controls.addView(button("›",()->advance(1)));
        controls.addView(button("Menu",this::menu)); root.addView(controls);
        cover.setOnClickListener(v->{ int visibility=controls.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE; controls.setVisibility(visibility); caption.setVisibility(visibility); });
        display();
    }
    private void advance(int delta) { if(collection.length()>0) {index=(index+delta+collection.length())%collection.length();display();} }
    private void display() {
        int token=++generation;
        JSONObject item=collection.optJSONObject(index); if(item==null)return;
        caption.setText(item.optString("title")+" · "+(index+1)+" / "+collection.length());
        cover.setContentDescription(item.optString("title")); cover.setImageDrawable(null);
        images.execute(()->{
            if(token!=generation) return;
            Bitmap bitmap=null; String error=null;
            try {
                URL url=new URL(item.getString("url"));
                if(!"https".equals(url.getProtocol())) throw new IOException();
                HttpURLConnection connection=(HttpURLConnection)url.openConnection();
                connection.setConnectTimeout(15000);connection.setReadTimeout(15000);connection.setInstanceFollowRedirects(false);
                try {
                    if(connection.getResponseCode()!=200) throw new IOException();
                    byte[] bytes;
                    try(InputStream in=connection.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()) {
                        byte[] buffer=new byte[8192];int n;
                        while((n=in.read(buffer))!=-1){if(out.size()+n>20*1024*1024)throw new IOException();out.write(buffer,0,n);}bytes=out.toByteArray();
                    }
                    BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(bytes,0,bytes.length,bounds);
                    BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=1;
                    while(bounds.outWidth/options.inSampleSize>2400 || bounds.outHeight/options.inSampleSize>2400) options.inSampleSize*=2;
                    bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);if(bitmap==null)throw new IOException();
                } finally {connection.disconnect();}
            }catch(Exception e){error="Cover unavailable. Use Next to continue.";}
            Bitmap loaded=bitmap;String failure=error;
            handler.post(()->{if(token!=generation){if(loaded!=null)loaded.recycle();return;} cover.setImageBitmap(loaded);if(currentBitmap!=null)currentBitmap.recycle();currentBitmap=loaded;if(failure!=null)caption.setText(failure);});
        });
    }
    private void stopPlayback(){playing=false;handler.removeCallbacks(slideshow);getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);getWindow().getDecorView().setSystemUiVisibility(0);}
    private void menu(){new AlertDialog.Builder(this).setItems(new String[]{"Refresh / Account","Credits","Sign out"},(d,i)->{if(i==0)showLogin();else if(i==1)credits();else{collection=new JSONArray();CookieManager.getInstance().removeAllCookies(ignored->{CookieManager.getInstance().flush();league.clearCache(true);WebStorage.getInstance().deleteAllData();league.loadUrl("https://leagueofcomicgeeks.com/login");showLogin();});}}).show();}
    private void credits(){String license="";try(InputStream in=getAssets().open("leagueofcomicgeeks-LICENSE.txt")){license=new String(readBytes(in),java.nio.charset.StandardCharsets.UTF_8);}catch(IOException ignored){}
        TextView t=new TextView(this);t.setPadding(28,20,28,20);t.setText(android.text.Html.fromHtml("<h2>Credits</h2><p><a href=\"https://www.flaticon.com/free-icons/comic-book\" title=\"comic-book icons\">Comic-book icons created by Magnific - Flaticon</a></p><p>Collection adapter based on <a href=\"https://github.com/alistairjcbrown/leagueofcomicgeeks\">leagueofcomicgeeks by Alistair Brown</a> (MIT).</p><p>Unofficial app. League of Comic Geeks and cover artwork belong to their respective owners.</p>",0));t.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
        ScrollView scroll=new ScrollView(this);LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.addView(t);TextView l=new TextView(this);l.setPadding(28,0,28,20);l.setText(license);content.addView(l);scroll.addView(content);new AlertDialog.Builder(this).setView(scroll).setPositiveButton("Close",null).show();}
    private void handleBack(){if(cover!=null && cover.getParent()==root)menu();else if(league.canGoBack())league.goBack();else finish();}
    // API 33+ uses ModernBack; this callback supports Android 8 through 12.
    @android.annotation.SuppressLint("GestureBackNavigation")
    @Override public void onBackPressed(){handleBack();}
    @Override protected void onPause(){super.onPause();stopPlayback();CookieManager.getInstance().flush();}
    @Override protected void onResume(){super.onResume();if(cover!=null && cover.getParent()==root){getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);getWindow().getDecorView().setSystemUiVisibility(5894);}}
    @Override protected void onDestroy(){generation++;importing=false;handler.removeCallbacksAndMessages(null);images.shutdownNow();league.destroy();super.onDestroy();}
}
