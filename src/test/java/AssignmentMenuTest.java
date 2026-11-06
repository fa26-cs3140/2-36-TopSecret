import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AssignmentMenuTest {

    private ModelDatabase db;
    private AuditLog audit;
    private ByteArrayOutputStream output;

    @BeforeEach
    void setUp() {
        db = mock(ModelDatabase.class);
        audit = mock(AuditLog.class);
        output = new ByteArrayOutputStream();
    }

    private void run(String typed) {
        new AssignmentMenu(db, new Scanner(typed), new PrintStream(output), audit).run("smayan");
    }

    @Test
    void noInputExits() {
        run("");
        assertTrue(output.toString().contains("Choose an option"));
    }

    @Test
    void optionSixExits() {
        run("6\n");
        assertEquals(1, output.toString().split("Choose an option", -1).length - 1);
    }

    @Test
    void badChoiceShowsError() {
        run("9\n6\n");
        assertTrue(output.toString().contains("Invalid choice"));
    }

    @Test
    void listShowsFacilityAgentsAndBriefCount() throws Exception {
        when(db.getMissions()).thenReturn(List.of(
                new Mission(1, "Nightfall", "b", "2026-01-01", 1, List.of(2, 77), List.of(5))));
        when(db.getAgents()).thenReturn(List.of(new Agent(2, "Ghost", null, null, "", 1)));
        when(db.getFacilities()).thenReturn(List.of(new Facility(1, "Alpha Station", "ALP")));

        run("1\n6\n");
        assertTrue(output.toString().contains("Mission #1: Nightfall (2026-01-01)"));
        assertTrue(output.toString().contains("Facility: Alpha Station"));
        assertTrue(output.toString().contains("Agents: Ghost, #77"));
        assertTrue(output.toString().contains("Briefs: 1"));
    }

    @Test
    void listShowsNoneForEmptyMission() throws Exception {
        when(db.getMissions()).thenReturn(List.of(new Mission(1, "Old", "b", "2019-01-01")));
        run("1\n6\n");
        assertTrue(output.toString().contains("Facility: none"));
        assertTrue(output.toString().contains("Agents: none"));
    }

    @Test
    void listWithNoMissions() throws Exception {
        when(db.getMissions()).thenReturn(List.of());
        run("1\n6\n");
        assertTrue(output.toString().contains("No missions."));
    }

    @Test
    void createMissionStoresAndLogs() throws Exception {
        when(db.getFacilities()).thenReturn(List.of(new Facility(1, "Alpha Station", "ALP")));
        when(db.addMission("Nightfall", "Recon", "2026-01-01", 1)).thenReturn(5);

        run("2\nNightfall\n2026-01-01\nRecon\n1\n6\n");
        assertTrue(output.toString().contains("1: Alpha Station (ALP)"));
        assertTrue(output.toString().contains("Mission #5 created."));
        verify(audit).record("smayan", AuditLog.CREATE, "Mission #5: Nightfall");
    }

    @Test
    void createMissionRejectsBadFacilityNumber() throws Exception {
        run("2\nNightfall\n2026-01-01\nRecon\nabc\n6\n");
        assertTrue(output.toString().contains("'abc' is not a number"));
        verify(db, never()).addMission(any(), any(), any(), anyInt());
    }

    @Test
    void createMissionStopsWhenInputEnds() throws Exception {
        run("2\nNightfall\n");
        verify(db, never()).addMission(any(), any(), any(), anyInt());
    }

    @Test
    void createMissionShowsErrors() throws Exception {
        when(db.addMission(any(), any(), any(), anyInt()))
                .thenThrow(new IllegalArgumentException("No such facility: 9"));
        run("2\nX\n2026-01-01\nb\n9\n6\n");
        assertTrue(output.toString().contains("Error: No such facility: 9"));
        verifyNoInteractions(audit);
    }

    @Test
    void assignAgentConfirmsAndLogs() throws Exception {
        when(db.getAgents()).thenReturn(List.of(new Agent(2, "Ghost", null, null, "", 1)));
        when(db.assignAgent(1, 2)).thenReturn(true);

        run("3\n1\n2\n6\n");
        assertTrue(output.toString().contains("2: Ghost (facility 1)"));
        assertTrue(output.toString().contains("Agent #2 assigned to mission #1."));
        verify(audit).record("smayan", AuditLog.CREATE, "Assignment: agent #2 to mission #1");
    }

    @Test
    void assignAgentAlreadyAssignedIsNotLogged() throws Exception {
        when(db.assignAgent(1, 2)).thenReturn(false);
        run("3\n1\n2\n6\n");
        assertTrue(output.toString().contains("already assigned"));
        verifyNoInteractions(audit);
    }

    @Test
    void assignAgentRejectsNonNumbers() throws Exception {
        run("3\nx\n6\n");
        assertTrue(output.toString().contains("'x' is not a number"));
        run("3\n1\ny\n6\n");
        assertTrue(output.toString().contains("'y' is not a number"));
        verify(db, never()).assignAgent(anyInt(), anyInt());
    }

    @Test
    void assignAgentShowsUnknownIdAndSqlErrors() throws Exception {
        when(db.assignAgent(1, 9)).thenThrow(new IllegalArgumentException("No such agent: 9"));
        when(db.assignAgent(1, 2)).thenThrow(new SQLException("boom"));
        run("3\n1\n9\n3\n1\n2\n6\n");
        assertTrue(output.toString().contains("Error: No such agent: 9"));
        assertTrue(output.toString().contains("Error: boom"));
    }

    @Test
    void removeAgentConfirmsAndLogsDelete() throws Exception {
        when(db.unassignAgent(1, 2)).thenReturn(true);
        run("4\n1\n2\n6\n");
        assertTrue(output.toString().contains("Agent #2 removed from mission #1."));
        verify(audit).record(eq("smayan"), eq("DELETE"), contains("agent #2 from mission #1"));
    }

    @Test
    void removeAgentNotAssigned() throws Exception {
        when(db.unassignAgent(1, 2)).thenReturn(false);
        run("4\n1\n2\n6\n");
        assertTrue(output.toString().contains("is not assigned to mission #1"));
        verifyNoInteractions(audit);
    }

    @Test
    void removeAgentRejectsNonNumbers() throws Exception {
        run("4\nx\n6\n");
        run("4\n1\ny\n6\n");
        assertTrue(output.toString().contains("not a number"));
        verify(db, never()).unassignAgent(anyInt(), anyInt());
    }

    @Test
    void attachBriefConfirmsAndLogs() throws Exception {
        when(db.getBriefs()).thenReturn(List.of(new Brief(3, "Project Bluebird", "2020-01-01", "t")));
        when(db.attachBrief(1, 3)).thenReturn(true);

        run("5\n1\n3\n6\n");
        assertTrue(output.toString().contains("3: Project Bluebird"));
        assertTrue(output.toString().contains("Brief #3 attached to mission #1."));
        verify(audit).record("smayan", AuditLog.CREATE, "Brief #3 attached to mission #1");
    }

    @Test
    void attachBriefAlreadyAttached() throws Exception {
        when(db.attachBrief(1, 3)).thenReturn(false);
        run("5\n1\n3\n6\n");
        assertTrue(output.toString().contains("already attached"));
        verifyNoInteractions(audit);
    }

    @Test
    void attachBriefShowsOwnershipError() throws Exception {
        when(db.attachBrief(2, 3))
                .thenThrow(new IllegalArgumentException("Brief #3 already belongs to mission #1"));
        run("5\n2\n3\n6\n");
        assertTrue(output.toString().contains("already belongs to mission #1"));
    }

    @Test
    void attachBriefRejectsNonNumbers() throws Exception {
        run("5\nx\n6\n");
        run("5\n1\ny\n6\n");
        assertTrue(output.toString().contains("not a number"));
        verify(db, never()).attachBrief(anyInt(), anyInt());
    }

    @Test
    void worksWithoutAnAuditLog() throws Exception {
        when(db.assignAgent(1, 2)).thenReturn(true);
        new AssignmentMenu(db, new Scanner("3\n1\n2\n6\n"), new PrintStream(output), null).run("smayan");
        assertTrue(output.toString().contains("assigned to mission #1"));
    }
}