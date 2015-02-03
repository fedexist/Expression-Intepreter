import java.io.FileReader;
import java.util.Scanner;


public class Main {

	public static void main(String[] args) {
		
		if(args.length < 1){ // Controllo se è stato dato un file in input
			
			System.err.println("Missing Argument");
			System.exit(-1);
			
		}
		
		assert(args.length >= 1);
		
		String fileContent = null;
		String inputFile = args[0];
		
		Interpreter ExpParser = new Interpreter(); //Inizializzo l'interprete
		
		FileReader inFile = null;
		try {
			inFile = new FileReader(inputFile);
		} catch (Exception e) {
			System.err.println("Non posso aprire il file:" + inputFile);
		}
		
		Scanner scan = new Scanner(inFile);
		
			
		fileContent = scan.useDelimiter("\\A").next(); //Prendo l'intero file come Stringa
			
		//Do la Stringa costituente il file come input alla funzione computeFile, che si occuperà di effettuare l'analisi lessicale, 
		//l'analisi sintattica e l'analisi semantica, nonchè la stampa di errori dove sia necessario

		ExpParser.computeFile(fileContent); 
			
		
		scan.close();
		
		
		

	}
	


}
