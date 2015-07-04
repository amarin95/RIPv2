package ripv2;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import static ripv2.GestionPaquetes.masksetter;
import static ripv2.Rip.COSTE_INF;
import static ripv2.Rip.POS_ADDRESS_FAMILY_ID_AUT;
import static ripv2.Rip.vectorList;

public class RipPacket {

    private static final int PUERTO_UDP = 5000;
    private static final int CABECERA = 4;
    private static final int RIP_LENGTH = 20;
    private static final int IP_LENGTH = 4;
    private static final int CIFRADO_LENGTH = 20;
    private static final int TIPO_AUT = 2;
    private static final int ADDRESS_FAMILY_ID_AUT = -128; //Todo a unos para byte
    private static final int POS_CONSULTA = 0;
    private static final int POS_VERSION = 1;
    private static final int POS_ADDRESS_FAMILY_ID_AUT = 4;
    private static final int POS_TIPO_AUT = 7;
    private static final int POS_PASSWORD = 8;
    private static final int POS_IP_ADDRESS_REL = 8;
    private static final int POS_MASCARA_REL = 12;
    private static final int POS_COSTE_REL = 20;

    //Factor de correccion para pasar de ip, mascara y password (0 a 255) a byte (-128 a 127) y viceversa
    private static final int CORRECCION_BYTE = 128;

    private int consulta = 0;
    private int version = 0;
    private String[] password;
    private LinkedList<Inputs> entradas = new LinkedList<Inputs>();

    RipPacket(int consulta, int version, LinkedList<Inputs> entradas, String[] password) {
        this.consulta = consulta;//Tipo de consulta
        this.version = version;//version indicada
        this.entradas = entradas;//entradas del vector
        this.password = password;//Contraseña para la autentificación
    }

    // Metodo para el formateo del paquete UDP
    public DatagramPacket toDatagramPacket(String ipDestino) {

        //Creamos variables
        Inputs ripEntry;
        ArrayList<Byte> datos = new ArrayList<Byte>();

        //Sacamos la lista de Inputs del paquete
        LinkedList<Inputs> entradas = getEntradas();

        //Inicializamos el numero de entradas totales a 1 porque nuestros paquetes siempre se mandan con autentificacion
        int numeroEntrada = 1;

        //Hacemos que el array de datos tenga un tamaño minimo para poder acceder a las posiciones correspondientes
        tamañoMinimo(datos, RIP_LENGTH * entradas.size() + CABECERA + CIFRADO_LENGTH);

        //Procesamos la cabecera
        int consulta = getConsulta();
        int version = getVersion();

        //Añadimos la cabecera a los datos
        datos.add(POS_CONSULTA, Byte.parseByte((new Integer(consulta)).toString()));
        datos.add(POS_VERSION, Byte.parseByte((new Integer(version)).toString()));

        //Añadimos la parte de autentificacion
        datos.add(POS_ADDRESS_FAMILY_ID_AUT, Byte.parseByte((new Integer(ADDRESS_FAMILY_ID_AUT)).toString()));
        datos.add(POS_ADDRESS_FAMILY_ID_AUT + 1, Byte.parseByte((new Integer(ADDRESS_FAMILY_ID_AUT)).toString()));
        datos.add(POS_TIPO_AUT, Byte.parseByte((new Integer(TIPO_AUT)).toString()));

        for (int i = 0; i < 4; i++) {
            Integer integer = Integer.parseInt(password[i]);
            //Le añadimos un factor de correccion para poder pasarlo a byte (password de 0 a 255 y byte de -128 a 127)
            integer = integer - CORRECCION_BYTE;
            datos.add(POS_PASSWORD + i, Byte.parseByte(integer.toString()));
        }

        Iterator<Inputs> it = entradas.iterator();

        //Vamos añadiendo la informacion de cada entrada RIP
        while (it.hasNext()) {
            ripEntry = (Inputs) it.next();

            datos = ripEntry.procesarInputs(datos, numeroEntrada);

            //Incrementamos el contador de entradas procesadas
            numeroEntrada++;
        }

        //Conversion de ArrayList a byte[]
        Byte[] bit = datos.toArray(new Byte[datos.size()]);
        byte[] datosByte = new byte[entradas.size() * RIP_LENGTH + CABECERA + CIFRADO_LENGTH];
        for (int i = 0; i < entradas.size() * RIP_LENGTH + CABECERA + CIFRADO_LENGTH; i++) {
            datosByte[i] = bit[i];
        }

        try {
            return new DatagramPacket(datosByte, datosByte.length, InetAddress.getByName(ipDestino), PUERTO_UDP);
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    private static void tamañoMinimo(ArrayList<Byte> datos, int tamañoMin) {
        datos.ensureCapacity(tamañoMin);
        Byte b = new Byte((byte) 0);
        while (datos.size() < tamañoMin) {
            datos.add(b);
        }
    }

    //Metodo que devuelve un RipPacket a partir de un DatagamPacket
    public static RipPacket toRipPacket(DatagramPacket datagram) {

        //Obtenemos la longitud (en bytes) del paquete
        int longitud = datagram.getLength();
        int numeroEntradas = 0;
        String[] password = new String[4];
        try {
            //Cálculo del número de entradas.
            numeroEntradas = (longitud - CABECERA - CIFRADO_LENGTH) / RIP_LENGTH;
        } catch (NumberFormatException e) {

            e.printStackTrace();
            return null;
        }
        //Obtenemos los datos del paquete
        byte[] datos = datagram.getData();
        //Array con los datos del datagrama
        byte datosRip[] = new byte[RIP_LENGTH];

        //Lista que devolverá
        LinkedList<Inputs> list = new LinkedList<Inputs>();

        //Cabecera
        int consulta = datos[POS_CONSULTA];
        int version = datos[POS_VERSION];

        //Procesado del número de entradas
        for (int i = 0; i < numeroEntradas; i++) {
            for (int k = 0; k < RIP_LENGTH; k++) {

                datosRip[k] = datos[CABECERA + CIFRADO_LENGTH + i * RIP_LENGTH + k];//Copiado del array

            }

            list.add(procesarDatagrama(datosRip));
        }

        //Obtenemos la password y se le aplica el factor de corrección
        for (int i = 0; i < 4; i++) {
            Integer integer = Integer.parseInt(Byte.toString(datos[POS_PASSWORD + i]));
            integer = integer + CORRECCION_BYTE;
            password[i] = integer.toString();
        }
        //Devolvemos la lista
        return new RipPacket(consulta, version, list, password);
    }

    //Método para el procesamiento de las entradas RIP
    private static Inputs procesarDatagrama(byte[] entrada) {
        //variables locales auxiliares
        String ipAddressAux = "";
        String mascaraAux = "";
        byte[] byteCoste = new byte[4];
        ByteBuffer bB;

        //Sacamos la direccion IP de paquete (sabemos la posicion en la que deberia estar)
        for (int i = (POS_IP_ADDRESS_REL - CABECERA); i < POS_IP_ADDRESS_REL - CABECERA + IP_LENGTH; i++) {
            if (i != (POS_IP_ADDRESS_REL - CABECERA)) {
                ipAddressAux = ipAddressAux.concat(".");
            }

            Integer integer = Integer.parseInt(Byte.valueOf(entrada[i]).toString());
            //Sumamos el factor de correccion utilizado antes para pasar de byte (-128 a 127) a IP (0 a 255)
            integer = integer + CORRECCION_BYTE;
            ipAddressAux = ipAddressAux.concat(integer.toString());

        }

        //Sacamos la mascara del paquete por su posicion
        for (int i = (POS_MASCARA_REL - CABECERA); i < POS_MASCARA_REL - CABECERA + 4; i++) {
            if (i != 8) {
                mascaraAux = mascaraAux.concat(".");
            }
            Integer integer = Integer.parseInt(Byte.valueOf(entrada[i]).toString());
            //Sumamos el facto de correccion utilizado antes para pasar de byte (-128 a 127) a Mascara (0 a 255)
            integer = integer + CORRECCION_BYTE;
            mascaraAux = mascaraAux.concat(integer.toString());

        }

        for (int i = 0; i < IP_LENGTH; i++) {
            byteCoste[i] = entrada[POS_COSTE_REL - CABECERA + i];
        }

        //Obtenemos el coste
        bB = ByteBuffer.wrap(byteCoste);
        int coste = bB.getInt();

        //Construimos el RipPacket y lo devolvemos
        return new Inputs(ipAddressAux, mascaraAux, coste);
    }

    public String toString() {
        String doc;
        doc = "Tipo de consulta: " + consulta + " \tVersion: " + version + "\n" + "Password: ";
        for (int i = 0; i < password.length; i++) {
            if (i != 0) {
                doc = doc.concat(".");
            }
            doc = doc.concat(password[i]);
        }
        doc = doc.concat("\n");
        for (int i = 0; i < entradas.size(); i++) {
            doc = doc.concat(entradas.get(i).toString() + "\n");
        }
        return doc;
    }

    public static boolean coincidePassword(DatagramPacket datagram) {
        boolean ok = true;
        byte[] datos = datagram.getData();

        //Comprobamos si el datagrama tiene contraseña de autentificación
        int address_family_ID_1 = datos[POS_ADDRESS_FAMILY_ID_AUT];
        int address_family_ID_2 = datos[POS_ADDRESS_FAMILY_ID_AUT + 1];
        if (address_family_ID_1 == ADDRESS_FAMILY_ID_AUT && address_family_ID_2 == ADDRESS_FAMILY_ID_AUT) {
            //Si la tiene y coincide, retornamos un true
            RipPacket ripPacket = RipPacket.toRipPacket(datagram);
            String[] password = ripPacket.getPassword();
            for (int i = 0; i < password.length; i++) {
                ok = ok & password[i].equals(Rip.PASSWORD[i]);
            }
            return ok;
        }
        //Si no tiene contraseña, mandamos un false
        return false;
    }

    static DatagramPacket vectorListToDatagram(String ipDestino, int tipoConsulta, int version, String[] password) {

        LinkedList<Inputs> list = new LinkedList<Inputs>();
        Set<String> ipKeys = (Set<String>) vectorList.keySet();

        Iterator<String> it = ipKeys.iterator();
        while (it.hasNext()) {
            String ipNext = it.next();
            String[] ipSufijo = ipNext.split("/");
            String ipSubred = ipSufijo[0];
            String mascara = masksetter(Integer.parseInt(ipSufijo[1]));
            Vector v = vectorList.get(ipNext);
            int coste = v.getCoste();
            String nextHop = v.getNextHop();
            //Split Horizon: si el siguiente salto para un destino es la IP => coste infinito
            if (nextHop.equals(ipDestino)) {
                coste = COSTE_INF;
            }
            list.add(new Inputs(ipSubred, mascara, coste));
        }
        RipPacket ripPacket = new RipPacket(tipoConsulta, version, list, password);
        return ripPacket.toDatagramPacket(ipDestino);
    }

    //Getters y Setters
    public int getConsulta() {
        return consulta;
    }

    public void setConsulta(int consulta) {
        this.consulta = consulta;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public LinkedList<Inputs> getEntradas() {
        return entradas;
    }

    public void setEntradas(LinkedList<Inputs> entradas) {
        this.entradas = entradas;
    }

    public String[] getPassword() {
        return password;
    }

    public void setPassword(String[] password) {
        this.password = password;
    }

}
