package ryzen.ownitall;

public class YoutubeCredentials {
    private String applicationName;
    private String clientId;
    private String clientSecret;

    public YoutubeCredentials() {
        System.out.println("Enter youtube application name: ");
        this.applicationName = Input.getInstance().getString();
        System.out.println("Enter youtube client id: ");
        this.clientId = Input.getInstance().getString();
        System.out.println("Enter youtube client secret: ");
        this.clientSecret = Input.getInstance().getString();
    }

    public YoutubeCredentials(String applicationName, String clientId, String clientSecret) {
        this.applicationName = applicationName;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public String getClientId() {
        return this.clientId;
    }

    public String getClientSecret() {
        return this.clientSecret;
    }

    public String getApplicationName() {
        return this.applicationName;
    }

    public boolean isNull() {
        if (this.getClientId() == null || this.getClientSecret() == null || this.getApplicationName() == null) {
            return true;
        }
        return false;
    }
}
