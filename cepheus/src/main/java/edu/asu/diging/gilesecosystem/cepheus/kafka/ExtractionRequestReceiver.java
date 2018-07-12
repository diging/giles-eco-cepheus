package edu.asu.diging.gilesecosystem.cepheus.kafka;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.asu.diging.gilesecosystem.cepheus.exceptions.CepheusExtractionException;
import edu.asu.diging.gilesecosystem.cepheus.service.pdf.IImageExtractionManager;
import edu.asu.diging.gilesecosystem.requests.IImageExtractionRequest;
import edu.asu.diging.gilesecosystem.requests.impl.ImageExtractionRequest;
import edu.asu.diging.gilesecosystem.septemberutil.properties.MessageType;
import edu.asu.diging.gilesecosystem.septemberutil.service.ISystemMessageHandler;

@PropertySource("classpath:/config.properties")
public class ExtractionRequestReceiver {

    @Autowired
    private ISystemMessageHandler messageHandler;

    @Autowired
    private IImageExtractionManager imageExtractionManager;

    @KafkaListener(id = "cepheus.extraction", topics = { "${topic_extract_images_request}" })
    public void receiveMessage(String message, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        extractImage(message);
    }

    private void extractImage(String message) {
        ObjectMapper mapper = new ObjectMapper();
        IImageExtractionRequest request = null;
        try {
            request = mapper.readValue(message, ImageExtractionRequest.class);
        } catch (IOException e) {
            messageHandler.handleMessage("Could not unmarshell request", e, MessageType.ERROR);
            // FIXME: handle this case
            return;
        }

        try {
            imageExtractionManager.extractImages(request);
        } catch (CepheusExtractionException e) {
            messageHandler.handleMessage("Could not extract text.", e, MessageType.ERROR);
        }
    }
}
