package edu.asu.diging.gilesecosystem.cepheus.kafka;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.asu.diging.gilesecosystem.cepheus.exceptions.CepheusExtractionException;
import edu.asu.diging.gilesecosystem.cepheus.service.IPropertiesManager;
import edu.asu.diging.gilesecosystem.cepheus.service.pdf.IImageExtractionManager;
import edu.asu.diging.gilesecosystem.cepheus.service.pdf.ITextExtractionManager;
import edu.asu.diging.gilesecosystem.requests.IImageExtractionRequest;
import edu.asu.diging.gilesecosystem.requests.ITextExtractionRequest;
import edu.asu.diging.gilesecosystem.requests.impl.ImageExtractionRequest;
import edu.asu.diging.gilesecosystem.requests.impl.TextExtractionRequest;

@PropertySource("classpath:/config.properties")
public class ExtractionRequestReceiver {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    @Autowired
    private ITextExtractionManager textExtractionManager;
    
    @Autowired
    private IImageExtractionManager imageExtractionManager;
    
    @Autowired
    protected IPropertiesManager propertiesManager;
    
    
    @KafkaListener(id="cepheus.extraction", topics = {"${topic_extract_text_request}", "${topic_extract_images_request}"})
    public void receiveMessage(String message, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        if (topic.equals(propertiesManager.getProperty(IPropertiesManager.KAFKA_EXTRACTION_TOPIC))) {
            extractText(message);
        } else if (topic.equals(propertiesManager.getProperty(IPropertiesManager.KAFKA_IMAGE_EXTRACTION_TOPIC))) {
            extractImage(message);
        }
    }

    private void extractText(String message) {
        ObjectMapper mapper = new ObjectMapper();
        ITextExtractionRequest request = null;
        try {
            request = mapper.readValue(message, TextExtractionRequest.class);
        } catch (IOException e) {
            logger.error("Could not unmarshall request.", e);
            // FIXME: handle this case
            return;
        }
        
        try {
            textExtractionManager.extractText(request);
        } catch (CepheusExtractionException e) {
           logger.error("Could not extract text.");
           // FIXME: send to monitoring app
        }
    }
    
    private void extractImage(String message) {
        ObjectMapper mapper = new ObjectMapper();
        IImageExtractionRequest request = null;
        try {
            request = mapper.readValue(message, ImageExtractionRequest.class);
        } catch (IOException e) {
            logger.error("Could not unmarshall request.", e);
            // FIXME: handle this case
            return;
        }
        
        try {
            imageExtractionManager.extractImages(request);
        } catch (CepheusExtractionException e) {
           logger.error("Could not extract text.");
           // FIXME: send to monitoring app
        }
    }
}
