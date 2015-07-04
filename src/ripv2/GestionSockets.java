/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ripv2;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketTimeoutException;
import static ripv2.Rip.DS;

/**
 *
 * @author Alberto
 */

//Clase para la gestión de Socket (por ahora solo para leerlo)
public class GestionSockets {
 public static DatagramPacket leerSocket() throws SocketTimeoutException, IOException{
		//Creamos el buffer donde almacenaremos el datagrama entrante
		byte[] buffer = new byte[25*Rip.RIP_LENGTH+Rip.CABECERA];
		//Creamos un datagrama vacio para poder leer del DS
		DatagramPacket paquete = new DatagramPacket(buffer, buffer.length);
				
		DS.receive(paquete);
		return paquete;						
	}  
}
