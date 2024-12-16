import lancer.core.util.DirectoryWatchDog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.*;

public class DirectoryWatcherTest {

    @Test
    public void testSubDirectoryWatch(@TempDir Path sources) throws IOException, InterruptedException {
        System.out.println(sources);
        new DirectoryWatchDog(sources, 2).processEvents();
    }
}
