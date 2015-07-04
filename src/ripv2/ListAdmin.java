/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ripv2;

import java.util.LinkedList;

/**
 *
 * @author Alberto
 */
//Clase para actualizar lista y administrarlas.
public class ListAdmin {

    public static void actualizalista(RipPacket paquete) {
        LinkedList<Inputs> aux = paquete.getEntradas();
        for (int j = 0; j < aux.size(); j++) {
            int k;
            for (k = 0; k < Rip.nodes.size(); k++) {
                if ((Rip.nodes.get(k) == (aux.get(j).getIpAddress())));
                break;
            }
            if (k == Rip.nodes.size()) {
                Rip.nodes.add(aux.get(j).getIpAddress());
            }
        }
        System.out.println(Rip.nodes);
    }
}
