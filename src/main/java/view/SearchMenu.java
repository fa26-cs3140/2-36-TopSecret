package view;

import model.AuditLog;
import model.RecordSearch;
import model.SearchResult;

import java.io.PrintStream;
import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

// Terminal search across every text field of every record type (Member D).
// Asks for a word or phrase and lists each matching record. Searches are audited.
public class SearchMenu {

    private final RecordSearch search;
    private final AuditLog audit;
    private final String username;
    private final Scanner in;
    private final PrintStream out;

    public SearchMenu(RecordSearch search, AuditLog audit, String username,
                      Scanner in, PrintStream out) {
        this.search = search;
        this.audit = audit;
        this.username = username;
        this.in = in;
        this.out = out;
    }

    // One search: ask for the term, then print the matches
    public void run() {
        out.print("Search all records for: ");
        String term = in.hasNextLine() ? in.nextLine().trim() : "";
        if (term.isEmpty()) {
            out.println("Nothing to search for.");
            return;
        }
        List<SearchResult> results;
        try {
            results = search.search(term);
        } catch (SQLException e) {
            out.println("Database error: " + e.getMessage());
            return;
        }
        audit.record(username, AuditLog.READ, "Search: " + term);
        if (results.isEmpty()) {
            out.println("No matches.");
            return;
        }
        out.println(results.size() + (results.size() == 1 ? " match:" : " matches:"));
        for (SearchResult result : results) {
            out.println("  " + result);
        }
    }
}
