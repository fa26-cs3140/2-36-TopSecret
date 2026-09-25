public class TopSecret {
    public static void main(String[] args) {
        ProgramControl programControl = new ProgramControl();
        UserInterface ui = new UserInterface(programControl);
        ui.run(args);
    }
}