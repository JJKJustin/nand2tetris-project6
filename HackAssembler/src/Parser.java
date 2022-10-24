import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class Parser {
    private final BufferedReader reader;
    private String currentLine;

    private Command commandType;
    private String symbol;
    private String dest;
    private String comp;
    private String jump;
    private boolean commandIsValid;

    public Parser(File file) {
        if (file == null)
            throw new IllegalArgumentException("File should not be null!");

        try {
            reader = new BufferedReader(new FileReader(file));
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("File not found or is directory!");
        }

        advanceToNextCommand();
    }

    public boolean hasMoreCommands() {
        return currentLine != null && currentLine.length() > 0;
    }

    // advance to next command and parse it
    public void advance() {
        commandIsValid = false;
        char firstChar = currentLine.charAt(0);

        switch (firstChar) {
        case '@':   // A instruction
            if (currentLine.length() < 2)
                return;    

            commandType = Command.A_COMMAND;
            symbol = currentLine.substring(1, currentLine.length());

            // check if symbol or decimal
            if (!isValidSymbol(symbol) && !isValidDecimal(symbol))
                return;
            
            dest = null;
            comp = null;
            jump = null;

            break;
        case '(':   // L instruction
            if (currentLine.length() < 3)
                return;

            commandType = Command.L_COMMAND;
            symbol = currentLine.substring(1, currentLine.length() - 1);

            // check if symbol for label
            if (!isValidSymbol(symbol))
                return;
            
            dest = null;
            comp = null;
            jump = null;

            break;
        default:    // C instruction
            commandType = Command.C_COMMAND;
            String[] semiColonSplit = currentLine.split(";");

            if (semiColonSplit.length > 2)
                return;

            String[] equalsSignSplit = semiColonSplit[0].split("=");
            
            if (equalsSignSplit.length > 2)
                return;
            
            dest = equalsSignSplit.length == 2 ? equalsSignSplit[0] : null;
            comp = equalsSignSplit.length == 2 ? equalsSignSplit[1] : equalsSignSplit[0];
            jump = semiColonSplit.length == 2 ? semiColonSplit[1] : null;
        }
    
        commandIsValid = true;
        advanceToNextCommand();
    }

    public Command commandType() {
        return commandIsValid ? commandType : null;
    }

    public String symbol() {
        return commandIsValid ? symbol : null;
    }

    public String dest() {
        return commandIsValid ? dest : null;
    }

    public String comp() {
        return commandIsValid ? comp : null;
    }

    public String jump() {
        return commandIsValid ? jump : null;
    }

    public void close() throws IOException {
        reader.close();
    }

    // returns true if it consists of digits, letters, underscores, periods
    // dollar signs, and colons
    private boolean isValidSymbol(String symbol) {
        if (symbol.length() == 0)
            return false;
        
        char firstChar = symbol.charAt(0);

        if (Character.isDigit(firstChar) || !isValidCharacter(firstChar))
            return false;

        for (int i = 1; i < symbol.length(); i++) {
            char currentChar = symbol.charAt(i);

            if (!isValidCharacter(currentChar))
                return false;
        }

        return true;
    }

    // returns true if all characters are digits
    private boolean isValidDecimal(String decimal) {
        if (decimal.length() == 0)
            return false;
        
        for (int i = 0; i < decimal.length(); i++) {
            char currentChar = decimal.charAt(i);

            if (!Character.isDigit(currentChar))
                return false;
        }

        return true;
    }

    // returns true if char is letter, digit, underscore, period, dollar sign
    // or colon
    private boolean isValidCharacter(char character) {
        return Character.isLetterOrDigit(character) || character == '_' || character == '.' || 
                character == '$' || character == ':';
    }

    // finds next nonempty line that has a command
    private void advanceToNextCommand() {
        boolean done = false;

        // repeatedly read lines until it finds a nonempty line with a command
        do {
            try {
                currentLine = reader.readLine();
            } catch (IOException e) {
                throw new IllegalStateException(e.getMessage());
            }

            if (currentLine != null) {
                int commentIndex = currentLine.indexOf("//");

                // remove comments
                if (commentIndex >= 0)
                    currentLine = currentLine.substring(0, commentIndex);
                
                // remove white space
                currentLine = currentLine.replaceAll("\\s+", "");

                // check if it is nonempty
                if (currentLine.length() > 0)
                    done = true;
            } else {
                done = true;
            }
        } while (!done);
    }

    public enum Command {
        A_COMMAND,
        C_COMMAND,
        L_COMMAND
    }
}
