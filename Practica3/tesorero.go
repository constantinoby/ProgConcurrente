package main

import (
	"fmt"
	"log"
	"math/rand"
	"os"
	"sync"
	"time"

	amqp "github.com/rabbitmq/amqp091-go"
)

var wg sync.WaitGroup

// Tresorer representa al tesorero con su balance actual.
type Tresorer struct {
	Balance int
}

// Remove the existing failOnError function declaration
// Replace it with the correct failOnError function declaration

// failOnError handles the error and panics if there is an error.
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

	err = ch.Qos(
		1,     // prefetch count
		0,     // prefetch size
		false, // global
	)
	failOnError(err, "Failed to set QoS")
	tresorer := &Tresorer{Balance: 0}

	msgs, err := ch.Consume(
		q.Name, // queue
		"",     // consumer
		false,  // auto-ack
		false,  // exclusive
		false,  // no-local
		false,  // no-wait
		nil,    // args
	)

	failOnError(err, "Failed to register a consumer")
	random := rand.New(rand.NewSource(time.Now().Unix()))
	botiMinim := random.Intn(10) + 1 // Genera un número aleatorio de operaciones entre 1 y 10
	log.Println("El tresorer és al despatx. El botí mínim és: " + fmt.Sprint(botiMinim) + "€")
	currentTime := time.Now()
	formattedDataTime := currentTime.Format("2006-01-02 15:04:05")

	fmt.Println(formattedDataTime + "   [*] Esperant clients")
	// Procesar operaciones de la cola
	go func() {
		for d := range msgs {
			operationInfo := string(d.Body)
			//log.Printf("Mensaje recibido en el tesorero: %s", operationInfo)

			nombreCliente, isDeposit, amount := parseOperationInfo(operationInfo)

			if isDeposit {
				log.Println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>")
				log.Println("Operació rebuda: +", amount, "€"+" del Client: ", nombreCliente)
				tresorer.Balance += amount
				log.Println("Balanç actual:", tresorer.Balance)
				//if tresorer.Balance >= botiMinim {
				//	log.Println("El tesorero cierra. Balance alcanzado:", tresorer.Balance)
				//	return // Salir de la función main y finalizar la ejecución del programa
				//}
				log.Println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<")
			} else {
				log.Println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>")
				log.Println("Operació rebuda: -", amount, "€"+" del Client: ", nombreCliente)
				// Verificar si hay suficientes fondos antes de realizar el reintegro
				if amount <= tresorer.Balance {
					tresorer.Balance -= amount
					log.Println("Balanç: ", tresorer.Balance)
					log.Println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<")
				} else {
					log.Printf("OPERACIÓ NO PERMESA, NO HI FONS")
					log.Println("Balanç: ", tresorer.Balance)
					log.Println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<")
				}
			}
			if(tresorer.Balance >= botiMinim){
				amqp.ExchangeFanout("El banquer ha dit: l'oficina acaba de tancar")
			}
			// Publicar el balance actual
			mensaje := fmt.Sprintf("%d", tresorer.Balance)
			err := os.WriteFile("balance.txt", []byte(mensaje), 0644)
			if err != nil {
			}
			// Simular procesamiento de la operación
			time.Sleep(1 * time.Second)

			d.Ack(false)
		}
		log.Println("El tesorero cierra. Balance alcanzado:", tresorer.Balance)

	}()

	os.Remove("balance.txt")
	// Esperar a que haya al menos un cliente
	for tresorer.Balance == 0 {
		time.Sleep(1 * time.Second)
	}

	// Mantener el tesorero en ejecución
	select {}
}

// Función para parsear la información de la operación
func parseOperationInfo(operationInfo string) (nombreCliente string, isDeposit bool, amount int) {
	fmt.Sscanf(operationInfo, "%s %t %d", &nombreCliente, &isDeposit, &amount)

	if isDeposit == false {
		amount = -amount
	}

	return
}
