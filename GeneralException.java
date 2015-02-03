
public class GeneralException extends Throwable {

	/**
	 * 
	 * Questa eccezione gestisce gli errori di tipo lessicale e sintattico
	 * rilevati durante la chiamata delle funzione parseStatement all'interno di syntaxAnalysis.
	 * I suoi membri e i metodi ricalcano la classe Exception, facendo l'override del metodo Throwable.getMessage()
	 * 
	 */
	private static final long serialVersionUID = 7629026305144152942L;
	private int indexError;
	private String detailMessage;
	private Throwable cause;
	
	public GeneralException(String message, int i) {
		fillInStackTrace();
		detailMessage = message;
		indexError = i;
	}
	
	public GeneralException(String message){
		
		detailMessage = message;
		
	}

	public GeneralException(Throwable cause, int i) {
		fillInStackTrace();
		this.cause = cause;
		indexError = i;
	}

	public GeneralException(String message, Throwable cause, int i) {

		fillInStackTrace();
		this.cause = cause;
		indexError = i;
		detailMessage = message;
	}

	@Override
	public String getMessage(){
		
		if(cause != null)
			return detailMessage + ". Index: " + indexError + " is " + cause.toString();
		else
			return detailMessage + indexError;
		
	}
	
	public int getIndexError(){
		
		return indexError;
		
	}

	

}
