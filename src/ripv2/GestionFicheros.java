/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ripv2;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Scanner;
import static ripv2.Rip.ipAddr;

/**
 *
 * @author Alberto
 */
//Clase donde se leerán los archivos de configuración
public class GestionFicheros {

    public static Scanner fichero = null;

    public static boolean compFicheroConfig() {//Método para la comprobación del fichero de configuración

        String nombreFichero = "ripconf-";

        boolean correcto = false;

        try {

            NetworkInterface ni = NetworkInterface.getByName("eth0");
            Enumeration<InetAddress> addresses = ni.getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress iaddr = addresses.nextElement();
                String ip = iaddr.getHostAddress();
                System.out.println(ip);
                if (ip.equals("127.0.0.1"))//(localhost)
                {
                    continue;
                }
                if (ip.split("\\.").length == 4) {
                    Rip.localAddr = iaddr;
                    Rip.ipAddr = ip;
                    break;
                }
            }
            if (Rip.localAddr == null && ipAddr == null) {
                Rip.localAddr = InetAddress.getLocalHost();
                ipAddr = Rip.localAddr.getHostAddress();
            }

            nombreFichero = nombreFichero.concat(ipAddr + ".txt");

            //Abrimos el archivo e indicamos que no ha habido errores
            fichero = new Scanner(new FileInputStream(nombreFichero));
            correcto = true;

        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return correcto;
    }

    public static void leerFichero() {

        while (fichero.hasNextLine()) {//Lectura del fichero hasta el final

            String linea = fichero.nextLine().trim();

            if (linea.equals(""))//Caso de linea en blanco
            {
                continue;
            }

            if (linea.contains("/"))//subred
            {
                Rip.subredes.add(linea);
            } else//router vecino
            {
                Rip.vecinos.add(linea);
            }
        }

        fichero.close();
    }
}
