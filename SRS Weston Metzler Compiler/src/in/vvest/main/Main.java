package in.vvest.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import in.vvest.lexer.Lexer;
import in.vvest.lexer.Token;

public class Main {

	public static void main(String[] args) throws FileNotFoundException {
		Lexer lex = new Lexer(new File("res/Theta1.txt"));
		List<Token> tokens = lex.tokenize();
	}

}
