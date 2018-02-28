package studio.ui;

public class AboutHandler {
    private StudioPanel s;

    public AboutHandler(StudioPanel s) {
        this.s = s;
    }

    public void about() {
        s.about();
    }
}
