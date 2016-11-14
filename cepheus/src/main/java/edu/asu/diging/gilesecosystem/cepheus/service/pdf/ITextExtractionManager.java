package edu.asu.diging.gilesecosystem.cepheus.service.pdf;

import edu.asu.diging.gilesecosystem.cepheus.exceptions.CepheusTextExtractionException;
import edu.asu.diging.gilesecosystem.requests.ITextExtractionRequest;

public interface ITextExtractionManager {

    public abstract void extractText(ITextExtractionRequest request) throws CepheusTextExtractionException;

}