import java.util.List;

public class UserInterface {

    private final ProgramControl programControl;

    public UserInterface(ProgramControl programControl) {
        this.programControl = programControl;
    }

    public void run(String[] array) {
        if (array.length == 0) {
            displayFileList();
        } else {
            displayFileContents(array);
        }
    }

    private void displayFileList() {
        List<String> files = programControl.getAvailableFiles();
        for (int i = 0; i < files.size(); i++) {
            String number = (i + 1) < 10 ? "0" + (i + 1) : "" + (i + 1);
            System.out.println(number + " " + files.get(i));
        }
    }

    private void displayFileContents(String[] array) {
        int fileNumber;
        try {
            fileNumber = Integer.parseInt(array[0]);
        } catch (NumberFormatException e) {
            System.out.println("Error: '" + array[0] + "' is not a valid file number.");
            return;
        }

        List<String> files = programControl.getAvailableFiles();
        if (fileNumber < 1 || fileNumber > files.size()) {
            System.out.println("Error: No file found matching number " + fileNumber + ".");
            return;
        }
        String fileName = files.get(fileNumber - 1);

        String contents;
        if (array.length > 1) {
            contents = programControl.decipherFile(fileName, array[1]);
        } else {
            contents = programControl.decipherFile(fileName);
        }

        if (contents == null) {
            System.out.println("Error: Unable to read or decipher '" + fileName + "'.");
            return;
        }

        System.out.println(contents);
    }
}