package lancer.core.util;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static java.nio.file.StandardWatchEventKinds.*;

public class DirectoryWatchDog {

    private final Map<WatchKey, Path> observables = new ConcurrentHashMap<>();
    private final Set<WatchDogSubscriber> subscribers = new HashSet<>(2, .9F);

    private final WatchService watcher;
    private final Path root;
    private final int depth;

    public DirectoryWatchDog(Path path, int depth) throws IOException {
        this.watcher = FileSystems.getDefault().newWatchService();
        this.root = path;
        this.depth = depth;
        // registering root path with sub-dirs existence check and registering:
        walkAndRegisterDirectories(this.root);
    }

    /**
     * @param subscriber instance that will catch events
     * @return success of event subscription if true otherwise false == or if subscriber already exists in list
     */
    public boolean subscribeEvents(WatchDogSubscriber subscriber) {
        return this.subscribers.add(subscriber);
    }

    public void registerDirectory(Path dir) throws IOException {
        if (this.root.relativize(dir).getNameCount() >= this.depth)
            return;

        this.observables.put(
            dir.register(this.watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY),
            dir
        );
    }

    private void walkAndRegisterDirectories(Path initialRoute) throws IOException {
        if (!Files.isDirectory(initialRoute) )
            return;

        Files.walkFileTree(initialRoute,
                Collections.emptySet(),
                this.depth,
                new SimpleFileVisitor<>() {

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                registerDirectory(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public void processEvents() throws InterruptedException {
        WatchKey key;
        while ((key = this.watcher.take()) != null) {

            Path dir = this.observables.get(key);
            if (dir == null) {
                System.err.println("WatchKey not recognized!!");
                continue;
            }

            for (WatchEvent<?> event : key.pollEvents() ) {
                this.subscribers.forEach(s -> s.onEvent(event) );

                @SuppressWarnings("rawtypes")
                WatchEvent.Kind kind = event.kind();

                // Context for directory entry event is the file name of entry
                @SuppressWarnings("unchecked")
                Path name = ((WatchEvent<Path>) event).context();
                Path child = dir.resolve(name);

                // print out event
                System.out.format("%s: %s\n", event.kind().name(), child);

                // if directory is created, and watching recursively, then register it and its subdirectories
                if (kind == ENTRY_CREATE) {
                    try {
                        if (Files.isDirectory(child)) {
                            walkAndRegisterDirectories(child);
                        }
                    } catch (IOException x) {
                        // do something useful
                    }
                }
            }

            // reset key and remove from set if directory no longer accessible
            boolean valid = key.reset();
            if (!valid) {
                this.observables.remove(key);

                // all directories are inaccessible
                if (this.observables.isEmpty()) {
                    break;
                }
            }
        }
    }
}
