import java.util.List;

public class UserInterface {

    private final ProgramControl programControl;

    public UserInterface(ProgramControl programControl) {
        this.programControl = programControl;
    }

//    public void run(String[] args) {
//        if (args.length == 0) {
//            displayFileList();
//        } else {
//            displayFileContents(args);
//        }
//    }

//    private void displayFileList() {
//        List<String> files = programControl.getFileList();
//        for (int i = 0; i < files.size(); i++) {
//            System.out.printf("%02d %s%n", i + 1, files.get(i));
//        }
//    }

//    private void displayFileContents(String[] args) {
//        int fileNumber;
//        try {
//            fileNumber = Integer.parseInt(args[0]);
//        } catch (NumberFormatException e) {
//            System.out.println("Error: '" + args[0] + "' is not a valid file number.");
//            return;
//        }
//
//        String keyFilePath = args.length > 1 ? args[1] : null;
//
//        try {
//            String contents = programControl.getFileContents(fileNumber, keyFilePath);
//            System.out.println(contents);
//        } catch (IllegalArgumentException e) {
//            System.out.println("Error: " + e.getMessage());
//        }
//    }
}