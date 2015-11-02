package studio.ui;

public class AboutHandler {
    private Studio s;

    public AboutHandler(Studio s) {
        this.s = s;
    }

    public void about() {
        s.about();
    }
}
