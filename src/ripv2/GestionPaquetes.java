/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ripv2;

/**
 *
 * @author Alberto
 */
public class GestionPaquetes {
    public static String masksetter(int length){
		
		int[] array = new int[32];
		String mascara = new String();
		
		//Se recorre el array
		for(int i=0; i < length; i++){
			array[i] = 1;
		}
		
		//Creamos un array dividiendo el anterior entre 4
		for(int i=0; i < 4; i++){
			int num = 0;
			//Para cada uno de los bytes, obtenemos el numero sumandole potencias descendentes de 2, empezando en 2^8, tantas
			//veces como 1 haya
			for(int k = 0; k < 8 && array[(i*8)+k] != 0; k++){
				num = num + (int) Math.pow(2, 8-(k+1));
			}
			if(i != 0)
				mascara = mascara.concat(".");
			//Concatenamos los numeros para obtener la mascara
			mascara = mascara.concat(new Integer(num).toString());
		}
		
		return mascara;
	}
public static int masksetterStr(String mascara){
		
		String[] m = mascara.trim().split("\\.");
		int contador = 0;
		
		//Comprobación de bytes
		for(int i = 0; i < 4; i++){
			int num = Integer.parseInt(m[i]);
			int k = 1;
			//Si el byte no es cero, le vamos restando potencias de 2, empezando por 128
			while(num != 0){
				num = num - (int) Math.pow(2,(8-k));
				k++;
				//Vamos incrementando el contador que nos da la longitud
				contador ++;
			}
		}
		
		return contador;
	}
}
