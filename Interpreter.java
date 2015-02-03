import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


/*
 * L'interprete funziona nel seguente modo:
 * 
 * fase 1: l'interprete prende come input tutto il file sotto forma di stringa, lo scorre carattere per carattere costruendo un array di token. 
 * 		   I caratteri non validi nella grammatica NON vengono ignorati, ma vengono marcati come "UNKNOWN" all'interno dell'array (vedi classe Token per maggiori informazioni)
 * 		   Vedi computeFile(String)
 * 
 * fase 2: viene effettuata l'analisi sintattica tramite un ciclo che analizza uno statement per iterazione (vedi syntaxAnalysis()).
 *         Contestualmente ad ogni statement analizzato vengono stampati i risultati ottenuti o gli errori rilevati.
 * 
 * 
 */





public class Interpreter {
	
	Interpreter(){
		
		varMap = new HashMap <String, Long>();
		syntTree = new NodeExpr();
		token_list = new ArrayList<Token>();

				
	}
	
	/*
	 * 
	 * MEMBRI PRIVATI
	 * 
	 * 
	 */
	
	private Map<String, Long> varMap; //Symbol table che contiene le variabili inserite insieme ai loro relativi valori
	private NodeExpr syntTree; //E' la radice del sottoalbero contenente le espressioni da calcolare
	private Statement definition; //è la radice dell'albero sintattico: ha un solo figlio (Espressione) se l'istruzione è di tipo GET, 2 figli (variabile ed espressione) se è di tipo SET
	private ArrayList <Token> token_list; //contiene TUTTI i caratteri, convertiti in TOKEN, presenti all'interno del file
	private final static long MAX_VALUE = (long) Math.pow(2,32); //costante necessaria per il controllo dell'overflow: il dominio di valori validi è [0,2^32]
	
	/*
	 * 
	 * CLASSI PER L'ALBERO SINTATTICO
	 * 
	 * 
	 */
	
	public enum key{ //Enum che elenca i possibili tipi di token che possono essere trovati all'interno del file in input
		PAR_APERTA, 
		PAR_CHIUSA, 
		STATEMENT_SET, 
		STATEMENT_GET, 
		OP_ADD, 
		OP_MUL, 
		OP_DIV, 
		OP_SUB, 
		NUMBER, 
		VARIABLE, 
		UNKNOWN 
		
		/*
		 * Laddove tutti i membri sono auto-esplicativi col loro nome, 
		 * il membro UNKNOWN costituisce tutti i caratteri e token 
		 * che non sono previsti dalla grammatica, come simboli all'infuori delle parentesi tonde, 
		 * variabili non valide e numeri non validi (che iniziano con 0)
		 * 
		 */

		};
	
	private abstract class Expression {	//Classe astratta vuota che serve come generalizzazione per: Variabili, Numeri e Operatori 	

	}

	private abstract class Statement { //Classe astratta vuota che serve come generalizzazione per: SET e GET

		NodeExpr exp;
		
	}
	
	private class NodeExpr { //Nodo dell'albero sintattico, ha 2 figli e rappresenta un'espressione, cioè uno tra Variabili, Numeri e Operatori
		
		NodeExpr left;
		NodeExpr right;
		
		Expression expr_type;
		
		NodeExpr(){
			
		}
		
	}
		
	private class Operator extends Expression{ //classe derivata da Expression che identifica gli operatori con una Stringa
		
		String op_type; 
		
		//Può essere: 
		//"*" per Token di tipo key.OP_MUL, 
		//"+" per Token di tipo key.OP_ADD, 
		//"-" per Token di tipo key.OP_SUB, 
		//"/" per Token di tipo key.OP_DIV
		
		Operator(String o){
			
			op_type = o;
			
		}
		
	}

	private class Number extends Expression{
		
		long value;
		
		Number(long v){
			
			value = v;
			
		}
		
	}
	
	private class Variable extends Expression{
		
		String name; //Nome della variabile: serve per l'identificazione all'interno della Symbol Table
		
		Variable(String n){
			
			name = n;
			
		}
		
	}
	
	private class varDef extends Statement{ 
		
		//Classe che si riferisce agli Statement di tipo SET
		//Ha come figli un sottoalbero di espressioni (vedi NodeExpr) e una variabile
		
		Variable var;
		
		varDef(Variable v, NodeExpr x){
			
			var = v;
			exp = x;
			
		}
		
	}
	
	private class expDef extends Statement{
		
		//Classe che si riferisce agli Statement di tipo GET
		//Ha come figlio un sottoalbero di espressioni (vedi NodeExpr)
		
		expDef(NodeExpr x){
			
			exp = x;
			
		}
		
	}
	
	
	/*
	 * 
	 * METODI ACCESSORI
	 * 
	 * 
	 */

	private int nextValidStatement(int i){ //Ritorna l'indice al primo statement valido successivo all'indice parametro
		
		//Questo metodo viene chiamato unicamente in presenza di errori all'interno del costrutto catch
		//Prende in input l'indice dell'array a cui l'iterazione si è bloccata a causa di errori.
		//Questo indice viene ottenuto dal metodo GeneralException.getIndexError()
		//Una volta avviato questo metodo, scorre l'ArrayList di Token fino all'inizio di un **POSSIBILE** statement valido, restituendone l'indice
		
		//Ovviamente, non c'è nessuna certezza che questo statement sia effettivamente valido, in quanto
		//viene ricercato unicamente il Pattern ( key.PAR_APERTA + {GET | SET} )
				
		while(  i < token_list.size() &&
				!(token_list.get(i).token_type.equals(key.PAR_APERTA) && ( 
					token_list.get(i+1).token_type.equals(key.STATEMENT_GET) || 
					token_list.get(i+1).token_type.equals(key.STATEMENT_SET) 
				))){

			++i;
		}
		
		return i;
		
	}
	
	private int parseVariable(String str, int i){ //Esegue il parsing di una variabile
		
		/*
		 * Viene chiamato ogni volta che viene incontrata una lettera all'interno della Stringa costituente il file.
		 * Condizione per cui venga chiamato, tuttavia, è che tale lettera non faccia parte di un operatore
		 * 
		 * Una volta chiamato, questo metodo scorre la stringa fino al primo Whitespace o a una parentesi aperta o chiusa:
		 * --nel primo caso, abbiamo l'inizio di un'espressione e.g. (SET var(ADD 5 5))
		 * --nel secondo caso, abbiamo la fine di un'espressione o statement e.g. (SET var nuovavar), (GET (ADD var var))
		 * 
		 * Se prima del whitespace viene incontrato un carattere non permesso (carattere numerico o simbolo non permesso)
		 * la variabile non è valida (tramite l'apposita flag isValid ) e viene inserita come Token UNKNOWN all'interno dell'array
		 * 
		 */
		
		String var_name = new String();
		
		boolean isValid = true;
		
		while(!Character.isWhitespace(str.charAt(i))){
			
			if(str.charAt(i)==')' || str.charAt(i) == '(')
				break;
			if(!Character.isLetter(str.charAt(i)))
				isValid = false;
			
			var_name = var_name + str.charAt(i);

			++i;
		}
		
		if(isValid)
			token_list.add(new Token (key.VARIABLE, var_name));
		else 
			token_list.add(new Token (key.UNKNOWN, var_name));
		
		return i;
	}
	
	private boolean isOperator(String line, int i){ //Verifica se ci sono delle sottostringhe valide per la presenza di un'operatore
		
		/*
		 * Questo metodo viene chiamato durante l'analisi lessicale
		 * e ha il compito di trovare Operatori all'interno della stringa presa in analisi
		 * 
		 * Restituisce true se viene verificato il Pattern = "{ADD|SUB|MUL|DIV}{\s|(}"
		 * cioè, se è presente una sottostringa operatore seguita da un whitespace o una parentesi aperta
		 * 
		 */
		
		return (line.substring(i, i+3).equals("ADD") || 
				line.substring(i, i+3).equals("MUL") || 
				line.substring(i, i+3).equals("SUB") || 
				line.substring(i, i+3).equals("DIV")) &&
				(Character.isWhitespace(line.charAt(i+3)) || line.charAt(i+3) == '(')
				;
		
	}
	
	private boolean isOperator(key op){//Verifica se il token preso come parametro è un operatore
		
		/*
		 * Questo metodo viene chiamato durante l'analisi sintattica, cioè, durante lo scorrimento
		 * dell'ArrayList di Token
		 * 
		 */
		
		return op.equals(key.OP_ADD ) || op.equals(key.OP_DIV)  || op.equals(key.OP_MUL)  || op.equals(key.OP_SUB) ;
		
	}

	private int parseDigit(String line, int i){ //Esegue il parsing di un numero
		
		/*
		 * Questo metodo viene chiamato durante l'analisi lessicale e si occupa di identificare numeri validi all'interno
		 * della stringa esaminata. Scorre la stringa esaminata finchè sono presenti caratteri numerici. 
		 * 
		 * Se un numero non è valido, cioè se inizia con uno zero seguito da un carattere numerico
		 * e.g. "085" allora viene contrassegnato come Token UNKNOWN e si riprende l'analisi lessicale
		 * 
		 */
		
		long number = 0;
		
		boolean isValid = true;

		if(line.charAt(i) == '0' && char2int(line.charAt(i+1)) > 0 && char2int(line.charAt(i+1)) < 10 ) //Se il numero è nel formato non valido "0" + resto
			isValid = false;
			
		while(char2int(line.charAt(i)) >= 0 && char2int(line.charAt(i)) < 10){ //Ciclo che dura finchè si ha un carattere numerico presente
			number = number * 10 + char2int(line.charAt(i));
			++i;			
		}
				
		if (isValid)
			token_list.add(new Token (key.NUMBER, number));
			
		else
			token_list.add(new Token (key.UNKNOWN, "0" + number));
		
		return i;
		
	}
	
	public static int char2int(char ch){
		
		//Semplice metodo che converte il carattere nel suo valore numerico relativo al carattere '0'
		//Quindi, un carattere numerico potrà avere solamente un ritorno >=0 e <10
		
		return Character.getNumericValue(ch)-Character.getNumericValue('0');
		
	}
	
	//Per il calcolo delle espressioni, viene utilizzata questa interfaccia insieme a questa mappa
	//Così come viene inizializzata, è possibile utilizzarla nel seguente modo
	
	//int result = opByName.get(string_operator).calculate(operand1, operand2);
	//Dove string_operator può essere "+", "-", "*" o "/"

	public interface Operation {
	    long calculate(long a, long b);
	}
	
	public static final Map<String,Operation> opByName = new HashMap<String,Operation>();
	static {
	    opByName.put("+", new Operation() {
	        public long calculate(long a, long b) {
	            return a+b;
	        }
	    });
	    opByName.put("-", new Operation() {
	        public long calculate(long a, long b) {
	            return a-b;
	        }
	    });
	    opByName.put("*", new Operation() {
	        public long calculate(long a, long b) {
	            return a*b;
	        }
	    });
	    opByName.put("/", new Operation() {
	        public long calculate(long a, long b) {
	            return a/b;
	        }
	    });
	}
	
	
	/*
	 * 
	 * CORE METHODS AND CLASSES
	 * 
	 * 
	 */
	
	public static class Token extends Throwable{ //Classe utilizzata per immagazzinare le informazioni sui token trovati
		
		//E' stata resa Throwable per riuscire ad avere le informazioni relative al token interessato dagli errori,
		//in particolare, l'identifier e il tipo enum del token
			
		
		private static final long serialVersionUID = 1L;
		key token_type; //Tipo di token
		String identifier; //Stringa che identifica il token all'interno del file preso in input
		long number; //Utilizzato solo dai Token con token_type = key.NUMBER
		
		
		Token(key k, String v){
			
			token_type = k;
			identifier = v;
			
		}
		
		Token(key k, long n){
			
			token_type = k;
			number = n;
			identifier = Long.toString(n);
			
		}
		@Override
		public String toString(){ //Override del metodo toString() da stampare in caso di errori
			
			return "\"" + identifier + "\" of " + token_type + " Token type";
			
			
		}
		
	}

	public void computeFile(String line){ //Metodo principale, che viene chiamato dalla classe Main
		int i=0;
		
		/*
		 *Il ciclo while esegue l'analisi lessicale carattere per carattere per tutta la lunghezza della stringa presa in input 
		 *Il comportamento è il seguente:
		 *
		 *---Se il carattere rilevato è numerico, chiamo parseDigit(), che restituisce l'indice del nuovo carattere da analizzare. N.B: il numero può essere non valido ed etichettato come UNKNOWN
		 *---Se il carattere rilevato è una parentesi, viene aggiunto il relativo token a token_list
		 *---Se il carattere rilevato è un whitespace (\s, \t etc.), viene aumentato l'indice di 1
		 *---Se viene rilevata una sottostringa "GET" o "SET", aggiungo il relativo token a aumento l'indice di 3
		 *---Se il ritorno di isOperator(String, int) è TRUE, è presente una sottostringa Operatore "ADD", "MUL", "SUB" o "DIV", aggiungo il token e aumento l'indice di 3
		 *---Se il carattere rilevato è una lettera, può essere presente una variabile e viene chiamata parseVariable() N.B: la variabile può essere non valida ed etichettata come UNKNOWN
		 *---In tutti gli altri casi, che vedono tutti i caratteri che non sono consentiti nella grammatica, viene inserito un token UNKNOWN e aumentato l'indice di 1
		 *
		 */

		while(i<line.length()){
			if(Character.isDigit(line.charAt(i)))
				
				i=parseDigit(line, i);
				
			else if(line.charAt(i) == '(' || line.charAt(i) == ')'){
				
				
				if(line.charAt(i) == '(')
					token_list.add(new Token(key.PAR_APERTA, "("));
					
				else
					token_list.add(new Token(key.PAR_CHIUSA, ")"));
					
				
				++i;
			}else if (Character.isWhitespace(line.charAt(i))){
				
				++i;
				
			}else if(line.substring(i, i+3).equals("GET") || line.substring(i, i+3).equals("SET")){

				if (line.charAt(i) == 'G')
					
					token_list.add(new Token(key.STATEMENT_GET, "GET"));
					
				else
					
					token_list.add(new Token(key.STATEMENT_SET, "SET"));
					
				i= i+3;
			}else if (isOperator(line, i)){
				
				if(line.substring(i, i+3).equals("ADD"))
					
					token_list.add(new Token(key.OP_ADD, "+"));
				
				else if(line.substring(i, i+3).equals("MUL"))
					
					token_list.add(new Token(key.OP_MUL, "*"));
				
				else if(line.substring(i, i+3).equals("SUB"))
					
					token_list.add(new Token(key.OP_SUB, "-"));
				
				else
					
					token_list.add(new Token(key.OP_DIV, "/"));
				

				i=i+3;
			}
			else if(Character.isLetter(line.charAt(i)))
				
				i=parseVariable(line, i);
			
			else{
				
				
				String unknown_token = "" + line.charAt(i);
				++i;				
				
				token_list.add(new Token(key.UNKNOWN, unknown_token));
				
			}

			
		}
		
		
		/*
		 * Il metodo syntaxAnalysis si occupa dell'analisi sintattica di token_list
		 * Maggiori informazioni all'interno della sua definizione.
		 * 
		 */
			
		syntaxAnalysis(token_list);
			
		}
	
	private void syntaxAnalysis(ArrayList<Token> list){ //Analisi sintattica e lessicale, statement per statement
		
		/*
		 * Questo metodo costituisce le fondamenta dell'analisi sintattica operata.
		 * Esegue un ciclo lungo tutta la lista di Token per analizzare la presenza di pattern validi e compatibili con le ragole della grammatica
		 * Ad ogni ciclo viene analizzato uno statement e ne viene fatta contestualmente l'analisi semantica per il risultato che si vuole ottenere
		 * 
		 */
		
		int i=0;
		while(i<list.size()){
		
			try{
				
				i = parseStatement(list, i, syntTree ); //Analizzo lo statement che parte dall'indice i
				
			}catch(GeneralException e){
				
				printResult(e); //Stampo l'errore che è stato "lanciato" da parseStatement()
				
				i=nextValidStatement(e.getIndexError()); //Subito dopo, cerco il primo pattern valido per un possibile nuovo statement da analizzare
				
				definition = null; //Visto che l'analisi dello statement è risultata non valida, dereferenzio i puntatori alla radice dell'albero
				syntTree = null; //e alla radice del sotto-albero che è costituito dall'espressioni
				
				continue; //ricomincio il loop a partire dal nuovo indice ottenuto da nextValidStatement
				
			}
			
			try{ //viene eseguito SOLAMENTE se non ci sono stati errori nell'analisi dello statement
				
				if (definition instanceof expDef){
					
					//Se la radice dell'albero corrisponde a uno Statement GET
					//posso calcolare il risultato a partire dalla radice del suo sotto-albero
				
					long result = calculateTreeExpression(definition.exp);
				
					System.out.println(result); //Stampo i risultati
				
									
				}else if (definition instanceof varDef){
				
					//Se la radice dell'albero corrisponde a uno Statement SET
					//posso calcolarmi il valore che assumerà la variabile interessata dallo statement
					//per poi inserire la variabile nella Symbol Table varMap
					
					Variable v = ((varDef) definition).var;
					long value = calculateTreeExpression(definition.exp);
					
					varMap.put(v.name, value);
					
					printResult(v);
				
				}
				
				
			}catch(ComputingException e){
				
				printResult(e); //Stampa errori di tipo semantico: Underflow, Overflow, Divisione per 0 e Variabile non definita
				
			}

	
		}
	}
	
	private int parseStatement(ArrayList<Token> list, int i, NodeExpr x ) throws GeneralException{//Analisi di un singolo statement
		
		/*
		 * Questa funzione analizza la lista per individuare un pattern di token che può costituire uno statement , il quale può essere
		 * 
		 * ( GET expression ) --> PAR_APERTA,STATEMENT_GET,(espressione),PAR_CHIUSA
		 * ( SET variable_id expression ) --> PAR_APERT,STATEMENT_SET,VARIABLE,(espressione),PAR_CHIUSA
		 * 
		 * N.B: La s-exp (espressione) viene a sua volta definita in parseExpressionRecursive
		 * 
		 * Se non viene rilevato un pattern di questo tipo viene lanciata un'opportuna eccezione.
		 * Contestualmente viene costruito l'albero sintattico, il quale viene completato quando
		 * il metodo parseExpressionRecursive conclude con successo.
		 * 
		 * Se conclude con successo, questo metodo ritorna l'indice allo statement successivo, che quindi può far continuare 
		 * il loop originario all'interno di syntaxAnalysis.
		 * 
		 */
		
		if(list.get(i).token_type.equals(key.PAR_APERTA)){
			++i;
			
			if(i == list.size() ) //fa in modo che la lettura dell'array non continui se l'indice ha raggiunto la sua dimensione massima prima della chiusura con una parentesi chiusa
				throw new GeneralException("END OF FILE. No token at index ", i);
			
			if(list.get(i).token_type.equals(key.STATEMENT_GET)){ //Se si tratta di uno statement per la definizione di un'espressione
				
				if(x == null)
					x = new NodeExpr();
				
				definition = new expDef(x); //Inizializzo la radice dell'albero sintattico
				
				//Controllo se il pattern (GET expression) è valido

				
				if(!(list.get(i+1).token_type.equals(key.NUMBER) || list.get(i+1).token_type.equals(key.VARIABLE) || list.get(i+1).token_type.equals(key.PAR_APERTA)))
					throw new GeneralException("Expression expected in Statement", list.get(i+1), i+1);
				
				i = parseExpressionRecursive(list, i+1, x); //Chiamata ricorsiva per il riconoscimento dell'espressione
												
			}else if (list.get(i).token_type.equals(key.STATEMENT_SET)){ //Se si tratta di uno statement per la definizione di una variabile
				
				if(list.get(i+1).token_type.equals(key.VARIABLE)){ //Se è presente una variabile, altrimenti è un errore
					
					if(x == null)
						x = new NodeExpr();
					
					Variable var = new Variable(list.get(i+1).identifier);
					definition = new varDef(var, x); //Inizializzo la radice dell'albero sintattico
					
					//Controllo se il pattern (SET variable_id expression) è valido
					
					if(!(list.get(i+2).token_type.equals(key.NUMBER) || list.get(i+2).token_type.equals(key.VARIABLE) || list.get(i+2).token_type.equals(key.PAR_APERTA)))
						throw new GeneralException("Expression expected in Statement", list.get(i+2), i+2);
					
					i = parseExpressionRecursive(list, i+2, x);//Chiamata ricorsiva per il riconoscimento dell'espressione

				} else throw new GeneralException("Variable token in SET statement expected", list.get(i+1), i+1);//Se non c'è un token valido
				
			} else
				throw new GeneralException("GET or SET statement token expected", list.get(i), i);
			
			if(i == list.size() ) //fa in modo che la lettura dell'array non continui se l'indice ha raggiunto la sua dimensione massima prima della chiusura con una parentesi chiusa
				throw new GeneralException("END OF FILE. No token at index ", i);
			
			if(!list.get(i).token_type.equals(key.PAR_CHIUSA)) //L'espressione si deve necessariamente chiudere con una parentesi chiusa, altrimenti è un errore
				throw new GeneralException("Closing Parenthesis token expected in Statement", list.get(i), i);
			
			
		} else throw new GeneralException ("Starting parenthesis token expected in Statement", list.get(i), i);
		
			
		
		return (i+1);
	}
	
	private int parseExpressionRecursive(ArrayList<Token> list, int i, NodeExpr x) throws GeneralException{//Analisi di un'espressione completa 
		
		/*
		 *Un'espressione può essere:
		 *
		 *-1-un numero (nodo foglia dell'albero sintattico)
		 *-2-una variabile (nodo foglia dell'albero sintattico)
		 *-3-un'espressione del tipo ( {ADD|MUL|SUB|DIV} (exp_operando1) (exp_operando2) ) 
		 * 
		 * -4-Se il Token è UNKNOWN viene lanciata un'opportuna eccezione. 
		 * 
		 */
		
		if(i == list.size() ) //fa in modo che la lettura dell'array non continui se l'indice ha raggiunto la sua dimensione massima prima della chiusura con una parentesi chiusa
			throw new GeneralException("END OF FILE. No token at index ", i);
		
		if(list.get(i).token_type.equals(key.UNKNOWN)) //Caso -4-
			throw new GeneralException("Parenthesis or Number or Variable Expected in Expression", list.get(i), i);
		
		else if(list.get(i).token_type.equals(key.NUMBER) ){ //Caso -1-
			
			x.expr_type = new Number(list.get(i).number);
			x.left = null;
			x.right = null;
			
		} else if(list.get(i).token_type.equals(key.VARIABLE)){ //Caso -2-
														
				x.expr_type = new Variable(list.get(i).identifier);
				x.left = null;
				x.right = null;
			
			
		}else if(list.get(i).token_type.equals(key.PAR_APERTA)  ){ //Caso -3-
			++i;
			
			if(i == list.size() ) //fa in modo che la lettura dell'array non continui se l'indice ha raggiunto la sua dimensione massima prima della chiusura con una parentesi chiusa
				throw new GeneralException("END OF FILE. No token at index ", i);
			
			if(isOperator(list.get(i).token_type)){ //Viene creato il nodo dell'operatore, con il suo relativo valore "+", "*", "/", "-"
				
				String op = list.get(i).identifier;
				x.expr_type = new Operator(op);		
				
			}else throw new GeneralException ("Operator token expected", list.get(i), i);
			
			/*
			 * Vengono creati i nodi figli dell'espressione, che sono esaminati da una chiamata ricorsiva.
			 * Tra la costruzione del figlio sinistro e del figlio destro, viene verificato il rispetto della grammatica dell'espressione,
			 * la quale prevede un'inizio di espressione costituito da una parentesi aperta, o un numero o una variabile.
			 *  
			 * 
			 */
			
			
			NodeExpr left_child = new NodeExpr();
			i=parseExpressionRecursive(list, i+1, left_child);
			
			if(i == list.size() ) //fa in modo che la lettura dell'array non continui se l'indice ha raggiunto la sua dimensione massima prima della chiusura con una parentesi chiusa
				throw new GeneralException("END OF FILE. No token at index ", i);
			
			if(!(list.get(i).token_type.equals(key.NUMBER) || list.get(i).token_type.equals(key.VARIABLE) || list.get(i).token_type.equals(key.PAR_APERTA)))
				throw new GeneralException("Operand expected in Expression", list.get(i), i);
				
			NodeExpr right_child = new NodeExpr();
			i=parseExpressionRecursive(list, i, right_child);
			
						
			if(i == list.size() ) //fa in modo che la lettura dell'array non continui se l'indice ha raggiunto la sua dimensione massima prima della chiusura con una parentesi chiusa
				throw new GeneralException("END OF FILE. No token at index ", i);
			
			
			else if(!(list.get(i).token_type.equals(key.PAR_CHIUSA))) //Una parentesi aperta deve essere sempre chiusa, altrimenti si ha un errore sintattico
				throw new GeneralException("Closing parenthesis token expected in Expression", list.get(i), i);
			
			
			x.left = left_child;
			x.right = right_child;			
			
			
		}
		
		return i+1;		
		
	}
	
	private long calculateTreeExpression(NodeExpr node) throws ComputingException{//Calcolo dell'albero sintattico di un'espressione
		
		/*
		 * Esegue la visita dell'albero che ha come radice NodeExpr node eseguendone il casting appropriato e ritornando il risultato delle operazioni.
		 * 
		 * Se il nodo è numerico, ritorna il suo valore
		 * Se il nodo è di tipo variabile, controlla la sua presenza nella Symbol Table (se la variabile è assente viene lanciata un'eccezione) e ritorna il suo valore
		 * Se il nodo è di tipo operatore:
		 * --calcola l'espressione relativa al figlio sinistro
		 * --calcola l'espressione relativa al figlio destro (viene lanciata un'eccezione se l'operatore è di divisione e il divisore è 0)
		 * --ottiene il risultato stampabile facendo ricorso alla mappa opByName
		 * --ritorna il risultato o lancia un'eccezione per underflow se il risultato calcolato è <0
		 * 
		 * 
		 */
		
		long result=-1;		
		
		if(node.expr_type instanceof Number) //Se il nodo è numerico ritorno il valore del numero
			return ((Number) node.expr_type).value;
			
		else if(node.expr_type instanceof Variable){ //Se il nodo è di tipo variabile
			
			String var_name = ((Variable) node.expr_type).name;
			
			if(!varMap.containsKey(var_name)) //Controllo se la variabile è contenuta nella Symbol Table
				throw new ComputingException("Undefined Variable " + "\"" + var_name + "\""); 
			
			return varMap.get(var_name);
		}
			
		else if(node.expr_type instanceof Operator){ //Se il nodo è di tipo operatore
			
			String op=((Operator) node.expr_type).op_type;
			
			long op1 = calculateTreeExpression(node.left); //Calcolo il primo operando
			long op2 = calculateTreeExpression(node.right); //Calcolo il secondo operando
						
			if(op2 == 0 && op.equals("/")) //In questo caso sto per dividere per 0
				throw new ComputingException("Dividing by 0, impossible to compute Expression");
			
			result = opByName.get(op).calculate(op1, op2); //Uso la mappa Main.opByName
			
			if (result < 0 && op.equals("-")) //Caso di underflow
				
				throw new ComputingException("Underflow, result is negative and not supported: " + result);
			
			if (result > MAX_VALUE ) //Caso di overflow
				
				throw new ComputingException("Overflow, result is beyond MAX_VALUE of domain: " + result);
			
		}
		
		return result;
	}
	
	
	/*
	 * 
	 * METODI DI STAMPA
	 * 
	 * 
	 */
	
	
	private void printResult(Variable v){ //Stampa il risultato, nel caso che lo statement chiamante sia un SET
		
		long value = varMap.get(v.name);

		System.out.println(v.name + " = " + value);
		
		
	}
	
	private void printResult(GeneralException e){ //Stampa le eccezioni di tipo lessicale o sintattico
		
		System.out.println("ERROR : " + e.getMessage());
		
	}
	
	private void printResult(ComputingException e){ //Stampa le eccezioni di tipo semantico
		
		System.out.println("ERROR : " + e.getMessage());
		
	}


	
	
}
