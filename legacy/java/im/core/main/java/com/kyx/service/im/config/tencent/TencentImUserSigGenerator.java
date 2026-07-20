package com.kyx.service.im.config.tencent;

import com.kyx.foundation.common.util.json.JsonUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.Deflater;

@Component
public class TencentImUserSigGenerator {

    @Resource
    private TencentImProperties properties;

    public String genUserSig(String userId) {
        long currTime = System.currentTimeMillis() / 1000;
        long expire = properties.getExpireSeconds();

        Map<String, Object> sigDoc = new LinkedHashMap<>();
        sigDoc.put("TLS.ver", "2.0");
        sigDoc.put("TLS.identifier", userId);
        sigDoc.put("TLS.sdkappid", properties.getSdkAppId());
        sigDoc.put("TLS.expire", expire);
        sigDoc.put("TLS.time", currTime);
        sigDoc.put("TLS.sig", hmacSha256(userId, currTime, expire));

        Deflater compressor = new Deflater();
        compressor.setInput(JsonUtils.toJsonString(sigDoc).getBytes(StandardCharsets.UTF_8));
        compressor.finish();
        byte[] compressedBytes = new byte[2048];
        int compressedBytesLength = compressor.deflate(compressedBytes);
        compressor.end();
        return new String(base64EncodeUrl(Arrays.copyOfRange(compressedBytes, 0, compressedBytesLength)))
                .replaceAll("\\s*", "");
    }

    private String hmacSha256(String identifier, long currTime, long expire) {
        String contentToBeSigned = "TLS.identifier:" + identifier + "\n"
                + "TLS.sdkappid:" + properties.getSdkAppId() + "\n"
                + "TLS.time:" + currTime + "\n"
                + "TLS.expire:" + expire + "\n";
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(
                    properties.getSecretKey().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256");
            hmac.init(keySpec);
            byte[] sig = hmac.doFinal(contentToBeSigned.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(sig).replaceAll("\\s*", "");
        } catch (Exception e) {
            throw new IllegalStateException("Generate Tencent IM UserSig failed", e);
        }
    }

    private byte[] base64EncodeUrl(byte[] input) {
        byte[] base64 = Base64.getEncoder().encode(input);
        for (int i = 0; i < base64.length; ++i) {
            switch (base64[i]) {
                case '+':
                    base64[i] = '*';
                    break;
                case '/':
                    base64[i] = '-';
                    break;
                case '=':
                    base64[i] = '_';
                    break;
                default:
                    break;
            }
        }
        return base64;
    }
}
