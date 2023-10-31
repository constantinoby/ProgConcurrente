package jutjatgotham;

import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * 
 * HECHO POR CONSTANTINO BYELOV SERDIUK
 */

public class p1 {

    static int nSospechoso = 20;

    static Semaphore sala = new Semaphore(1);
    static Semaphore jutge = new Semaphore(0);
    static Semaphore esperarSospitosos = new Semaphore(0);
    static Semaphore fixarSemaphore = new Semaphore(1);
    static Semaphore declaracio = new Semaphore(0);
    static Semaphore llibertat = new Semaphore(0);
    static Semaphore esperarDeclara = new Semaphore(1);

    static int sospitososDinsSala = 1;
    static int sospitososFichats = 1;
    static int sospitososDeclarados = 1;
    static int sospitososEsperando = 1;
    static boolean fichado = true;
    static boolean acabat = false;
    static boolean nadie = true;

    final int TEMPS_ESPERA_MAXIM = 400;
    final int TEMPS_ESPERA_MINIM = 300;

    final int JUEZ_ESPERA_MAXIM = 2100;
    final int JUEZ_ESPERA_MINIM = 1000;

    static Random ran = new Random();

    class Juez extends Thread {

        @Override
        public void run() {
            try {
                Thread.sleep(ran.nextInt(JUEZ_ESPERA_MINIM, JUEZ_ESPERA_MAXIM));// delay para llegar
                System.out.println("----> Jutge Dredd: Jo som la llei!");
                Thread.sleep(ran.nextInt(JUEZ_ESPERA_MINIM, JUEZ_ESPERA_MAXIM));// delay para entrar

                System.out.println("----> Jutge Dredd: Som a la sala, tanqueu porta!");

                jutge.release();

                if (sospitososDinsSala == 1) {

                    System.out.println("----> Jutge Dredd: Si no hi ha ningú me'n vaig!");
                    System.out.println("----> Jutge Dredd: La justícia descansa, prendré declaració als" +
                            " sospitosos que queden");
                    while (nadie) {
                        sleep(0,5);
                    }
                    acabat = true;
                    esperarSospitosos.release(nSospechoso);
                    return;
                }

                // System.out.println("Jutge Espera release:" + jutge.availablePermits());
                esperarSospitosos.release(sospitososDinsSala - 1);// el juez esta en la sala empezamos a fichar
                                                                  // desbloqueamos a los que han entrado a la sala
                System.out.println("----> Jutge Dredd: Fitxeu als sospitosos presents");
                // Una vez que han fichado todos, hacemos que declaren semaforo release
                while (fichado) {
                    // espera activa
                    // System.out.println("JUEZ FICHADO" + fichado);
                    Thread.sleep(ran.nextInt(10000, 11000));

                }
                System.out.println("----> Jutge Dredd: Preniu declaració als presents");
                // System.out.println("JUEZ CAMBIA ");// NO AVANZA DE AQUI
                fichado = true;

                declaracio.release(sospitososDinsSala - 1);

                while (fichado) {// hasta que no hayan declarado todos espero
                    // espera activa
                    // System.out.println("JUEZ DECLARADO" + fichado);
                    Thread.sleep(ran.nextInt(10000, 11000));
                }
                // una vez el veredicto mandamos a todos al asilo
                System.out.println("----> Judge Dredd: Podeu abandonar la sala tots a l'asil!");

                System.out.println("----> Jutge Dredd: La justícia descansa, prendré declaració als" +
                        " sospitosos que queden");
                llibertat.release(sospitososDinsSala - 1);
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

                sala.acquire();
                if (jutge.availablePermits() == 0) {
                    System.out.println(nom + ": Som innocent!");
                }
                Thread.sleep(ran.nextInt(TEMPS_ESPERA_MINIM, TEMPS_ESPERA_MAXIM));// delay para entrar
                sospitososEsperando++;
                sala.release();

                if (jutge.availablePermits() == 0) {
                    System.out.println(nom + " entra al jutjat. Sospitosos: " + sospitososDinsSala);
                    sospitososDinsSala++;

                }
                if (sospitososEsperando == nSospechoso + 1) {
                    nadie = false;
                }

                esperarSospitosos.acquire();// bloqueamos hasta que el juez no diga de fichar

                if (acabat) {
                    System.out.println(nom + ": No és just vull declarar! Som innocent!");
                    return;
                }

                fixarSemaphore.acquire();// bloqueo para fichar
                System.out.println(nom + " fitxa. Fitxats: " + sospitososFichats);
                Thread.sleep(ran.nextInt(TEMPS_ESPERA_MINIM, TEMPS_ESPERA_MAXIM)); // espera para fichar
                sospitososFichats++;
                fixarSemaphore.release();// release

                if (sospitososDinsSala == sospitososFichats) {
                    fichado = false;
                }

                declaracio.acquire();// esperamos para declarar

                esperarDeclara.acquire();
                System.out.println(nom + " declara. Declaracions: " + sospitososDeclarados);
                sospitososDeclarados++;
                esperarDeclara.release();

                if (sospitososDinsSala == sospitososDeclarados) {
                    fichado = false;
                }

                llibertat.acquire();
                acabat = true;
                sleep(2);

                System.out.println(nom + " entra a l'Asil d'Arkham");

            } catch (InterruptedException ex) {
                Logger.getLogger(p1.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    private void metodePrincipal() throws InterruptedException {

        Thread sospechosos[] = new Thread[nSospechoso];
        Thread juez = new Thread(new p1.Juez());

        for (int i = 0; i < nSospechoso; i++) {
            sospechosos[i] = new Thread(new p1.Sospechoso(i));
        }

        juez.start();
        for (int i = 0; i < nSospechoso; i++) {
            sospechosos[i].start();
        }

        juez.join();
        for (int i = 0; i < nSospechoso; i++) {
            sospechosos[i].join();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        new p1().metodePrincipal();
    }
}

// release suelte semaforo libero 0-->1
// aquire lo coge si esta libre bloqueo 1-->0