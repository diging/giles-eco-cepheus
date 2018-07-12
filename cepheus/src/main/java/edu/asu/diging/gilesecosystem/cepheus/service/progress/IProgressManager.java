package edu.asu.diging.gilesecosystem.cepheus.service.progress;

import edu.asu.diging.gilesecosystem.requests.IImageExtractionRequest;

public interface IProgressManager {

    void startNewRequest(IImageExtractionRequest request);

    void setTotalPages(int total);

    void updateCurrentPage(int currentPage);

    void setPhase(ProgressPhase phase);

    ProgressPhase getPhase();

    IImageExtractionRequest getCurrentRequest();

    int getCurrentPage();

    int getTotalPages();

    void reset();

}