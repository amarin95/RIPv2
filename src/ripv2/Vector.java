/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ripv2;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.util.Iterator;
import java.util.LinkedList;
import static ripv2.Rip.COSTE_VECINO;
import static ripv2.Rip.ipAddr;
import static ripv2.Rip.subredes;
import static ripv2.Rip.vectorList;

/**
 *
 * @author Alberto
 */
public class Vector {

    private int coste;
    private String nextHop;

    Vector(int coste, String nextHop) {
        this.coste = coste;
        this.nextHop = nextHop;
    }

    public String toString() {
        return (" " + coste + " - " + nextHop);
    }

    public int getCoste() {
        return coste;
    }

    public void setCoste(int coste) {
        this.coste = coste;
    }

    public String getNextHop() {
        return nextHop;
    }

    public void setNextHop(String nextHop) {
        this.nextHop = nextHop;
    }

    public static void inicializarVector() {
        //Ponemos nuestra direccion con la mascara, con coste 0
        String ipPropia = ipAddr.concat("/32");
        vectorList.put(ipPropia, new Vector(0, ipAddr));

		//Para nuestras subredes, ponemos su direccion y coste al vecino (1 de forma predeterminada)
        //y que a ellas se accede a traves de nosotros
        Iterator<String> it = subredes.iterator();
        while (it.hasNext()) {
            String ipSubred = (String) it.next();
            vectorList.put(ipSubred, new Vector(Rip.COSTE_VECINO, ipAddr));
        }
    }

    public static String DatagramToString(DatagramPacket datagram) {
        byte[] bytes = datagram.getData();
        String s = "";
        for (int i = 0; i < bytes.length; i++) {
            s = s.concat((bytes[i] + "\t"));
            if ((i + 1) % 4 == 0) {
                s = s.concat("\n");
            }
        }
        return s;
    }

    //Método para la comprobación y actualización del vector.

    public static void actualizarVector(String ipFuente, RipPacket paquete) {
        LinkedList<Inputs> entradas = paquete.getEntradas();
        Iterator<Inputs> it = entradas.iterator();
        LinkedList<Inputs> cambios = new LinkedList<Inputs>();

        //Recorrer la lista.
        while (it.hasNext()) {
            Inputs ripEntry = (Inputs) it.next();

            //Obtenemos de la entrada la Ip de la subred y su mascara
            int longitudMascara = GestionPaquetes.masksetterStr(ripEntry.getMascara());
            String ipSubred = ripEntry.getIpAddress().concat("/" + longitudMascara);
            //Calculamos el coste del enlace
            int coste = ripEntry.getCoste() + COSTE_VECINO;

            //Comprobamos si la subred existe
            Vector vector = vectorList.get(ipSubred);

            //Si devuelve null(no existe)
            if (vector == null) {

                //En caso de acabarlo de descubrir el coste es de calculo inmediato
                vectorList.put(ipSubred, new Vector(coste, ipFuente));

            } else {//Caso conocido, cálculo.
                boolean trigup = false;
                if (ipFuente.equals(vector.getNextHop()) || coste < vector.getCoste()) {
                    vectorList.remove(ipSubred);
                    vectorList.put(ipSubred, new Vector(coste, ipFuente));
                    //Sustitución en la lista con el cálculo nuevo

                }

                //Si cambia el coste, se envía un paquete para actualizar el resto de nodos.
                if (coste > vector.getCoste() & vector.getNextHop().equals(ipFuente)) {
                    trigup = true;
                    cambios.add(0, ripEntry);
                    RipPacket cambio = new RipPacket(1, 2, cambios, Rip.PASSWORD);
                    for (int i = 0; i < Rip.vecinos.size(); i++) {
                        if (ipFuente.equals(Rip.vecinos.get(i))) {
                            continue;
                        } else {
                            try {
                                Rip.DS.send(cambio.toDatagramPacket(Rip.vecinos.get(i)));
                            } catch (IOException e) {
                                System.out.println("Problema al enviar paquete");

                            }
                        }
                    }
                }

                //Si se ha enviado algun update se vuelve a seguir procesando los paquetes y actualizando el vector
                if (trigup) {
                    int tiempo = 0;
                    try {

                        tiempo = Rip.DS.getSoTimeout();
                        Rip.DS.setSoTimeout(3);
                    } catch (SocketException e) {
                        try {
                            Rip.DS.setSoTimeout(tiempo);
                            continue;
                        } catch (SocketException a) {

                        }

                    }
                }

            }
        }
    }

}
