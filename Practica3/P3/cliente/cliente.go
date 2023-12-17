package main

import (
	"context"
	"fmt"
	"log"
	"math/rand"
	"time"
	"os"

	amqp "github.com/rabbitmq/amqp091-go"
)

func failOnError(err error, msg string) {
	if err != nil {
		log.Panicf("%s: %s", msg, err)
	}
}

func main() {

	if len(os.Args) < 2 {
        log.Fatal("Usage: go run cliente.go \"<nombre>\"")
    }

	nombreCliente := os.Args[1]

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
	string trolleada;
	trolleada := "juanjo"
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()

	log.Printf("Hola el meu nom es: %s", nombreCliente)

	random := rand.New(rand.NewSource(time.Now().Unix()))
	numOperations := random.Intn(10) + 1 // Genera un número aleatorio de operaciones entre 1 y 10

	// Balance inicializado en cero Prueba
	balance := 0

	log.Printf("%s vol fer %d operacions", nombreCliente, numOperations)

	for i := 0; i < numOperations; i++ {
		// Genera un número aleatorio para decidir entre ingreso o reintegro
		isDeposit := random.Intn(2) == 0
		amount := random.Intn(10) + 1 // Monto aleatorio entre 1 y 100
		posible := 0

		operation := "reintegro"
		if isDeposit {
			operation = "ingreso"
			balance += amount
			posible = 1
		} else {
			// Verificar si hay suficientes fondos antes de realizar el reintegro
			if amount <= balance {
				balance -= amount
				posible = 1
			} else {
				posible = 0
			}
		}

		if !isDeposit {
			amount = -amount
		}

		//log.Printf("Client %s - Operación %d: %s de %d", nombreCliente, i+1, operation, amount)
		log.Printf("%s operació %d: %d", nombreCliente, i+1, amount)
		log.Printf("Operació sol·licitada")
		//Hacemos una espera de 1seg para simular el tiempo de la operación
		time.Sleep(1000 * time.Millisecond)

		if posible == 0 { //caso de reintegro sin saldo
			log.Printf("NO HI HA SALDO")
		} else if posible == 1 && operation == "reintegro" { //caso de reintegro con saldo
			log.Printf("ES FARÀ EL REINTEGRO SI ÉS POSSIBLE")
		}else if posible == 1 && operation == "ingreso" { //caso de ingreso
			log.Printf("INGRÉS CORRECTE")
		}

		log.Printf("Balanç actual: %d", balance)

		log.Printf("%d----------------------------------------",i+1)

		body := fmt.Sprintf("%s|%t|%d", nombreCliente, isDeposit, amount)
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
}
