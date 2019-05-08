/**
 * CSV의 데이터를 읽어 저장할 Model Class
 * 
 *
 */
public class StorageModel {

    /**
     * yyyy-MM-dd
     */
    private String date;

    /**
     * hh:mm:ss
     */
    private String time;

    /**
     * source ip
     */
    private String sourceIp;

    /**
     * destination ip
     */
    private String destinationIp;

    /**
     * destination port
     */
    private String destinationPort;

    /**
     * currentTimeMillis
     */
    private long eventTime = 0L;

    public String getDestinationPort() {
        return destinationPort;
    }

    public void setDestinationPort(String destinationPort) {
        this.destinationPort = destinationPort;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getSourceIp() {
        return sourceIp;
    }

    public void setSourceIp(String sourceIp) {
        this.sourceIp = sourceIp;
    }

    public String getDestinationIp() {
        return destinationIp;
    }

    public void setDestinationIp(String destinationIp) {
        this.destinationIp = destinationIp;
    }

    public long getEventTime() {
        return eventTime;
    }

    public void setEventTime(long eventTime) {
        this.eventTime = eventTime;
    }

    @Override
    public String toString() {
        return "StorageModel [date=" + date + ", time=" + time + ", sourceIp=" + sourceIp + ", destinationIp=" + destinationIp + ", destinationPort=" + destinationPort + ", eventTime=" + eventTime + "]";
    }

}
