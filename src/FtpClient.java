import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

public class FtpClient {
    String ip;
    String id;
    String pwd;
    int port;
    String downloadPath;

    /**
     * 생성자
     *
     * @param ip
     * @param id
     * @param pwd
     * @param port
     * @param downloadPath
     */
    public FtpClient(String ip, int port, String id, String pwd, String downloadPath) {
        this.ip = ip;
        this.id = id;
        this.pwd = pwd;
        this.port = port;
        this.downloadPath = downloadPath;
    }

    /**
     * 다운로드
     *
     * @param filePath
     * @param fileName
     * @return
     * @throws Exception
     */
    public int download(String filePath, String fileName) throws Exception {
        FTPClient client = null;
        BufferedOutputStream bos = null;
        File fPath = null;
        File fDir = null;
        File f = null;

        int result = -1;

        try {
            // download 경로에 해당하는 디렉토리 생성
            fPath = new File(downloadPath);
            fDir = fPath;
            fDir.mkdirs();

            f = new File(downloadPath, fileName);

            client = new FTPClient();
            client.setControlEncoding("UTF-8");
            client.connect(ip, port);

            int resultCode = client.getReplyCode();
            if (FTPReply.isPositiveCompletion(resultCode) == false) {
                client.disconnect();
                throw new Exception("FTP 서버에 연결할 수 없습니다.");
            } else {
                client.setSoTimeout(5000);
                boolean isLogin = client.login(id, pwd);

                if (isLogin == false) {
                    throw new Exception("FTP 서버에 로그인 할 수 없습니다.");
                }

                client.setFileType(FTP.BINARY_FILE_TYPE);
                client.changeWorkingDirectory(filePath);

                bos = new BufferedOutputStream(new FileOutputStream(f));
                boolean isSuccess = client.retrieveFile(fileName, bos);

                if (isSuccess) {
                    result = 1; // 성공
                } else {
                    throw new Exception("파일 다운로드를 할 수 없습니다.");
                }

                client.logout();
                result = 1;
            }
        } catch (Exception e) {
            throw new Exception("FTP Exception : " + e);
        }
        return result;
    }
}
