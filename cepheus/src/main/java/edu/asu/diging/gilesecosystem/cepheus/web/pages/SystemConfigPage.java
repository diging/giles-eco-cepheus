package edu.asu.diging.gilesecosystem.cepheus.web.pages;

public class SystemConfigPage {

    private String baseUrl;
    private String gilesAccessToken;

    
    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String nepomukUrl) {
        this.baseUrl = nepomukUrl;
    }
    
    public String getGilesAccessToken() {
        return gilesAccessToken;
    }

    public void setGilesAccessToken(String gilesAccessToken) {
        this.gilesAccessToken = gilesAccessToken;
    }

}
