package com.cleartax.training_superheroes.services;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ConsumerRunner {
    @Autowired
    private SuperheroConsumer superheroConsumer;

    @PostConstruct
    public void startConsumer() {
        new Thread(() -> superheroConsumer.consumeSuperhero()).start(); // Run in a separate thread
    }
}
