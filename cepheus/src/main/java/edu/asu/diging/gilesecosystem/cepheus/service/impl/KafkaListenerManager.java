package edu.asu.diging.gilesecosystem.cepheus.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.stereotype.Service;

import edu.asu.diging.gilesecosystem.cepheus.service.IKafkaListenerManager;

@Service
public class KafkaListenerManager implements IKafkaListenerManager {

    @Autowired
    private KafkaListenerEndpointRegistry registry;
    
    /* (non-Javadoc)
     * @see edu.asu.diging.gilesecosystem.cepheus.service.impl.IKafkaListenerManager#shutdownListeners()
     */
    @Override
    public void shutdownListeners() {
        if (registry.isRunning()) {
            registry.stop();
        }
    }
    
    /* (non-Javadoc)
     * @see edu.asu.diging.gilesecosystem.cepheus.service.impl.IKafkaListenerManager#startListeners()
     */
    @Override
    public void startListeners() {
        if (!registry.isRunning()) {
            registry.start();
        }
    }
    
    /* (non-Javadoc)
     * @see edu.asu.diging.gilesecosystem.cepheus.service.impl.IKafkaListenerManager#isListening()
     */
    @Override
    public boolean isListening() {
        return registry.isRunning();
    }
}
