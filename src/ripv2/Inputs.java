package ripv2;

import java.util.ArrayList;

//Case que administrará las entradas del vector.
public class Inputs {

    private static final int RIP_LENGTH = 20;
    private static final int IP_LENGTH = 4;
    private static final int POS_ADDRESS_FAMILY_ID = 5;
    private static final int POS_IP_ADDRESS = 8;
    private static final int POS_MASCARA = 12;
    private static final int POS_COSTE = 23;
    private static final String addressFamilyID = "2";
    private static final int CORRECCION_BYTE = 128;

    private String ipAddress;
    private String mascara;
    private int coste;

    Inputs(String ipAddress, String mascara, int coste) {
        this.ipAddress = ipAddress;
        this.mascara = mascara;
        this.coste = coste;
    }

    public ArrayList<Byte> procesarInputs(ArrayList<Byte> datos, int numeroEntrada) {
        String[] auxiliar;
        String ipAddress = getIpAddress();
        String mascara = getMascara();
        int coste = getCoste();
        auxiliar = new String[IP_LENGTH];

        //Ponemos los bytes de la direccion IP
        auxiliar = ipAddress.split("\\.");

        datos.add(numeroEntrada * RIP_LENGTH + POS_ADDRESS_FAMILY_ID, Byte.parseByte(addressFamilyID));
        for (int i = 0; i < IP_LENGTH; i++) {
            Integer integer = Integer.parseInt(auxiliar[i]);
            //Añadimos un factor de correccion para poder pasar de IP (0 a 255) a byte (-128 a 127)
            integer = integer - CORRECCION_BYTE;
            datos.add(numeroEntrada * RIP_LENGTH + POS_IP_ADDRESS + i, Byte.parseByte(integer.toString()));
        }

        //Ponemos los bytes de la mascara
        auxiliar = mascara.split("\\.");
        for (int i = 0; i < IP_LENGTH; i++) {
            Integer integer = Integer.parseInt(auxiliar[i]);
            //Añadimos un factor de correccion para poder pasar de Mascara (0 a 255) a byte (-128 a 127)
            integer = integer - CORRECCION_BYTE;
            datos.add(numeroEntrada * RIP_LENGTH + POS_MASCARA + i, Byte.parseByte(integer.toString()));
        }

        //Añadimos el coste
        datos.add(numeroEntrada * RIP_LENGTH + POS_COSTE, Byte.parseByte(new Integer(coste).toString()));

        return datos;
    }

    //toString, getters & setters.
    public String toString() {
        return "Destino: " + ipAddress + "\tMascara: " + mascara + "\tCoste: " + coste;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getMascara() {
        return mascara;
    }

    public void setMascara(String mascara) {
        this.mascara = mascara;
    }

    public int getCoste() {
        return coste;
    }

    public void setCoste(int coste) {
        this.coste = coste;
    }

}
