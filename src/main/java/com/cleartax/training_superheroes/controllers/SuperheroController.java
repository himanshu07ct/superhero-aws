package com.cleartax.training_superheroes.controllers;


import com.cleartax.training_superheroes.config.SqsConfig;
import com.cleartax.training_superheroes.entities.Superhero;
import com.cleartax.training_superheroes.dto.SuperheroRequestBody;
import com.cleartax.training_superheroes.services.SuperheroConsumer;
import com.cleartax.training_superheroes.services.SuperheroService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.List;

@RestController
public class SuperheroController {

    private SuperheroService superheroService;

    @Autowired
    private SqsClient sqsClient;

    @Autowired
    private SqsConfig sqsConfig;

    @Autowired
    private SuperheroConsumer superheroConsumer;

    @Autowired
    public SuperheroController(SuperheroService superheroService){

        this.superheroService = superheroService;
        this.sqsClient = sqsClient;
    }

    @GetMapping("/hello")
    public String hello(@RequestParam(value = "username", defaultValue = "World") String username) {
        sqsClient.sendMessage(SendMessageRequest.builder()
                .queueUrl(sqsConfig.getQueueUrl())
                .messageBody("Himanshu").build());
        return String.format("Hello %s!, %s", username, sqsConfig.getQueuename());
    }

    @GetMapping("/update_superhero_async")
    public String updateSuperhero(@RequestParam(value = "superHeroName", defaultValue = "ironMan") String superHeroName) {
        SendMessageResponse result = sqsClient.sendMessage(SendMessageRequest.builder()
                .queueUrl(sqsConfig.getQueueUrl())
                .messageBody(superHeroName)
                .build());

        return String.format("Message sent to queue with message id %s and superHero %s", result.messageId(), superHeroName);
    }

    @GetMapping("/get_message_from_queue")
    public String getMessage() {
        return superheroConsumer.consumeSuperhero();
    }


    @GetMapping("/get_and_delete_message_from_queue")
    public String getAndDeleteMessage() {
        // Receive messages from the SQS queue
        ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
                .queueUrl(sqsConfig.getQueueUrl())
                .maxNumberOfMessages(1) // Receive 1 message at a time
                .build();

        List<Message> messages = sqsClient.receiveMessage(receiveRequest).messages();

        if (messages.isEmpty()) {
            return "No messages available in the queue.";
        }

        // Get the first message
        Message message = messages.get(0);
        String messageBody = message.body();
        String receiptHandle = message.receiptHandle();

        // Process the message (your business logic here)
        String response = String.format("Message received: %s", messageBody);

        // Delete the message from the queue
        DeleteMessageRequest deleteRequest = DeleteMessageRequest.builder()
                .queueUrl(sqsConfig.getQueueUrl())
                .receiptHandle(receiptHandle)
                .build();

        sqsClient.deleteMessage(deleteRequest);

        return String.format("%s - The message has been successfully deleted from the queue.", response);
    }

//    @GetMapping("/superhero")
//    public Superhero getSuperhero(@RequestParam(value = "name", defaultValue = "Batman") String name,
//                                  @RequestParam(value = "universe", defaultValue = "DC") String universe){
//        return superheroService.getSuperhero(name, universe);
//    }

    @PostMapping("/superhero")
    public Superhero persistSuperhero(@RequestBody SuperheroRequestBody superheroRequestBody){
        return superheroService.persistSuperhero(superheroRequestBody, superheroRequestBody.getUniverse());
    }
}
