package com.jaffa.rpc.test.callbacks;

import com.jaffa.rpc.lib.callbacks.Callback;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ServiceCallback implements Callback<Void> {


    @Override
    public void onSuccess(String key, Void result) {
        log.debug("Received in onSuccess {}", key);
    }

    @Override
    public void onError(String key, Throwable exception) {
        log.debug("Received in onError {}", key);
    }
}
