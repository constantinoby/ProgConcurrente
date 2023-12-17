package main

import (
	"context"
	"fmt"
	"log"
	"math/rand"
	"time"

	amqp "github.com/rabbitmq/amqp091-go"
)

func failOnError(err error, msg string) {
	if err != nil {
		log.Panicf("%s: %s", msg, err)
	}
}

func main() {
	conn, err := amqp.Dial("amqp://guest:guest@localhost:5672/")
	failOnError(err, "Failed to connect to RabbitMQ")
	defer conn.Close()

	ch, err := conn.Channel()
	failOnError(err, "Failed to open a channel")
	defer ch.Close()

	q, err := ch.QueueDeclare(
		"task_queue", // name
		true,         // durable
		false,        // delete when unused
		false,        // exclusive
		false,        // no-wait
		nil,          // arguments
	)
	failOnError(err, "Failed to declare a queue")

	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()

	clientID := rand.Intn(1000)
	log.Printf("Client %d started", clientID)

	random := rand.New(rand.NewSource(time.Now().Unix()))
	numOperations := random.Intn(10) + 1 // Genera un número aleatorio de operaciones entre 1 y 10

	// Balance inicializado en cero
	balance := 0

	for i := 0; i < numOperations; i++ {
		// Genera un número aleatorio para decidir entre ingreso o reintegro
		isDeposit := random.Intn(2) == 0
		amount := random.Intn(100) + 1 // Monto aleatorio entre 1 y 100

		operation := "reintegro"
		if isDeposit {
			operation = "ingreso"
			balance += amount
		} else {
			// Verificar si hay suficientes fondos antes de realizar el reintegro
			if amount <= balance {
				balance -= amount
			} else {
				log.Printf("OPERACIÓN NO PERMITIDA, NO HAY SUFICIENTES FONDOS")
			}
		}

		log.Printf("Client %d - Operación %d: %s de %d", clientID, i+1, operation, amount)
		log.Printf("Balance actual: %d", balance)

		body := fmt.Sprintf("%d|%t|%d", clientID, isDeposit, amount)
		err = ch.PublishWithContext(ctx,
			"",     // exchange
			q.Name, // routing key
			false,  // mandatory
			false,
			amqp.Publishing{
				DeliveryMode: amqp.Persistent,
				ContentType:  "text/plain",
				Body:         []byte(body),
			})
		failOnError(err, "Failed to publish a message")

		time.Sleep(1000 * time.Millisecond) // Simula cierto tiempo entre operaciones
	}

	log.Printf("Client %d - Operaciones completadas", clientID)
}
