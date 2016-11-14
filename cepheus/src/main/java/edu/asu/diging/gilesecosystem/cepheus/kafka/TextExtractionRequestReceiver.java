package edu.asu.diging.gilesecosystem.cepheus.kafka;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.kafka.annotation.KafkaListener;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.asu.diging.gilesecosystem.cepheus.exceptions.CepheusTextExtractionException;
import edu.asu.diging.gilesecosystem.cepheus.service.pdf.ITextExtractionManager;
import edu.asu.diging.gilesecosystem.requests.ITextExtractionRequest;
import edu.asu.diging.gilesecosystem.requests.impl.TextExtractionRequest;

@PropertySource("classpath:/config.properties")
public class TextExtractionRequestReceiver {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    @Autowired
    private ITextExtractionManager extractionManager;
    
    @KafkaListener(id="cepheus.extract.text", topics = "${topic_extract_text_request}")
    public void receiveMessage(String message) {
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
            extractionManager.extractText(request);
        } catch (CepheusTextExtractionException e) {
           logger.error("Could not extract text.");
           // FIXME: send to monitoring app
        }
    }
}
