import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Scanner;

public class main {
	
	public static HashSet<String> varSet=new HashSet<String>();
	public static int plusCount=0; //to create labels with different name for add
	public static int outputCount=0; //to create labels with different name for output
	public static int powCount=0; //to create labels with different name for pow
	public static int lineCount=1; //to count the lines that has read for error giving
	
	/*
	 This program reads the language line by line. For each line it parse the syntax with recursive and non-recursive
	 method calls and print necessary a86 statements to output file. The parsing strategy is creating an operational
	 post-fix order parse tree with functions such as expr, term, factor, id_num instead of creating 
	 a real tree data structure.
	 */
	public static void main(String[] args) throws FileNotFoundException {
		Scanner reader = new Scanner(new File("./test.txt"));
		PrintStream ps = new PrintStream(new File("./out.asm"));
		prepare(ps); //this method call creates necessary variable to used in multiply and pow operations
		while(reader.hasNext()) { //this loop reads lines from input file
			// this calls line method and if there is a syntax error give an error message
			if(!line(reader.nextLine(),ps)) {
				giveError(ps);
				return; //exit program
			}
			lineCount++; 
		}
		
		
		
		ps.println("int 20h"); //exit to dos
	}
	public static void prepare(PrintStream ps) {//creates necessary variable to used in multiply and pow operations
		ps.println("eVar1 dw 0h"); //an extra variable to use in multiply
		ps.println("eVar2 dw 0h"); //an extra variable to use in multiply
		ps.println("eVarpl dw 0h"); //an extra variable to use in pow
		ps.println("eVarpr dw 0h"); //an extra variable to use in pow
		ps.println("eVarp2l dw 0h"); //an extra variable to use in pow
		ps.println("eVarp2r dw 0h"); //an extra variable to use in pow
		ps.println("eVarn dw 0h"); //an extra variable to use in pow
		ps.println("oVar dw 0h");// a variable for storing overflow bit in multiply
		ps.println("exproutvar dw 0h");// to output an expression
	}
	
	public static Boolean line(String s,PrintStream ps) { //this method decides whether the statement is an output or an assignment statement.
		if(s.length()==0) return true;
		s=purgeFromSpaces(s); //deletes the space characters from the beginning and the end of the string
	
		int equalSign=s.indexOf('=');
		
		if(equalSign<0) { //if the statement is an output statement
			//calls expression method then assing it to our extra variable then output that value
			if(!expr(s,ps)) return false;
			assign("exproutvar",ps);
			output("exproutvar",ps);
		}
		else { //if the statement is an assignment statement
			//call expression method for the left of the equal sign and assing method for the left of the equal sign
			if(!expr(s.substring(equalSign+1),ps)) return false;
			if(!assign(s.substring(0, equalSign),ps)) return false;
		}
		return true;
	}
	
	//this method decides whether the expression is combination of expr and term or just a term
	public static Boolean expr(String s,PrintStream ps) { 
		s=purgeFromSpaces(s); //deletes the space characters from the beginning and the end of the string

		int index=-1; //index of plus which is not in parenthesis
		int counter=0; //to check whether we are in a parenthesis or not
		char[] ch=s.toCharArray();
		for(int i=s.length()-1; i>-1; i--) { //this loop search for a plus sign which is not in a parenthesis
			if(ch[i]==')') counter++;
			else if(ch[i]=='(') counter--;
			else if(ch[i]=='+' && counter==0) index=i;
			if(counter<0) return false; //if there is an unclosed left parenthesis then it is a syntax error
		}
		if(counter!=0) return false; //if there is imbalance of parenthesis then it is a syntax error
		
		if(index<0) { //call term method there is no plus sign which is not in a parenthesis
			if(!term(s,ps)) return false;
		}
		else { //if there is at least one plus sign which is not in a parenthesis
			if(!expr(s.substring(index+1),ps)) return false; //expression method for the left of the plus sign 
			if(!term(s.substring(0, index),ps)) return false;//term method for the right of the plus sign 
			plus(ps); //call add method to print necessary assembly statements to realize addition
		}
		return true;
	}
	
	//this method decides whether the term is combination of term and factor or just a factor
	public static Boolean term(String s,PrintStream ps) {
		s=purgeFromSpaces(s); //deletes the space characters from the beginning and the end of the string
		
		int index=-1; //index of multiplier which is not in paranthesis
		int counter=0; //to check whether we are in a parenthesis or not
		char[] ch=s.toCharArray();
		for(int i=s.length()-1; i>-1; i--) { //this loop search for a multiply sign which is not in a parenthesis
			if(ch[i]==')') counter++;
			else if(ch[i]=='(') counter--;
			else if(ch[i]=='*' && counter==0) index=i;
			if(counter<0) return false; //if there is an unclosed left parenthesis then it is a syntax error
		}
		if(counter!=0) return false; //if there is imbalance of parenthesis then it is a syntax error
		
		if(index<0) { //call term method there is no multiply sign which is not in a parenthesis
			if(!factor(s,ps)) return false;
		}
		else { //if there is at least one multiply sign which is not in a parenthesis
			if(!term(s.substring(index+1),ps)) return false; //term method for the left of the multiplier 
			if(!factor(s.substring(0, index),ps)) return false; //factor method for the right of the multiplier 
			mult(ps); //call multiply method to print necessary assembly statements to realize multiplication
		}
		return true;
	}

	//this method decides whether the factor is an expression in parenthesis or a pow operation or a variable or number
	public static Boolean factor(String s,PrintStream ps) {
		s=purgeFromSpaces(s); //deletes the space characters from the beginning and the end of the string
		
		int leftParantehis=s.indexOf('(');
		int rightParantehis=s.indexOf(')');
		int lastRightP=s.lastIndexOf(')');

		if(s.contains("pow") && leftParantehis!=0) { //if the factor is an pow operation which not in a parenthesis
			int indexOfCama=s.indexOf(',');
			if(lastRightP!=s.length()-1 || leftParantehis==-1 || indexOfCama==-1 || s.indexOf("pow")!=0) return false;
			if(!pow(s.substring(leftParantehis+1, indexOfCama), s.substring(indexOfCama+1, s.lastIndexOf(')')),ps)) return false;
		}
		else {
			if(leftParantehis<0) {//if the factor is a single variable or number
				if(!id_num(s,ps)) return false;
			}
			else { //if the factor is an expression in parenthesis
				if(lastRightP!=s.length()-1) return false;
				else if(!expr(s.substring(1, s.length()-1),ps)) return false;
			}
		}
		return true;
	}
	
	public static Boolean id_num(String s,PrintStream ps) { //this method is for variables and numbers
		if(s.length()==0) return false; //if the string is empty
		int length=s.length();
		for(int i=0; i<length; i++) { //check whether the variable or number is valid, otherwise give syntax error
			char current=s.charAt(i);
			if((current<'0' || current>'9') && (current<'a' || current>'z') && (current<'A' || current>'Z'))
				return false;
		}
		
		if(!isVariable(s)) { //if the string is a number
			int lengthToAdd=8-s.length();
			for(int i=0; i<lengthToAdd; i++) { //completes the number to length of 8
				s="0"+s;
			}
			ps.println("push 0"+s.substring(4)+"h"); //push the right half of the number to the stack
			ps.println("push 0"+s.substring(0,4)+"h"); //push the left half of the number to the stack
		}	
		else { //if the string is a variable
			s=stringTransFormer(s);
			if(!varSet.contains(s)) { //check whether the variable is created otherwise create a new variable
				createVar(s,ps);
			}
			ps.println("push "+s+"right"); //push the right half of the variable to the stack
			ps.println("push "+s+"left");  //push the left half of the variable to the stack
		}
		return true;
	}
	
	public static Boolean isVariable(String s) { //check whether the string is variable or number
		int firstChar=s.charAt(0);
		return(firstChar<'0' || firstChar>'9');
	}
	
	public static Boolean assign(String s,PrintStream ps) { //the assignment operation
		s=purgeFromSpaces(s); //deletes the space characters from the beginning and the end of the string
		int length=s.length();
		for(int i=0; i<length; i++) { //check if the string is valid(is a variable)
			char current=s.charAt(i);
			if((current<'0' || current>'9') && (current<'a' || current>'z') && (current<'A' || current>'Z'))
				return false;
		}
		
		s=stringTransFormer(s);
		if(!varSet.contains(s)) { //check whether the variable is created otherwise create a new variable
			createVar(s,ps);
		}	
		
			//assignment expressions in assembly:
			ps.println("pop cx");
			ps.println("mov "+s+"left,cx");
			ps.println("pop cx");
			ps.println("mov "+s+"right,cx");
			return true;
		
	}
	
	//deletes the space characters from the beginning and the end of the string:
	public static String purgeFromSpaces(String s) {
		int length=s.length();
		int indexleft=0;
		while(length>indexleft) { //purge left
			if(s.charAt(indexleft)!=' ') break;
			indexleft++;
		}
		int indexRight=s.length()-1;
		while(-1<indexRight) { //purge right
			if(s.charAt(indexRight)!=' ') break;
			indexRight--;
		}
		return s.substring(indexleft, indexRight+1); //purge
	}
	
	public static void output(String s,PrintStream ps) { //print output assembly codes into the output file
		s=stringTransFormer(s);
		if(!varSet.contains(s)) { //check whether the variable is created otherwise create a new variable
			createVar(s,ps);
		}
		//output the left half of the variable:
		ps.println("mov bx,"+s+"left");
		ps.println("mov cx,4h");
		ps.println("mov dx,ax");
		ps.println("mov ah,2h");
		ps.println("loop"+outputCount+":");
		ps.println("mov dx,0fh");
		ps.println("rol bx,4h");
		ps.println("and dx,bx");
		ps.println("cmp dl,0ah");
		ps.println("jae char"+outputCount);
		ps.println("add dl,'0'");
		ps.println("jmp output"+outputCount);
		ps.println("char"+outputCount+":");
		ps.println("add dl,'A'");
		ps.println("sub dl,0ah");
		ps.println("output"+outputCount+":");
		ps.println("int 21h");
		ps.println("dec cx");
		ps.println("jnz loop"+outputCount);
		outputCount++;

		//output the right half of the variable:
		ps.println("mov bx,"+s+"right");
		ps.println("mov cx,4h");
		ps.println("mov dx,ax");
		ps.println("mov ah,2h");
		ps.println("loop"+outputCount+":");
		ps.println("mov dx,0fh");
		ps.println("rol bx,4h");
		ps.println("and dx,bx");
		ps.println("cmp dl,0ah");
		ps.println("jae char"+outputCount);
		ps.println("add dl,'0'");
		ps.println("jmp output"+outputCount);
		ps.println("char"+outputCount+":");
		ps.println("add dl,'A'");
		ps.println("sub dl,0ah");
		ps.println("output"+outputCount+":");
		ps.println("int 21h");
		ps.println("dec cx");
		ps.println("jnz loop"+outputCount);
		outputCount++;
		
		//print a new line:
		ps.println("MOV dl, 10");
		ps.println("MOV ah, 02h");
		ps.println("INT 21h");
		ps.println("MOV dl, 13");
		ps.println("MOV ah, 02h");
		ps.println("INT 21h");

		

	}
	public static void createVar(String s,PrintStream ps) { //creates new variable
		varSet.add(s); //insert the name of the variable to the variable set
		ps.println(s+"left dw 0h");
		ps.println(s+"right dw 0h");
	}
	public static void plus(PrintStream ps) { //prints assembly codes of addition operation
		ps.println("pop ax");
		ps.println("pop bx");
		ps.println("pop cx");
		ps.println("pop dx");
		ps.println("add bx,dx");
		ps.println("jnc plus"+plusCount); //if there is no overflow pass the next line
		ps.println("add ax,1h"); //if there is overflow add 1 to left half of the number
		ps.println("plus"+plusCount+":");
		ps.println("add ax,cx");
		ps.println("push bx");
		ps.println("push ax");
		plusCount++;

		
	}
	public static void mult(PrintStream ps) { //prints assembly codes of multiplication operation
		ps.println("mov eVar1,0h");
		ps.println("mov eVar2,0h");
		ps.println("pop bx");
		ps.println("pop ax");
		ps.println("pop cx");
		ps.println("pop dx");
		ps.println("add eVar1,ax");
		ps.println("add eVar2,dx");
		ps.println("mul dx");	
		ps.println("mov oVar,dx");
		ps.println("push ax");
		ps.println("mov ax,eVar1");
		ps.println("mul cx");
		ps.println("mov eVar1,ax");
		ps.println("mov ax,bx");
		ps.println("mul eVar2");
		ps.println("add ax,eVar1");
		ps.println("add ax,oVar");
		ps.println("push ax");
		
	}
	
	public static Boolean pow(String left, String right,PrintStream ps) { //prints assembly codes for power operation
		if(!expr(right,ps)) return false; //calculate the exponent part of power function
		if(!expr(left,ps)) return false; //calculate the base part of power function
		
		ps.println("pop ax");
		ps.println("pop bx");
		ps.println("pop cx");
		ps.println("pop dx");
		ps.println("mov eVarp2l,ax");
		ps.println("mov eVarp2r,bx");
		ps.println("mov eVarn,dx");
		ps.println("mov eVarpl,0h");
		ps.println("mov eVarpr,1h");
		ps.println("powloop"+powCount+":");
		ps.println("mov ax,0h");
		ps.println("add ax,eVarn");
		ps.println("and ax,1h");
		ps.println("cmp ax,0h");
		ps.println("jz afterIf"+powCount);
		ps.println("mov ax,0h");
		ps.println("mov bx,0h");
		ps.println("add ax,eVarp2l");
		ps.println("add bx,eVarp2r");
		ps.println("mov cx,eVarpl");
		ps.println("mov dx,eVarpr");
		ps.println("push bx");
		ps.println("push ax");
		ps.println("push dx");
		ps.println("push cx");
		mult(ps);
		
		//these are to handle conditional jump line limit(128)
		ps.println("jmp afterjmp"+powCount);
		ps.println("jumplabel"+powCount+":");
		ps.println("jmp powloop"+powCount);
		ps.println("afterjmp"+powCount+":");

		ps.println("pop ax");
		ps.println("pop bx");
		ps.println("mov eVarpl,ax");
		ps.println("mov eVarpr,bx");
		ps.println("afterIf"+powCount+":");
		ps.println("mov ax,0h");
		ps.println("mov bx,0h");
		ps.println("mov cx,0h");
		ps.println("mov dx,0h");
		ps.println("add ax,eVarp2l");
		ps.println("add bx,eVarp2r");
		ps.println("add cx,eVarp2l");
		ps.println("add dx,eVarp2r");
		ps.println("push bx");
		ps.println("push ax");
		ps.println("push dx");
		ps.println("push cx");
		mult(ps);
		ps.println("pop ax");
		ps.println("pop bx");
		ps.println("mov eVarp2l,ax");
		ps.println("mov eVarp2r,bx");
		ps.println("mov ax,0h");
		ps.println("add ax,eVarn");
		ps.println("rcr ax,1h");
		ps.println("cmp ax,0h");
		ps.println("mov eVarn,ax");
		ps.println("jnz jumplabel"+powCount);
		
		ps.println("push eVarpr"); //push right half of the result
		ps.println("push eVarpl"); //push left half of the result


		powCount++;
		return true;
	}
	
	public static String stringTransFormer(String s) { //transform string when it includes uppercase characters/character
		int length=s.length();
		String newS="";
		for(int i=0; i<length; i++) {
			char current=s.charAt(i);
			newS+=current;
			if(current>='A' && current<='Z') newS+='_'; //if the current char is uppercase then add an underscore after it
		}
		return newS;
	}
	
	public static void giveError(PrintStream ps) { //gives syntax error
		ps.println("There is a syntax error on the line "+lineCount);

	}
	

}