package controller;

// What a page handler gives back to the web server (Member B, control):
// either a page to show (status, title, body HTML) or a redirect to another page.
public record PageResult(int status, String title, String body, String redirect) {

    // A normal page
    public static PageResult page(String title, String body) {
        return new PageResult(200, title, body, null);
    }

    // A page with an error status, such as 400, 403 or 404
    public static PageResult error(int status, String title, String body) {
        return new PageResult(status, title, body, null);
    }

    // Send the browser to another page (used after a form is saved)
    public static PageResult redirectTo(String location) {
        return new PageResult(303, null, null, location);
    }

    public boolean isRedirect() {
        return redirect != null;
    }
}
