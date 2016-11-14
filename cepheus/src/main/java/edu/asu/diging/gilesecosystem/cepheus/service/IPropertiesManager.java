package edu.asu.diging.gilesecosystem.cepheus.service;

import java.util.Map;

import edu.asu.diging.gilesecosystem.cepheus.exceptions.CepheusPropertiesStorageException;

public interface IPropertiesManager {
    
    public final static String CEPHEUS_URL = "cepheus_url";
    public final static String PDF_TO_IMAGE_DPI = "pdf_to_image_dpi";
    public final static String PDF_TO_IMAGE_TYPE = "pdf_to_image_type";
    public final static String PDF_EXTRACT_TEXT = "pdf_extract_text";
    public final static String PDF_TO_IMAGE_FORMAT = "pdf_to_image_format";
     
    public final static String KAFKA_HOSTS = "kafka_hosts";
    public final static String KAFKA_EXTRACTION_COMPLETE_TOPIC = "topic_extract_text_request_complete";
    public final static String GILES_ACCESS_TOKEN = "giles_access_token";
    

    public abstract void setProperty(String key, String value) throws CepheusPropertiesStorageException;

    public abstract String getProperty(String key);

    public abstract void updateProperties(Map<String, String> props)
            throws CepheusPropertiesStorageException;

}
