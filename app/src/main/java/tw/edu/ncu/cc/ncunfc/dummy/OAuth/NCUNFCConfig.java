package tw.edu.ncu.cc.ncunfc.dummy.OAuth;

public class NCUNFCConfig {

    private String serverAddress;
    private String language;

    public NCUNFCConfig(String serverAddress, String language) {
        this.serverAddress = serverAddress;
        this.language = language;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public String getLanguage() {
        return language;
    }
}