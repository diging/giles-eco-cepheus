package edu.asu.diging.gilesecosystem.cepheus.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import edu.asu.diging.gilesecosystem.cepheus.kafka.ExtractionRequestReceiver;
import edu.asu.diging.gilesecosystem.cepheus.service.IPropertiesManager;
import edu.asu.diging.gilesecosystem.requests.kafka.KafkaConfig;

@Configuration
@EnableKafka
public class CepheusKafkaConfig implements KafkaConfig {
    
    @Autowired
    private IPropertiesManager propertiesManager;
    
    @Bean
    public Map<String, Object> consumerConfigs() {
        Map<String, Object> props = new HashMap<>();
        // list of host:port pairs used for establishing the initial connections
        // to the Kakfa cluster
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                getHosts());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                IntegerDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class);
        props.put(ConsumerConfig.CLIENT_ID_CONFIG, "geco.consumer.cepheus." + new Random().nextInt(100));
        // consumer groups allow a pool of processes to divide the work of
        // consuming and processing records
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "gileseco.pdf.processing");

        return props;
    }

    @Bean
    public ConsumerFactory consumerFactory() {
        return new DefaultKafkaConsumerFactory<>(consumerConfigs());
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());

        return factory;
    }

    @Bean
    public ExtractionRequestReceiver receiver() {
        return new ExtractionRequestReceiver();
    }

    @Override
    public String getHosts() {
        return propertiesManager.getProperty(IPropertiesManager.KAFKA_HOSTS);
    }

    @Override
    public String getProducerId() {
        return "geco.producer.cepheus." + new Random().nextInt(100);
    }

}
