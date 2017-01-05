package in.vvest.main;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.InterruptedException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

import in.vvest.generator.Generator;
import in.vvest.lexer.Lexer;
import in.vvest.lexer.Token;
import in.vvest.parser.Parser;
import in.vvest.parser.TreeNode;

public class Main {

	public static void main(String[] args) throws FileNotFoundException {
		Scanner src; 
		src = new Scanner(new File("res/Theta0.txt"));
		Lexer lex = new Lexer(src);
		List<Token> tokens = lex.tokenize();
		//System.out.println(tokens);
		TreeNode ast = Parser.parse(tokens);
		ast.print();
		if (args.length > 0) {
			try {
				Thread.sleep(Integer.parseInt(args[0]) * 1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (NumberFormatException e) {
				// Do nothing
			}
		}
		List<String> code = Generator.generateCode(ast);
		try {
			compileWeb(code);
		} catch (IOException e) {
			System.err.println("Unable to connect to ClrHome Assembler.");
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			System.err.println("Assembler response not able to be parsed");
		}
	}

	private static void compileWeb(List<String> code) throws IOException, ParserConfigurationException {
		String assemblerURL = "http://clrhome.org/asm/";
		URL url = new URL(assemblerURL);
		HttpURLConnection http = (HttpURLConnection) url.openConnection();
		http.setRequestMethod("POST");
		http.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

		PrintWriter print = new PrintWriter("out/asm.txt", "UTF-8");
		StringBuilder prgm = new StringBuilder();
		for (String statement : code) {
			String line = (statement.endsWith(":") || statement.startsWith(".") || statement.startsWith("#") ? "" : "\t") + statement;
			prgm.append(URLEncoder.encode(line + "\n", "UTF-8"));
			print.println(line);	
		}
		print.close();
		
		http.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(http.getOutputStream());
		wr.writeBytes(""
				+ "action=p&"
				+ "start=foo_z80&"
				+ "foo_z80="
				+ prgm);
		wr.flush();
		wr.close();
		
		BufferedReader in = new BufferedReader(new InputStreamReader(http.getInputStream()));
		String response = "";
		String input;
		while ((input = in.readLine()) != null)
			response += input;
		in.close();
		System.out.println();
		System.out.println("URL: " + http.getURL());
		System.out.println("Response Code: " + http.getResponseCode());
		System.out.println("Content-Type: " + http.getContentType());
		
		try {
			Thread.sleep(500); // Unnecessary. Makes sure output Streams don't mix in console
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		Pattern errorPat = Pattern.compile("<p class=\"en err\">(.[^<]*)<\\/p>");
		Matcher errorMat = errorPat.matcher(response);
		boolean foundErrors = false;
		while (errorMat.find()) {
			System.err.println(errorMat.group(1));
			foundErrors = true;
		}
		System.out.println();
		if (!foundErrors) {
			Pattern outputPat = Pattern.compile("<a href=\"(.*)\">download<\\/a>");
			Matcher outputMat = outputPat.matcher(response);
			if (outputMat.find()) {
				try {
					downloadPrgm(assemblerURL + outputMat.group(1));		
				} catch (IOException e) {
					System.err.println("Unable to download and write compiler program");
					e.printStackTrace();
				}
			}
		}
	}
	
	private static void downloadPrgm(String download) throws IOException {
		System.out.println("Downloading from " + download);
		URL url = new URL(download);
		ReadableByteChannel rbc = Channels.newChannel(url.openStream());
		FileOutputStream fos;
		try {
			fos = new FileOutputStream("out/vvestin.8xp");
		} catch (FileNotFoundException e) {
			System.out.println("Creating out/vvestin.8xp");
			File f = new File("out/vvestin.8xp");
			f.createNewFile();
			fos = new FileOutputStream("/out/vvestin.8xp");
		}
		fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
		fos.close();
	}
}