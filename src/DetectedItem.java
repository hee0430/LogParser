import java.util.ArrayList;
import java.util.List;

/**
 * 검출된 항목 저장 class
 * 
 *
 */
public class DetectedItem {
    private String source;
    private String destination;
    private String port;
    private String key;
    private String firstEventDate;
    private long firstEventTime;
    private int accessCount = 1;
    private List<StorageModel> modelList = new ArrayList<>();

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public long getFirstEventTime() {
        return firstEventTime;
    }

    public void setFirstEventTime(long firstEventTime) {
        this.firstEventTime = firstEventTime;
    }

    public int getAccessCount() {
        return accessCount;
    }

    public void setAccessCount(int accessCount) {
        this.accessCount = accessCount;
    }

    public List<StorageModel> getModelList() {
        return modelList;
    }

    public void setModelList(List<StorageModel> modelList) {
        this.modelList = modelList;
    }

    public String getFirstEventDate() {
        return firstEventDate;
    }

    public void setFirstEventDate(String firstEventDate) {
        this.firstEventDate = firstEventDate;
    }

    @Override
    public String toString() {
        return "DetectedItem [source=" + source + ", destination=" + destination + ", port=" + port + ", key=" + key + ", firstEventDate=" + firstEventDate + ", firstEventTime=" + firstEventTime + ", accessCount=" + accessCount + ", modelList=" + modelList
                + "]";
    }

}
