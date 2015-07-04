package ripv2;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.*;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.Set;
import java.util.Timer;
import java.util.Date;
import static ripv2.GestionPaquetes.masksetter;

public class Rip {

	//Declaramos las variables
    static HashSet<String> subredes = new HashSet<String>();
    static DatagramSocket DS;

    //Constantes RIPv2 
    static final int PUERTO_UDP = 5000;//Puerto provisional, ya que el 520 al ser menor que 1024 da problemas de permisos
    static final int COSTE_INF = 16;
    static final int VERSION = 2;
    static final int ADDRESS_FAMILY_ID_AUT = -128;
    static final int POS_ADDRESS_FAMILY_ID_AUT = 4;
    static final int RIP_LENGTH = 20;
    static final int CABECERA = 4;
    static final int COSTE_VECINO = 1;
	//***************

    static String[] PASSWORD = {"0", "0", "0", "0"};// Pass Autentif (0-255 max) (inicializada así por comodidad)

    public static String ipAddr;
    public static InetAddress localAddr;
    //Listas que contendrán la informacion que necesitamos
    static LinkedHashMap<String, Vector> vectorList = new LinkedHashMap<String, Vector>();
    static LinkedList<String> vecinos = new LinkedList<String>();
    static LinkedList<String> nodes = new LinkedList<String>();
    static LinkedList<Nodo> nodeact = new LinkedList<Nodo>();

    public static void main(String[] args) {

        //Intentamos abrir el fichero de configuracion correspondiente a este nodo
        if (GestionFicheros.compFicheroConfig() == false) {
            System.exit(0);
        }
        //Una vez abierto, lo leemos
        GestionFicheros.leerFichero();
        //Inicializamos el vector de distancias
        Vector.inicializarVector();

        //Primera iteracion, solo existen los vecinos
        nodes.addAll(vecinos);
        for (int i = 0; i < vecinos.size(); i++) {
            nodeact.add(new Nodo(vecinos.get(i)));
        }
        //Empezamos, para las autentificaciones metemos 4 números por teclado, siendo estos numeros por comodidad.
        System.out.println("Introducir 4 números del 0 al 255 para la autentificación, número e intro para introducir");
        Scanner teclado = new Scanner(System.in);
        for (int i = 0; i < 4; i++) {

            PASSWORD[i] = teclado.nextLine();
            if ((Integer.parseInt(PASSWORD[i]) < 0) || (Integer.parseInt(PASSWORD[i]) > 255)) {
                System.err.println("Contraseña no válida (0-255)");
                i--;
            } else {

            }
        }

        teclado.close();
        try {
            //Abrimos un DatagramSocket UDP 
            try {
                DS = new DatagramSocket(PUERTO_UDP, InetAddress.getByName(ipAddr));
            } catch (UnknownHostException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } catch (SocketException e) {
            //Si no se puede abrir el DatagramSocket, informamos de ello
            System.out.println("No se ha podido abrir el DatagramSocket");
            e.printStackTrace();
            return;
        }

        //timers para obtener tiempos
        Calendar inicialtimems = Calendar.getInstance();
        Calendar finaltimems = null;
        int Tiempo = 0;
        int saltos = 0;
        System.out.println("Inicial Time: " + inicialtimems.getTimeInMillis());;
//Inicio del bucle
        while (true) {
            try {

                for (int i = 0; i < nodeact.size(); i++) {

                    if (nodeact.get(i).getsaltos() == 4) {//Caso de 4 saltos
                        String ipfuen = nodeact.get(i).getip();
                        Set s = vectorList.keySet();
                        Iterator it = s.iterator();
                        //Se recorre el vector
                        while (it.hasNext()) {
                            String aux = (String) it.next();
                            Vector vc = (Vector) vectorList.get(aux);

                            if (vc.getNextHop().equals(ipfuen)) {
                                vectorList.remove(vc);
                                vectorList.put(aux, new Vector(COSTE_INF, ipfuen));//Coste infinito (16) si existe
                            }
                        }

                    }
                }
                DS.setSoTimeout(4000 - Tiempo);

                if (saltos == 6) {//6 saltos = reset
                    saltos = 0;
                    vecinos.clear();
                    vectorList.clear();
                    //inicialización de vector:
                    if (GestionFicheros.compFicheroConfig() == false) {
                        break;
                    }
                    GestionFicheros.leerFichero();
                    Vector.inicializarVector();

                    //En la primera iteracion, para nuestro nodo solo existen sus vecinos en la red
                    nodes.addAll(vecinos);
                    nodeact = new LinkedList<Nodo>();
                    for (int i = 0; i < vecinos.size(); i++) {
                        nodeact.add(new Nodo(vecinos.get(i)));
                    }
                    //Volver a empezar de cero, reiniciar todo
                    DS.setSoTimeout(10000);
                    inicialtimems = Calendar.getInstance();
                    saltos = 0;
                    continue;

                }

                DatagramPacket datagram = GestionSockets.leerSocket();
                //Si llega uno, comprobamos que no sea nulo y que coincida con nuestra password, si no lo deshechamos
                if (datagram != null && RipPacket.coincidePassword(datagram) == true) {
                    System.out.println("Datagrama recibido, comprobado y aceptado");
                    String ipFuente = datagram.getAddress().getHostAddress();
                    RipPacket paquete = RipPacket.toRipPacket(datagram);
                    nodeact.remove(ipFuente);
                    nodeact.add(new Nodo(ipFuente));
                    for (int i = 0; i < nodeact.size(); i++) {
                        if (nodeact.get(i).getip().equals(ipFuente)) {

                            Nodo nod = nodeact.get(i);
                            nodeact.remove(nod);
                            nodeact.add(new Nodo(ipFuente));
                        }

                    }
                    //Tras comprobación del vector se actualiza.
                    Vector.actualizarVector(ipFuente, paquete);
                }
                //Cada vez que llega un paquete, calculamos cuando tiempo paso desde que el DatagramSocket empezo a escuchar
                finaltimems = Calendar.getInstance();
                System.out.println("Tfinal: " + finaltimems.getTimeInMillis());
                Tiempo = (int) (finaltimems.getTimeInMillis() - inicialtimems.getTimeInMillis());

            } catch (SocketTimeoutException e) {
                //Excepcion timeout, no ha recibido nada
                try {

                    System.out.println("Enviado");

                    //Se envia el vector de Distancias a cada uno de los vecinos
                    for (int i = 0; i < vecinos.size(); i++) {
                        DS.send(RipPacket.vectorListToDatagram((vecinos.get(i)), 1, VERSION, PASSWORD));
                    }
                    //Se actualiza el tiempo inicial y la diferencia
                    inicialtimems = Calendar.getInstance();
                    System.out.println("Tinicial: " + inicialtimems.getTimeInMillis());
                    Tiempo = 0;
                    saltos++;

                    for (int i = 0; i < nodeact.size(); i++) {
                        Nodo nod = nodeact.get(i);
                        nodeact.remove(nod);
                        nod.aumentasalto();
                        nodeact.add(i, nod);
                    }

                    System.out.println(vectorList);

                } catch (IOException a) {
                    a.printStackTrace();
                }

            } catch (SocketException e) {
                e.printStackTrace();
            } catch (IOException a) {
                a.printStackTrace();
            }
        }
    }

}
