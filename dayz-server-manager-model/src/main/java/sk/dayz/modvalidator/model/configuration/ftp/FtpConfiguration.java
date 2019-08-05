package sk.dayz.modvalidator.model.configuration.ftp;

import lombok.Data;

@Data
public class FtpConfiguration {

    private String host;
    private int port;
    private String username;
    private String password;

    @Override
    public String toString() {
        return "FtpConfiguration{" +
                "host='" + host + '\'' +
                ", port=" + port +
                ", username='" + username + '\'' +
                ", password='" + "*******" + '\'' +
                '}';
    }
}
