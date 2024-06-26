package fengliu.cloudmusic.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fengliu.cloudmusic.config.Configs;
import fengliu.cloudmusic.music163.ActionException;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

public class HttpClient {
    private final String MainPath;
    private final Map<String, String> Header;

    public HttpClient(String path){
        this.MainPath = path;
        this.Header = new HashMap<String, String>();
    }

    public HttpClient(String path, Map<String, String> header){
        this.MainPath = path;
        this.Header = header;
    }

    public void setCookies(String cookies){
        this.Header.put("Cookie", cookies);
    }

    public Map<String, String> getHeader(){
        return this.Header;
    }

    public String getCookies(){
        if(!this.Header.containsKey("Cookie")){
            return "";
        }
        return this.Header.get("Cookie");
    }

    public String getUrl(String path){
        return MainPath + path;
    }

    public HttpResult GET(String path, @Nullable Map<String, Object> data){
        return this.connection(this.getUrl(path), data, (HttpURLConnection connection) -> {
            try {
                connection.setRequestMethod("GET");
            } catch (ProtocolException exception) {
                exception.printStackTrace();
            }
            return connection;
        }, 0);
    }

    public HttpResult POST(String path, @Nullable Map<String, Object> data){
        return this.connection(this.getUrl(path), data, (HttpURLConnection connection) -> {
            try {
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setDoInput(true);
            } catch (ProtocolException exception) {
                exception.printStackTrace();
            }
            return connection;
        }, 0);
    }

    public static class ApiException extends RuntimeException{

        public ApiException(int code, String msg){
            super("请求错误 " + code + ": " + msg);
        }
    }

    public JsonObject POST_API(String path, @Nullable Map<String, Object> data){
        HttpResult result = this.POST(path, data);
        JsonObject json = result.getJson();
        
        int code = json.get("code").getAsInt();
        if(code == 301){
            throw new ActionException(Text.translatable("cloudmusic.exception.cookie.use"));
        }

        if(code != 200){
            if(json.has("msg")){
                throw new ApiException(json.get("code").getAsInt(), json.get("msg").getAsString());
            }

            throw new ApiException(json.get("code").getAsInt(), json.get("message").getAsString());
        }

        return json;
    }

    public String POST_LOGIN(String path, Map<String, Object> data){
        HttpResult result = this.POST(path, data);
        JsonObject json = result.getJson();
        
        int code = json.get("code").getAsInt();
        if(code == 400){
            throw new ActionException(Text.translatable("cloudmusic.exception.login.400"));
        }

        if(code == 501){
            throw new ActionException(Text.translatable("cloudmusic.exception.login.501"));
        }

        if(code == 502){
            throw new ActionException(Text.translatable("cloudmusic.exception.login.502"));
        }

        if(code == 503){
            throw new ActionException(Text.translatable("cloudmusic.exception.login.503"));
        }

        if(code != 200){
            throw new ActionException(Text.translatable("cloudmusic.exception.login.err.code", json.toString()));
        }

        return result.getSetCookie();
    }

    private HttpURLConnection setRequestHeader(HttpURLConnection httpConnection){
        this.Header.forEach(httpConnection::setRequestProperty);
        return httpConnection;
    }

    private byte[] setData(Map<String, Object> data){
        StringBuilder dataStr = new StringBuilder();
        for (Entry<String, Object> value: data.entrySet()){
            dataStr.append(value.getKey()).append("=").append(URLEncoder.encode(String.valueOf(value.getValue()), StandardCharsets.UTF_8)).append("&");
        }
        return dataStr.toString().getBytes();
    }

    private static URLConnection openHttpUrlProxy(String httpUrl) throws IOException {
        if(!Configs.HTTP.HTTP_PROXY.getBooleanValue()){
            return new URL(httpUrl).openConnection();
        }

        return new URL(httpUrl).openConnection(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(
                Configs.HTTP.HTTP_PROXY_IP.getStringValue(),
                Configs.HTTP.HTTP_PROXY_PORT.getIntegerValue())
        ));
    }

    private HttpResult connection(String httpUrl, @Nullable Map<String, Object> data, Connection connection, int retry){
        HttpURLConnection httpConnection = null;
        InputStream inputStream = null;
        try {
            //创建连接
            httpConnection = this.setRequestHeader(connection.set((HttpURLConnection) openHttpUrlProxy(httpUrl)));
            if(data != null){
                httpConnection.getOutputStream().write(this.setData(data));
            }
            //设置连接超时时间
            httpConnection.setReadTimeout(Configs.HTTP.TIME_OUT.getIntegerValue() * 1000);
            httpConnection.connect();
            //获取响应数据
            int code = httpConnection.getResponseCode();
            if (code == 200) {
                inputStream = httpConnection.getInputStream();
                return new HttpResult(code, true, inputStream.readAllBytes(), httpConnection.getHeaderFields().get("Set-Cookie"));
            }else{
                inputStream = httpConnection.getErrorStream();
                return new HttpResult(code, false, inputStream.readAllBytes(), httpConnection.getHeaderFields().get("Set-Cookie"));
            }
        } catch (Exception err) {
            if(retry <= Configs.HTTP.MAX_RETRY.getIntegerValue()){
                throw new ActionException(Text.translatable("cloudmusic.exception.http", Configs.HTTP.MAX_RETRY.getIntegerValue(), err.getMessage()));
            }
            return this.connection(httpUrl, data, connection, ++retry);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException exception) {
                    exception.printStackTrace();
                }
            }
            //关闭远程连接
            assert httpConnection != null;
            httpConnection.disconnect();
        }
    }

    public static File download(String path, File targetFile) {
        return  HttpClient.download(path, targetFile, 0);
    }

    private static File download(String path, File targetFile, int retry) {
        try {
            if (!targetFile.getParentFile().exists()) {
                targetFile.getParentFile().mkdirs();
            } else if (targetFile.exists()){
                return targetFile;
            }

            // 统一资源
            URLConnection urlConnection = openHttpUrlProxy(path);
            HttpURLConnection httpURLConnection = (HttpURLConnection) urlConnection;
            // 设定请求的方法
            httpURLConnection.setInstanceFollowRedirects(true);
            httpURLConnection.setRequestMethod("GET");
            // 设置字符编码
            httpURLConnection.setRequestProperty("Charset", "UTF-8");
            httpURLConnection.setReadTimeout(Configs.HTTP.TIME_OUT.getIntegerValue() * 1000);
            // 打开到此 URL 引用的资源的通信链接
            httpURLConnection.connect();

            BufferedInputStream bin = new BufferedInputStream(httpURLConnection.getInputStream());
            OutputStream out = new FileOutputStream(targetFile);
            
            int size = 0;
            byte[] buf = new byte[1024];
            while ((size = bin.read(buf)) != -1) {
                out.write(buf, 0, size);
            }

            bin.close();
            out.close();
        } catch (Exception err) {
            if(retry <= Configs.HTTP.MAX_RETRY.getIntegerValue()){
                throw new ActionException(Text.translatable("cloudmusic.exception.http.download", Configs.HTTP.MAX_RETRY.getIntegerValue(), err.getMessage()));
            }
            return HttpClient.download(path, targetFile, ++retry);
        }
        return targetFile;
    }

    public static InputStream downloadStream(String path) {
        return HttpClient.downloadStream(path, 0);
    }

    private static InputStream downloadStream(String path, int retry) {
        InputStream bin = null;
        try {
            // 统一资源
            URLConnection urlConnection = openHttpUrlProxy(path);
            HttpURLConnection httpURLConnection = (HttpURLConnection) urlConnection;
            // 设定请求的方法
            httpURLConnection.setInstanceFollowRedirects(true);
            httpURLConnection.setRequestMethod("GET");
            // 设置字符编码
            httpURLConnection.setRequestProperty("Charset", "UTF-8");
            httpURLConnection.setReadTimeout(Configs.HTTP.TIME_OUT.getIntegerValue() * 1000);
            // 打开到此 URL 引用的资源的通信链接
            httpURLConnection.connect();

            bin = httpURLConnection.getInputStream();
        } catch (Exception err) {
            if(retry <= Configs.HTTP.MAX_RETRY.getIntegerValue()){
                throw new ActionException(Text.translatable("cloudmusic.exception.http.download", Configs.HTTP.MAX_RETRY.getIntegerValue(), err.getMessage()));
            }
            return HttpClient.downloadStream(path, ++retry);
        }
        return bin;
    }

    private interface Connection{
        HttpURLConnection set(HttpURLConnection connection);
    }

    public class HttpResult{
        public final int code;
        public final boolean status;
        private final byte[] data;
        private final List<String> cookies;

        public HttpResult(int code, boolean status, byte[] data, List<String> cookies){
            this.code = code;
            this.status = status;
            this.cookies = cookies;

            if(data == null){
                byte[] edata = {};
                this.data = edata;
                return;
            }
            this.data = data;
        }

        public String getString(){
            return new String(this.data, StandardCharsets.UTF_8);
        }

        public JsonObject getJson(){
            return JsonParser.parseString(getString()).getAsJsonObject();
        }

        public String getSetCookie(){
            Map<String, String> cookiesMap = new HashMap<>();
            for (String cookie : this.cookies) {
                String[] cookiekeys = cookie.split("; ")[0].split("=");
                if(cookiekeys.length == 1){
                    cookiesMap.put(cookiekeys[0], "");
                    continue;
                }
                cookiesMap.put(cookiekeys[0], cookiekeys[1]);
            }

            StringBuilder cookieData = new StringBuilder();
            for (Entry<String, String> cookiekeys: cookiesMap.entrySet()) {
                cookieData.append(cookiekeys.getKey()).append("=").append(cookiekeys.getValue()).append("; ");
            }
            return cookieData.toString();
        }
    }
}
