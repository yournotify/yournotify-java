package com.yournotify;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class Yournotify {
    public static final class ApiException extends IOException {
        public final int status; public final String body; public final String requestId;
        ApiException(int status, String body, String requestId) { super("Yournotify API request failed with status " + status); this.status=status; this.body=body; this.requestId=requestId; }
    }
    private final String apiKey; private final String baseUrl; private final HttpClient http; private final int maxRetries;
    public Yournotify(String apiKey) { this(apiKey, "https://api.yournotify.com/", Duration.ofSeconds(30), 2); }
    public Yournotify(String apiKey, String baseUrl, Duration timeout, int maxRetries) {
        if (apiKey == null || apiKey.isBlank()) throw new IllegalArgumentException("Yournotify API key is required");
        this.apiKey=apiKey; this.baseUrl=baseUrl.replaceAll("/+$", "") + "/"; this.http=HttpClient.newBuilder().connectTimeout(timeout).build(); this.maxRetries=Math.max(0,maxRetries);
    }
    public String request(String method, String endpoint, Map<String, ?> data) throws IOException, InterruptedException {
        String url=baseUrl+endpoint.replaceFirst("^/+", "");
        if (method.equals("GET") && data != null && !data.isEmpty()) url += "?" + query(data);
        String body=data == null ? "{}" : json(data); Object idempotencyValue=data == null ? null : data.get("idempotency_key"); if(idempotencyValue==null&&data!=null)idempotencyValue=data.get("event_id"); String idempotency=Objects.toString(idempotencyValue,null);
        boolean retryable=Set.of("GET","HEAD","PUT","DELETE").contains(method) || idempotency != null;
        for(int attempt=0;;attempt++) {
            HttpRequest.Builder builder=HttpRequest.newBuilder(URI.create(url)).header("Authorization","Bearer "+apiKey).header("Accept","application/json");
            if(idempotency!=null) builder.header("Idempotency-Key",idempotency);
            builder.method(method, method.equals("GET") ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body)).header("Content-Type","application/json");
            try {
                HttpResponse<String> response=http.send(builder.build(),HttpResponse.BodyHandlers.ofString());
                if(response.statusCode()>=200 && response.statusCode()<300) return response.body();
                if(!retryable || attempt>=maxRetries || (response.statusCode()!=429 && response.statusCode()<500)) throw new ApiException(response.statusCode(),response.body(),response.headers().firstValue("x-request-id").orElse(null));
                long delay=response.headers().firstValue("retry-after").map(Long::parseLong).orElse(0L)*1000L; Thread.sleep(delay>0?delay:250L*(1L<<attempt));
            } catch(ApiException error) { throw error; }
              catch(IOException error) { if(!retryable || attempt>=maxRetries) throw error; Thread.sleep(250L*(1L<<attempt)); }
        }
    }
    public String identify(Map<String,?> data) throws Exception { return request("POST","automations/identify",data); }
    private static Map<String,Object> normalizeEvent(Map<String,?> input){Map<String,Object> value=new LinkedHashMap<>(input);value.putIfAbsent("event_id",Objects.toString(value.getOrDefault("idempotency_key",UUID.randomUUID().toString())));value.putIfAbsent("occurred_at",Instant.now().toString());return value;}
    public String track(Map<String,?> data) throws Exception { return request("POST","automations/events",normalizeEvent(data)); }
    public String trackBatch(List<Map<String,?>> events) throws Exception { return request("POST","automations/events/batch",Map.of("events",events.stream().map(Yournotify::normalizeEvent).toList())); }
    public String alias(Map<String,?> data) throws Exception { return request("POST","automations/alias",data); }
    public Channel email(){return new Channel("email");} public Channel sms(){return new Channel("sms");} public Channel whatsapp(){return new Channel("whatsapp");} public Channel voice(){return new Channel("voice");} public Channel push(){return new Channel("push");} public Channel inapp(){return new Channel("inapp");}
    public Contact contact(){return new Contact();} public Lists lists(){return new Lists();} public Rewards rewards(){return new Rewards();} public Loyalty loyalty(){return new Loyalty();} public Referrals referrals(){return new Referrals();}
    public final class Channel { private final String name; Channel(String name){this.name=name;} public String send(Map<String,Object> payload)throws Exception{Map<String,Object> data=new LinkedHashMap<>(payload);if(name.equals("voice"))return request("POST","campaigns/voice",data);data.put("channel",name);return request("POST","campaigns",data);} }
    public final class Contact { public String create(Map<String,?> d)throws Exception{return request("POST","contacts",d);} public String all(Map<String,?> p)throws Exception{return request("GET","contacts",p);} public String get(Object id)throws Exception{return request("GET","contacts/"+id,Map.of());} public String update(Object id,Map<String,?> d)throws Exception{return request("PUT","contacts/"+id,d);} public String delete(Object id)throws Exception{return request("DELETE","contacts/"+id,Map.of());} public String summary(Map<String,?> p)throws Exception{return request("GET","contacts/summary",p);} public String createSession(Map<String,?> d)throws Exception{return request("POST","contacts/session",d);} }
    public final class Lists { public String create(Map<String,?> d)throws Exception{return request("POST","lists",d);} public String all(Map<String,?> p)throws Exception{return request("GET","lists",p);} public String get(Object id)throws Exception{return request("GET","lists/"+id,Map.of());} public String update(Object id,Map<String,?> d)throws Exception{return request("PUT","lists/"+id,d);} public String delete(Object id)throws Exception{return request("DELETE","lists/"+id,Map.of());} public String export(Object id)throws Exception{return request("GET","lists/export/"+id,Map.of());} }
    public final class Rewards { public String all(Map<String,?>p)throws Exception{return request("GET","rewards",p);}public String get(Object id)throws Exception{return request("GET","rewards/"+id,Map.of());}public String create(Map<String,?>d)throws Exception{return request("POST","rewards",d);}public String update(Object id,Map<String,?>d)throws Exception{return request("PUT","rewards/"+id,d);}public String delete(Object id)throws Exception{return request("DELETE","rewards/"+id,Map.of());}public String issue(Map<String,?>d)throws Exception{return request("POST","rewards/send",d);} }
    public final class Loyalty { public String programs(Map<String,?>p)throws Exception{return request("GET","loyalty/programs",p);}public String createProgram(Map<String,?>d)throws Exception{return request("POST","loyalty/programs",d);}public String track(Object id,Map<String,?>d)throws Exception{return request("POST","loyalty/programs/"+id+"/events",d);}public String adjust(Object id,Map<String,?>d)throws Exception{return request("POST","loyalty/programs/"+id+"/points",d);}public String redeem(Object id,Map<String,?>d)throws Exception{return request("POST","loyalty/programs/"+id+"/redeem",d);} }
    public final class Referrals { public String programs(Map<String,?>p)throws Exception{return request("GET","referrals/programs",p);}public String createProgram(Map<String,?>d)throws Exception{return request("POST","referrals/programs",d);}public String track(Object id,Map<String,?>d)throws Exception{return request("POST","referrals/programs/"+id+"/events",d);}public String analytics(Object id,Map<String,?>p)throws Exception{return request("GET","referrals/programs/"+id+"/analytics",p);} }
    public static boolean verifyWebhook(byte[] payload, String signature, String timestamp, String secret, Duration tolerance) {
        try {
            Map<String,String> parts=new HashMap<>();
            for(String part:Objects.toString(signature,"").split(",")){int i=part.indexOf('=');if(i>0)parts.put(part.substring(0,i),part.substring(i+1));}
            String signedAt=(timestamp==null||timestamp.isBlank())?parts.getOrDefault("t",""):timestamp;
            String supplied=parts.getOrDefault("v1",Objects.toString(signature,""));
            if(supplied.startsWith("sha256="))supplied=supplied.substring(7);
            long seconds=Long.parseLong(signedAt);
            if(Math.abs(Instant.now().getEpochSecond()-seconds)>tolerance.toSeconds())return false;
            Mac mac=Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));
            mac.update((signedAt+".").getBytes(StandardCharsets.UTF_8));
            byte[] expected=mac.doFinal(payload);
            return MessageDigest.isEqual(expected,HexFormat.of().parseHex(supplied));
        } catch(Exception ignored){return false;}
    }
    private static String query(Map<String,?> data){StringJoiner join=new StringJoiner("&");data.forEach((k,v)->{if(v!=null)join.add(URLEncoder.encode(k,StandardCharsets.UTF_8)+"="+URLEncoder.encode(v.toString(),StandardCharsets.UTF_8));});return join.toString();}
    private static String json(Object value){if(value==null)return"null";if(value instanceof String s)return"\""+s.replace("\\","\\\\").replace("\"","\\\"")+"\"";if(value instanceof Number||value instanceof Boolean)return value.toString();if(value instanceof Map<?,?> map){StringJoiner j=new StringJoiner(",","{","}");map.forEach((k,v)->j.add(json(k.toString())+":"+json(v)));return j.toString();}if(value instanceof Iterable<?> list){StringJoiner j=new StringJoiner(",","[","]");list.forEach(v->j.add(json(v)));return j.toString();}return json(value.toString());}
}
