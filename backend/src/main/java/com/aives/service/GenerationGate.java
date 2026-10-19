package com.aives.service;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.*;

/** Process-local duplicate/in-flight guard, not a cross-server persistent idempotency store. */
@Component
public class GenerationGate {
    private final Map<String,Instant> used=new HashMap<>();
    private final Set<Integer> active=new HashSet<>();
    public synchronized AutoCloseable begin(int actorId,String requestId) {
        try { if (!UUID.fromString(requestId).toString().equals(requestId)) throw new IllegalArgumentException(); }
        catch (Exception e) { throw new IllegalArgumentException("Idempotency-Key phải là UUID hợp lệ."); }
        Instant now=Instant.now(); used.values().removeIf(time->time.isBefore(now.minusSeconds(600)));
        String key=actorId+":"+requestId;
        if (used.containsKey(key) || active.contains(actorId)) throw new ResponseStatusException(HttpStatus.CONFLICT,"Yêu cầu đã được gửi hoặc đang xử lý. Không tự gửi lại.");
        if (used.size()>=5000 || active.size()>=4) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Hệ thống đang bận, thử lại sau.");
        used.put(key,now); active.add(actorId);
        return ()->{synchronized(this){active.remove(actorId);}};
    }
}
