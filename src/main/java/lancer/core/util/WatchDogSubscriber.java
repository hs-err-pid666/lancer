package lancer.core.util;

import java.nio.file.WatchEvent;

public interface WatchDogSubscriber {

    void onEvent(WatchEvent<?> event);
}
