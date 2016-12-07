package edu.asu.diging.gilesecosystem.cepheus.service.pdf;

import edu.asu.diging.gilesecosystem.cepheus.exceptions.CepheusExtractionException;
import edu.asu.diging.gilesecosystem.requests.IImageExtractionRequest;

public interface IImageExtractionManager {

    public abstract void extractImages(IImageExtractionRequest request)
            throws CepheusExtractionException;

}