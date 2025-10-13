package guimemorygame.model;


public class LatinAlphabet implements Alphabet {
	
	@Override
	public char[] toCharArray() {
	
		int alphabet_length = '\u007E' - '\u0021' + 1;
		
	    char[] latin;
	    latin = new char[alphabet_length];
	
	    for(int i = 0; i < alphabet_length; i += 1) {
	    	latin[i] = (char)('\u0021' + i);
	    }
	
	    return latin;
	}
}