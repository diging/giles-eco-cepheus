package edu.asu.diging.gilesecosystem.cepheus.service;

public interface IKafkaListenerManager {

    void shutdownListeners();

    void startListeners();

    boolean isListening();

}