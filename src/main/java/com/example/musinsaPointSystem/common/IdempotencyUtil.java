package com.example.musinsaPointSystem.common;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Component;

@Component
public class IdempotencyUtil {
    public String createRequestKey(
            String condition,
            String purpose,
            String areaName
    ) {

        String raw = condition + "|" + purpose + "|" + areaName;

        return DigestUtils.sha256Hex(raw);
    }
}
