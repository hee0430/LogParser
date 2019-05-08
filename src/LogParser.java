import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 로그 분석
 *
 * @ 3개의 파라메터를 받아서 실행한다.
 *   1 시간 범위
 *   2 횟수
 *   3 포트( 다중 포트는 ','(콤마)로 구분해서 입력
 */
public class LogParser {

    private final static String sepChr = "_";
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static String[] targetPortArray = { "20", "21", "22", "443", "135", "137", "25" };
    private static Map<String, String> COUNTRY_CODE = new HashMap<>();;
    private static String PERIOD;
    private static String ACCESSCOUNT;
    private static String SEARCHPORT;

    public static void main(String[] args) throws IOException, ParseException {

        /**
         * 검색 조건 값을 설정한다.
         */
        // 다중접속 건으로 판단할 기준 시간. ex) 20(초)
        PERIOD = "120";
        // period 시간 내에 초과 접속으로 판단할 기준값. ex) 10(회)
        ACCESSCOUNT = "2";
        // 검색 대상이 될 port. 단일 포트를 입력하거나 ,로 구분하여 여러개 입력 가능
        SEARCHPORT = "20,21,22,443,135,137,25";

        // 검출된 모든 source IP정보를 담아놓을 리스트, 자연스러운 중복데이터 제거를 위해  Map 형식으로 선언함
        Map<String, Object> detectedSourceIpMap = new HashMap<>();

        System.out.println("-Load country Code-");
        // 국가 코드 로딩
        readCountryCodeCsv();

        // ','로 구분되어 있는경우가 있으므로 split하여 array로 변경
        String[] searchPortArray = SEARCHPORT.split("\\,");

        //--------FTP 추가  : 시작
        //FTP에 접속해서 파일을 받아온다.
        // ip, port, id, pw, download경로

/*        FtpClient ftp = new FtpClient("192.168.1.64", 21, "aster", "aster", "data");
try {
    ftp.download("ftp경로", "받아올파일명");
} catch (Exception e) {
    System.out.println("FTP 파일 다운로드 실패");
    e.printStackTrace();
}*/
        //--------FTP 추가  : 종료

        // 1. data 폴더 아래의 파일들을 List<Model>로 읽어온다. 읽어올 파일의 확장자는 csv로 한정한다.
        System.out.println("-Load log files-");
        List<StorageModel> storageList = new ArrayList<>();
        readCsvToStorageList(storageList, "data");

        System.out.println("-Find 1:1-");
        // 2. storageList 에서 1:1 의 경우를 검색하고 결과 폴더에 파일을 생성한다.
        Map<String, DetectedItem> oneByOneMap = findMatches(storageList, PERIOD, ACCESSCOUNT, searchPortArray, SearchModeType.ONE_VS_ONE, detectedSourceIpMap);
        //결과 출력
        writeResult(oneByOneMap, SearchModeType.ONE_VS_ONE, ACCESSCOUNT);

        System.out.println("-Find 1:N-");
        // 3.storageList 에서 1:N 의 경우를 검색하고 결과 폴더에 파일을 생성한다.
        Map<String, DetectedItem> oneByMultiMap = findMatches(storageList, PERIOD, ACCESSCOUNT, searchPortArray, SearchModeType.ONE_VS_MULTI, detectedSourceIpMap);
        //결과 출력
        writeResult(oneByMultiMap, SearchModeType.ONE_VS_MULTI, ACCESSCOUNT);

        // 4. 2차 파일 읽어온다.
        System.out.println("-Load second log files-");
        storageList = new ArrayList<>();
        readCsvToStorageList(storageList, "data2");

        // 5. 2차 파일에서 검색한다.
        System.out.println("-Find second data");
        Map<String, DetectedItem> secondSearchMap = findMatches2(detectedSourceIpMap, storageList);
        //결과 출력
        writeResult(secondSearchMap, SearchModeType.ETC, "");

    }

    /**
     * csv 파일을 읽어 검색 대상으로 사용할 list 형태로 변경
     *
     * @throws IOException
     * @throws ParseException
     */
    private static void readCsvToStorageList(List<StorageModel> storageList, String dirName) throws IOException, ParseException {
        // data 폴더 하위에 존재하는 파일 중 csv 파일만 골라온다.
        String csvBaseDir = dirName;
        File baseDir = new File(csvBaseDir);
        File[] targetFileList = baseDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".csv") ? true : false;
            }
        });

        // 대상 파일을 읽어 필요한 내용을 storageList에 적재한다.
        // TODO OOM이 발생할수 있음
        RandomAccessFile file = null;
        // data 폴더 하위에 존재하는 csv 파일 수만큼 loop
        for (File target : targetFileList) {
            file = new RandomAccessFile(target, "r");
            String line;
            // cvs 파일에 존재하는 로그의 line 수 만큼 loop
            while ((line = file.readLine()) != null) {
                // 읽어온 데이터가 공백인 경우 skip하고 다음 데이터 처리
                if (line.trim() == "") {
                    continue;
                }
                storageList.add(parseLineToModel(line));
            }
        }
    }

    /**
     * Ipv4 IP Address를 C Class 까지만 남긴 형태로 반환
     *
     * @param plainIp
     * @return
     */
    private static String makeCClassIp(String plainIp) {
        String[] ipArray = plainIp.split("\\.");
        return ipArray[0] + "." + ipArray[1] + "." + ipArray[2];
    }

    /**
     * line parse
     *
     * @param line
     *            csv 파일에서 읽어온 1줄
     * @return StorageModel
     * @throws ParseException
     */
    private static StorageModel parseLineToModel(String line) throws ParseException {
        StorageModel model = new StorageModel();
        Pattern pattern = Pattern.compile(",");
        Pattern pattern2 = Pattern.compile("=");

        //csv 파일의 1줄을 , 구분자를 이용하여 각 항목으로 분리한다.
        String[] lineSplitArray = pattern.split(line);

        // 필요한 정보만 StrageModel에 담는다.
        for (String column : lineSplitArray) {
            //key=value 형태로 분리된 항목을 다시 = 구분자로 분리한다.
            String[] columnSplitArray = pattern2.split(column);
            if (columnSplitArray.length < 2) {
                continue;
            }
            String value = columnSplitArray[1].replace("\"", "");
            if (columnSplitArray[0].equals("srcip")) { // 출발지아이피
                model.setSourceIp(value);
            } else if (columnSplitArray[0].equals("dstip")) { // 목적지아이피
                model.setDestinationIp(value);
            } else if (columnSplitArray[0].equals("dstport")) { // 목적지포트
                model.setDestinationPort(value);
            } else if (columnSplitArray[0].equals("date")) { // 이벤트발생시간
                model.setDate(value);
            } else if (columnSplitArray[0].equals("time")) { // 이벤트발생시간
                model.setTime(value);
            }
        }

        // 날짜 정보로 currentTimeMillis 값을 구해온다.
        model.setEventTime(dateFormat.parse(model.getDate() + " " + model.getTime()).getTime());
        return model;
    }

    /**
     * storageList에서 검색 대상을 검색한다.
     *
     * @param storageList
     * @param period
     * @param accessCount
     * @param searchPortArray
     * @param searchMode
     */
    private static Map<String, DetectedItem> findMatches(List<StorageModel> storageList, String period, String accessCount, String[] searchPortArray, SearchModeType searchMode, Map<String, Object> detectedSourceIpMap) {
        Map<String, DetectedItem> detectedItemMap = new HashMap<>();
        // 로그 파일에서 가쟈온 전체 검색 대상 목록 loop
        for (StorageModel model : storageList) {
            // 파라메터로 입력한 검색하기를 원하는 포트 목록 loop
            for (String port : searchPortArray) {
                // 1. 해당 접속 로그가 검색 대상 포트와 일치하는 경우
                if (model.getDestinationPort() != null && model.getEventTime() != 0 && model.getDestinationPort().equals(port)) {
                    // 1.1 검색 대상 포트와 일치 하는 경우 : source 주소+목적지 주소+포트 값을 key로 하고, 최초
                    // 접속시간 기준으로 검색 기준시간 내에 몇번이나 접속하였는가 판단한다.

                    String key = "";
                    if (searchMode == SearchModeType.ONE_VS_ONE) {
                        // source+목적지+포트를 키 조합으로 사용
                        key = model.getSourceIp() + sepChr + model.getDestinationIp() + sepChr + model.getDestinationPort();
                    } else if (searchMode == SearchModeType.ONE_VS_MULTI) {
                        // source +목적지 포트를 키 조합으로 사용
                        key = model.getSourceIp() + sepChr + model.getDestinationPort();
                    }

                    if (!detectedItemMap.containsKey(key)) {
                        /**
                         * 검색 대상 데이터에 해당하지만 처음으로 접속한 데이터를 찾은경우에 대해 처리한다.
                         * setInitData()를 이용하여 필요한 값을 DetectedItem에 설정하고
                         * detectedItemMap에 key와 함께 담는다.
                         */
                        DetectedItem item = new DetectedItem();
                        setInitData(item, model);
                        detectedItemMap.put(key, item);

                        //source ip 저장, value는 무시하므로 null로 설정
                        detectedSourceIpMap.put(model.getSourceIp(), null);
                    } else {
                        // 두번째 검출되었다면 처음 검출되었을때의 시간값과 비교하여 기준범위 안쪽인지 판단한다.

                        /**
                         * 검색 대상 데이터에 해당하고 두번째로 접속한 데이터를 찾은경우에 대해 처리한다.
                         * 처음 접속한 시간 데이터를 detectedItemMap으로부터 꺼내 현재 접속 데이터와 처음
                         * 접속 데이터의 접속시간 차를 계산한다.
                         */
                        DetectedItem item = detectedItemMap.get(key);

                        // 1. (x초내에 y번 접속에 대한 검출 조건 확인 )
                        if ((model.getEventTime() - item.getFirstEventTime()) < (Long.parseLong(period) * 1000L)) {
                            // 계산된 시간차가 허용 범위 안쪽이라면  검출 대상으로 판단하고 처리한다. (접속횟수 +1, 접속로그 데이터 목록에 추가)
                            item.setAccessCount(item.getAccessCount() + 1);
                            item.getModelList().add(model);
                        } else {
                            /**
                             * 계산된 시간차가 허용범위 바깥이라면 이전에 저장해놓은 최초 접속 시간값이 의미가
                             * 없어지므로 이전 데이터는 삭제하고
                             * 현재 검출된 데이터로 최초 접속시간 값을 설정해준다.
                             */
                            detectedItemMap.remove(key);

                            // 현재값을 초기값으로 사용
                            item = new DetectedItem();
                            setInitData(item, model);
                            detectedItemMap.put(key, item);
                        }

                    }
                }
            }
        }
        return detectedItemMap;
    }

    /**
     * 검출 아이템 객체 초기값 생성
     *
     * @param item
     * @param model
     */
    private static void setInitData(DetectedItem item, StorageModel model) {
        item.setFirstEventTime(model.getEventTime());
        item.setFirstEventDate(dateFormat.format(model.getEventTime()));
        item.setSource(model.getSourceIp());
        item.setDestination(model.getDestinationIp());
        item.setPort(model.getDestinationPort());
        item.getModelList().add(model);
    }

    /**
     * 2차 리스트에서 검색
     *
     * @param detectedSourceIpMap
     * @param storageList
     */
    private static Map<String, DetectedItem> findMatches2(Map<String, Object> detectedSourceIpMap, List<StorageModel> storageList) {
        Map<String, DetectedItem> detectedItemMap = new HashMap<>();
        // 1차에서 찾아낸 source ip로 2차 로그 파일을 검색한다.
        Iterator<String> it = detectedSourceIpMap.keySet().iterator();
        while (it.hasNext()) {
            String sourceIp = it.next();
            for (StorageModel secondModel : storageList) {
                String key = secondModel.getSourceIp() + sepChr + secondModel.getDestinationPort();

                // 1차 소스아이피와 2차의 소스 아이피가 같으면서, 포트가 targetPortArray에 해당하는 경우 검출대상
                if (secondModel.getSourceIp().equals(sourceIp) && comparePort(secondModel.getDestinationPort())) {
                    if (!detectedItemMap.containsKey(key)) {
                        // 처음 검출된 경우 초기값 설정
                        DetectedItem item = new DetectedItem();
                        setInitData(item, secondModel);
                        detectedItemMap.put(key, item);
                    } else {
                        // 2회차 이상인 경우 count +1
                        DetectedItem item = detectedItemMap.get(key);
                        item.setAccessCount(item.getAccessCount() + 1);
                        item.getModelList().add(secondModel);
                    }
                }
            }
        }

        return detectedItemMap;
    }

    /**
     * 2차 검색 조건에 맞는 포트인지 확인하여 true / false를 반환
     *
     * @param port
     * @return
     */
    private static boolean comparePort(String port) {
        boolean result = false;
        for (String portItem : targetPortArray) {
            if (port.equals(portItem)) {
                result = true;
                break;
            }
        }

        return result;
    }

    /**
     * 검색 결과 출력
     *
     * @param detectedItemMap
     * @param searchMode
     * @param accessCount
     */
    private static void writeResult(Map<String, DetectedItem> detectedItemMap, SearchModeType searchMode, String accessCount) {
        String format = "";
        if (detectedItemMap != null) {
            if (searchMode != SearchModeType.ETC) {
                // 1차 검색결과 프린트 헤더
                format = "%20s%20s%10s%20s%10s";
                System.out.println("------------------------------------------------------------------------------------");
                System.out.println(String.format(format, "source", "destination", "port", "first asscess", "count"));
                System.out.println("------------------------------------------------------------------------------------");
            } else {
                // 2차 검색결과 프린트 헤더
                format = "%20s%20s%10s%20s%10s%10s";
                System.out.println("------------------------------------------------------------------------------------");
                System.out.println(String.format(format, "source", "destination", "port", "first asscess", "count", "country"));
                System.out.println("------------------------------------------------------------------------------------");

            }
            Iterator<String> it = detectedItemMap.keySet().iterator();
            while (it.hasNext()) {
                String key = it.next();
                DetectedItem item = detectedItemMap.get(key);

                if (searchMode != SearchModeType.ETC) {
                    // 1차 검색 결과 프린트
                    if (item.getAccessCount() >= Integer.parseInt(accessCount)) {

                        if (searchMode == SearchModeType.ONE_VS_ONE) {
                            System.out.println(String.format(format, item.getSource(), item.getDestination(), item.getPort(), dateFormat.format(new Date(item.getFirstEventTime())), item.getAccessCount()));
                        } else if (searchMode == SearchModeType.ONE_VS_MULTI) {
                            //1:N의 경우엔 목적지 포트를 대상으로 검색하므로,  요약 항목엔 목적지 아이피를 보여주지 않는다.
                            System.out.println(String.format(format, item.getSource(), "", item.getPort(), dateFormat.format(new Date(item.getFirstEventTime())), item.getAccessCount()));
                        }

                        System.out.println("상세------------------------------------------------------------------------------------");
                        for (StorageModel storageModel : item.getModelList()) {
                            System.out.println(String.format(format, storageModel.getSourceIp(), storageModel.getDestinationIp(), storageModel.getDestinationPort(), storageModel.getDate() + " " + storageModel.getTime(), ""));
                        }
                        System.out.println("----------------------------------------------------------------------------------------");
                    }
                } else {
                    // 2차 검색결과 프린트
                    System.out.println(String.format(format, item.getSource(), item.getDestination(), item.getPort(), dateFormat.format(new Date(item.getFirstEventTime())), item.getAccessCount(), ""));

                    System.out.println("상세------------------------------------------------------------------------------------");
                    for (StorageModel storageModel : item.getModelList()) {
                        System.out.println(String.format(format, storageModel.getSourceIp(), storageModel.getDestinationIp(), storageModel.getDestinationPort(), storageModel.getDate() + " " + storageModel.getTime(), "",
                                getContryCode(storageModel.getDestinationIp())));
                    }
                    System.out.println("----------------------------------------------------------------------------------------");
                }
            }
        }
    }

    /**
     * 국가 코드 파일 읽어 메모리에 올린다.
     *
     * @throws IOException
     * @throws ParseException
     */
    private static void readCountryCodeCsv() throws IOException, ParseException {

        File baseDir = new File("address" + File.separatorChar + "address.csv");

        RandomAccessFile file = null;
        int i = 0;
        // data 폴더 하위에 존재하는 csv 파일 수만큼 loop
        try {
            file = new RandomAccessFile(baseDir, "r");
            String line;
            // cvs 파일에 존재하는 로그의 line 수 만큼 loop
            while ((line = file.readLine()) != null) {
                // 첫 행 무시
                if (i == 0) {
                    i++;
                    continue;
                }
                // 읽어온 데이터가 공백인 경우 skip하고 다음 데이터 처리
                if (line.trim() == "") {
                    continue;
                }

                String[] lineArray = line.split(",");

                COUNTRY_CODE.put(makeCClassIp(lineArray[1]), lineArray[0]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (file != null)
                file.close();
        }
    }

    /**
     * 목적지 아이피에 따른 국가 코드 반환
     *
     * @param ip
     * @return
     */
    private static String getContryCode(String ip) {
        String cClassIp = makeCClassIp(ip);
        return COUNTRY_CODE.get(cClassIp);
    }
}
