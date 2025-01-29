package com.cleartax.training_superheroes.services;

import com.cleartax.training_superheroes.config.SqsConfig;
import com.cleartax.training_superheroes.dto.SuperheroRequestBody;
import com.cleartax.training_superheroes.entities.Superhero;
import com.cleartax.training_superheroes.repos.SuperheroRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import java.util.List;

@Service
public class SuperheroConsumer {

    private final SqsConfig sqsConfig;
    private final SqsClient sqsClient;
    private final SuperheroRepository superheroRepository;
    private final ObjectMapper objectMapper;

    public SuperheroConsumer(SqsConfig sqsConfig, SqsClient sqsClient, SuperheroRepository superheroRepository) {
        this.sqsConfig = sqsConfig;
        this.sqsClient = sqsClient;
        this.superheroRepository = superheroRepository;
        this.objectMapper = new ObjectMapper();
    }

    @Scheduled(fixedDelay = 5000) // Poll every 5 seconds
    public String consumeSuperhero() {
        while(true) {
            ReceiveMessageRequest request = ReceiveMessageRequest.builder()
                    .queueUrl(sqsConfig.getQueueUrl())
                    .maxNumberOfMessages(10) // Batch size
                    .build();

            ReceiveMessageResponse response = sqsClient.receiveMessage(request);
            List<Message> messages = response.messages();

            for (Message message : messages) {
                try {
                    // Deserialize message body into SuperheroRequestBody
                    SuperheroRequestBody superheroRequestBody = objectMapper.readValue(message.body(), SuperheroRequestBody.class);

                    // Save to MongoDB
                    Superhero superhero = new Superhero();
                    superhero.setName(superheroRequestBody.getSuperheroName());
                    superhero.setPower(superheroRequestBody.getPower());
                    superhero.setUniverse(superheroRequestBody.getUniverse());
                    superheroRepository.save(superhero);

                    System.out.println("Processed and saved: " + superhero);

                    // Delete message from the queue after processing
                    sqsClient.deleteMessage(builder ->
                            builder.queueUrl(sqsConfig.getQueueUrl()).receiptHandle(message.receiptHandle()));
                } catch (Exception e) {
                    System.err.println("Error processing message: " + e.getMessage());
                }
            }
            return null;
        }
    }
}
