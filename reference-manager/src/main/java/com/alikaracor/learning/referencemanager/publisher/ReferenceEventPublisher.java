package com.alikaracor.learning.referencemanager.publisher;

import com.alikaracor.learning.referencemanager.event.ReferenceEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class ReferenceEventPublisher {

   private static final String  Topic_Name = "reference.events";

   private final KafkaTemplate<String, ReferenceEvent> kafkaTemplate;

   public ReferenceEventPublisher(KafkaTemplate<String, ReferenceEvent> kafkaTemplate) {
      this.kafkaTemplate = kafkaTemplate;
   }


   public void publish(ReferenceEvent referenceEvent) {

      kafkaTemplate.send(Topic_Name, referenceEvent);
   }


}
