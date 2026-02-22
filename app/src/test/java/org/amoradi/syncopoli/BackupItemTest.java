package org.amoradi.syncopoli;

import org.junit.Test;
import java.util.ArrayList;
import static org.junit.Assert.*;

public class BackupItemTest {

    @Test
    public void testCopyConstructor_LocalDirection() {
        BackupItem original = new BackupItem();
        original.name = "Test Local Backup";
        original.direction = BackupItem.Direction.LOCAL;
        original.sources = new ArrayList<>();
        original.sources.add("/source/path");
        original.destination = "/dest/path";
        original.rsync_options = "--verbose";

        BackupItem copy = new BackupItem(original);

        assertEquals("Name should match", original.name, copy.name);
        assertEquals("Direction should match", original.direction, copy.direction);
        assertEquals("Direction should be LOCAL", BackupItem.Direction.LOCAL, copy.direction);
        assertEquals("Sources should match", original.sources, copy.sources);
        assertEquals("Destination should match", original.destination, copy.destination);
        assertEquals("Rsync options should match", original.rsync_options, copy.rsync_options);
    }

    @Test
    public void testCopyConstructor_IncomingDirection() {
        BackupItem original = new BackupItem();
        original.direction = BackupItem.Direction.INCOMING;
        original.sources = new ArrayList<>();

        BackupItem copy = new BackupItem(original);
        assertEquals("Direction should be INCOMING", BackupItem.Direction.INCOMING, copy.direction);
    }

    @Test
    public void testCopyConstructor_OutgoingDirection() {
        BackupItem original = new BackupItem();
        original.direction = BackupItem.Direction.OUTGOING;
        original.sources = new ArrayList<>();

        BackupItem copy = new BackupItem(original);
        assertEquals("Direction should be OUTGOING", BackupItem.Direction.OUTGOING, copy.direction);
    }
}
