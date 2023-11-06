import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * 
 * HECHO POR CONSTANTINO BYELOV SERDIUK
 */

public class p1 {

    // Numero de sospechosos que hay
    static int nSospechoso = 20;

    // Semaforo usado para entrar a la sala, fichar y declarar.
    static Semaphore sala = new Semaphore(1);
    // Semaforo del juez que indica si esta o no en la sala.
    static Semaphore jutge = new Semaphore(0);
    // Semaforo usado para bloquear a los sospechosos de entrar a la sala.
    static Semaphore esperarSospitosos = new Semaphore(0);
    // Semaforo usado para que los sospechosos esperen para declarar y para ser
    // aliberados.
    static Semaphore declaracio = new Semaphore(0);

    //semaforo para bloquear al juez
    static Semaphore bloqueoJuez = new Semaphore(0);

    //semaforo para bloquear al juez en caso de que no haya nadie
    static Semaphore bloqNadie = new Semaphore(0);

    // Variables de control
    static int sospitososDinsSala = 1;
    static int sospitososFichats = 1;
    static int sospitososDeclarados = 1;
    static int sospitososEsperando = 1;
    static boolean acabat = false;

    // Tiempos de espera
    final int TEMPS_ESPERA_MAXIM = 100;
    final int TEMPS_ESPERA_MINIM = 50;

    final int JUEZ_ESPERA_MAXIM = 200;
    final int JUEZ_ESPERA_MINIM = 150;

    static Random ran = new Random();

    class Juez extends Thread {

        @Override
        public void run() {
            try {
                Thread.sleep(ran.nextInt(JUEZ_ESPERA_MINIM, JUEZ_ESPERA_MAXIM));// delay para llegar
                System.out.println("----> Jutge Dredd: Jo som la llei!");
                Thread.sleep(ran.nextInt(JUEZ_ESPERA_MINIM, JUEZ_ESPERA_MAXIM));// delay para entrar

                System.out.println("----> Jutge Dredd: Som a la sala, tanqueu porta!");

                // Una vez que el juez esta en la sala bloqueamos el semaforo
                jutge.release();

                // En el caso de que no haya gente cuando entre el juez se va
                if (sospitososDinsSala == 1) {

                    System.out.println("----> Jutge Dredd: Si no hi ha ningú me'n vaig!");
                    System.out.println("----> Jutge Dredd: La justícia descansa, prendré declaració als" +
                            " sospitosos que queden");
                    // Bloqueamos hasta que todos los sospechoso no se hayan inicializado
                    bloqNadie.acquire();
                    // Cambiamos el valor de acabat para que los sospechosos sepan que el juez esta
                    // dentro de la sala.
                    acabat = true;
                    // Hacemos release para que los sospechosos puedan seguir con su ejecucion
                    esperarSospitosos.release(nSospechoso);
                    return;
                }

                // el juez esta en la sala empezamos a fichar desbloqueamos a los que han
                // entrado a la sala
                esperarSospitosos.release(sospitososDinsSala - 1);

                System.out.println("----> Jutge Dredd: Fitxeu als sospitosos presents");
                // Bloqueamos hasta que todos los sospechosos no hayan fichado
                bloqueoJuez.acquire();

                System.out.println("----> Jutge Dredd: Preniu declaració als presents");

                // Desbloqueamos a los sospechosos para que declaren
                declaracio.release(sospitososDinsSala - 1);

                bloqueoJuez.acquire();
                // una vez que han declarado todos los sospechosos los mando al asilo
                System.out.println("----> Judge Dredd: Podeu abandonar la sala tots a l'asil!");

                System.out.println("----> Jutge Dredd: La justícia descansa, prendré declaració als" +
                        " sospitosos que queden");

                // Desbloqueamos todos los hilos que hayan declarado para mandarlos al asilo
                declaracio.release(sospitososDinsSala - 1);
                sleep(5);
                // Desbloqueamos a los sospechosos que no han entrado a la sala
                esperarSospitosos.release(nSospechoso);

            } catch (InterruptedException ex) {
                Logger.getLogger(p1.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    class Sospechoso extends Thread {

        String nom;
        String noms[] = {
                "Bane", "Ra's al Ghul", "Riddler", "Catwoman", "Two-Face", "Poison Ivy", "Joker", "Scarecrow",
                "Mr. Freeze", "Penguin", "Harley Quinn", "Killer Croc", "Jason Todd", "Hush", "Hugo Strange",
                "Talia al Ghul", "Deathstroke", "Clayface", "Deadshot", "Mad Hatter"
        };

        public Sospechoso(int id) {
            nom = noms[id];
        }

        @Override
        public void run() {
            try {

                // Cada sospechoso entra a la sala de manera ordenada
                sala.acquire();
                // Si el juez esta dentro de la sala no printeamos el mensaje
                if (jutge.availablePermits() == 0) {
                    System.out.println(nom + ": Som innocent!");
                }
                Thread.sleep(ran.nextInt(TEMPS_ESPERA_MINIM, TEMPS_ESPERA_MAXIM));// delay para entrar
                sospitososEsperando++;
                sala.release();

                // Si el juez no esta dentro de la sala dejamos entrar al hilo en la sala,
                // aumentando el numero de sospechosos dentro de ella.
                if (jutge.availablePermits() == 0) {
                    System.out.println(nom + " entra al jutjat. Sospitosos: " + sospitososDinsSala);
                    sospitososDinsSala++;

                }
                // El ultimo sospechoso que entra indica que es el ultimo al juez mediante la
                // variable del semaforo
                if (sospitososEsperando == nSospechoso + 1) {
                    bloqNadie.release();
                }

                esperarSospitosos.acquire();// bloqueamos hasta que el juez no diga de fichar

                // Si el juez o el ultimo suspechoso en declarar dicen que han acabado los
                // sospechosos que no han podido declarar se van a quejar
                if (acabat) {
                    System.out.println(nom + ": No és just vull declarar! Som innocent!");
                    return;
                }

                sala.acquire();// bloqueo para fichar
                System.out.println(nom + " fitxa. Fitxats: " + sospitososFichats);
                Thread.sleep(ran.nextInt(TEMPS_ESPERA_MINIM, TEMPS_ESPERA_MAXIM)); // espera para fichar
                sospitososFichats++;
                sala.release();// release del fichado

                if (sospitososDinsSala == sospitososFichats) {
                    bloqueoJuez.release();
                }

                declaracio.acquire();// esperamos para declarar

                sala.acquire(); // bloqueamos para declarar
                System.out.println(nom + " declara. Declaracions: " + sospitososDeclarados);
                Thread.sleep(ran.nextInt(TEMPS_ESPERA_MINIM, TEMPS_ESPERA_MAXIM)); // espera para declarar
                sospitososDeclarados++;
                sala.release(); // release del declarado

                // Una vez que el ultimo en declarar haya acabado se lo notificamos al juez
                // mediante la variable boleana.
                if (sospitososDinsSala == sospitososDeclarados) {
                    bloqueoJuez.release();
                }

                // El juez manda al asilo a todos los que han entrado a la sala
                declaracio.acquire();
                acabat = true;
                sleep(15);

                System.out.println(nom + " entra a l'Asil d'Arkham");

            } catch (InterruptedException ex) {
                Logger.getLogger(p1.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    private void metodePrincipal() throws InterruptedException {

        // Declaramos los hilos
        Thread sospechosos[] = new Thread[nSospechoso];
        Thread juez = new Thread(new p1.Juez());

        // Declaramos los hilos de los sospechosos
        for (int i = 0; i < nSospechoso; i++) {
            sospechosos[i] = new Thread(new p1.Sospechoso(i));
        }

        // Iniciamos el hilo del juez
        juez.start();
        // Iniciamos los hilos de los sospechosos
        for (int i = 0; i < nSospechoso; i++) {
            sospechosos[i].start();
        }

        // Esperamos a que acaben los hilos
        juez.join();
        for (int i = 0; i < nSospechoso; i++) {
            sospechosos[i].join();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        new p1().metodePrincipal();
    }
}
