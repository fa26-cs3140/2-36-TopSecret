public class ProgramControl {

    private final FileHandler fileHandler;

    public ProgramControl() {
        fileHandler = new FileHandler();
    }

    public String getFileContents(String fileName) {
        return fileHandler.readFile(fileName);
    }
}