package com.yournotify;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

final class YournotifyTest {
    @Test void exposesLowercaseResources() {
        Yournotify sdk=new Yournotify("test");
        assertNotNull(sdk.inapp()); assertNotNull(sdk.voice()); assertNotNull(sdk.contact()); assertNotNull(sdk.lists()); assertNotNull(sdk.rewards()); assertNotNull(sdk.loyalty()); assertNotNull(sdk.referrals());
    }
    @Test void verifiesSignedWebhooks() throws Exception {
        byte[] body="{\"event\":\"reward.fulfilled\"}".getBytes(StandardCharsets.UTF_8);String timestamp=Long.toString(System.currentTimeMillis()/1000);String secret="secret";
        Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));mac.update((timestamp+".").getBytes(StandardCharsets.UTF_8));String signature=HexFormat.of().formatHex(mac.doFinal(body));
        assertTrue(Yournotify.verifyWebhook(body,"t="+timestamp+",v1="+signature,"",secret,Duration.ofMinutes(5)));
        assertFalse(Yournotify.verifyWebhook("changed".getBytes(StandardCharsets.UTF_8),"t="+timestamp+",v1="+signature,"",secret,Duration.ofMinutes(5)));
    }
}
