package sk.dayz.modvalidator.ftp;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.springframework.stereotype.Component;
import sk.dayz.modvalidator.model.exception.FTPException;

import java.io.IOException;

@Slf4j
@Component
public class FtpConnector {

    public FTPClient connect(String host, int port, String user, String password) {
        log.info("Try to connect to FTP: {}", host);
        try {
            FTPClient ftp = new FTPClient();
            ftp.connect(host, port);
            int replyCode = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(replyCode)) {
                ftp.disconnect();
                throw new FTPException(String.format("Exception in connecting to FTP Server! Reply code: %s", replyCode));
            }
            ftp.login(user, password);
            log.info("Successfully connected to FTP!");
            return ftp;
        } catch (IOException e) {
            throw new FTPException("Failed to connect !", e);
        }
    }

    public FTPFile[] getDirectoriesInRoot(FTPClient ftpClient) {
        log.info("Getting directories in root directory from FTP ...");
        try {
            return ftpClient.listDirectories();
        } catch (IOException e) {
            throw new FTPException("Failed to execute command !", e);
        }
    }
}
