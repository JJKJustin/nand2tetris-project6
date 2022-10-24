import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class Assembler {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        boolean done = false;

        // prompt user for filename of assembly file
        do {
            System.out.print("Enter Assembly File Filename: ");

            String filename = in.nextLine();
            try {
                assembleAssemblyFile(filename);
                done = true;
            } catch (IllegalStateException|IllegalArgumentException e) {
                System.out.println(e.getMessage());
            }
        } while (!done);
        
        System.out.println("Finished assembling assembly file.");
    }

    // assembly in two passes
    private static void assembleAssemblyFile(String filename) {
        SymbolTable symbolTable = new SymbolTable();
        File file = new File(filename);
       
        performFirstPass(file, symbolTable);
        performSecondPass(file, filename, symbolTable);
    }

    // first pass processes all L commands and adds the labels to the symbol table
    private static void performFirstPass(File file, SymbolTable symbolTable) {
        Parser firstParser = new Parser(file);
        
        int currentAddress = 0;

        while (firstParser.hasMoreCommands()) {
            firstParser.advance();

            Parser.Command commandType = firstParser.commandType();

            if (commandType == null)
                throw new IllegalStateException("Syntax error at instruction " + (currentAddress + 1));

            switch (commandType) {
            case A_COMMAND:
            case C_COMMAND:
                currentAddress++;
                break;
            case L_COMMAND:
                String symbol = firstParser.symbol();
                
                if (Character.isDigit(symbol.charAt(0)))
                    throw new IllegalStateException("Symbol syntax error at instruction " + (currentAddress + 1));
                
                // add label to symbol table
                symbolTable.addEntry(symbol, currentAddress);
            }
        }

        try {
            firstParser.close();
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    // second pass processes A commands and C commands, and adds variable symbols
    // to symbol table
    private static void performSecondPass(File file, String filename, SymbolTable symbolTable) {
        Parser secondParser = new Parser(file);
        String filenameWithoutExtension = filename.substring(0, filename.lastIndexOf("."));

        int currentDataAddress = 16;
        BufferedWriter writer;

        try {
            writer = new BufferedWriter(new FileWriter(new File(filenameWithoutExtension + ".hack")));
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage());
        }

        String resultOfLine;

        while (secondParser.hasMoreCommands()) {
            secondParser.advance();

            switch (secondParser.commandType()) {
                case A_COMMAND:
                    String symbol = secondParser.symbol();
                    boolean isDecimal = Character.isDigit(symbol.charAt(0)); 
                    
                    // add symbol to table if it is not a symbol
                    if (!isDecimal && !symbolTable.contains(symbol))
                        symbolTable.addEntry(symbol, currentDataAddress++);
                    
                    // get the integer value of the symbol
                    int value = isDecimal ? Integer.parseInt(symbol) : symbolTable.getAddress(symbol);

                    resultOfLine = "0" + String.format("%15s", Integer.toBinaryString(value))
                            .replaceAll(" ", "0") + "\n";

                    try {
                        writer.append(resultOfLine);
                    } catch (IOException e) {
                        throw new IllegalStateException(e.getMessage());
                    }

                    break;
                case C_COMMAND:
                    // get binary of comp, dest, and jump
                    String comp = Code.comp(secondParser.comp());
                    String dest = Code.dest(secondParser.dest());
                    String jump = Code.jump(secondParser.jump());

                    resultOfLine = "111" + comp + dest + jump + "\n";

                    try {
                        writer.write(resultOfLine);
                    } catch (IOException e) {
                        throw new IllegalStateException(e.getMessage());
                    }

                    break;
                default:
            }
        }

        try {
            writer.close();
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage());
        }

        try {
            secondParser.close();
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage());
        }
    }
}
