package centralSite;

import java.util.Set;
import java.util.HashSet;

public class Pair {
    Set<Integer> readList = new HashSet<>();
    Set<Integer> writeList = new HashSet<>();

    public Pair(Set<Integer> readList, Set<Integer> writeList) {
        this.readList = readList;
        this.writeList = writeList;
    }

    public Set<Integer> getReadList() {
        return readList;
    }

    public void setReadList(Set<Integer> readList) {
        this.readList = readList;
    }

    public Set<Integer> getWriteList() {
        return writeList;
    }

    public void setWriteList(Set<Integer> writeList) {
        this.writeList = writeList;
    }
}
